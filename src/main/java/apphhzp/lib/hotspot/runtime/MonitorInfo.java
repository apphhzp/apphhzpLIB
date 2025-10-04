package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.oops.oop.Oop;

// A MonitorInfo is a ResourceObject that describes a the pair:
// 1) the owner of the monitor
// 2) the monitor lock
public class MonitorInfo {

    private Oop _owner; // the object owning the monitor
    private BasicLock _lock;
    private Oop _owner_klass; // klass (mirror) if owner was scalar replaced
    private boolean       _eliminated;
    private boolean       _owner_is_scalar_replaced;
}
