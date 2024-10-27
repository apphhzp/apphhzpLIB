package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.gc.ThreadLocalAllocBuffer;

import java.util.HashMap;

public class Thread extends ThreadShadow{
    public static final Type TYPE= JVM.type("Thread");
    public static final int SIZE=TYPE.size;
    public static final long TLAB_OFFSET=TYPE.offset("_tlab");
    private final ThreadLocalAllocBuffer tlabCache;
    private static final HashMap<Long, Thread> CACHE = new HashMap<>();
    public static Thread getOrCreate(long addr) {
        if (addr == 0L) {
            throw new IllegalArgumentException("Pointer is NULL(0).");
        }
        if (CACHE.containsKey(addr)) {
            return CACHE.get(addr);
        }
        Thread re = new Thread(addr);
        CACHE.put(addr, re);
        return re;
    }

    public static void clearCacheMap(){
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public Thread(long addr) {
        super(addr);
        tlabCache=new ThreadLocalAllocBuffer(this.address+TLAB_OFFSET);
    }

    public ThreadLocalAllocBuffer getTLAB(){
        return this.tlabCache;
    }

    @Override
    public String toString() {
        return "Thread@0x"+Long.toHexString(this.address);
    }
}
