package me.mrletsplay.jareditor.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.mrletsplay.mrcore.misc.ByteUtils;
import me.mrletsplay.mrcore.misc.classfile.ByteCode;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;
import me.mrletsplay.mrcore.misc.classfile.InstructionInformation;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolEntry;

public class ByteCodeFormatter {

	private static short shortOffset(byte[] bytes) {
		return (short) (((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF));
	}

	private static int intOffset(byte[] bytes) {
		return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
	}

	public static String formatByteCode(ClassFile cf, ByteCode code, int indent) {
		StringBuilder b = new StringBuilder();
		List<InstructionInformation> instrs = code.parseCode();
		int loc = 0;
		Map<Integer, String> labels = new HashMap<>();
		int labelIndex = 0;
		for(InstructionInformation i : instrs) {
			int loc2 = 0;
			o: for(InstructionInformation j : instrs) {
				switch(j.getInstruction()) {
					case GOTO:
					case IFEQ:
					case IFNE:
					case IFLT:
					case IFGE:
					case IFGT:
					case IFLE:
					case IF_ACMPEQ:
					case IF_ACMPNE:
					case IFNONNULL:
					case IFNULL:
					case IF_ICMPEQ:
					case IF_ICMPNE:
					case IF_ICMPLT:
					case IF_ICMPGE:
					case IF_ICMPGT:
					case IF_ICMPLE:
					{
						short off = shortOffset(j.getInformation());
						if(loc2 + off == loc) {
							labels.put(loc, "label" + (labelIndex++));
							break o;
						}
						break;
					}
					case GOTO_W:
					{
						int off = intOffset(j.getInformation());
						if(loc2 + off == loc) {
							labels.put(loc, "label" + (labelIndex++));
							break o;
						}
						break;
					}
					default:
						break;
				}

				loc2 += j.getSize();
			}
			loc += i.getSize();
		}

		loc = 0;
		for(InstructionInformation i : instrs) {
			b.append(formatInstruction(cf, i, loc, labels, indent));
			loc += i.getSize();
		}
		return b.toString();
	}

	private static CharSequence formatInstruction(ClassFile cf, InstructionInformation info, int loc, Map<Integer, String> labels, int indent) {
		StringBuilder b = new StringBuilder();
		b.append(indent(indent));
		if(labels.containsKey(loc)) b.append(labels.get(loc)).append(": ");
		b.append(info.getInstruction().name().toLowerCase());
		if(info.getInformation().length > 0) b.append(" ").append(formatInstructionInformation(cf, info, loc, labels));
		b.append("\n");
		return b;
	}

	private static CharSequence formatInstructionInformation(ClassFile cf, InstructionInformation info, int loc, Map<Integer, String> labels) {
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
				b.append(ClassFileFormatter.formatConstantPoolEntry(cf, e));
				break;
			}
			case GOTO:
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			{
				int off = shortOffset(i);
				if(labels.containsKey(loc + off)) b.append("label:").append(labels.get(loc + off));
				break;
			}
			default:
				return "0x" + ByteUtils.bytesToHex(info.getInformation());
		}
		return b;
	}

	private static CharSequence indent(int n) {
		StringBuilder b = new StringBuilder();
		while(n-- > 0) b.append("\t");
		return b;
	}

}
