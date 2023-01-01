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
import me.mrletsplay.mrcore.misc.classfile.attribute.AttributeStackMapTable;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapAppendFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapChopFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapFrameType;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapSameFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapSameFrameExtended;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VariableInfoGeneric;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VariableInfoObject;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VariableInfoUninitialized;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VerificationType;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VerificationTypeInfo;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolClassEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolEntry;
import me.mrletsplay.mrcore.misc.classfile.util.ClassFileUtils;

public class ClassFileParser {

	public static Result<ClassFile, ParseError> parse(ClassFile original, String str) {
		// Copy old ClassFile
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		ClassFile cf = null;
		try {
			original.write(bOut);
			cf = new ClassFile(new ByteArrayInputStream(bOut.toByteArray()));
		} catch (IOException ignored) {
			throw new IllegalStateException("This should never happen", ignored);
		}

		ParseString parse = new FullParseString(str);

		List<ConstantPoolEntry> constantPool = null;
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
				case "constantpool":
				{
					var cp = readConstantPool(cf, parse);
					if(cp.isErr()) return cp.up();
					if(constantPool != null) return Result.err(new ParseError("Duplicate constant pool", parse.mark()));
					constantPool = cp.value();
					continue;
				}
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

		String major = properties.get("major");
		if(major == null) return Result.err(new ParseError("Missing major version", 0));
		String minor = properties.get("minor");
		if(minor == null) return Result.err(new ParseError("Missing minor version", 0));

		try {
			cf.setMajorVersion(Integer.parseInt(major));
			cf.setMinorVersion(Integer.parseInt(minor));
		}catch(NumberFormatException e) {
			return Result.err(new ParseError("Invalid major/minor version", 0));
		}

		String superclass = properties.get("superclass");
		if(superclass == null) return Result.err(new ParseError("Missing superclass", 0));
		String interfaces = properties.get("interfaces");
		if(interfaces == null) return Result.err(new ParseError("Missing interfaces", 0));

		cf.setSuperClass(ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, superclass)));
		List<ConstantPoolClassEntry> ifs = new ArrayList<>();
		if(!interfaces.isEmpty()) {
			for(String s : interfaces.split(",")) {
				ifs.add((ConstantPoolClassEntry) cf.getConstantPool().getEntry(ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, s))));
			}
		}
		cf.setInterfaces(ifs.toArray(ConstantPoolClassEntry[]::new));

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

		try {
			if(a == null) {
				switch(attr.getName()) {
					case "Code":
					{
						AttributeCode code = new AttributeCode(cf);
						var c = ByteCodeParser.parse(cf, str);
						if(c.isErr()) {
							str.reset(m);
							return c.up();
						}

						String locals = attr.getProperties().get("locals");
						if(locals == null) {
							str.reset(m);
							return Result.err(new ParseError("Missing locals attribute", m));
						}

						String stack = attr.getProperties().get("stack");
						if(stack == null) {
							str.reset(m);
							return Result.err(new ParseError("Missing stack attribute", m));
						}

						try {
							code.setMaxLocals(Integer.parseInt(locals));
							code.setMaxStack(Integer.parseInt(stack));
						}catch(NumberFormatException e) {
							str.reset(m);
							return Result.err(new ParseError("Invalid number", m));
						}

						code.getCode().replace(c.value());
						a = code;
						break;
					}
					case "StackMapTable":
					{
						AttributeStackMapTable smt = new AttributeStackMapTable(cf);
						List<StackMapFrame> frames = new ArrayList<>();

						while(!str.stripLeading().end()) {
							String type = str.nextToken();
							if(str.end()) {
								str.reset(m);
								return Result.err(new ParseError("Unexpected end of input", m));
							}

							Result<ParseString, ParseError> block = readBlock(str);
							if(block.isErr()) return block.up();

							ParseString blk = block.value();

							Map<String, String> properties = new HashMap<>();
							Map<String, ParseString> blocks = new HashMap<>();

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
									return Result.err(new ParseError("Unexpected end of input", blk.mark()));
								}

								var subBlk = readBlock(blk);
								if(subBlk.isErr()) {
									str.reset(m);
									return Result.err(new ParseError("Unexpected end of input", blk.mark()));
								}

								blocks.put(token, subBlk.value());
							}

							StackMapFrameType frameType;
							try {
								frameType = StackMapFrameType.valueOf(type.toUpperCase());
							}catch(IllegalArgumentException e) {
								str.reset(m);
								return Result.err(new ParseError("Invalid stack map frame type", blk.mark()));
							}

							if(!properties.containsKey("offset")) {
								str.reset(m);
								return Result.err(new ParseError("No offset property", attr.getInfo().mark()));
							}

							int offset;
							try {
								offset = Integer.parseInt(properties.get("offset"));
							}catch(NumberFormatException e) {
								str.reset(m);
								return Result.err(new ParseError("Invalid stack map frame offset", blk.mark()));
							}

							switch(frameType) {
								case APPEND_FRAME:
								{
									if(!blocks.containsKey("types")) {
										str.reset(m);
										return Result.err(new ParseError("No types block", attr.getInfo().mark()));
									}

									var types = readTypes(cf, str);
									if(types.isErr()) {
										str.reset(m);
										return types.up();
									}

									frames.add(new StackMapAppendFrame(offset, types.value().toArray(VerificationTypeInfo[]::new)));
									break;
								}
								case CHOP_FRAME:
								{
									if(!properties.containsKey("absent")) {
										str.reset(m);
										return Result.err(new ParseError("No absent property", attr.getInfo().mark()));
									}

									try {
										frames.add(new StackMapChopFrame(offset, Integer.parseInt(properties.get("offset"))));
									}catch(NumberFormatException e) {
										str.reset(m);
										return Result.err(new ParseError("Invalid stack map frame offset", blk.mark()));
									}
									break;
								}
								case FULL_FRAME:
								{
									// TODO
									break;
								}
								case SAME_LOCALS_1_STACK_ITEM_FRAME:
								{
									// TODO
									break;
								}
								case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
								{
									// TODO
									break;
								}
								case SAME_FRAME:
								{
									frames.add(new StackMapSameFrame(offset));
									break;
								}
								case SAME_FRAME_EXTENDED:
								{
									frames.add(new StackMapSameFrameExtended(offset));
									break;
								}
								default:
									break;
							}
						}

						a = smt;
						break;
					}
					default:
						return Result.err(new ParseError("Invalid attribute contents", attr.getInfo().mark()));
				}
			}
		}catch(IOException e) {
			throw new FriendlyException(e);
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

	private static Result<List<VerificationTypeInfo>, ParseError> readTypes(ClassFile cf, ParseString str) {
		int m = str.mark();
		List<VerificationTypeInfo> types = new ArrayList<>();
		while(true) {
			str.stripLeading();
			if(str.end()) return Result.of(types);

			String token = str.nextToken();
			VerificationTypeInfo type = parseType(cf, token);
			if(type == null) {
				int mark = str.mark();
				str.reset(m);
				return Result.err(new ParseError("Invalid type", mark));
			}

			types.add(type);
		}
	}

	private static VerificationTypeInfo parseType(ClassFile cf, String str) {
		String[] spl = str.split(":");
		VerificationType type;
		try {
			type = VerificationType.valueOf(spl[0].toUpperCase());
		}catch(IllegalArgumentException e) {
			return null;
		}

		switch(type) {
			case DOUBLE:
			case FLOAT:
			case INTEGER:
			case LONG:
			case NULL:
			case TOP:
			case UNINITIALIZED_THIS:
				if(spl.length != 1) return null;
				return new VariableInfoGeneric(type);
			case OBJECT:
				if(spl.length != 2) return null;
				return new VariableInfoObject(cf.getConstantPool().getEntry(ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[1]))).as(ConstantPoolClassEntry.class));
			case UNINITIALIZED_VARIABLE:
				if(spl.length != 2) return null;
				String offsetStr = spl[1];
				int offset;
				try {
					if(!offsetStr.startsWith("0x")) {
						offset = Integer.parseInt(offsetStr);
					}else {
						byte[] bytes = ByteUtils.hexToBytes(offsetStr);
						offset = (bytes[0] << 8) | bytes[1];
					}
				}catch(IllegalArgumentException e) {
					return null;
				}
				return new VariableInfoUninitialized(offset);
			default:
				throw new IllegalArgumentException("Unsupported type");
		}
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

	private static Result<List<ConstantPoolEntry>, ParseError> readConstantPool(ClassFile cf, ParseString str) {
		int m = str.mark();
		str.stripLeading();

		var block = readBlock(str);
		if(block.isErr()) {
			str.reset(m);
			return block.up();
		}

		List<ConstantPoolEntry> entries = new ArrayList<>();
		ParseString blk = block.value();
		Result<ConstantPoolEntry, ParseError> err;
		while(true) {
			blk.stripLeading();
			var en = parseConstantPoolEntry(cf, blk);
			if(en.isErr()) {
				err = en;
				break;
			}

			entries.add(en.value());
		}

		if(!blk.end()) {
			str.reset(m);
			return err.up();
		}

		return Result.of(entries);
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

	public static Result<ConstantPoolEntry, ParseError> parseConstantPoolEntry(ClassFile cf, ParseString str) {
		int m = str.mark();
		str.stripLeading();
		if(str.end()) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", m));
		}

		int i = 0;
		while(i < str.remaining() && str.peek(i) != '{') i++;

		String tag = str.next(i);
		if(str.end()) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", m));
		}

		str.advance();

		i = 0;
		while(i < str.remaining() && str.peek(i) != '}') i++;

		if(str.end()) {
			str.reset(m);
			return Result.err(new ParseError("Unexpected end of input", m));
		}

		String content = str.next(i);
		str.advance();
		content = content.substring(1, content.length() - 1);
		String[] spl = content.split(":");

		int idx;
		// TODO: checks, allow escaping
		switch(tag) {
			case "class":
			{
				idx = ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[0]));
				break;
			}
			case "field":
			{
				idx = ClassFileUtils.getOrAppendMethodRef(cf,
					ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[0])),
					ClassFileUtils.getOrAppendNameAndType(cf,
						ClassFileUtils.getOrAppendUTF8(cf, spl[1]),
						ClassFileUtils.getOrAppendUTF8(cf, spl[2])));
				break;
			}
			case "method":
			{
				idx = ClassFileUtils.getOrAppendMethodRef(cf,
					ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[0])),
					ClassFileUtils.getOrAppendNameAndType(cf,
						ClassFileUtils.getOrAppendUTF8(cf, spl[1]),
						ClassFileUtils.getOrAppendUTF8(cf, spl[2])));
				break;
			}
			case "interfacemethod":
			{
				idx = ClassFileUtils.getOrAppendMethodRef(cf,
					ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[0])),
					ClassFileUtils.getOrAppendNameAndType(cf,
						ClassFileUtils.getOrAppendUTF8(cf, spl[1]),
						ClassFileUtils.getOrAppendUTF8(cf, spl[2])));
				break;
			}
			case "utf8":
			{
				idx = ClassFileUtils.getOrAppendUTF8(cf, spl[0]);
				break;
			}
			case "string":
			{
				idx = ClassFileUtils.getOrAppendString(cf, spl[0]);
				break;
			}
			case "integer":
			{
				idx = ClassFileUtils.getOrAppendInteger(cf, Integer.parseInt(spl[0]));
				break;
			}
			case "float":
			{
				idx = ClassFileUtils.getOrAppendFloat(cf, Float.parseFloat(spl[0]));
				break;
			}
			case "long":
			{
				idx = ClassFileUtils.getOrAppendLong(cf, Long.parseLong(spl[0]));
				break;
			}
			case "nameandtype":
			{
				idx = ClassFileUtils.getOrAppendLong(cf, ClassFileUtils.getOrAppendNameAndType(cf,
					ClassFileUtils.getOrAppendUTF8(cf, spl[0]),
					ClassFileUtils.getOrAppendUTF8(cf, spl[1])));
				break;
			}
			default:
				str.reset(m);
				return Result.err(new ParseError("Invalid constant pool tag '" + tag + "'", str.mark()));
		}
		return Result.of(cf.getConstantPool().getEntry(idx));
	}

}
