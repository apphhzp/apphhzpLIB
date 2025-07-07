package apphhzp.lib.disassemble.x86.amd64.instruction;

public abstract class AMD64Instruction {

    public abstract int size();

    public abstract byte getOpcode();

    public static byte low8(int val){
        return (byte) (val&0xff);
    }

    public static byte midLow8(int val){
        return (byte) ((val>>8)&0xff);
    }

    public static byte midHigh8(int val){
        return (byte) ((val>>16)&0xff);
    }

    public static byte high8(int val){
        return (byte) ((val>>24)&0xff);
    }
}
