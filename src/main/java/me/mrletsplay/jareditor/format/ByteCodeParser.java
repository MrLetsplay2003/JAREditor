package me.mrletsplay.jareditor.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Map<String, Integer> labels = new HashMap<>();
		Map<Integer, String> toResolve = new HashMap<>();
		int loc = 0;
		while(true) {
			str.stripLeading();
			String instr = str.nextToken();
			if(instr == null) break;
			if(instr.endsWith(":")) {
				String label = instr.substring(0, instr.length() - 1);
				if(labels.containsKey(label)) return  Result.err(new ParseError("Invalid instruction", str.mark() - instr.length()));
				labels.put(label, loc);
				str.stripLeading();
				instr = str.nextToken();
				if(instr == null) break;
			}

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
				val = parseInstructionArgument(cf, arg, instrs.size(), toResolve);
				if(val == null) return Result.err(new ParseError("Invalid instruction argument '" + arg + "'", str.mark() - arg.length()));
			}

			InstructionInformation ii = new InstructionInformation(i, val);
			instrs.add(ii); // TODO: validate byte count
			loc += ii.getSize();
		}

		loc = 0;
		for(int i = 0; i < instrs.size(); i++) {
			InstructionInformation ii = instrs.get(i);
			if(toResolve.containsKey(i)) {
				String label = toResolve.get(i);
				Integer refLoc = labels.get(label);
				if(refLoc == null) return Result.err(new ParseError("Referenced label '" + label + "' does not exist", 0));
				instrs.set(i, new InstructionInformation(ii.getInstruction(), ClassFileUtils.getShortBytes(refLoc - loc)));
			}
			loc += ii.getSize();
		}

		if(instrs.isEmpty()) return Result.err(new ParseError("Empty bytecode", str.mark()));
		return Result.of(ByteCode.of(instrs));
	}

	private static byte[] parseInstructionArgument(ClassFile cf, String str, int idx, Map<Integer, String> toResolve) {
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
				case "label":
				{
					if(spl.length != 2) return null;
					// Don't resolve immediately, as the label might not have appeared yet
					toResolve.put(idx, spl[1]);
					return ClassFileUtils.getShortBytes(0);
				}
				default:
					return null;
			}
		}
	}

}
