package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public class G1ConcurrentMark extends JVMObject {
    public static final long CM_THREAD_OFFSET=0L;
    public static final long G1H_OFFSET=JVM.computeOffset(JVM.oopSize,CM_THREAD_OFFSET+JVM.oopSize);
    //public static final long MARK_BITMAP_1_OFFSET=JVM.computeOffset(,G1H_OFFSET+JVM.oopSize);
    public G1ConcurrentMark(long addr) {
        super(addr);
    }
}
