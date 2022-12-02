package me.mrletsplay.jareditor.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.mrletsplay.jareditor.format.entity.ParserAttribute;
import me.mrletsplay.jareditor.format.entity.ParserField;
import me.mrletsplay.jareditor.format.entity.ParserMethod;
import me.mrletsplay.jareditor.format.string.FullParseString;
import me.mrletsplay.jareditor.format.string.ParseString;
import me.mrletsplay.mrcore.misc.ByteUtils;
import me.mrletsplay.mrcore.misc.EnumFlagCompound;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.mrcore.misc.Result;
import me.mrletsplay.mrcore.misc.classfile.ClassField;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;
import me.mrletsplay.mrcore.misc.classfile.ClassMethod;
import me.mrletsplay.mrcore.misc.classfile.MethodAccessFlag;
import me.mrletsplay.mrcore.misc.classfile.attribute.Attribute;
import me.mrletsplay.mrcore.misc.classfile.attribute.AttributeCode;
import me.mrletsplay.mrcore.misc.classfile.attribute.AttributeRaw;
import me.mrletsplay.mrcore.misc.classfile.util.ClassFileUtils;

public class ClassFileParser {

	public static Result<ClassFile, ParseError> parse(ClassFile original, String str) {
		// Copy old ClassFile
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		ClassFile cf = null;
		try {
			original.write(bOut);
			cf = new ClassFile(new ByteArrayInputStream(bOut.toByteArray()));
		} catch (IOException ignored) {}

		ParseString parse = new FullParseString(str);

		Map<String, String> properties = new HashMap<>();
		List<ParserAttribute> attributes = new ArrayList<>();
		List<ParserField> fields = new ArrayList<>();
		List<ParserMethod> methods = new ArrayList<>();
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
			if(token == null) return Result.err(new ParseError("Unexpected end of input", parse.mark()));
			switch(token) {
				case "attribute":
				{
					var attr = readAttribute(parse);
					if(attr.isErr()) return attr.up();
					attributes.add(attr.value());
					continue;
				}
				case "field":
				{
					var field = readField(parse);
					if(field.isErr()) return field.up();
					fields.add(field.value());
					continue;
				}
				case "method":
				{
					var method = readMethod(parse);
					if(method.isErr()) return method.up();
					methods.add(method.value());
					continue;
				}
				default:
				{
					return Result.err(new ParseError("Unexpected token '" + token + "'", parse.mark()));
				}
			}
		}

		List<Attribute> attrs = new ArrayList<>();
		for(ParserAttribute a : attributes) {
			var at = createAttribute(cf, a);
			if(at.isErr()) return at.up();
			attrs.add(at.value());
		}
		cf.setAttributes(attrs.toArray(Attribute[]::new));

		List<ClassMethod> mths = new ArrayList<>();
		for(ParserMethod m : methods) {
			var mth = createMethod(cf, m);
			if(mth.isErr()) return mth.up();
			mths.add(mth.value());
		}
		cf.setMethods(mths.toArray(ClassMethod[]::new));

		List<ClassField> fs = new ArrayList<>();
		for(ParserField m : fields) {
			var fl = createField(cf, m);
			if(fl.isErr()) return fl.up();
			fs.add(fl.value());
		}
		cf.setFields(fs.toArray(ClassField[]::new));

		return Result.of(cf);
	}

	private static Result<ClassMethod, ParseError> createMethod(ClassFile cf, ParserMethod method) {
		EnumFlagCompound<MethodAccessFlag> flags = EnumFlagCompound.noneOf(MethodAccessFlag.class);
		String fStr = method.getProperties().get("flags");
		if(fStr == null) return Result.err(new ParseError("Missing method access flags", method.getIndex()));
		String[] fArr = fStr.split(",");
		for(String f : fArr) {
			MethodAccessFlag fl = Arrays.stream(MethodAccessFlag.values())
				.filter(flag -> flag.getName().equals(f))
				.findFirst().orElse(null);
			if(fl == null) return Result.err(new ParseError("Invalid method access flag '" + f + "'", method.getIndex()));
			flags.addFlag(fl);
		}

		String desc = method.getProperties().get("descriptor");
		if(desc == null) return Result.err(new ParseError("Missing method descriptor", method.getIndex()));

		List<Attribute> mAttrs = new ArrayList<>();
		for(ParserAttribute a : method.getAttributes()) {
			var at = createAttribute(cf, a);
			if(at.isErr()) return at.up();
			mAttrs.add(at.value());
		}

		ClassMethod cm = new ClassMethod(cf,
			(int) flags.getCompound(),
			ClassFileUtils.getOrAppendUTF8(cf, method.getName()),
			ClassFileUtils.getOrAppendUTF8(cf, desc),
			mAttrs.toArray(Attribute[]::new));
		return Result.of(cm);
	}

	private static Result<ClassField, ParseError> createField(ClassFile cf, ParserField field) {
		EnumFlagCompound<MethodAccessFlag> flags = EnumFlagCompound.noneOf(MethodAccessFlag.class);
		String fStr = field.getProperties().get("flags");
		if(fStr == null) return Result.err(new ParseError("Missing field access flags", field.getIndex()));
		String[] fArr = fStr.split(",");
		for(String f : fArr) {
			MethodAccessFlag fl = Arrays.stream(MethodAccessFlag.values())
				.filter(flag -> flag.getName().equals(f))
				.findFirst().orElse(null);
			if(fl == null) return Result.err(new ParseError("Invalid field access flag '" + f + "'", field.getIndex()));
			flags.addFlag(fl);
		}

		String desc = field.getProperties().get("descriptor");
		if(desc == null) return Result.err(new ParseError("Missing field descriptor", field.getIndex()));

		List<Attribute> fAttrs = new ArrayList<>();
		for(ParserAttribute a : field.getAttributes()) {
			var at = createAttribute(cf, a);
			if(at.isErr()) return at.up();
			fAttrs.add(at.value());
		}

		ClassField f = new ClassField(cf,
			(int) flags.getCompound(),
			ClassFileUtils.getOrAppendUTF8(cf, field.getName()),
			ClassFileUtils.getOrAppendUTF8(cf, desc),
			fAttrs.toArray(Attribute[]::new));
		return Result.of(f);
	}

	private static Result<Attribute, ParseError> createAttribute(ClassFile cf, ParserAttribute attr) {
		ParseString str = attr.getInfo();
		int m = str.mark();
		str.stripLeading();
		Attribute a = null;
		if(str.expect("0x")) {
			String tok = attr.getInfo().nextToken();
			if(!str.stripLeading().end()) {
				str.reset(m);
				return Result.err(new ParseError("Extra content", str.mark()));
			}
			a = new AttributeRaw(cf,
				ClassFileUtils.getOrAppendUTF8(cf, attr.getName()),
				ByteUtils.hexToBytes(tok));
		}

		if(a == null) {
			switch(attr.getName()) {
				case "Code":
				{
					try {
						AttributeCode code = new AttributeCode(cf);
						var c = ByteCodeParser.parse(cf, str);
						if(c.isErr()) {
							str.reset(m);
							return c.up();
						}
						code.getCode().replace(c.value());
						a = code;
						break;
					} catch (IOException e) {
						throw new FriendlyException(e);
					}
				}
				default:
					return Result.err(new ParseError("Invalid attribute contents", attr.getInfo().mark()));
			}
		}

		List<Attribute> attrs = new ArrayList<>();
		for(ParserAttribute p : attr.getAttributes()) {
			var at = createAttribute(cf, p);
			if(at.isErr()) return at.up();
			attrs.add(at.value());
		}

		if(attrs.size() > 0) {
			str.reset(m);
			if(a instanceof AttributeRaw) return Result.err(new ParseError("Raw attributes can't have child attributes", str.mark()));
			a.setAttributes(attrs.toArray(Attribute[]::new));
		}

		return Result.of(a);
	}

	private static Result<Map.Entry<String, String>, ParseError> readPair(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		String pair = str.nextToken();
		if(pair == null) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", str.mark()));
		}

		if(!pair.contains("=")) {
			str.reset(m);
			return Result.err(new ParseError("Pair needs to contain '='", str.mark()));
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
			return Result.err(new ParseError("Unexpected end of input", str.mark()));
		}

		var block = readBlock(str);
		if(block.isErr()) {
			str.reset(m);
			return block.up();
		}

		ParseString blk = block.value();

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
				return Result.err(new ParseError("Unexpected end of input", str.mark() + blk.mark()));
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
					return Result.err(new ParseError("Unexpected token '" + token + "'", str.mark() + blk.mark()));
				}
			}

			break;
		}

		return Result.of(new ParserField(m, name, properties, attributes));
	}

	private static Result<ParserAttribute, ParseError> readAttribute(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		String name = str.nextToken();
		if(name == null) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", str.mark()));
		}

		var block = readBlock(str);
		if(block.isErr()) {
			str.reset(m);
			return block.up();
		}

		ParseString blk = block.value();

		Map<String, String> properties = new HashMap<>();
		List<ParserAttribute> attributes = new ArrayList<>();
		ParseString info = null;
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
				return Result.err(new ParseError("Unexpected end of input", str.mark() + blk.mark()));
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
					if(info != null) return Result.err(new ParseError("Duplicate info block", str.mark()));
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
					return Result.err(new ParseError("Unexpected token '" + token + "'", str.mark() + blk.mark()));
				}
			}

			break;
		}

		return Result.of(new ParserAttribute(name, info, properties, attributes));
	}

	private static Result<ParserMethod, ParseError> readMethod(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		String name = str.nextToken();
		if(name == null) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", str.mark()));
		}

		var block = readBlock(str);
		if(block.isErr()) {
			str.reset(m);
			return block.up();
		}

		ParseString blk = block.value();

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
				return Result.err(new ParseError("Unexpected end of input", str.mark() + blk.mark()));
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
					return Result.err(new ParseError("Unexpected token '" + token + "'", str.mark() + blk.mark()));
				}
			}

			break;
		}

		return Result.of(new ParserMethod(m, name, properties, attributes));
	}

	private static Result<ParseString, ParseError> readBlock(ParseString str) {
		int m = str.mark();
		str.stripLeading();
		if(str.get() != '{') {
			str.reset(m);
			return Result.err(new ParseError("'{' expected", str.mark()));
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
			return Result.err(new ParseError("'}' expected", str.mark() + i));
		}

		str.advance();
		ParseString block = str.sub(i - 1);
		str.advance(i);
		return Result.of(block);
	}

}
