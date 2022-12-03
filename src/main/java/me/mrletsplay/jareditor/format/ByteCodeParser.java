package me.mrletsplay.jareditor.format;

import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.jareditor.format.string.ParseString;
import me.mrletsplay.mrcore.misc.ByteUtils;
import me.mrletsplay.mrcore.misc.Result;
import me.mrletsplay.mrcore.misc.classfile.ByteCode;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;
import me.mrletsplay.mrcore.misc.classfile.Instruction;
import me.mrletsplay.mrcore.misc.classfile.InstructionInformation;
import me.mrletsplay.mrcore.misc.classfile.util.ClassFileUtils;

public class ByteCodeParser {

	public static Result<ByteCode, ParseError> parse(ClassFile cf, ParseString str) {
		List<InstructionInformation> instrs = new ArrayList<>();
		while(true) {
			str.stripLeading();
			String instr = str.nextToken();
			if(instr == null) break;
			Instruction i;
			try {
				i = Instruction.valueOf(instr.toUpperCase());
			}catch(IllegalArgumentException e) {
				return Result.err(new ParseError("Invalid instruction", str.mark() - instr.length()));
			}

			int j = 0;
			while(j < str.remaining() && str.peek(j) != '\n') j++;
			String arg = str.next(j).trim();
			byte[] val;
			if(arg.isEmpty()) {
				val = new byte[0];
			}else {
				val = parseInstructionArgument(cf, arg);
				if(val == null) return Result.err(new ParseError("Invalid instruction argument '" + arg + "'", str.mark() - arg.length()));
			}

			instrs.add(new InstructionInformation(i, val)); // TODO: validate byte count
		}

		if(instrs.isEmpty()) return Result.err(new ParseError("Empty bytecode", str.mark()));
		return Result.of(ByteCode.of(instrs));
	}

	private static byte[] parseInstructionArgument(ClassFile cf, String str) {
		if(str.startsWith("0x")) {
			try {
				return ByteUtils.hexToBytes(str.substring(2));
			}catch(IllegalArgumentException e) {
				return null;
			}
		}else {
			String[] spl = str.split(":");
			switch(spl[0]) {
				case "method":
				{
					if(spl.length != 4) return null;
					return ClassFileUtils.getShortBytes(ClassFileUtils.getOrAppendMethodRef(cf,
						ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[1])),
						ClassFileUtils.getOrAppendNameAndType(cf,
							ClassFileUtils.getOrAppendUTF8(cf, spl[2]),
							ClassFileUtils.getOrAppendUTF8(cf, spl[3]))));
				}
				case "interfacemethod":
				{
					if(spl.length != 4) return null;
					return ClassFileUtils.getShortBytes(ClassFileUtils.getOrAppendInterfaceMethodRef(cf,
						ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[1])),
						ClassFileUtils.getOrAppendNameAndType(cf,
							ClassFileUtils.getOrAppendUTF8(cf, spl[2]),
							ClassFileUtils.getOrAppendUTF8(cf, spl[3]))));
				}
				case "field":
				{
					if(spl.length != 4) return null;
					return ClassFileUtils.getShortBytes(ClassFileUtils.getOrAppendFieldRef(cf,
						ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[1])),
						ClassFileUtils.getOrAppendNameAndType(cf,
							ClassFileUtils.getOrAppendUTF8(cf, spl[2]),
							ClassFileUtils.getOrAppendUTF8(cf, spl[3]))));
				}
				case "class":
				{
					if(spl.length != 2) return null;
					return ClassFileUtils.getShortBytes(ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, spl[1])));
				}
				default:
					return null;
			}
		}
	}

}
