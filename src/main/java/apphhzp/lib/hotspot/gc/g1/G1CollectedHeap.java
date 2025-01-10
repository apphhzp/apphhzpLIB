package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.gc.CollectedHeap;

import java.util.Iterator;

public class G1CollectedHeap extends CollectedHeap {
    public static final Type TYPE= JVM.type("G1CollectedHeap");
    public static final int SIZE=TYPE.size;
    public static final long OLD_SET_OFFSET=TYPE.offset("_old_set");
    public static final long ARCHIVE_SET_OFFSET=TYPE.offset("_archive_set");
    public static final long HUMONGOUS_SET_OFFSET=TYPE.offset("_humongous_set");
    public static final long HRM_OFFSET=TYPE.offset("_hrm");
    public static final long SUMMARY_BYTES_USED_OFFSET=TYPE.offset("_summary_bytes_used");
    public static final long G1MM_OFFSET=TYPE.offset("_g1mm");
    public final HeapRegionSetBase oldSet=new HeapRegionSetBase(this.address+OLD_SET_OFFSET);
    public final HeapRegionSetBase archiveSet=new HeapRegionSetBase(this.address+ARCHIVE_SET_OFFSET);
    public final HeapRegionSetBase humongousSet=new HeapRegionSetBase(this.address+HUMONGOUS_SET_OFFSET);
    public final HeapRegionManager hrm=new HeapRegionManager(this.address+HRM_OFFSET);
    public G1CollectedHeap(long addr) {
        super(addr);
    }

    public long getSummaryBytesUsed(){
        return JVM.getSizeT(this.address+SUMMARY_BYTES_USED_OFFSET);
    }

    public void setSummaryBytesUsed(long bytes){
        JVM.putSizeT(this.address+SUMMARY_BYTES_USED_OFFSET, bytes);
    }

    public Iterator<HeapRegion> heapRegionIterator() {
        return hrm.heapRegionIterator();
    }

    @Override
    public void liveRegionsIterate(LiveRegionsClosure closure) {
        Iterator<HeapRegion> iter = hrm.regions.heapRegionIterator(hrm.regions.length());
        while (iter.hasNext()) {
            HeapRegion hr = iter.next();
            closure.doLiveRegions(hr);
        }
    }

    public void heapRegionIterate(HeapRegionClosure closure){
        Iterator<HeapRegion> iter = hrm.regions.heapRegionIterator(hrm.regions.length());
        while (iter.hasNext()) {
            HeapRegion hr = iter.next();
            closure.doHeapRegion(hr);
        }
    }
}
