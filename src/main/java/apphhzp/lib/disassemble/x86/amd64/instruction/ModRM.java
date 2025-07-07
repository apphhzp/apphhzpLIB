package apphhzp.lib.disassemble.x86.amd64.instruction;

import apphhzp.lib.disassemble.x86.amd64.General64BitRegister;

public class ModRM extends AMD64Instruction {
    private int val;
    public boolean r;
    public boolean b;
    public ModRM(int val) {
        this.val = val&0xff;
    }

    public int mod(){
        return (val&0b11000000)>>6;
    }

    public int reg(){
        return (r?(val&0b111000)|0b1000000:val&0b111000)>>3;
    }

    public int rm(){
        //0b1000
        //mod:00
        //reg:001
        //r/m:000
        return b?(val&0b111)|0b1000:val&0b111;
    }

    public void setMod(int val){
        this.val&=~(0b11000000);
        this.val|=(val&0b11)<<6;
    }

    public void setReg(int val){
        this.val&=~(0b111000);
        this.val|=(val&0b111)<<3;
        if ((val&0b1000)!=0){
            this.r=true;
        }
    }

    public void setRM(int val){
        this.val&=~(0b111);
        this.val|=(val&0b111);
        if ((val&0b1000)!=0){
            this.b=true;
        }
    }

    public void setRMReg(General64BitRegister register){
        this.setMod(0b11);
        int val=register.is64Only?0b1000:0;
        val|= switch (register){
            case rAX,r8 ->0;
            case rCX,r9->0b1;
            case rDX,r10->0b10;
            case rBX,r11->0b11;
            case rSP,r12->0b100;
            case rBP,r13->0b101;
            case rSI,r14->0b110;
            case rDI,r15->0b111;
        };
        this.setRM(val);
    }

    public void setRMRegWithDisp(General64BitRegister register,int disp){
        this.setMod(0b11);
        int val=register.is64Only?0b1000:0;
        val|= switch (register){
            case rAX,r8 ->0;
            case rCX,r9->0b1;
            case rDX,r10->0b10;
            case rBX,r11->0b11;
            case rSP,r12->0b100;
            case rBP,r13->0b101;
            case rSI,r14->0b110;
            case rDI,r15->0b111;
        };
        this.setRM(val);
    }

    public boolean hasSIB(){
        return (this.mod()!=0b11&&this.rm()==0b100)||(this.mod()==0&&this.rm()==0b101);
    }

    @Override
    public int size() {
        if (true){
            throw new UnsupportedOperationException("TODO...");
        }
        return 1;
    }

    @Override
    public byte getOpcode() {
        return (byte) (val&0xff);
    }
}
