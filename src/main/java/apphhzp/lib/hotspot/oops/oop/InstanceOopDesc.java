package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

public class InstanceOopDesc extends OopDesc{
    public static final Type TYPE= JVM.type("instanceOopDesc");
    public static final int SIZE= TYPE.size;;

    protected InstanceOopDesc(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "instanceOopDesc@0x"+Long.toHexString(this.address);
    }
}
