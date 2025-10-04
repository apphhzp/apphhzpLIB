package apphhzp.lib.hotspot.compiler;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class ImmutableOopMapPair extends JVMObject {
    public static final Type TYPE = JVM.type("ImmutableOopMapPair");
    public static final int SIZE = TYPE.size;
    public static final long PC_OFFSET_OFFSET = TYPE.offset("_pc_offset");
    public static final long OOPMAP_OFFSET_OFFSET = TYPE.offset("_oopmap_offset");

    public ImmutableOopMapPair(long addr) {
        super(addr);
    }

    public ImmutableOopMapPair(int pc_offset, int oopmap_offset){
        super(unsafe.allocateMemory(SIZE));
        if (!(pc_offset >= 0 && oopmap_offset >= 0)){
            if (this.address!=0){
                unsafe.freeMemory(this.address);
            }
            throw new IllegalArgumentException("check");
        }
        unsafe.putInt(this.address+PC_OFFSET_OFFSET, pc_offset);
        unsafe.putInt(this.address+OOPMAP_OFFSET_OFFSET, oopmap_offset);
    }

    public int pc_offset() {
        return unsafe.getInt(this.address+PC_OFFSET_OFFSET);
    }

    public int oopmap_offset() {
        return unsafe.getInt(this.address+OOPMAP_OFFSET_OFFSET);
    }


    public ImmutableOopMap get_from(ImmutableOopMapSet set){
        return set.oopmap_at_offset(oopmap_offset());
    }
}
