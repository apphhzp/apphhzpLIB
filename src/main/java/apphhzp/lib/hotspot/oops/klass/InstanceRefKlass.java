package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

public class InstanceRefKlass extends InstanceKlass{
    public static final Type TYPE= JVM.type("InstanceRefKlass");
    public static final int SIZE=TYPE.size;
    protected InstanceRefKlass(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "InstanceRefKlass("+this.name()+")@0x"+Long.toHexString(this.address);
    }
}
