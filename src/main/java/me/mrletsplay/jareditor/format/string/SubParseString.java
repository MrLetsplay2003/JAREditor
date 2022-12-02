package me.mrletsplay.jareditor.format.string;

public class SubParseString implements ParseString {

	private FullParseString parent;
	private int start, length; // abs start index + char count
	private int index; // abs index into parent string

	public SubParseString(FullParseString parent, int start, int length) {
		this.parent = parent;
		this.start = start;
		this.length = length;
		this.index = start;
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
		if(index + count > start + length) throw new IllegalArgumentException("Read beyond end of string");
		index += count;
		return this;
	}

	@Override
	public char get() {
		if(end()) throw new IllegalArgumentException("Read beyond end of string");
		return parent.str.charAt(index);
	}

	@Override
	public char peek(int count) {
		if(index + count >= start + length) throw new IllegalArgumentException("Requested count exceeds string length");
		return parent.str.charAt(index + count);
	}

	@Override
	public String next(int count) {
		if(index + count > start + length) throw new IllegalArgumentException("Requested count exceeds string length");
		String string = parent.str.substring(index, index + count);
		index += count;
		return string;
	}

	@Override
	public boolean expect(String str) {
		if(index + str.length() <= start + length && parent.str.substring(index).startsWith(str)) {
			next(str.length());
			return true;
		}
		return false;
	}

	@Override
	public int remaining() {
		return start + length - index;
	}

	@Override
	public boolean end() {
		return index >= start + length;
	}

	@Override
	public ParseString sub(int count) {
		if(index + count > start + length) throw new IllegalArgumentException("Requested count exceeds string length");
		return new SubParseString(parent, index, count);
	}

	@Override
	public String toString() {
		return parent.str.substring(start, start + length);
	}

}
