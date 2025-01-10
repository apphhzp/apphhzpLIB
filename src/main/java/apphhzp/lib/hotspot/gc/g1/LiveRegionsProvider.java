package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.hotspot.memory.MemRegion;

import java.util.List;

public interface LiveRegionsProvider {
    List<MemRegion> getLiveRegions();
}
