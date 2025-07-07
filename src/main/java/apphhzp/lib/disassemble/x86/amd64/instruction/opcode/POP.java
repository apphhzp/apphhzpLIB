package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

import apphhzp.lib.disassemble.x86.amd64.General64BitRegister;

import static apphhzp.lib.disassemble.x86.amd64.General64BitRegister.*;

public class POP extends AMD64Opcode {
    public General64BitRegister register;
    public POP(General64BitRegister register) {
        this.register = register;
    }
    @Override
    public byte[] toOpcodes() {
        if (register.is64Only){
            return new byte[]{(byte) 0x41,reg2code(register)};
        }
        return new byte[]{reg2code(register)};
    }

    public static byte reg2code(General64BitRegister reg){
        return (byte)switch (reg){
            case rAX,r8 -> 0x58;
            case rCX,r9 -> 0x59;
            case rDX,r10 -> 0x5a;
            case rBX,r11 -> 0x5b;
            case rSP,r12 -> 0x5c;
            case rBP,r13 -> 0x5d;
            case rSI,r14 -> 0x5e;
            case rDI,r15 -> 0x5f;
        };
    }

    public static General64BitRegister code2reg(byte code, boolean is64Only){
        return switch ((code&0xff)){
            case 0x58->is64Only?r8:rAX;
            case 0x59->is64Only?r9:rCX;
            case 0x5a->is64Only?r10:rDX;
            case 0x5b->is64Only?r11:rBX;
            case 0x5c->is64Only?r12:rSP;
            case 0x5d->is64Only?r13:rBP;
            case 0x5e->is64Only?r14:rSI;
            case 0x5f->is64Only?r15:rDI;
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public byte getOpcode() {
        return reg2code(register);
    }

    @Override
    public int size() {
        return register.is64Only?2:1;
    }
}
