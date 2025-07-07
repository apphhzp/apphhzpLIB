package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.memory.MemRegion;
import apphhzp.lib.hotspot.oops.oop.Oop;

import java.util.ArrayList;
import java.util.List;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class HeapRegion extends JVMObject implements LiveRegionsProvider {
    public static final Type TYPE= JVM.type("HeapRegion");
    public static final int SIZE= TYPE.size;
    public static final long _LogOfHRGrainBytes_ADDRESS=TYPE.global("LogOfHRGrainBytes");
    public static final long _GrainBytes_ADDRESS=TYPE.global("GrainBytes");
    public static final int LogOfHRGrainBytes= unsafe.getInt(_LogOfHRGrainBytes_ADDRESS);
    public static final long GrainBytes=JVM.getSizeT(_GrainBytes_ADDRESS);
    public static final long BOTTOM_OFFSET=TYPE.offset("_bottom");
    public static final long END_OFFSET=TYPE.offset("_end");
    public static final long TOP_OFFSET=TYPE.offset("_top");
    public static final long COMPACTION_TOP_OFFSET=TYPE.offset("_compaction_top");
    public static final long TYPE_OFFSET=TYPE.offset("_type");
    public static final long HUMONGOUS_START_REGION_OFFSET=JVM.computeOffset(JVM.oopSize,TYPE_OFFSET+HeapRegionType.SIZE);
    public final HeapRegionType type=new HeapRegionType(this.address+TYPE_OFFSET);
    private HeapRegion humongousStartRegionCache;
    public HeapRegion(long addr) {
        super(addr);
    }

    public long bottom(){
        return unsafe.getAddress(this.address+BOTTOM_OFFSET);
    }
    public long end(){
        return unsafe.getAddress(this.address+END_OFFSET);
    }
    public long top(){
        return unsafe.getAddress(this.address+TOP_OFFSET);
    }
    public void setTop(long value) {
        unsafe.putAddress(this.address+TOP_OFFSET, value);
    }

    public long compactionTop(){
        return unsafe.getAddress(this.address+COMPACTION_TOP_OFFSET);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setCompactionTop(long compaction_top) {
        unsafe.putAddress(this.address+COMPACTION_TOP_OFFSET, compaction_top);
    }
    public long capacity()    { return this.end()-this.bottom();}
    public long used() { return this.top()-this.bottom();}
    public long free() { return this.end()-this.top();}
    public boolean isInReserved(long p){ return this.bottom() <= p && p < this.end(); }

    public boolean isFree(){ return this.type.isFree(); }

    public boolean isYoung()   { return this.type.isYoung();    }
    public boolean isEden()    { return this.type.isEden();     }
    public boolean isSurvivor(){ return this.type.isSurvivor(); }

    public boolean isHumongous(){ return this.type.isHumongous(); }
    public boolean isStartsHumongous(){ return this.type.isStartsHumongous(); }
    public boolean isContinuesHumongous(){ return this.type.isContinuesHumongous();   }

    public boolean isOld(){ return this.type.isOld(); }

    public boolean isOldOrHumongous(){ return this.type.isOldOrHumongous(); }

    public boolean isOldOrHumongousOrArchive(){ return this.type.isOldOrHumongousOrArchive(); }

    // A pinned region contains objects which are not moved by garbage collections.
    // Humongous regions and archive regions are pinned.
    public boolean isPinned(){ return this.type.isPinned(); }

    // An archive region is a pinned region, also tagged as old, which
    // should not be marked during mark/sweep. This allows the address
    // space to be shared by JVM instances.
    public boolean isArchive()       { return this.type.isArchive(); }
    public boolean isOpenArchive()  { return this.type.isOpenArchive(); }
    public boolean isClosedArchive(){ return this.type.isClosedArchive(); }


    public HeapRegion getHumongousStartRegion(){
        long addr=unsafe.getAddress(this.address+HUMONGOUS_START_REGION_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.humongousStartRegionCache,addr)){
            this.humongousStartRegionCache=new HeapRegion(addr);
        }
        return this.humongousStartRegionCache;
    }

    @Override
    public List<MemRegion> getLiveRegions() {
        List<MemRegion> res = new ArrayList<>();
        res.add(MemRegion.createLimitFake(bottom(), top()));
        return res;
    }

    public boolean is_in(long p){
        return this.isInReserved(p);
    }
    public boolean is_in(Oop obj){
        return is_in(obj.getAddress());
    }

//    public boolean is_obj_dead(Oop obj,  G1CMBitMap*  prev_bitmap)  {
//        if (!this.isInReserved(obj.getAddress())){
//            throw new IllegalArgumentException("Object 0x"+Long.toHexString(obj.getAddress())+" must be in region");
//        }
//        return !obj_allocated_since_prev_marking(obj) &&
//                !prev_bitmap->is_marked(obj) &&
//                        !is_closed_archive();
//    }
//
//    public boolean blockIsObj(long p){
//        G1CollectedHeap g1h = G1CollectedHeap.heap();
//
//        if (!this.is_in(p)) {
//            if (!this.isContinuesHumongous()){
//                throw new RuntimeException("This case can only happen for humongous regions");
//            }
//            return (p == this.getHumongousStartRegion().bottom());
//        }
//        // When class unloading is enabled it is not safe to only consider top() to conclude if the
//        // given pointer is a valid object. The situation can occur both for class unloading in a
//        // Full GC and during a concurrent cycle.
//        // During a Full GC regions can be excluded from compaction due to high live ratio, and
//        // because of this there can be stale objects for unloaded classes left in these regions.
//        // During a concurrent cycle class unloading is done after marking is complete and objects
//        // for the unloaded classes will be stale until the regions are collected.
//        if (JVM.classUnloading) {
//            return !g1h.is_obj_dead(new Oop(p), this);
//        }
//        return p < top();
//    }
//
//    public void objectIterate(ObjectClosure blk) {
//        long p = bottom();
//        while (p < this.top()) {
//            if (this.blockIsObj(p)) {
//                blk.doOop(new Oop(p).get());
//            }
//            p += block_size(p);
//        }
//    }

//    public long block_size(long addr) {
//        if (addr == top()) {
//            return JVM.pointerDeltaHeapWord(end(), addr);
//        }
//        if (this.blockIsObj(addr)) {
//            return new Oop(addr).get().getObjectSize();
//        }
//        return block_size_using_bitmap(addr, G1CollectedHeap.heap()->concurrent_mark()->prev_mark_bitmap());
//    }
//
//    public long block_size_using_bitmap(long addr, G1CMBitMap*prev_bitmap) {
//        if (!JVM.classUnloading){
//            throw new RuntimeException("All blocks should be objects if class unloading isn't used, so this method should not be called. \nHR: [0x"+
//                    Long.toHexString(this.bottom())+", 0x"+Long.toHexString(this.top())+", 0x"+Long.toHexString(this.end())+") \naddr: 0x"+Long.toHexString(addr));
//        }
//        // Old regions' dead objects may have dead classes
//        // We need to find the next live object using the bitmap
//        long next = prev_bitmap->get_next_marked_addr(addr, prev_top_at_mark_start());
//        if (next<=addr){
//            throw new RuntimeException("must get the next live object");
//        }
//        return JVM.pointerDeltaHeapWord(next, addr);
//    }

}
