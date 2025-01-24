package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.gc.CollectedHeap;
import apphhzp.lib.hotspot.gc.PLABStats;
import apphhzp.lib.hotspot.gc.PreservedMarksSet;
import apphhzp.lib.hotspot.oops.oop.Oop;

import java.util.Iterator;

import static apphhzp.lib.ClassHelper.unsafe;

public class G1CollectedHeap extends CollectedHeap {
    public static final Type TYPE= JVM.type("G1CollectedHeap");
    public static final int SIZE=TYPE.size;
    public static final long OLD_SET_OFFSET=TYPE.offset("_old_set");
    public static final long ARCHIVE_SET_OFFSET=TYPE.offset("_archive_set");
    public static final long HUMONGOUS_SET_OFFSET=TYPE.offset("_humongous_set");
    public static final long HRM_OFFSET=TYPE.offset("_hrm");
    public static final long ALLOCATOR_OFFSET=JVM.computeOffset(JVM.oopSize,HRM_OFFSET+HeapRegionManager.SIZE);
    public static final long VERIFIER_OFFSET=JVM.computeOffset(JVM.oopSize,ALLOCATOR_OFFSET+JVM.oopSize);
    public static final long SUMMARY_BYTES_USED_OFFSET=TYPE.offset("_summary_bytes_used");
//    public static final long BYTES_USED_DURING_GC_OFFSET=JVM.computeOffset(JVM.size_tSize,SUMMARY_BYTES_USED_OFFSET+JVM.size_tSize);
//    public static final long ARCHIVE_ALLOCATOR_OFFSET=JVM.computeOffset(JVM.oopSize,BYTES_USED_DURING_GC_OFFSET+JVM.size_tSize);
//    public static final long SURVIVOR_EVAC_STATS_OFFSET=JVM.computeOffset(PLABStats.maxSize,ARCHIVE_ALLOCATOR_OFFSET+JVM.oopSize);
//    public static final long OLD_EVAC_STATS_OFFSET=JVM.computeOffset(PLABStats.maxSize,SURVIVOR_EVAC_STATS_OFFSET+G1EvacStats.SIZE);
//    public static final long EXPAND_HEAP_AFTER_ALLOC_FAILURE_OFFSET=JVM.computeOffset(1,OLD_EVAC_STATS_OFFSET+G1EvacStats.SIZE);
    public static final long G1MM_OFFSET=TYPE.offset("_g1mm");
    public static final long HUMONGOUS_RECLAIM_CANDIDATES_OFFSET=JVM.computeOffset(JVM.oopSize,G1MM_OFFSET+JVM.oopSize);
    public static final long NUM_HUMONGOUS_OBJECTS_OFFSET=JVM.computeOffset(JVM.intSize,HUMONGOUS_RECLAIM_CANDIDATES_OFFSET+G1BiasedMappedArray.SIZE);
    public static final long NUM_HUMONGOUS_RECLAIM_CANDIDATES_OFFSET=JVM.computeOffset(JVM.intSize,NUM_HUMONGOUS_OBJECTS_OFFSET+JVM.intSize);
    public static final long HR_PRINTER_OFFSET=JVM.computeOffset(1,NUM_HUMONGOUS_RECLAIM_CANDIDATES_OFFSET+JVM.intSize);
    public static final long COLLECTOR_STATE_OFFSET=JVM.computeOffset(1,HR_PRINTER_OFFSET+1);
    public static final long OLD_MARKING_CYCLES_STARTED_OFFSET=JVM.computeOffset(JVM.intSize,COLLECTOR_STATE_OFFSET+7);
    public static final long OLD_MARKING_CYCLES_COMPLETED_OFFSET=JVM.computeOffset(JVM.intSize,OLD_MARKING_CYCLES_STARTED_OFFSET+JVM.intSize);
    public static final long EDEN_OFFSET=JVM.computeOffset(Math.max(JVM.oopSize,JVM.size_tSize),OLD_MARKING_CYCLES_COMPLETED_OFFSET+JVM.intSize);
    public static final long SURVIVOR_OFFSET=JVM.computeOffset(Math.max(JVM.oopSize,JVM.size_tSize),EDEN_OFFSET+G1EdenRegions.SIZE);
    public static final long GC_TIMER_STW_OFFSET=JVM.computeOffset(JVM.oopSize,SURVIVOR_OFFSET+G1SurvivorRegions.SIZE);
    public static final long GC_TRACER_STW_OFFSET=JVM.computeOffset(JVM.oopSize,GC_TIMER_STW_OFFSET+JVM.oopSize);
    public static final long POLICY_OFFSET=JVM.computeOffset(JVM.oopSize,GC_TRACER_STW_OFFSET+JVM.oopSize);
    public static final long HEAP_SIZING_POLICY_OFFSET=JVM.computeOffset(JVM.oopSize,POLICY_OFFSET+JVM.oopSize);
    public static final long COLLECTION_SET_OFFSET=JVM.computeOffset(Math.max(JVM.oopSize,Math.max(JVM.size_tSize,JVM.doubleSize)),HEAP_SIZING_POLICY_OFFSET+JVM.oopSize);
    public static final long HOT_CARD_CACHE_OFFSET=JVM.computeOffset(JVM.oopSize,COLLECTION_SET_OFFSET+G1CollectionSet.SIZE);
    public static final long REM_SET_CACHE_OFFSET=JVM.computeOffset(JVM.oopSize,HOT_CARD_CACHE_OFFSET+JVM.oopSize);
    public static final long CM_OFFSET=JVM.computeOffset(JVM.oopSize,REM_SET_CACHE_OFFSET+JVM.oopSize);
    public static final long CM_THREAD_OFFSET=JVM.computeOffset(JVM.oopSize,CM_OFFSET+JVM.oopSize);
    public static final long CR_OFFSET=JVM.computeOffset(JVM.oopSize,CM_THREAD_OFFSET+JVM.oopSize);
    public static final long TASK_QUEUES_OFFSET=JVM.computeOffset(JVM.oopSize,CR_OFFSET+JVM.oopSize);
    public static final long NUM_REGIONS_FAILED_EVACUATION_OFFSET=JVM.computeOffset(JVM.intSize,TASK_QUEUES_OFFSET+JVM.oopSize);
    public static final long REGIONS_FAILED_EVACUATION_OFFSET=JVM.computeOffset(JVM.oopSize,NUM_REGIONS_FAILED_EVACUATION_OFFSET+JVM.intSize);
    public static final long EVACUATION_FAILED_INFO_ARRAY_OFFSET=JVM.computeOffset(JVM.oopSize,REGIONS_FAILED_EVACUATION_OFFSET+JVM.oopSize);
    public static final long PRESERVED_MARKS_SET_OFFSET=JVM.computeOffset(JVM.oopSize,EVACUATION_FAILED_INFO_ARRAY_OFFSET+JVM.oopSize);
    //PRODUCT_ONLY
    public static final long EVACUATION_FAILURE_ALOT_FOR_CURRENT_GC_OFFSET=JVM.computeOffset(1,PRESERVED_MARKS_SET_OFFSET+ PreservedMarksSet.SIZE);
    public static final long EVACUATION_FAILURE_ALOT_GC_NUMBER_OFFSET=JVM.computeOffset(JVM.size_tSize,EVACUATION_FAILURE_ALOT_FOR_CURRENT_GC_OFFSET+1);
    public static final long EVACUATION_FAILURE_ALOT_COUNT_OFFSET=JVM.computeOffset(JVM.size_tSize,EVACUATION_FAILURE_ALOT_GC_NUMBER_OFFSET+JVM.size_tSize);
    //END
    public static final long REF_PROCESSOR_STW_OFFSET=JVM.computeOffset(JVM.oopSize, JVM.product ? (PRESERVED_MARKS_SET_OFFSET + PreservedMarksSet.SIZE) : (EVACUATION_FAILURE_ALOT_COUNT_OFFSET + JVM.size_tSize));
    public static final long IS_ALIVE_CLOSURE_STW_OFFSET=JVM.computeOffset(JVM.oopSize,REF_PROCESSOR_STW_OFFSET+JVM.oopSize);
    public static final long IS_SUBJECT_TO_DISCOVERY_STW_OFFSET=JVM.computeOffset(JVM.oopSize,IS_ALIVE_CLOSURE_STW_OFFSET+JVM.oopSize*2L);
    public static final long REF_PROCESSOR_CM_OFFSET=JVM.computeOffset(JVM.oopSize,IS_SUBJECT_TO_DISCOVERY_STW_OFFSET+JVM.oopSize*2L);
    public static final long IS_ALIVE_CLOSURE_CM_OFFSET=JVM.computeOffset(JVM.oopSize,REF_PROCESSOR_CM_OFFSET+JVM.oopSize);
    public static final long IS_SUBJECT_TO_DISCOVERY_CM_OFFSET=JVM.computeOffset(JVM.oopSize,IS_ALIVE_CLOSURE_CM_OFFSET+JVM.oopSize*2L);
    public static final long REGION_ATTR_OFFSET=JVM.computeOffset(Math.max(JVM.size_tSize,JVM.oopSize),IS_SUBJECT_TO_DISCOVERY_CM_OFFSET+JVM.oopSize*2L);

    static{
        if (JVM.computeOffset(Math.max(JVM.oopSize,Math.max(JVM.size_tSize,JVM.doubleSize)), G1CollectedHeap.REGION_ATTR_OFFSET+G1BiasedMappedArray.SIZE)!=SIZE
                ||JVM.computeOffset(JVM.size_tSize,VERIFIER_OFFSET+JVM.oopSize)!=SUMMARY_BYTES_USED_OFFSET
                ){//||JVM.computeOffset(JVM.oopSize,EXPAND_HEAP_AFTER_ALLOC_FAILURE_OFFSET+1)!=G1MM_OFFSET
            throw new InternalError("Wrong offset for G1CollectedHeap");
        }
    }

    public final HeapRegionSetBase oldSet=new HeapRegionSetBase(this.address+OLD_SET_OFFSET);
    public final HeapRegionSetBase archiveSet=new HeapRegionSetBase(this.address+ARCHIVE_SET_OFFSET);
    public final HeapRegionSetBase humongousSet=new HeapRegionSetBase(this.address+HUMONGOUS_SET_OFFSET);
    public final HeapRegionManager hrm=new HeapRegionManager(this.address+HRM_OFFSET);
    private static G1CollectedHeap heap;
    private G1ConcurrentMark cmCache;
    public G1CollectedHeap(long addr) {
        super(addr);
        heap=this;
    }

    public static G1CollectedHeap heap(){
        return heap;
    }

    public long getSummaryBytesUsed(){
        return JVM.getSizeT(this.address+SUMMARY_BYTES_USED_OFFSET);
    }

    public void setSummaryBytesUsed(long bytes){
        JVM.putSizeT(this.address+SUMMARY_BYTES_USED_OFFSET, bytes);
    }

    public G1ConcurrentMark cm(){
        long add= unsafe.getAddress(this.address+CM_OFFSET);
        if (!isEqual(this.cmCache,add)){
            this.cmCache=new G1ConcurrentMark(add);
        }
        return this.cmCache;
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

    public boolean is_obj_dead(Oop oop, HeapRegion heapRegion) {
        throw new UnsupportedOperationException();
        //return heapRegion.is_obj_dead(oop, _cm->prev_mark_bitmap());
    }
}
