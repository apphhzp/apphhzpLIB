package apphhzp.lib.hotspot.gc;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public class PreservedMarksSet extends JVMObject {
    public static final long IN_C_HEAP_OFFSET=0L;
    public static final long NUM_OFFSET= JVM.computeOffset(JVM.intSize, IN_C_HEAP_OFFSET+1);
    public static final long STACKS_OFFSET=JVM.computeOffset(JVM.oopSize,NUM_OFFSET+JVM.intSize);
    public static final int SIZE= (int) JVM.computeOffset(JVM.oopSize,STACKS_OFFSET+JVM.oopSize);
    public PreservedMarksSet(long addr) {
        super(addr);
    }
}
