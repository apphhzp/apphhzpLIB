package apphhzp.lib.hotspot.gc;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public class PLABStats extends JVMObject {
    public static final long DESCRIPTION_OFFSET;
    public static final long ALLOCATED_OFFSET;
    public static final long WASTED_OFFSET;
    public static final long UNDO_WASTED_OFFSET;
    public static final long UNUSED_OFFSET;
    public static final long DEFAULT_PLAB_SZ_OFFSET;
    public static final long DESIRED_NET_PLAB_SZ_OFFSET;
    public static final long FILTER_OFFSET;
    public static final int SIZE;
    public static final int maxSize;
    static {
        long[] offsets = JVM.computeOffsets(true,
                new long[]{JVM.oopSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,Math.max(JVM.unsignedSize,JVM.floatSize)},
                new long[]{JVM.oopSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,AdaptiveWeightedAverage.SIZE});
        DESCRIPTION_OFFSET=offsets[0];
        ALLOCATED_OFFSET=offsets[1];
        WASTED_OFFSET=offsets[2];
        UNDO_WASTED_OFFSET=offsets[3];
        UNUSED_OFFSET=offsets[4];
        DEFAULT_PLAB_SZ_OFFSET=offsets[5];
        DESIRED_NET_PLAB_SZ_OFFSET=offsets[6];
        FILTER_OFFSET=offsets[7];
        maxSize=Math.max(JVM.size_tSize,Math.max(JVM.floatSize,Math.max(JVM.unsignedSize,JVM.oopSize)));
        SIZE= (int) JVM.computeOffset(maxSize,FILTER_OFFSET+AdaptiveWeightedAverage.SIZE);
    }
    public PLABStats(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "PLABStats@0x"+Long.toHexString(this.address);
    }
}
