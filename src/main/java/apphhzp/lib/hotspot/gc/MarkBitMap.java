package apphhzp.lib.hotspot.gc;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.memory.MemRegion;
import apphhzp.lib.hotspot.utilities.BitMap;
import apphhzp.lib.hotspot.utilities.BitMapView;

public class MarkBitMap extends JVMObject {
    public static final long COVERED_OFFSET=JVM.oopSize;
    public static final long SHIFTER_OFFSET= JVM.computeOffset(JVM.intSize,COVERED_OFFSET+MemRegion.SIZE);
    public static final long BM_OFFSET=JVM.computeOffset(Math.max(JVM.oopSize,JVM.size_tSize),SHIFTER_OFFSET+JVM.intSize);
    public static final int SIZE= (int) JVM.computeOffset(Math.max(JVM.oopSize,JVM.size_tSize),BM_OFFSET+ BitMap.SIZE);
    public final BitMapView bm=new BitMapView(this.address+BM_OFFSET);
    public MarkBitMap(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "MarkBitMap@0x"+Long.toHexString(this.address);
    }
}
