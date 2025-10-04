package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.code.blob.PcDescCache;
import apphhzp.lib.hotspot.util.RawCType;

public class PcDescContainer extends JVMObject {
    public final PcDescCache _pc_desc_cache;
    public PcDescContainer(long addr) {
        super(addr);
        _pc_desc_cache = new PcDescCache(addr);
    }
    public void    reset_to(PcDesc initial_pc_desc){
        _pc_desc_cache.reset_to(initial_pc_desc);
    }

    public PcDesc find_pc_desc(@RawCType("address")long pc, boolean approximate,@RawCType("PcDescSearch&")PcDescSearch search) {
        @RawCType("address")long base_address = search.code_begin();
        PcDesc desc = _pc_desc_cache.last_pc_desc();
        if (desc != null && desc.pc_offset() == pc - base_address) {
            return desc;
        }
        return find_pc_desc_internal(pc, approximate, search);
    }

    // Finds a PcDesc with real-pc equal to "pc"
    public PcDesc find_pc_desc_internal(@RawCType("address")long pc, boolean approximate,@RawCType("PcDescSearch&") PcDescSearch search) {
        @RawCType("address")long base_address = search.code_begin();
        if ((Long.compareUnsigned(pc,base_address)  < 0) ||
                (pc - base_address) >=  PcDesc.upper_offset_limit) {
            return null;  // PC is wildly out of range
        }
        int pc_offset = (int) (pc - base_address);
        // Check the PcDesc cache if it contains the desired PcDesc
        // (This as an almost 100% hit rate.)
        PcDesc res = _pc_desc_cache.find_pc_desc(pc_offset, approximate);
        if (res != null) {
            if (JVM.ENABLE_EXTRA_CHECK){
                if (!(res.equals(linear_search(search, pc_offset, approximate)))){
                    throw new RuntimeException("cache ok");
                }
            }
            return res;
        }

        // Fallback algorithm: quasi-linear search for the PcDesc
        // Find the last pc_offset less than the given offset.
        // The successor must be the required match, if there is a match at all.
        // (Use a fixed radix to avoid expensive affine pointer arithmetic.)
        PcDesc lower = search.scopes_pcs_begin();
        PcDesc upper = search.scopes_pcs_end();
        upper = new PcDesc(upper.address-PcDesc.SIZE); // exclude final sentinel
        if (lower.address >= upper.address){
            // native method; no PcDescs at all
            return null;
        }
        if (!(upper.pc_offset() >= pc_offset)){
            throw new RuntimeException("sanity");
        }
        if (!(lower.pc_offset() <  pc_offset)){
            throw new RuntimeException("sanity");
        }

        // Use the last successful return as a split point.
        PcDesc mid = _pc_desc_cache.last_pc_desc();
        if (mid.pc_offset() < pc_offset) {
            lower = mid;
        } else {
            upper = mid;
        }

        // Take giant steps at first (4096, then 256, then 16, then 1)
        final int LOG2_RADIX = 4;
        final int RADIX = (1 << LOG2_RADIX);
        for (int step = (1 << (LOG2_RADIX*3)); step > 1; step >>= LOG2_RADIX) {
            while ((mid = new PcDesc(lower.address+ (long) step *PcDesc.SIZE)).address < upper.address) {
                if (!(lower.pc_offset() <  pc_offset)){
                    throw new RuntimeException("sanity");
                }
                if (mid.pc_offset() < pc_offset) {
                    lower = mid;
                } else {
                    upper = mid;
                    break;
                }
            }
            if (!(lower.pc_offset() <  pc_offset)){
                throw new RuntimeException("sanity");
            }
        }

        // Sneak up on the value with a linear search of length ~16.
        while (true) {
            if (!(lower.pc_offset() <  pc_offset)){
                throw new RuntimeException("sanity");
            }
            mid = new PcDesc(lower.address+PcDesc.SIZE);
            if (mid.pc_offset() < pc_offset) {
                lower = mid;
            } else {
                upper = mid;
                break;
            }
        }

        if (NMethod.match_desc(upper, pc_offset, approximate)) {
            if (JVM.ENABLE_EXTRA_CHECK){
                if (!(upper.equals(linear_search(search, pc_offset, approximate)))){
                    throw new RuntimeException("search ok");
                }
            }
            return upper;
        } else {
            if (JVM.ENABLE_EXTRA_CHECK){
                if (null != linear_search(search, pc_offset, approximate)){
                    throw new RuntimeException("search ok");
                }
            }
            return null;
        }
    }
    public static PcDesc linear_search(PcDescSearch search, int pc_offset, boolean approximate) {
        PcDesc lower = search.scopes_pcs_begin();
        PcDesc upper = search.scopes_pcs_end();
        lower =new PcDesc(lower.address+PcDesc.SIZE); // exclude initial sentinel
        PcDesc res = null;
        for (PcDesc p = lower; p.address < upper.address; p=new PcDesc(p.address+PcDesc.SIZE)) {
            if (NMethod.match_desc(p, pc_offset, approximate)) {
                if (res == null)
                    res = p;
                else
                    res = new PcDesc (-2);
            }
        }
        return res;
    }
}
