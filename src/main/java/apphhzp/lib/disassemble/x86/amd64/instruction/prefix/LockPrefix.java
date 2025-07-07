package apphhzp.lib.disassemble.x86.amd64.instruction.prefix;

import apphhzp.lib.disassemble.x86.amd64.instruction.AMD64Instruction;
import apphhzp.lib.disassemble.x86.amd64.instruction.opcode.AMD64Opcode;

public class LockPrefix extends AMD64Instruction {
    public final AMD64Opcode opcode;
    public LockPrefix(final AMD64Opcode opcode) {
        //ADC,ADD,AND,BTC,BTR,BTS,CMPXCHG,CMPXCHG8B,CMPXCHG16B,DEC,INC,NEG,NOT,OR,SBB,SUB,XADD,XCHG, XOR
        if (!isValid(opcode)){
            throw new IllegalArgumentException("invalid opcode: " + opcode);
        }
        this.opcode = opcode;
    }

    private static boolean isValid(AMD64Opcode code){
        //TODO...

        return true;
    }

    @Override
    public byte getOpcode() {
        return (byte) 0xf0;
    }

    @Override
    public int size() {
        return 1;
    }
}
