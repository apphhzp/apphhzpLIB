package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.oop.ObjArrayOopDesc;
import apphhzp.lib.hotspot.oops.oop.OopDesc;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class ObjArrayKlass extends ArrayKlass{
    public static final Type TYPE= JVM.type("ObjArrayKlass");
    public static final int SIZE=TYPE.size;
    public static final long ELEMENT_KLASS_OFFSET=TYPE.offset("_element_klass");
    public static final long BOTTOM_KLASS_OFFSET=TYPE.offset("_bottom_klass");
    protected ObjArrayKlass(long addr) {
        super(addr);
    }

    public Klass bottom_klass(){
        return Klass.getOrCreate(unsafe.getAddress(this.address+BOTTOM_KLASS_OFFSET));
    }

    public Klass element_klass(){
        return Klass.getOrCreate(unsafe.getAddress(this.address+ELEMENT_KLASS_OFFSET));
    }

    @Override
    public long oopSize(OopDesc oop) {
        if (!(oop instanceof ObjArrayOopDesc)){
            throw new IllegalArgumentException("must be object array");
        }
        return ((ObjArrayOopDesc) oop).objectSize();
    }

    @Override
    public String internal_name() {
        return this.external_name();
    }

    @Override
    public String toString() {
        return "Obj"+super.toString();
    }
}
