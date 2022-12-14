package me.mrletsplay.jareditor.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import me.mrletsplay.mrcore.misc.classfile.Instruction;

public class SyntaxHighlighting {

	private static final Pattern KEYWORD_PATTERN;

	static {
		List<String> kws = new ArrayList<>();
		for(Instruction i : Instruction.values()) kws.add(i.name().toLowerCase());
		String bytecodeKW = kws.stream()
			.map(Pattern::quote)
			.collect(Collectors.joining("|"));

		List<String> additionalKWs = Arrays.asList("method", "attribute", "info", "field", "constantpool");
		String additionalKW = additionalKWs.stream()
			.map(Pattern::quote)
			.collect(Collectors.joining("|"));

		List<String> thingTypes = Arrays.asList("method", "field", "double", "float", "long", "integer", "utf8", "string", "class", "interfacemethod", "nameandtype", "methodhandle", "methodtype", "invokedynamic");
		String thingRef = "((?:" + thingTypes.stream().collect(Collectors.joining("|")) + ")\\{[^\\n]+\\})";

		String label = "(label:[^:\\s\\n]+|[^:\\s\\n]+:)";

		KEYWORD_PATTERN = Pattern.compile(
			"(?<!\\S)((?<thingref>" + thingRef + ")|"
			+ "(?<bytecode>" + bytecodeKW + ")|"
			+ "(?<additional>" + additionalKW + ")|"
			+ "(?<label>" + label + "))(?!\\S)");
	}

	public static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = KEYWORD_PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = null;
			if(matcher.group("bytecode") != null) styleClass = "highlight-bytecode";
			if(matcher.group("additional") != null) styleClass = "highlight-additional";
			if(matcher.group("thingref") != null) styleClass = "highlight-thingref";
			if(matcher.group("label") != null) styleClass = "highlight-label";
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

}
