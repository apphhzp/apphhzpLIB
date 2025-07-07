package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

import apphhzp.lib.disassemble.x86.amd64.instruction.ModRM;
import apphhzp.lib.disassemble.x86.amd64.instruction.SIB;

import javax.annotation.Nullable;

public class FarCALL extends AMD64Opcode {
    public ModRM modRM;
    @Nullable
    public SIB sib;
    public int disp;
    public FarCALL(ModRM modRM, SIB sib,int disp) {
        this.modRM = modRM;
        this.sib = sib;
        this.disp = disp;
    }


    @Override
    public byte[] toOpcodes() {
        if (true){
            throw new IllegalArgumentException("TODO...");
        }
        return new byte[]{(byte) 0xff,modRM.getOpcode(),};
    }

    @Override
    public int size() {
        return 1+modRM.size();
    }

    @Override
    public byte getOpcode() {
        //Group 5
        return (byte) 0xff;
    }
}
