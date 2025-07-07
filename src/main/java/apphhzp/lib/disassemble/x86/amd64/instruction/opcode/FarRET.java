package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class FarRET extends AMD64Opcode {
    private boolean has_val;
    private long ret_val;
    public FarRET() {
        has_val=false;
    }
    public FarRET(long val) {
        this.setValue(val);
    }

    public void setValue(long val) {
        ret_val = val;
        has_val=true;
    }

    public long getValue(){
        if (!has_val){
            throw new IllegalStateException("RET has no value");
        }
        return ret_val;
    }

    public void setNoValue(){
        has_val=false;
    }

    public boolean hasValue() {
        return has_val;
    }

    @Override
    public byte[] toOpcodes() {
        if (has_val){
            if (unsafe.addressSize()==4){
                int val=(int)(ret_val&0xffffffffL);
                return new byte[]{(byte) 0xca,low8(val),midLow8(val),midHigh8(val),high8(val)};
            }
            int low=(int)(ret_val&0xffffffffL),high=(int)((ret_val>>32L)&0xffffffffL);
            return new byte[]{(byte) 0xca,low8(low),midLow8(low),midHigh8(low),high8(low)
                    ,low8(high),midLow8(high),midHigh8(high),high8(high)};
        }
        return new byte[]{(byte) 0xcb};
    }

    @Override
    public byte getOpcode() {
        return (byte) (has_val?0xca:0xcb);
    }

    @Override
    public int size() {
        return has_val?1+unsafe.addressSize():1;
    }
}
