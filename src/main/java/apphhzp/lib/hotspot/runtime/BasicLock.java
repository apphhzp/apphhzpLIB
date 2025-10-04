package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class BasicLock extends JVMObject {
    public static final Type TYPE= JVM.type("BasicLock");
    public static final int SIZE=TYPE.size;
    public static final long DISPLACED_HEADER_OFFSET=TYPE.offset("_displaced_header");
    public BasicLock(long addr) {
        super(addr);
    }
}
