package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.memory.MemRegion;

import java.util.ArrayList;
import java.util.List;

import static apphhzp.lib.ClassHelper.unsafe;

public class HeapRegion extends JVMObject implements LiveRegionsProvider {
    public static final Type TYPE= JVM.type("HeapRegion");
    public static final int SIZE= TYPE.size;
    public static final long _LogOfHRGrainBytes_ADDRESS=TYPE.global("LogOfHRGrainBytes");
    public static final long _GrainBytes_ADDRESS=TYPE.global("GrainBytes");
    public static final int LogOfHRGrainBytes= unsafe.getInt(_LogOfHRGrainBytes_ADDRESS);
    public static final long GrainBytes=JVM.getSizeT(_GrainBytes_ADDRESS);
    public static final long BOTTOM_OFFSET=TYPE.offset("_bottom");
    public static final long END_OFFSET=TYPE.offset("_end");
    public static final long TOP_OFFSET=TYPE.offset("_top");
    public static final long COMPACTION_TOP_OFFSET=TYPE.offset("_compaction_top");
    public static final long TYPE_OFFSET=TYPE.offset("_type");
    public final HeapRegionType type=new HeapRegionType(this.address+TYPE_OFFSET);
    public HeapRegion(long addr) {
        super(addr);
    }

    public long bottom(){
        return unsafe.getAddress(this.address+BOTTOM_OFFSET);
    }
    public long end(){
        return unsafe.getAddress(this.address+END_OFFSET);
    }
    public long top(){
        return unsafe.getAddress(this.address+TOP_OFFSET);
    }
    public void setTop(long value) {
        unsafe.putAddress(this.address+TOP_OFFSET, value);
    }

    public long compactionTop(){
        return unsafe.getAddress(this.address+COMPACTION_TOP_OFFSET);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setCompactionTop(long compaction_top) {
        unsafe.putAddress(this.address+COMPACTION_TOP_OFFSET, compaction_top);
    }
    public long capacity()    { return this.end()-this.bottom();}
    public long used() { return this.top()-this.bottom();}
    public long free() { return this.end()-this.top();}

    @Override
    public List<MemRegion> getLiveRegions() {
        List<MemRegion> res = new ArrayList<>();
        res.add(MemRegion.createLimitFake(bottom(), top()));
        return res;
    }
}
