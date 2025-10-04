package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.gc.ThreadLocalAllocBuffer;

import java.util.HashMap;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class Thread extends ThreadShadow{
    public static final Type TYPE= JVM.type("Thread");
    public static final int SIZE=TYPE.size;
    public static final long ACTIVE_HANDLES_OFFSET=TYPE.offset("_active_handles");
    public static final long TLAB_OFFSET=TYPE.offset("_tlab");
    public final ThreadLocalAllocBuffer tlab=new ThreadLocalAllocBuffer(this.address+TLAB_OFFSET);
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
    private JNIHandleBlock activeHandlesCache;

    public Thread(long addr) {
        super(addr);
    }

    public JNIHandleBlock active_handles(){
        long addr=unsafe.getAddress(this.address+ACTIVE_HANDLES_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.activeHandlesCache, addr)) {
            this.activeHandlesCache = new JNIHandleBlock(addr);
        }
        return this.activeHandlesCache;
    }

    @Override
    public String toString() {
        return "Thread@0x"+Long.toHexString(this.address);
    }
}
