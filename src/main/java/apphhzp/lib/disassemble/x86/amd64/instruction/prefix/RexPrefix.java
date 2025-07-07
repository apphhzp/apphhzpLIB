package apphhzp.lib.disassemble.x86.amd64.instruction.prefix;

import apphhzp.lib.disassemble.x86.amd64.instruction.AMD64Instruction;

public class RexPrefix extends AMD64Instruction {
    public final int val;
    public RexPrefix(byte val) {
        this.val = val&0xff;
    }

    public RexPrefix(boolean w,boolean r,boolean x,boolean b) {
        int v=0b01000000;
        if (w){
            v|=0b1000;
        }
        if (r){
            v|=0b100;
        }
        if (x){
            v|=0b10;
        }
        if (b){
            v|=0b1;
        }
        this.val = v;
    }

    /**
     * 0 = 默认操作数大小<br>
     * 1 = 64位操作数大小
     * */
    public boolean w(){
        return (val&0b1000)!=0;
    }

    /**
     * 1-bit (msb) extension of the ModRM reg field, permitting access to 16 registers.
     * */
    public boolean r(){
        return (val&0b100)!=0;
    }

    /**
     * 1-bit (msb) extension of the SIB index field, permitting access to 16 registers.
     * */
    public boolean x(){
        return (val&0b10)!=0;
    }

    /**
     * 1-bit (msb) extension of the ModRM r/m field, SIB base field, or opcode reg field, permitting access to 16 registers.
     * */
    public boolean b(){
        return (val&0b1)!=0;
    }

    @Override
    public byte getOpcode() {
        return (byte)(val&0xff);
    }

    @Override
    public int size() {
        return 1;
    }
}
