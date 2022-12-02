package me.mrletsplay.jareditor.format;

import java.util.Arrays;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.misc.ByteUtils;
import me.mrletsplay.mrcore.misc.classfile.ByteCode;
import me.mrletsplay.mrcore.misc.classfile.ClassField;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;
import me.mrletsplay.mrcore.misc.classfile.ClassMethod;
import me.mrletsplay.mrcore.misc.classfile.attribute.Attribute;
import me.mrletsplay.mrcore.misc.classfile.attribute.AttributeCode;
import me.mrletsplay.mrcore.misc.classfile.attribute.AttributeRaw;

public class ClassFileFormatter {

	public static String formatClass(ClassFile cf) {
		StringBuilder b = new StringBuilder();

		b.append("name=").append(cf.getThisClass().getName().getValue()).append("\n");
		b.append("superclass=").append(cf.getSuperClass().getName().getValue()).append("\n");
		b.append("interfaces=").append(Arrays.stream(cf.getInterfaces())
			.map(i -> i.getName().getValue())
			.collect(Collectors.joining(","))).append("\n");
		b.append("flags=").append(cf.getAccessFlags().getApplicable().stream()
			.map(f-> f.name().toLowerCase())
			.collect(Collectors.joining(","))).append("\n\n");

		for(Attribute a : cf.getAttributes()) b.append(formatAttribute(cf, a, 0));
		for(ClassField f : cf.getFields()) b.append(formatField(cf, f, 0));
		for(ClassMethod m : cf.getMethods()) b.append(formatMethod(cf, m, 0));

		return b.toString();
	}

	private static CharSequence formatField(ClassFile cf, ClassField field, int indent) {
		StringBuilder b = new StringBuilder();
		b.append(indent(indent)).append("field ").append(field.getName().getValue()).append(" {\n");
		b.append(indent(indent + 1)).append("descriptor=").append(field.getDescriptor().getValue()).append("\n");
		b.append(indent(indent + 1)).append("flags=").append(field.getAccessFlags().getApplicable().stream()
			.map(f -> f.name().toLowerCase())
			.collect(Collectors.joining(","))).append("\n");
		if(field.getAttributes().length != 0) b.append("\n");
		for(Attribute a : field.getAttributes()) b.append(formatAttribute(cf, a, indent + 1));
		b.append(indent(indent)).append("}\n\n");
		return b;
	}

	private static CharSequence formatMethod(ClassFile cf, ClassMethod method, int indent) {
		StringBuilder b = new StringBuilder();
		b.append(indent(indent)).append("method ").append(method.getName().getValue()).append(" {\n");
		b.append(indent(indent + 1)).append("descriptor=").append(method.getDescriptor().getValue()).append("\n");
		b.append(indent(indent + 1)).append("flags=").append(method.getAccessFlags().getApplicable().stream()
			.map(f-> f.name().toLowerCase())
			.collect(Collectors.joining(","))).append("\n");
		if(method.getAttributes().length != 0) b.append("\n");
		for(Attribute a : method.getAttributes()) b.append(formatAttribute(cf, a, indent + 1));
		b.append(indent(indent)).append("}\n\n");
		return b;
	}

	private static CharSequence formatAttribute(ClassFile cf, Attribute attr, int indent) {
		StringBuilder b = new StringBuilder();
		b.append(indent(indent)).append("attribute ").append(attr.getNameString()).append(" {\n");
		b.append(formatAttributeInfo(cf, attr, indent + 1));

		if(!(attr instanceof AttributeRaw) && attr.getAttributes().length != 0) {
			b.append("\n");
			for(Attribute a : attr.getAttributes()) b.append(formatAttribute(cf, a, indent + 1));
		}

		b.append(indent(indent)).append("}\n\n");
		return b;
	}

	private static CharSequence formatAttributeInfo(ClassFile cf, Attribute attr, int indent) {
		StringBuilder b = new StringBuilder();
		b.append(indent(indent)).append("info {\n");
		if(attr instanceof AttributeCode) {
			AttributeCode code = (AttributeCode) attr;
			ByteCode bc = code.getCode();
			b.append(ByteCodeFormatter.formatByteCode(cf, bc, indent + 1));
		}else {
			b.append(indent(indent + 1)).append("0x" + ByteUtils.bytesToHex(attr.getInfo())).append("\n");
		}
		b.append(indent(indent)).append("}\n");
		return b;
	}

	private static CharSequence indent(int n) {
		StringBuilder b = new StringBuilder();
		while(n-- > 0) b.append("\t");
		return b;
	}

}
