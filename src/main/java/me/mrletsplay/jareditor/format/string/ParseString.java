package me.mrletsplay.jareditor.format.string;

public interface ParseString {

	public int mark();
	public ParseString reset(int mark);

	public ParseString advance(int count);
	public char get();
	public char peek(int count);
	public String next(int count);
	public boolean expect(String str);

	public int remaining();
	public boolean end();

	public ParseString sub(int count);

	public default ParseString advance() {
		return advance(1);
	}

	public default ParseString stripLeading() {
		while(!end() && Character.isWhitespace(get())) advance();
		return this;
	}

	public default String nextToken() {
		if(end()) return null;
		int i = 0;
		while(i < remaining() && !Character.isWhitespace(peek(i))) i++;
		return next(i);
	}

}
