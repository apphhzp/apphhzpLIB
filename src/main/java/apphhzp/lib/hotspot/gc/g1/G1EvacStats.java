package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.gc.AdaptiveWeightedAverage;
import apphhzp.lib.hotspot.gc.PLABStats;

public class G1EvacStats extends PLABStats {
    public static final long REGION_END_WASTE_OFFSET= JVM.computeOffset(JVM.size_tSize,PLABStats.FILTER_OFFSET+ AdaptiveWeightedAverage.SIZE);
    public static final long REGIONS_FILLED_OFFSET=JVM.computeOffset(JVM.intSize,REGION_END_WASTE_OFFSET+JVM.size_tSize);
    public static final long NUM_PLAB_FILLED_OFFSET=JVM.computeOffset(JVM.size_tSize,REGIONS_FILLED_OFFSET+JVM.intSize);
    public static final long DIRECT_ALLOCATED_OFFSET=JVM.computeOffset(JVM.size_tSize,NUM_PLAB_FILLED_OFFSET+JVM.size_tSize);
    public static final long NUM_DIRECT_ALLOCATED_OFFSET=JVM.computeOffset(JVM.size_tSize,DIRECT_ALLOCATED_OFFSET+JVM.size_tSize);
    public static final long FAILURE_USED_OFFSET=JVM.computeOffset(JVM.size_tSize,NUM_DIRECT_ALLOCATED_OFFSET+JVM.size_tSize);
    public static final long FAILURE_WASTE_OFFSET=JVM.computeOffset(JVM.size_tSize,FAILURE_USED_OFFSET+JVM.size_tSize);
    public static final int SIZE= (int) JVM.computeOffset(PLABStats.maxSize,FAILURE_WASTE_OFFSET+JVM.size_tSize);
    public G1EvacStats(long addr) {
        super(addr);
    }
}
