package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public class G1EdenRegions extends JVMObject {
    public static final long LENGTH_OFFSET=0L;
    public static final long USED_BYTES_OFFSET= JVM.computeOffset(JVM.size_tSize,LENGTH_OFFSET+JVM.intSize);
    public static final long REGIONS_ON_NODE_OFFSET=JVM.computeOffset(JVM.oopSize,USED_BYTES_OFFSET+JVM.size_tSize);
    public static final int SIZE= (int) JVM.computeOffset(Math.max(JVM.size_tSize,JVM.oopSize),REGIONS_ON_NODE_OFFSET+G1RegionsOnNodes.SIZE);
    public G1EdenRegions(long addr) {
        super(addr);
    }
}
