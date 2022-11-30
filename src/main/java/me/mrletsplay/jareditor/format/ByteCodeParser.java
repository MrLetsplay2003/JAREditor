package me.mrletsplay.jareditor.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.mrletsplay.jareditor.format.entity.ParserAttribute;
import me.mrletsplay.jareditor.format.entity.ParserField;
import me.mrletsplay.mrcore.misc.Result;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;

public class ByteCodeParser {

	public static Result<ClassFile, ParseError> parse(ClassFile original, String str) {
		// Copy old ClassFile
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		ClassFile cf = null;
		try {
			original.write(bOut);
			cf = new ClassFile(new ByteArrayInputStream(bOut.toByteArray()));
		} catch (IOException ignored) {}

		ParseString parse = new ParseString(str);

		Map<String, String> properties = new HashMap<>();
		while(true) {
			var pair = readPair(parse);
			if(!pair.isErr()) {
				var kv = pair.value();
				properties.put(kv.getKey(), kv.getValue());
				continue;
			}

			String token = parse.stripLeading().nextToken();
			switch(token) {
				case "field":
				{
					var field = readField(parse);
					if(field.isErr()) return field.up();
					break;
				}
				default:
				{
					return Result.err(new ParseError("Unexpected token '" + token + "'", parse.index));
				}
			}

			break;
		}

		return Result.of(cf);
	}

	private static Result<Map.Entry<String, String>, ParseError> readPair(ParseString str) {
		str.mark().stripLeading();
		String pair = str.nextToken();
		if(!pair.contains("=")) {
			str.reset();
			return Result.err(new ParseError("Pair needs to contain '='", str.index));
		}
		String[] kv = pair.split("=", 2);
		return Result.of(new AbstractMap.SimpleEntry<>(kv[0], kv[1]));
	}

	private static Result<ParserField, ParseError> readField(ParseString str) {
		str.mark().stripLeading();
		String name = str.nextToken();
		var block = readBlock(str);
		if(block.isErr()) return block.up();
		ParseString blk = new ParseString(block.value());
		System.out.println(blk.str);

		Map<String, String> properties = new HashMap<>();
		List<ParserAttribute> attributes = new ArrayList<>();
		while(true) {
			var pair = readPair(blk);
			if(!pair.isErr()) {
				var kv = pair.value();
				properties.put(kv.getKey(), kv.getValue());
				continue;
			}

			String token = blk.stripLeading().nextToken();
			System.out.println(token);
			switch(token) {
				case "attribute":
				{
					var attr = readAttribute(str);
					if(attr.isErr()) return attr.up();
					attributes.add(attr.value());
					break;
				}
				default:
				{
					return Result.err(new ParseError("Unexpected token '" + token + "'", str.index + blk.index));
				}
			}

			break;
		}

		return Result.of(new ParserField(name, properties, attributes));
	}

	private static Result<ParserAttribute, ParseError> readAttribute(ParseString str) {
		str.mark().stripLeading();
		String name = str.nextToken();
		var block = readBlock(str);
		if(block.isErr()) return block.up();
		ParseString blk = new ParseString(block.value());
		System.out.println(blk.str);

		Map<String, String> properties = new HashMap<>();
		List<ParserAttribute> attributes = new ArrayList<>();
		String info = null;
		while(true) {
			var pair = readPair(blk);
			if(!pair.isErr()) {
				var kv = pair.value();
				properties.put(kv.getKey(), kv.getValue());
				continue;
			}

			String token = blk.stripLeading().nextToken();
			System.out.println(token);
			switch(token) {
				case "attribute":
				{
					var attr = readAttribute(str);
					if(attr.isErr()) return attr.up();
					attributes.add(attr.value());
					break;
				}
				case "info":
				{
					if(info != null) return Result.err(new ParseError("Duplicate info block", str.index));
					var infoR = readBlock(str);
					if(infoR.isErr()) return infoR.up();
					info = infoR.value();
					break;
				}
				default:
				{
					return Result.err(new ParseError("Unexpected token '" + token + "'", str.index + blk.index));
				}
			}

			break;
		}

		return Result.of(new ParserAttribute(name, info, attributes));
	}

	private static Result<String, ParseError> readBlock(ParseString str) {
		str.mark().stripLeading();
		if(str.get() != '{') {
			str.reset();
			return Result.err(new ParseError("'{' expected", str.index));
		}

		int i = 0;
		int count = 1;
		while(count > 0 && ++i < str.remaining()) {
			switch(str.peek(i)) {
				case '{':
					count++;
					break;
				case '}':
					count--;
					break;
			}
		}

		if(i == str.remaining()) {
			str.reset();
			return Result.err(new ParseError("'}' expected", str.index + i));
		}

		String content = str.next(i).substring(1);
		str.advance();
		return Result.of(content);
	}

	private static class ParseString {

		private String str;
		private int index;
		private int mark = -1;

		public ParseString(String str) {
			this.str = str;
		}

		public ParseString mark() {
			this.mark = index;
			return this;
		}

		public ParseString reset() {
			if(mark == -1) throw new IllegalStateException("Mark not set");
			this.index = mark;
			this.mark = -1;
			return this;
		}

		public char get() {
			if(index >= str.length()) return 0;
			return str.charAt(index);
		}

		public char advance() {
			index++;
			if(end()) return 0;
			return str.charAt(index);
		}

		public char peek(int count) {
			if(index + count >= str.length()) throw new IllegalArgumentException("Requested count exceeds string length");
			return str.charAt(index + count);
		}

		public String next(int count) {
			if(index + count >= str.length()) throw new IllegalArgumentException("Requested count exceeds string length");
			String string = str.substring(index, index + count);
			index += count;
			return string;
		}

		public boolean end() {
			return index >= str.length();
		}

		public ParseString stripLeading() {
			while(!end() && Character.isWhitespace(get())) advance();
			return this;
		}

		public int remaining() {
			return str.length() - index;
		}

		public String nextToken() {
			if(end()) throw new IllegalStateException("Reached end of input");
			int i = 0;
			while(i < remaining() && !Character.isWhitespace(peek(i))) i++;
			return next(i);
		}

	}

}
