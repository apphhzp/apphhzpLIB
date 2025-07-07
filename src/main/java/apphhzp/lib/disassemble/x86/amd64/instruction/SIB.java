package apphhzp.lib.disassemble.x86.amd64.instruction;

public class SIB extends AMD64Instruction {
    private int val;
    private boolean x;
    private boolean b;
    public SIB(int val) {
        this.val = val&0xff;
    }

    public int scale(){
        return (val&0b11000000)>>6;
    }

    public int index(){
        return (x?(val&0b111000)|0b1000000:val&0b111000)>>3;
    }

    public int base(){
        return b? val&0b111 |0b1000:val&0b111;
    }

    public void setScale(int val){
        this.val&=~0b11000000;
        this.val|=(val&0b11)<<6;
    }

    public void setIndex(int val){
        this.val&=~0b111000;
        this.val|=(val&0b111)<<3;
        this.x=(val&0b1000)!=0;
    }
    public void setBase(int val){
        this.val&=~0b111;
        this.val|=val&0b111;
        this.b=(val&0b1000)!=0;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public byte getOpcode() {
        return (byte) (val&0xff);
    }
}
