package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

import apphhzp.lib.disassemble.x86.amd64.General64BitRegister;

import static apphhzp.lib.disassemble.x86.amd64.General64BitRegister.*;
import static apphhzp.lib.disassemble.x86.amd64.General64BitRegister.rDI;

public class PUSH extends AMD64Opcode {
    public General64BitRegister register;
    public PUSH(final General64BitRegister register){
        this.register = register;
    }
    @Override
    public byte[] toOpcodes() {
        if (register.is64Only){
            return new byte[] {(byte) 0x41,reg2code(register)};
        }
        return new byte[]{reg2code(register)};
    }

    public static byte reg2code(General64BitRegister reg){
        return (byte)switch (reg){
            case rAX,r8 -> 0x50;
            case rCX,r9 -> 0x51;
            case rDX,r10 -> 0x52;
            case rBX,r11 -> 0x53;
            case rSP,r12 -> 0x54;
            case rBP,r13 -> 0x55;
            case rSI,r14 -> 0x56;
            case rDI,r15 -> 0x57;
        };
    }

    public static General64BitRegister code2reg(byte code, boolean is64Only){
        return switch ((code&0xff)){
            case 0x50->is64Only?r8:rAX;
            case 0x51->is64Only?r9:rCX;
            case 0x52->is64Only?r10:rDX;
            case 0x53->is64Only?r11:rBX;
            case 0x54->is64Only?r12:rSP;
            case 0x55->is64Only?r13:rBP;
            case 0x56->is64Only?r14:rSI;
            case 0x57->is64Only?r15:rDI;
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
