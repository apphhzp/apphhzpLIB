package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class HeapRegionSetBase extends JVMObject {
    public static final Type TYPE= JVM.type("HeapRegionSetBase");
    public static final int SIZE=TYPE.size;
    public static final long LENGTH_OFFSET=TYPE.offset("_length");
    public static final long NAME_OFFSET=JVM.computeOffset(JVM.oopSize,LENGTH_OFFSET+JVM.intSize);
    public static final long VERIFY_IN_PROGRESS_OFFSET=NAME_OFFSET+JVM.oopSize;
    public HeapRegionSetBase(long addr) {
        super(addr);
    }

    public long length(){
        return unsafe.getInt(this.address+LENGTH_OFFSET)&0xffffffffL;
    }

    public void setLength(long len){
        unsafe.putInt(this.address+LENGTH_OFFSET,(int) (len&0xffffffffL));
    }

    public String name(){
        return JVM.getStringRef(this.address+NAME_OFFSET);
    }

    @Override
    public String toString() {
        return "HeapRegionSetBase@0x"+Long.toHexString(this.address);
    }
}
