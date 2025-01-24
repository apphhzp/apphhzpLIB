package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public class G1RegionsOnNodes extends JVMObject{
    public static final long COUNT_PER_NODE_OFFSET=JVM.oopSize;
    public static final long NUMA_OFFSET=JVM.oopSize*2L;
    public static final int SIZE= JVM.oopSize*3;
    public G1RegionsOnNodes(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "G1RegionsOnNodes@0x"+Long.toHexString(this.address);
    }
}
