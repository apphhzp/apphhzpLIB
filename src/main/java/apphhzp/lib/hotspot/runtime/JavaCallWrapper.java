package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class JavaCallWrapper extends JVMObject {
    public static final Type TYPE= JVM.type("JavaCallWrapper");
    public static final int SIZE= TYPE.size;
    public static final long ANCHOR_OFFSET=TYPE.offset("_anchor");
    public final JavaFrameAnchor anchor;
    public JavaCallWrapper(long addr) {
        super(addr);
        anchor=JavaFrameAnchor.create(addr+ANCHOR_OFFSET);
    }
    public boolean is_first_frame() {
        return anchor.last_Java_sp() == 0L;
    }
}
