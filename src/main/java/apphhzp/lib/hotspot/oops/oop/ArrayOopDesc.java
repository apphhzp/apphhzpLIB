package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.memory.Universe;

public class ArrayOopDesc extends OopDesc{
    public static final Type TYPE= JVM.type("arrayOopDesc");
    public static final int SIZE=TYPE.size;
    public static final long LENGTH_OFFSET=JVM.usingCompressedClassPointers?SIZE-JVM.intSize:SIZE;
    public static final long headerSize=JVM.usingCompressedClassPointers?SIZE:JVM.alignUp(SIZE+JVM.intSize,JVM.oopSize);
    protected ArrayOopDesc(long addr) {
        super(addr);
    }

    public int getLength() {
        return ClassHelperSpecial.unsafe.getInt(this.address+LENGTH_OFFSET);
    }

    public static long headerSize(int type) {
        if (Universe.elementTypeShouldBeAligned(type)) {
            return JVM.alignObjectSize(headerSize)/ JVM.oopSize;
        } else {
            return headerSize/JVM.oopSize;
        }
    }

    public static long baseOffsetInBytes(int type) {
        return headerSize(type) *JVM.oopSize;
    }

    @Override
    public String toString() {
        return "arrayOopDesc@0x"+Long.toHexString(this.address);
    }
}
