package apphhzp.lib.hotspot.gc;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.gc.g1.LiveRegionsClosure;
import apphhzp.lib.hotspot.memory.MemRegion;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class CollectedHeap extends JVMObject {
    public static final Type TYPE= JVM.type("CollectedHeap");
    public static final int SIZE=TYPE.size;
    public static final long RESERVED_OFFSET=TYPE.offset("_reserved");
    public static final long IS_GC_ACTIVE_OFFSET=TYPE.offset("_is_gc_active");
    public static final long TOTAL_COLLECTIONS_OFFSET=TYPE.offset("_total_collections");
    public final MemRegion reserved=new MemRegion(this.address+RESERVED_OFFSET);
    public CollectedHeap(long addr) {
        super(addr);
    }

    public boolean isGCActive(){
        return unsafe.getByte(this.address+IS_GC_ACTIVE_OFFSET) != 0;
    }

    public void setGCActive(boolean active){
        unsafe.putByte(this.address+IS_GC_ACTIVE_OFFSET, (byte) (active?1:0));
    }

    public long getTotalCollections(){
        return unsafe.getInt(this.address+TOTAL_COLLECTIONS_OFFSET)&0xffffffffL;
    }

    public void setTotalCollections(long collections){
        unsafe.putInt(this.address+TOTAL_COLLECTIONS_OFFSET, (int) (collections&0xffffffffL));
    }

    public void liveRegionsIterate(LiveRegionsClosure closure){}
}
