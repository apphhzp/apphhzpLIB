package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.klass.TypeArrayKlass;

public class TypeArrayOopDesc extends ArrayOopDesc{
//    public static final Type TYPE= JVM.type("typeArrayOop");
//    public static final int SIZE= TYPE.size;
    protected TypeArrayOopDesc(long addr) {
        super(addr);
    }

    public long objectSize(TypeArrayKlass klass){
        return object_size(klass.getLayout(),this.getLength());
    }

    public static long object_size(int lh, int length) {
        int instance_header_size = Klass.LayoutHelper.headerSize(lh);
        int element_shift = Klass.LayoutHelper.log2ElementSize(lh);
        long size_in_bytes = length;
        size_in_bytes <<= element_shift;
        size_in_bytes += instance_header_size;
        long size_in_words = ((size_in_bytes + (JVM.oopSize-1)));
        return JVM.alignObjectSize(size_in_words);
    }

    @Override
    public String toString() {
        return "typeArrayOop@0x"+Long.toHexString(this.address);
    }
}
