package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.utilities.BasicType;

public class ObjArrayOopDesc extends ArrayOopDesc{
    public static final Type TYPE= JVM.type("objArrayOopDesc");
    public static final int SIZE=TYPE.size;
    protected ObjArrayOopDesc(long addr) {
        super(addr);
    }

    public long objectSize() {
        return objectSize(this.getLength());
    }

    public static long objectSize(int length) {
        // This returns the object size in HeapWords.
        long asz = array_size(length);
        long osz = JVM.alignObjectSize(ArrayOopDesc.headerSize(BasicType.T_OBJECT) + asz);
        return (int)osz;
    }

    private static long array_size(int length) {
        long OopsPerHeapWord = JVM.oopSize/JVM.heapOopSize;
        long res = ((int)length + OopsPerHeapWord - 1)/OopsPerHeapWord;
        return res*JVM.oopSize;
    }

    @Override
    public String toString() {
        return "objArrayOopDesc@0x"+Long.toHexString(this.address);
    }
}
