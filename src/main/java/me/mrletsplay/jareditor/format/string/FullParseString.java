package me.mrletsplay.jareditor.format.string;

public class FullParseString implements ParseString {

	String str;
	int index;

	public FullParseString(String str) {
		this.str = str;
	}

	@Override
	public int mark() {
		return index;
	}

	@Override
	public ParseString reset(int mark) {
		this.index = mark;
		return this;
	}

	@Override
	public ParseString advance(int count) {
		if(index + count > str.length()) throw new IllegalArgumentException("Read beyond end of string");
		index += count;
		return this;
	}

	@Override
	public char get() {
		if(end()) throw new IllegalArgumentException("Read beyond end of string");
		return str.charAt(index);
	}

	@Override
	public char peek(int count) {
		if(index + count >= str.length()) throw new IllegalArgumentException("Requested count exceeds string length");
		return str.charAt(index + count);
	}

	@Override
	public String next(int count) {
		if(index + count > str.length()) throw new IllegalArgumentException("Requested count exceeds string length");
		String string = str.substring(index, index + count);
		index += count;
		return string;
	}

	@Override
	public boolean expect(String str) {
		if(str.substring(index).startsWith(str)) {
			next(str.length());
			return true;
		}

		return false;
	}

	@Override
	public int remaining() {
		return str.length() - index;
	}

	@Override
	public boolean end() {
		return index >= str.length();
	}

	@Override
	public ParseString sub(int count) {
		if(index + count > str.length()) throw new IllegalArgumentException("Requested count exceeds string length");
		return new SubParseString(this, index, count);
	}

	@Override
	public String toString() {
		return str;
	}

}
