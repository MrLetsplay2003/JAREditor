package me.mrletsplay.jareditor.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

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

			break;
		}

		return Result.of(cf);
	}

	private static Result<Map.Entry<String, String>, ParseError> readPair(ParseString str) {
		str.mark().stripLeading();
		int i = 0;
		while(i < str.remaining() && !Character.isWhitespace(str.peek(i))) i++;
		String pair = str.next(i);
		if(!pair.contains("=")) {
			str.reset();
			return Result.err(new ParseError("Pair needs to contain '='", str.index));
		}
		String[] kv = pair.split("=", 2);
		return Result.of(new AbstractMap.SimpleEntry<>(kv[0], kv[1]));
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

	}

}
