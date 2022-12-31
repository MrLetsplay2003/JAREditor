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
				return Result.err(new ParseError("Invalid instruction '" + instr + "'", str.mark() - instr.length()));
			}

			int j = 0;
			while(j < str.remaining() && str.peek(j) != '\n') j++;
			ParseString arg = str.sub(j).stripLeading();
			byte[] val;
			if(arg.end()) {
				val = new byte[0];
			}else {
				var parsed = parseInstructionArgument(cf, arg, instrs.size(), toResolve);
				if(parsed.isErr()) return parsed.up();
				str.advance(j);
				val = parsed.value();
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

	private static Result<byte[], ParseError> parseInstructionArgument(ClassFile cf, ParseString str, int idx, Map<Integer, String> toResolve) {
		// TODO: update to use constant pool format
		if(str.expect("0x")) {
			try {
				return Result.of(ByteUtils.hexToBytes(str.next(str.remaining())));
			}catch(IllegalArgumentException e) {
				return null;
			}
		}else {
			// TODO: parse labels
			var entry = ClassFileParser.parseConstantPoolEntry(cf, str);
			if(entry.isErr()) return entry.up();
			return Result.of(ClassFileUtils.getShortBytes(cf.getConstantPool().indexOf(entry.value())));
		}
	}

}
