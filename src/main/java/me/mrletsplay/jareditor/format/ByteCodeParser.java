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

			parse.stripLeading();
			if(parse.end()) break;
			String token = parse.nextToken();
			if(token == null) return Result.err(new ParseError("Unexpected end of input", parse.index));
			switch(token) {
				case "field":
				{
					var field = readField(parse);
					if(field.isErr()) return field.up();
					continue;
				}
				default:
				{
					return Result.err(new ParseError("Unexpected token '" + token + "'", parse.index));
				}
			}
		}

		return Result.of(cf);
	}

	private static Result<Map.Entry<String, String>, ParseError> readPair(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		String pair = str.nextToken();
		if(pair == null) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", str.index));
		}

		if(!pair.contains("=")) {
			str.reset(m);
			return Result.err(new ParseError("Pair needs to contain '='", str.index));
		}

		String[] kv = pair.split("=", 2);
		return Result.of(new AbstractMap.SimpleEntry<>(kv[0], kv[1]));
	}

	private static Result<ParserField, ParseError> readField(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		String name = str.nextToken();
		if(name == null) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", str.index));
		}

		var block = readBlock(str);
		if(block.isErr()) {
			str.reset(m);
			return block.up();
		}

		System.out.println(block.value().trim());

		ParseString blk = new ParseString(block.value());

		Map<String, String> properties = new HashMap<>();
		List<ParserAttribute> attributes = new ArrayList<>();
		while(true) {
			var pair = readPair(blk);
			if(!pair.isErr()) {
				var kv = pair.value();
				properties.put(kv.getKey(), kv.getValue());
				continue;
			}

			blk.stripLeading();
			if(blk.end()) break;
			String token = blk.nextToken();
			if(token == null) {
				str.reset(m);
				return Result.err(new ParseError("Unexpected end of input", str.index + blk.index));
			}

			switch(token) {
				case "attribute":
				{
					var attr = readAttribute(blk);
					if(attr.isErr()) {
						str.reset(m);
						return attr.up();
					}

					attributes.add(attr.value());
					break;
				}
				default:
				{
					str.reset(m);
					return Result.err(new ParseError("Unexpected token '" + token + "'", str.index + blk.index));
				}
			}

			break;
		}

		return Result.of(new ParserField(name, properties, attributes));
	}

	private static Result<ParserAttribute, ParseError> readAttribute(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		String name = str.nextToken();
		if(name == null) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", str.index));
		}

		var block = readBlock(str);
		if(block.isErr()) {
			str.reset(m);
			return block.up();
		}

		ParseString blk = new ParseString(block.value());

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

			blk.stripLeading();
			if(blk.end()) break;
			String token = blk.nextToken();
			if(token == null) {
				str.reset(m);
				return Result.err(new ParseError("Unexpected end of input", str.index + blk.index));
			}

			switch(token) {
				case "attribute":
				{
					var attr = readAttribute(blk);
					if(attr.isErr()) {
						str.reset(m);
						return attr.up();
					}

					attributes.add(attr.value());
					break;
				}
				case "info":
				{
					if(info != null) return Result.err(new ParseError("Duplicate info block", str.index));
					var infoR = readBlock(blk);
					if(infoR.isErr()) {
						str.reset(m);
						return infoR.up();
					}

					info = infoR.value();
					break;
				}
				default:
				{
					str.reset(m);
					return Result.err(new ParseError("Unexpected token '" + token + "'", str.index + blk.index));
				}
			}

			break;
		}

		return Result.of(new ParserAttribute(name, info, attributes));
	}

	private static Result<String, ParseError> readBlock(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		if(str.get() != '{') {
			str.reset(m);
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
			str.reset(m);
			return Result.err(new ParseError("'}' expected", str.index + i));
		}

		String content = str.next(i).substring(1);
		str.advance();
		return Result.of(content);
	}

	private static class ParseString {

		private String str;
		private int index;

		public ParseString(String str) {
			this.str = str;
		}

		public int mark() {
			return index;
		}

		public ParseString reset(int mark) {
			this.index = mark;
			return this;
		}

		public char get() {
			if(index >= str.length()) throw new IllegalArgumentException("Read beyond end of string");
			return str.charAt(index);
		}

		public ParseString advance() {
			if(end()) throw new IllegalArgumentException("Read beyond end of string");
			index++;
			return this;
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
			if(end()) return null;
			int i = 0;
			while(i < remaining() && !Character.isWhitespace(peek(i))) i++;
			return next(i);
		}

	}

}
