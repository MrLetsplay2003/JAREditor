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
import me.mrletsplay.mrcore.misc.classfile.attribute.AttributeStackMapTable;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapAppendFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapChopFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapFullFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapSameLocals1StackItemFrame;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.StackMapSameLocals1StackItemFrameExtended;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VariableInfoObject;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VariableInfoUninitialized;
import me.mrletsplay.mrcore.misc.classfile.attribute.stackmap.verification.VerificationTypeInfo;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolClassEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolDoubleEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolFieldRefEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolFloatEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolIntegerEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolInterfaceMethodRefEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolInvokeDynamicEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolLongEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolMethodHandleEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolMethodRefEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolMethodTypeEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolNameAndTypeEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolStringEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolUTF8Entry;
import me.mrletsplay.mrcore.misc.classfile.util.ClassFileUtils;

public class ClassFileFormatter {

	public static String formatClass(ClassFile cf) {
		StringBuilder b = new StringBuilder();

		b.append("major=").append(cf.getMajorVersion()).append("\n");
		b.append("minor=").append(cf.getMinorVersion()).append("\n");
		b.append("name=").append(cf.getThisClass().getName().getValue()).append("\n");
		b.append("superclass=").append(cf.getSuperClass().getName().getValue()).append("\n");
		b.append("interfaces=").append(Arrays.stream(cf.getInterfaces())
			.map(i -> i.getName().getValue())
			.collect(Collectors.joining(","))).append("\n");
		b.append("flags=").append(cf.getAccessFlags().getApplicable().stream()
			.map(f-> f.name().toLowerCase())
			.collect(Collectors.joining(","))).append("\n\n");

		b.append("constantpool {\n");
		for(ConstantPoolEntry e : cf.getConstantPool().getEntries()) {
			b.append(indent(1)).append(formatConstantPoolEntry(cf, e)).append("\n");
		}
		b.append("}\n\n");

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

		if(attr instanceof AttributeCode) {
			AttributeCode c = (AttributeCode) attr;
			b.append(indent(indent + 1)).append("locals=").append(c.getMaxLocals()).append("\n");
			b.append(indent(indent + 1)).append("stack=").append(c.getMaxStack()).append("\n\n");
			// TODO: exception table
		}

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
		}else if(attr instanceof AttributeStackMapTable) {
			AttributeStackMapTable smt = (AttributeStackMapTable) attr;

			for(StackMapFrame f : smt.getEntries()) {
				b.append(indent(indent + 1)).append(f.getType().name().toLowerCase()).append(" {\n");
				b.append(indent(indent + 2)).append("offset=").append(f.getOffsetDelta()).append("\n");
				switch(f.getType()) {
					case APPEND_FRAME:
					{
						StackMapAppendFrame fr = (StackMapAppendFrame) f;
						b.append(indent(indent + 2)).append("types {\n");

						for(VerificationTypeInfo i : fr.getAdditionalTypeInfo()) {
							b.append(indent(indent + 3)).append(formatVerificationTypeInfo(cf, i)).append("\n");
						}

						b.append(indent(indent + 2)).append("}\n");
						break;
					}
					case CHOP_FRAME:
					{
						StackMapChopFrame fr = (StackMapChopFrame) f;
						b.append(indent(indent + 2)).append("absent=").append(fr.getNumAbsentLocals()).append("\n");
						break;
					}
					case FULL_FRAME:
					{
						StackMapFullFrame fr = (StackMapFullFrame) f;
						b.append(indent(indent + 2)).append("locals {\n");
						for(VerificationTypeInfo i : fr.getLocals()) {
							b.append(indent(indent + 3)).append(formatVerificationTypeInfo(cf, i)).append("\n");
						}
						b.append(indent(indent + 2)).append("}\n\n");

						b.append(indent(indent + 2)).append("stack {\n");
						for(VerificationTypeInfo i : fr.getStack()) {
							b.append(indent(indent + 3)).append(formatVerificationTypeInfo(cf, i)).append("\n");
						}
						b.append(indent(indent + 2)).append("}\n");
						break;
					}
					case SAME_LOCALS_1_STACK_ITEM_FRAME:
					{
						StackMapSameLocals1StackItemFrame fr = (StackMapSameLocals1StackItemFrame) f;
						b.append(indent(indent + 3)).append("type=").append(formatVerificationTypeInfo(cf, fr.getTypeInfo())).append("\n");
						break;
					}
					case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
					{
						StackMapSameLocals1StackItemFrameExtended fr = (StackMapSameLocals1StackItemFrameExtended) f;
						b.append(indent(indent + 3)).append("type=").append(formatVerificationTypeInfo(cf, fr.getTypeInfo())).append("\n");
						break;
					}
					case SAME_FRAME:
					case SAME_FRAME_EXTENDED:
					default:
						break;
				}
				b.append(indent(indent + 1)).append("}\n\n");
			}
		}else {
			b.append(indent(indent + 1)).append("0x" + ByteUtils.bytesToHex(attr.getInfo())).append("\n");
		}
		b.append(indent(indent)).append("}\n");
		return b;
	}

	private static String formatVerificationTypeInfo(ClassFile cf, VerificationTypeInfo inf) {
		switch(inf.getType()) {
			case DOUBLE:
			case FLOAT:
			case INTEGER:
			case LONG:
			case NULL:
			case TOP:
			case UNINITIALIZED_THIS:
				return inf.getType().name().toLowerCase();
			case OBJECT:
			{
				VariableInfoObject i = (VariableInfoObject) inf;
				return i.getType().name().toLowerCase() + ":" + i.getVariableType().as(ConstantPoolClassEntry.class).getName().getValue();
			}
			case UNINITIALIZED_VARIABLE:
			{
				VariableInfoUninitialized i = (VariableInfoUninitialized) inf;
				return i.getType().name().toLowerCase() + ":0x" + ByteUtils.bytesToHex(ClassFileUtils.getShortBytes(i.getOffset()));
			}
			default:
				throw new IllegalArgumentException("Unsupported type");
		}
	}

	public static CharSequence formatConstantPoolEntry(ClassFile cf, ConstantPoolEntry entry) {
		StringBuilder b = new StringBuilder();
		switch(entry.getTag()) {
			case METHOD_REF:
			{
				ConstantPoolMethodRefEntry e = (ConstantPoolMethodRefEntry) entry;
				b.append("method{")
					.append(e.getClassInfo().getName().getValue()).append(":")
					.append(e.getNameAndType().getName().getValue()).append(":")
					.append(e.getNameAndType().getDescriptor().getValue())
					.append("}");
				break;
			}
			case INTERFACE_METHOD_REF:
			{
				ConstantPoolInterfaceMethodRefEntry e = (ConstantPoolInterfaceMethodRefEntry) entry;
				b.append("interfacemethod{")
					.append(e.getClassInfo().getName().getValue()).append(":")
					.append(e.getNameAndType().getName().getValue()).append(":")
					.append(e.getNameAndType().getDescriptor().getValue())
					.append("}");
				break;
			}
			case CLASS:
			{
				ConstantPoolClassEntry e = (ConstantPoolClassEntry) entry;
				b.append("class{")
					.append(e.getName().getValue())
					.append("}");
				break;
			}
			case FIELD_REF:
			{
				ConstantPoolFieldRefEntry e = (ConstantPoolFieldRefEntry) entry;
				b.append("field{")
					.append(e.getClassInfo().getName().getValue()).append(":")
					.append(e.getNameAndType().getName().getValue()).append(":")
					.append(e.getNameAndType().getDescriptor().getValue())
					.append("}");
				break;
			}
			case DOUBLE:
			{
				ConstantPoolDoubleEntry e = (ConstantPoolDoubleEntry) entry;
				b.append("double{")
					.append(e.getValue())
					.append("}");
				break;
			}
			case FLOAT:
			{
				ConstantPoolFloatEntry e = (ConstantPoolFloatEntry) entry;
				b.append("float{")
					.append(e.getValue())
					.append("}");
				break;
			}
			case INTEGER:
			{
				ConstantPoolIntegerEntry e = (ConstantPoolIntegerEntry) entry;
				b.append("integer{")
					.append(e.getValue())
					.append("}");
				break;
			}
			case INVOKE_DYNAMIC:
			{
				ConstantPoolInvokeDynamicEntry e = (ConstantPoolInvokeDynamicEntry) entry;
				b.append("invokedynamic{")
					.append(e.getBootstrapMethodAttributeIndex()).append(":")
					.append(e.getNameAndType().getName().getValue()).append(":")
					.append(e.getNameAndType().getDescriptor().getValue())
					.append("}");
				break;
			}
			case LONG:
			{
				ConstantPoolLongEntry e = (ConstantPoolLongEntry) entry;
				b.append("long{")
					.append(e.getValue())
					.append("}");
				break;
			}
			case METHOD_HANDLE:
			{
				ConstantPoolMethodHandleEntry e = (ConstantPoolMethodHandleEntry) entry;
				b.append("methodhandle{")
					.append(e.getReferenceType().name().toLowerCase()).append(":")
					.append(formatConstantPoolEntry(cf, e.getReference()))
					.append("}");
				break;
			}
			case METHOD_TYPE:
			{
				ConstantPoolMethodTypeEntry e = (ConstantPoolMethodTypeEntry) entry;
				b.append("methodtype{")
					.append(e.getDescriptor().getValue())
					.append("}");
				break;
			}
			case NAME_AND_TYPE:
			{
				ConstantPoolNameAndTypeEntry e = (ConstantPoolNameAndTypeEntry) entry;
				b.append("nameandtype{")
					.append(e.getName().getValue()).append(":")
					.append(e.getDescriptor().getValue())
					.append("}");
				break;
			}
			case STRING:
			{
				ConstantPoolStringEntry e = (ConstantPoolStringEntry) entry;
				b.append("string{")
					.append(escape(e.getString().getValue()))
					.append("}");
				break;
			}
			case UTF_8:
			{
				ConstantPoolUTF8Entry e = (ConstantPoolUTF8Entry) entry;
				b.append("utf8{")
					.append(escape(e.getValue()))
					.append("}");
				break;
			}
			default:
				throw new IllegalArgumentException("Unsupported constant pool entry tag '" + entry.getTag() + "'");
		}
		return b;
	}

	private static CharSequence escape(char c) {
		switch(c) {
			case '{':
				return "\\{";
			case '}':
				return "\\}";
			default:
				return String.valueOf(c);
		}
	}

	private static CharSequence escape(String str) {
		StringBuilder escaped = new StringBuilder();
		for(char c : str.toCharArray()) escaped.append(escape(c));
		return escaped;
	}

	private static CharSequence indent(int n) {
		StringBuilder b = new StringBuilder();
		while(n-- > 0) b.append("\t");
		return b;
	}

}
