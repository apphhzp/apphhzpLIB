package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.oops.oop.OopDesc;

public class InstanceMirrorKlass extends InstanceKlass{
    public static final Type TYPE= JVM.type("InstanceMirrorKlass");
    public static final int SIZE=TYPE.size;
    protected InstanceMirrorKlass(long addr) {
        super(addr);
    }

    @Override
    public long oopSize(OopDesc oop) {
        return JavaClasses.Class.getOopSizeRaw(oop.getObject());
    }

    @Override
    public String toString() {
        return "InstanceMirrorKlass("+this.getName()+")@0x"+Long.toHexString(this.address);
    }
}
