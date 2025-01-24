package apphhzp.lib.hotspot.gc;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public class AdaptiveWeightedAverage extends JVMObject {
    public static final long AVERAGE_OFFSET=0L;
    public static final long SAMPLE_COUNT_OFFSET=JVM.computeOffset(JVM.unsignedSize,AVERAGE_OFFSET+JVM.floatSize);
    public static final long WEIGHT_OFFSET=JVM.computeOffset(JVM.unsignedSize,SAMPLE_COUNT_OFFSET+JVM.unsignedSize);
    public static final long IS_OLD_OFFSET=JVM.computeOffset(1,WEIGHT_OFFSET+JVM.unsignedSize);
    public static final long LAST_SAMPLE_OFFSET=JVM.computeOffset(JVM.floatSize,IS_OLD_OFFSET+1);
    public static final int SIZE= (int) JVM.computeOffset(Math.max(JVM.unsignedSize,JVM.floatSize),LAST_SAMPLE_OFFSET+JVM.floatSize);
    public AdaptiveWeightedAverage(long addr) {
        super(addr);
    }
}
