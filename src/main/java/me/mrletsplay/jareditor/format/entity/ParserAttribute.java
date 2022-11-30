package me.mrletsplay.jareditor.format.entity;

import java.util.List;

public class ParserAttribute {

	private String name;
	private String info;
	private List<ParserAttribute> attributes;

	public ParserAttribute(String name, String info, List<ParserAttribute> attributes) {
		this.name = name;
		this.info = info;
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public String getInfo() {
		return info;
	}

	public List<ParserAttribute> getAttributes() {
		return attributes;
	}

}
