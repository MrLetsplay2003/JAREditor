package me.mrletsplay.jareditor.format;

import java.util.List;

import me.mrletsplay.mrcore.misc.classfile.ByteCode;
import me.mrletsplay.mrcore.misc.classfile.InstructionInformation;

public class ByteCodeFormatter {

	public static String formatByteCode(ByteCode code, int indent) {
		StringBuilder b = new StringBuilder();
		List<InstructionInformation> instrs = code.parseCode();
		for(InstructionInformation i : instrs) {
			b.append(formatInstruction(i, indent));
		}
		return b.toString();
	}

	private static CharSequence formatInstruction(InstructionInformation info, int indent) {
		StringBuilder b = new StringBuilder();
		b.append(indent(indent)).append(info.getInstruction().name().toLowerCase()).append("\n");
		return b;
	}

	private static CharSequence indent(int n) {
		StringBuilder b = new StringBuilder();
		while(n-- > 0) b.append("\t");
		return b;
	}

}
