package me.mrletsplay.jareditor.format;

import java.util.List;

import me.mrletsplay.mrcore.misc.ByteUtils;
import me.mrletsplay.mrcore.misc.classfile.ByteCode;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;
import me.mrletsplay.mrcore.misc.classfile.InstructionInformation;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolClassEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolFieldRefEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolInterfaceMethodRefEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolMethodRefEntry;
import me.mrletsplay.mrcore.misc.classfile.util.ClassFileUtils;

public class ByteCodeFormatter {

	public static String formatByteCode(ClassFile cf, ByteCode code, int indent) {
		StringBuilder b = new StringBuilder();
		List<InstructionInformation> instrs = code.parseCode();
		for(InstructionInformation i : instrs) {
			b.append(formatInstruction(cf, i, indent));
		}
		return b.toString();
	}

	private static CharSequence formatInstruction(ClassFile cf, InstructionInformation info, int indent) {
		StringBuilder b = new StringBuilder();
		b.append(indent(indent)).append(info.getInstruction().name().toLowerCase());
		if(info.getInformation().length > 0) b.append(" ").append(formatInstructionInformation(cf, info));
		b.append("\n");
		return b;
	}

	private static CharSequence formatInstructionInformation(ClassFile cf, InstructionInformation info) {
		StringBuilder b = new StringBuilder();
		byte[] i = info.getInformation();
		switch(info.getInstruction()) {
			case INVOKESTATIC:
			case INVOKESPECIAL:
			case INVOKEVIRTUAL:
			case INVOKEINTERFACE:
			case NEW:
			case CHECKCAST:
			case PUTFIELD:
			case PUTSTATIC:
			case GETFIELD:
			case GETSTATIC:
			{
				ConstantPoolEntry e = cf.getConstantPool().getEntry(((i[0] & 0xFF) << 8) | i[1] & 0xFF);
				b.append(formatConstantPoolEntry(cf, e));
				break;
			}
			default:
				return ByteUtils.bytesToHex(info.getInformation());
		}
		return b;
	}

	private static CharSequence formatConstantPoolEntry(ClassFile cf, ConstantPoolEntry entry) {
		StringBuilder b = new StringBuilder();
		switch(entry.getTag()) {
			case METHOD_REF:
			{
				ConstantPoolMethodRefEntry e = (ConstantPoolMethodRefEntry) entry;
				b.append("method:")
					.append(e.getClassInfo().getName().getValue()).append(":")
					.append(e.getNameAndType().getName().getValue()).append(":")
					.append(e.getNameAndType().getDescriptor().getValue());
				break;
			}
			case INTERFACE_METHOD_REF:
			{
				ConstantPoolInterfaceMethodRefEntry e = (ConstantPoolInterfaceMethodRefEntry) entry;
				b.append("interfacemethod:")
					.append(e.getClassInfo().getName().getValue()).append(":")
					.append(e.getNameAndType().getName().getValue()).append(":")
					.append(e.getNameAndType().getDescriptor().getValue());
				break;
			}
			case CLASS:
			{
				ConstantPoolClassEntry e = (ConstantPoolClassEntry) entry;
				b.append("class:")
					.append(e.getName().getValue());
				break;
			}
			case FIELD_REF:
			{
				ConstantPoolFieldRefEntry e = (ConstantPoolFieldRefEntry) entry;
				b.append("field:")
					.append(e.getClassInfo().getName().getValue()).append(":")
					.append(e.getNameAndType().getName().getValue()).append(":")
					.append(e.getNameAndType().getDescriptor().getValue());
				break;
			}
			default:
			{
				return ByteUtils.bytesToHex(ClassFileUtils.getShortBytes(cf.getConstantPool().indexOf(entry)));
			}
		}
		return b;
	}

	private static CharSequence indent(int n) {
		StringBuilder b = new StringBuilder();
		while(n-- > 0) b.append("\t");
		return b;
	}

}
