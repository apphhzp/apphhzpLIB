package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

import apphhzp.lib.disassemble.x86.amd64.instruction.AMD64Instruction;

public abstract class AMD64Opcode extends AMD64Instruction {

    public abstract byte[] toOpcodes();
}
