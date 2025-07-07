package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class G1BiasedMappedArray extends JVMObject {
    public static final Type TYPE= JVM.type("G1HeapRegionTable");
    public static final int SIZE=TYPE.size;
    public static final long BASE_OFFSET=TYPE.offset("_base");
    public static final long ALLOC_BASE_OFFSET=BASE_OFFSET-JVM.oopSize;
    public static final long LENGTH_OFFSET=TYPE.offset("_length");
    public static final long BIASED_BASE_OFFSET=TYPE.offset("_biased_base");
    public static final long BIAS_OFFSET=TYPE.offset("_bias");
    public static final long SHIFT_BY_OFFSET=TYPE.offset("_shift_by");
    public G1BiasedMappedArray(long addr) {
        super(addr);
    }

    public long allocBase(){
        return unsafe.getAddress(this.address+ALLOC_BASE_OFFSET);
    }

    public long base(){
        return unsafe.getAddress(this.address+BASE_OFFSET);
    }

    public long length() {
        return JVM.getSizeT(this.address+LENGTH_OFFSET);
    }

    public long biasedBase(){
        return unsafe.getAddress(this.address+BIASED_BASE_OFFSET);
    }

    public long bias() {
        return JVM.getSizeT(this.address+BIAS_OFFSET);
    }

    public long shiftBy() {
        return unsafe.getInt(this.address+SHIFT_BY_OFFSET)&0xffffffffL;
    }

    @Override
    public String toString() {
        return "G1BiasedMappedArray@0x"+Long.toHexString(this.address);
    }
}
