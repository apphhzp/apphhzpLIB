package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.hotspot.gc.MarkBitMap;

public class G1CMBitMap extends MarkBitMap {
    /*G1CMBitMapMappingChangedListener _listener;*/
    public G1CMBitMap(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "G1CMBitMap@0x"+Long.toHexString(this.address);
    }
}
