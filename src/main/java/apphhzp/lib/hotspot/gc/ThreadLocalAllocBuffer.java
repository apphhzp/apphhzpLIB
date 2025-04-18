package apphhzp.lib.hotspot.gc;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.oop.ArrayOopDesc;
import apphhzp.lib.hotspot.utilities.BasicType;

import static apphhzp.lib.ClassHelper.unsafe;

public class ThreadLocalAllocBuffer extends JVMObject {
    public static final Type TYPE= JVM.type("ThreadLocalAllocBuffer");
    public static final int SIZE=TYPE.size;
    public static final long reserve_for_allocation_prefetch_address=TYPE.global("_reserve_for_allocation_prefetch");
    public static final long START_OFFSET=TYPE.offset("_start");
    public static final long TOP_OFFSET=TYPE.offset("_top");
    public static final long PF_TOP_OFFSET=TYPE.offset("_pf_top");
    public static final long END_OFFSET=TYPE.offset("_end");
    public ThreadLocalAllocBuffer(long addr) {
        super(addr);
    }

    public long start(){
        return unsafe.getAddress(this.address+START_OFFSET);
    }

    public long top(){
        return unsafe.getAddress(this.address+TOP_OFFSET);
    }

    public long pfTop(){
        return unsafe.getAddress(this.address+PF_TOP_OFFSET);
    }

    public long end(){
        return unsafe.getAddress(this.address+END_OFFSET);
    }

    public long hardEnd()  { return end()+ JVM.alignObjectSize(this.endReserve()); }

    private long endReserve() {
        long minFillerArraySize = ArrayOopDesc.baseOffsetInBytes(BasicType.T_INT);
        long reserveForAllocationPrefetch = getReserveForAllocationPrefetch();
        long heapWordSize = JVM.oopSize;

        return Math.max(minFillerArraySize, reserveForAllocationPrefetch * heapWordSize);
    }

    public static int getReserveForAllocationPrefetch(){
        return unsafe.getInt(reserve_for_allocation_prefetch_address);
    }

    @Override
    public String toString() {
        return "ThreadLocalAllocBuffer@0x"+Long.toHexString(address);
    }
}
