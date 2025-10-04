package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

public class InstanceClassLoaderKlass extends InstanceKlass{
    public static final Type TYPE= JVM.type("InstanceClassLoaderKlass");
    public static final int SIZE=TYPE.size;
    protected InstanceClassLoaderKlass(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "InstanceClassLoaderKlass("+this.name()+")@0x"+Long.toHexString(this.address);
    }
}
