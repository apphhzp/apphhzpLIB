package apphhzp.lib.hotspot.utilities;

public class BitMapView extends BitMap{
    public BitMapView(long addr) {
        super(addr);
    }

    @Override
    public String toString() {
        return "BitMapView@0x"+Long.toHexString(this.address);
    }
}
