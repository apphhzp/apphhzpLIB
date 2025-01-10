package apphhzp.lib.hotspot.gc.g1;


public interface LiveRegionsClosure {
    void doLiveRegions(LiveRegionsProvider lrp);
}
