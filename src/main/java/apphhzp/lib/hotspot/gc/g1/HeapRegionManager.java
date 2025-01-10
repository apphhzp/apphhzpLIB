package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.util.Iterator;

public class HeapRegionManager extends JVMObject {
    public static final Type TYPE= JVM.type("HeapRegionManager");
    public static final int SIZE=TYPE.size;
    public static final long REGIONS_OFFSET=TYPE.offset("_regions");
    public final G1HeapRegionTable regions=new G1HeapRegionTable(this.address+REGIONS_OFFSET);
    public HeapRegionManager(long addr) {
        super(addr);
    }

    public long capacity() {
        return length() * HeapRegion.GrainBytes;
    }

    public long length(){
        return regions.length();
    }

    public long heapBottom() { return regions.bottomAddressMapped(); }
    public long heapEnd() {return regions.endAddressMapped(); }

    public HeapRegion addrToRegion(long addr) {
        if (addr>=this.heapEnd()){
            throw new IllegalArgumentException("addr: 0x"+Long.toHexString(addr)+" end: 0x"+Long.toHexString(this.heapEnd()));
        }
        if (addr<this.heapBottom()){
            throw new IllegalArgumentException("addr: 0x"+Long.toHexString(addr)+" bottom: 0x"+Long.toHexString(this.heapBottom()));
        }
        return regions.getByAddress(addr);
    }

//    public HeapRegion at(long index){
//        assert(is_available(index), "pre-condition");
//        HeapRegion* hr = regions.getByIndex(index);
//        assert(hr != NULL, "sanity");
//        assert(hr->hrm_index() == index, "sanity");
//        return hr;
//    }
//
//    public HeapRegion at_or_null(long index)  {
//        if (!is_available(index)) {
//            return NULL;
//        }
//        HeapRegion* hr = regions.getByIndex(index);
//        assert(hr != NULL, "All available regions must have a HeapRegion but index %u has not.", index);
//        assert(hr->hrm_index() == index, "sanity");
//        return hr;
//    }

    public Iterator<HeapRegion> heapRegionIterator() {
        return regions.heapRegionIterator(this.length());
    }
}
