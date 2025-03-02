package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class Frame extends JVMObject {
    public static final Type TYPE= JVM.type("frame");
    public static final int SIZE=TYPE.size;
    public static final long SP_OFFSET=0;
    public static final long PC_OFFSET=JVM.computeOffset(JVM.oopSize,SP_OFFSET+JVM.oopSize);
    public static final long CB_OFFSET=JVM.computeOffset(JVM.oopSize,PC_OFFSET+JVM.oopSize);
    public static final long DEOPT_STATE_OFFSET=JVM.computeOffset(JVM.intSize,CB_OFFSET+JVM.oopSize);
    public static final int
        not_deoptimized=0,
        is_deoptimized=1,
        unknown=2;
    public Frame(long addr) {
        super(addr);
    }
}
