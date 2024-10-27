package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.JVMObject;

public abstract class Stub extends JVMObject {
    public Stub(long addr) {
        super(addr);
    }

    public abstract int getSize();
}
