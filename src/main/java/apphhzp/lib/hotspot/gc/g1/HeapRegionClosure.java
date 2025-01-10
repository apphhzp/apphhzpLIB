package apphhzp.lib.hotspot.gc.g1;


public interface HeapRegionClosure {
    void doHeapRegion(HeapRegion hr);
}
