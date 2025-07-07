package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

public class NearRET extends AMD64Opcode {
    private boolean has_val;
    private int ret_val;
    public NearRET() {
        has_val=false;
    }
    public NearRET(int val) {
        this.setValue(val);
    }

    public void setValue(int val) {
        ret_val = val;
        has_val=true;
    }

    public void setNoValue(){
        has_val=false;
    }

    public int getValue(){
        if (!has_val){
            throw new IllegalStateException("RET has no value");
        }
        return ret_val;
    }

    public boolean hasValue() {
        return has_val;
    }

    @Override
    public byte[] toOpcodes() {
        if (has_val){
            return new byte[]{(byte) 0xc2,low8(ret_val),midLow8(ret_val)};
        }
        return new byte[]{(byte) 0xc3};
    }

    @Override
    public byte getOpcode() {
        return (byte) (has_val?0xc2:0xc3);
    }

    @Override
    public int size() {
        return has_val?3:1;
    }
}
