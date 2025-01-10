package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.oop.ObjArrayOopDesc;
import apphhzp.lib.hotspot.oops.oop.OopDesc;

public class ObjArrayKlass extends ArrayKlass{
    public static final Type TYPE= JVM.type("ObjArrayKlass");
    public static final int SIZE=TYPE.size;
    protected ObjArrayKlass(long addr) {
        super(addr);
    }

    @Override
    public long oopSize(OopDesc oop) {
        if (!(oop instanceof ObjArrayOopDesc)){
            throw new IllegalArgumentException("must be object array");
        }
        return ((ObjArrayOopDesc) oop).objectSize();
    }

    @Override
    public String toString() {
        return "Obj"+super.toString();
    }
}
