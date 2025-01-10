package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.oops.oop.TypeArrayOopDesc;

public class TypeArrayKlass extends ArrayKlass{
    public static final Type TYPE= JVM.type("TypeArrayKlass");
    public static final int SIZE= TYPE.size;
    protected TypeArrayKlass(long addr) {
        super(addr);
    }

    @Override
    public long oopSize(OopDesc oop) {
        if (!(oop instanceof TypeArrayOopDesc typeArrayOopDesc)){
            throw new IllegalArgumentException("must be a type array");
        }
        return typeArrayOopDesc.getObjectSize();
    }

    @Override
    public String toString() {
        return "Type"+super.toString();
    }
}
