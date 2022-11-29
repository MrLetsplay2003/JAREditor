package me.mrletsplay.jareditor.format;

public class ParseError extends RuntimeException {

	private static final long serialVersionUID = 3216364357326303484L;

	private int index;

	public ParseError(String message, int index) {
		super(message + " at index " + index);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

}
