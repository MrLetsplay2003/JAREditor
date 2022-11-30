package me.mrletsplay.jareditor.format.entity;

import java.util.List;
import java.util.Map;

public class ParserMethod {

	private String name;
	private Map<String, String> properties;
	private List<ParserAttribute> attributes;

	public ParserMethod(String name, Map<String, String> properties, List<ParserAttribute> attributes) {
		this.name = name;
		this.properties = properties;
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public List<ParserAttribute> getAttributes() {
		return attributes;
	}

}
