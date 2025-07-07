package apphhzp.lib.hotspot.memory;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class VirtualSpace extends JVMObject {
    public static final Type TYPE= JVM.type("VirtualSpace");
    public static final int SIZE=TYPE.size;
    public static final long LOW_BOUNDARY_OFFSET=TYPE.offset("_low_boundary");
    public static final long HIGH_BOUNDARY_OFFSET=TYPE.offset("_high_boundary");
    public static final long LOW_OFFSET=TYPE.offset("_low");
    public static final long HIGH_OFFSET=TYPE.offset("_high");
    public static final long LOWER_HIGH_OFFSET=TYPE.offset("_lower_high");
    public static final long MIDDLE_HIGH_OFFSET=TYPE.offset("_middle_high");
    public static final long UPPER_HIGH_OFFSET=TYPE.offset("_upper_high");
    public VirtualSpace(long addr) {
        super(addr);
    }

    public long low(){
        return unsafe.getAddress(this.address+LOW_OFFSET);
    }

    public long high(){
        return unsafe.getAddress(this.address+HIGH_OFFSET);
    }

    public long lowBoundary(){
        return unsafe.getAddress(this.address+LOW_BOUNDARY_OFFSET);
    }

    public long highBoundary(){
        return unsafe.getAddress(this.address+HIGH_BOUNDARY_OFFSET);
    }

    public boolean contains(long addr){
        return this.low()<=addr&&addr<this.high();
    }

    @Override
    public String toString() {
        return "VirtualSpace@0x"+Long.toHexString(this.address);
    }
}
