package me.mrletsplay.jareditor.format.entity;

import java.util.List;
import java.util.Map;

import me.mrletsplay.jareditor.format.string.ParseString;

public class ParserAttribute {

	private String name;
	private ParseString info;
	private Map<String, String> properties;
	private List<ParserAttribute> attributes;

	public ParserAttribute(String name, ParseString info, Map<String, String> properties, List<ParserAttribute> attributes) {
		this.name = name;
		this.info = info;
		this.properties = properties;
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public ParseString getInfo() {
		return info;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public List<ParserAttribute> getAttributes() {
		return attributes;
	}

}
