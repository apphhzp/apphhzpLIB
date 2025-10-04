package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.method.Method;

public class OopMapCache extends JVMObject {
    private static final int  _size        = 32,     // Use fixed size for now
            _probe_depth = 3       // probe depth in case of collisions
    ;

    public static final long ARRAY_OFFSET=0;
    public OopMapCache(long addr) {
        super(addr);
    }

    public static void compute_one_oop_map(Method method, int bci, InterpreterOopMap entry) {
        // Due to the invariants above it's tricky to allocate a temporary OopMapCacheEntry on the stack
        OopMapCacheEntry tmp = new OopMapCacheEntry();
        tmp.initialize();
        tmp.fill(method, bci);
        entry.resource_copy(tmp);
        tmp.flush();
    }
}
