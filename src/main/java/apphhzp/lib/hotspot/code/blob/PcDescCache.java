package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.PcDesc;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class PcDescCache extends JVMObject {
    public static final Type TYPE= JVM.type("PcDescCache");
    public static final int SIZE= TYPE.size;
    public static final int cache_size=4;
    public static final long PC_DESCS_OFFSET=0;
    static {
        JVM.assertOffset(SIZE,JVM.computeOffset(JVM.oopSize,PC_DESCS_OFFSET+ (long) JVM.oopSize *cache_size));
    }
    //typedef PcDesc* PcDescPtr;
    public PcDescCache(long addr) {
        super(addr);
    }
    public void reset_to(PcDesc initial_pc_desc) {
        if (initial_pc_desc == null) {
            unsafe.putAddress(this.address+PC_DESCS_OFFSET,0L);// native method; no PcDescs at all
            return;
        }
        // reset the cache by filling it with benign (non-null) values
        if (!(initial_pc_desc.pc_offset() < 0)){
            throw new RuntimeException("must be sentinel");
        }
        for (int i = 0; i < cache_size; i++){
            unsafe.putAddress(this.address+PC_DESCS_OFFSET+ (long) i *JVM.oopSize,initial_pc_desc.address);
        }
    }

    public PcDesc find_pc_desc(int pc_offset, boolean approximate) {
        // Note: one might think that caching the most recently
        // read value separately would be a win, but one would be
        // wrong.  When many threads are updating it, the cache
        // line it's in would bounce between caches, negating
        // any benefit.

        // In order to prevent race conditions do not load cache elements
        // repeatedly, but use a local copy:
        PcDesc res;

        // Step one:  Check the most recently added value.
        {
            long addr=unsafe.getAddress(this.address+PC_DESCS_OFFSET);
            if (addr==0L){
                // native method; no PcDescs at all
                return null;
            }
            res = new PcDesc(addr);
        }
        if (NMethod.match_desc(res, pc_offset, approximate)) {
            return res;
        }

        // Step two:  Check the rest of the LRU cache.
        for (int i = 1; i < cache_size; ++i) {
            res = new PcDesc(unsafe.getAddress(this.address+PC_DESCS_OFFSET+ (long) i *JVM.oopSize));
            if (res.pc_offset() < 0) break;  // optimization: skip empty cache
            if (NMethod.match_desc(res, pc_offset, approximate)) {
                return res;
            }
        }
        // Report failure.
        return null;
    }

    public void add_pc_desc(PcDesc pc_desc) {
        // Update the LRU cache by shifting pc_desc forward.
        for (int i = 0; i < cache_size; i++)  {
            PcDesc next = new PcDesc(unsafe.getAddress(this.address+PC_DESCS_OFFSET+ (long) i *JVM.oopSize));
            unsafe.putAddress(this.address+PC_DESCS_OFFSET+ (long) i *JVM.oopSize,pc_desc.address);
            pc_desc = next;
        }
    }

    public PcDesc last_pc_desc(){
        long addr= unsafe.getAddress(this.address+PC_DESCS_OFFSET);
        if (addr==0L){
            return null;
        }
        return new PcDesc(addr);
    }
}
