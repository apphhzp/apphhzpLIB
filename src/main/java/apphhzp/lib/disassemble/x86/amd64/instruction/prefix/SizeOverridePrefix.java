package apphhzp.lib.disassemble.x86.amd64.instruction.prefix;

import apphhzp.lib.disassemble.x86.amd64.instruction.AMD64Instruction;

/**
 * 指定为16位操作数大小
 * */
public class SizeOverridePrefix extends AMD64Instruction {
    @Override
    public byte getOpcode() {
        return 0x66;
    }

    @Override
    public int size() {
        return 0;
    }
}
