package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.JVMObject;

public class ThreadShadow extends JVMObject {
    public ThreadShadow(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "ThreadShadow@0x"+Long.toHexString(this.address);
    }
}
