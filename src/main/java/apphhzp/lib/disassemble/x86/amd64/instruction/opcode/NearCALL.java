package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

public class NearCALL extends AMD64Opcode {
    private long offset;
    private boolean overridePrefix;
    public NearCALL(long offset){
        checkOffset(offset);
        this.offset = offset;
        this.overridePrefix = false;
    }
    public NearCALL(int offset, boolean hasOverridePrefix){
        checkOffset(offset&0xffffffffL);
        this.offset = hasOverridePrefix?offset&0xffffL:offset&0xffffffffL;
        this.overridePrefix = hasOverridePrefix;
    }

    public void useOffset(long offset){
        checkOffset(offset);
        this.offset= offset;
        this.overridePrefix = false;
    }

    public long getCalledFunc(long base){
        return base+5+offset;
    }

    public void setCalledFuncOffset(long base,long dest){
        if (dest>=base&&dest-base<=0xffffffffL){
            offset= dest-base-5;
            overridePrefix=offset<=0xffffL;
        }else {
            throw new IllegalArgumentException();
        }
    }

    private static void checkOffset(long offset){
        if (offset<0||offset>0xffffffffL){
            throw new IllegalArgumentException("offset out of range: "+offset);
        }
    }

    @Override
    public byte[] toOpcodes(){
        int val=(int)(offset&0xffffffffL);
        if (overridePrefix){
            return new byte[]{0x66, (byte) 0xe8,low8(val),midLow8(val)};
        }
        return new byte[]{(byte) 0xe8,low8(val),midLow8(val),midHigh8(val),high8(val)};
    }

    @Override
    public int size(){
        return overridePrefix?4:5;
    }

    @Override
    public byte getOpcode() {
        return (byte) 0xe8;
    }
}
