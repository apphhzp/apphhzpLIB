package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

import static apphhzp.lib.ClassHelper.unsafe;

public class DeoptimizationBlob extends CodeBlob{
    public static final Type TYPE= JVM.type("DeoptimizationBlob");
    public static final int SIZE=TYPE.size;
    public static final long UNPACK_OFFSET_OFFSET=TYPE.offset("_unpack_offset");
    public DeoptimizationBlob(long addr) {
        super(addr,TYPE);
    }

    public int getUnpackOffset(){
        return unsafe.getInt(this.address+UNPACK_OFFSET_OFFSET);
    }

    public void setUnpackOffset(int offset){
        unsafe.putInt(this.address+UNPACK_OFFSET_OFFSET,offset);
    }
}
