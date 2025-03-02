package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.gc.CollectedHeap;
import apphhzp.lib.hotspot.gc.g1.LiveRegionsClosure;
import apphhzp.lib.hotspot.gc.g1.LiveRegionsProvider;
import apphhzp.lib.hotspot.gc.ThreadLocalAllocBuffer;
import apphhzp.lib.hotspot.memory.MemRegion;
import apphhzp.lib.hotspot.memory.Universe;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.JavaThread;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.util.ArrayList;

public class ObjectHeap {

    public static void iterateExact(HeapVisitor visitor, final Klass k) {
        iterateLiveRegions(collectLiveRegions(), visitor, obj -> {
            Klass tk = obj.getKlass();
            // null Klass is seen sometimes!
            return (tk != null && tk.equals(k));
        });
    }

    public static void iterateSubtypes(HeapVisitor visitor, final Klass k) {
        iterateLiveRegions(collectLiveRegions(), visitor, obj -> {
            Klass tk = obj.getKlass();
            // null Klass is seen sometimes!
            return (tk != null && tk.isAssignableFrom(k));
        });
    }

    public static void iterateLiveRegions(LongList liveRegions, HeapVisitor visitor, ObjectFilter of) {
        // Summarize size
        long totalSize = 0;
        for (int i = 0; i < liveRegions.size(); i += 2) {
            long bottom = liveRegions.getLong(i);
            long top = liveRegions.getLong(i + 1);
            totalSize += top - (bottom);
        }
        visitor.prologue(totalSize);

        for (int i = 0; i < liveRegions.size(); i += 2) {
            long bottom = liveRegions.getLong(i);
            long top = liveRegions.getLong(i + 1);

            try {
                // Traverses the space from bottom to top
                long handle = bottom;

                while (handle < (top)) {
                    OopDesc obj;

                    obj = OopDesc.of(handle);
                    if (obj==OopDesc.NULL) {
                        throw new RuntimeException();
                    }
                    if (of == null || of.canInclude(obj)) {
                        if (visitor.doObj(obj)) {
                            // doObj() returns true to abort this loop.
                            break;
                        }
                    }

                    handle = handle + (obj.getObjectSize());
                }
            } catch (Throwable e) {
                // This is okay at the top of these regions
            }
        }

        visitor.epilogue();
    }

    public interface ObjectFilter {
        boolean canInclude(OopDesc obj);
    }

    private static class LiveRegionsCollector implements LiveRegionsClosure {
        LiveRegionsCollector(LongList l) {
            liveRegions = l;
        }

        @Override
        public void doLiveRegions(LiveRegionsProvider lrp) {
            for (MemRegion reg : lrp.getLiveRegions()) {
                long top = reg.end();
                long bottom = reg.start();
                if (top == 0L) {
                    throw new RuntimeException("top address in a live region should not be null");
                }
                if (bottom == 0L) {
                    throw new RuntimeException("bottom address in a live region should not be null");
                }
                liveRegions.add(top);
                liveRegions.add(bottom);
            }
        }

        private final LongList liveRegions;
    }

    public static LongList collectLiveRegions() {
        // We want to iterate through all live portions of the heap, but
        // do not want to abort the heap traversal prematurely if we find
        // a problem (like an allocated but uninitialized object at the
        // top of a generation). To do this we enumerate all generations'
        // bottom and top regions, and factor in TLABs if necessary.

        // Addresses come in pairs.
        LongList liveRegions = new LongArrayList();
        LiveRegionsCollector lrc = new LiveRegionsCollector(liveRegions);
        CollectedHeap heap = Universe.getCollectedHeap();
        heap.liveRegionsIterate(lrc);

        // If UseTLAB is enabled, snip out regions associated with TLABs'
        // dead regions. Note that TLABs can be present in any generation.

        // FIXME: consider adding fewer boundaries to live region list.
        // Theoretically only need to stop at TLAB's top and resume at its
        // end.

        if (JVM.usingTLAB) {
            ArrayList<JavaThread> threads = JavaThread.getAllJavaThreads();
            for (JavaThread thread : threads) {
                ThreadLocalAllocBuffer tlab = thread.tlab;
                if (tlab.start() != 0L) {
                    if ((tlab.top() == 0L) || (tlab.end() == 0L)) {
                        if (JVM.ENABLE_EXTRA_CHECK) {
                            throw new RuntimeException("Skipping invalid TLAB for thread");
                        }
                    } else {
                        // Go from:
                        //  - below start() to start()
                        //  - start() to top()
                        //  - end() and above
                        liveRegions.add(tlab.start());
                        liveRegions.add(tlab.start());
                        liveRegions.add(tlab.top());
                        liveRegions.add(tlab.hardEnd());
                    }
                }
            }
        }

        // Now sort live regions
        sortLiveRegions(liveRegions);
        if (liveRegions.size() % 2 != 0) {
            throw new RuntimeException("Must have even number of region boundaries");
        }
        return liveRegions;
    }

    private static void sortLiveRegions(LongList liveRegions) {
        liveRegions.sort((a1, a2) -> {
            if (a1 < a2) {
                return -1;
            } else if (a1 > a2) {
                return 1;
            }
            return 0;
        });
    }
}
