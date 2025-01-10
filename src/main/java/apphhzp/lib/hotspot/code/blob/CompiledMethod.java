package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.RelocInfo;
import apphhzp.lib.hotspot.code.RelocIterator;
import apphhzp.lib.hotspot.oops.method.Method;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class CompiledMethod extends CodeBlob {
    public static final Type TYPE = JVM.type("CompiledMethod");
    public static final int SIZE = TYPE.size;
    public static final long MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET = CodeBlob.SIZE;
    public static final long METHOD_OFFSET = TYPE.offset("_method");
    private Method methodCache;

    public CompiledMethod(long addr) {
        super(addr, TYPE);
    }

    public CompiledMethod(long addr, Type type) {
        super(addr, type);
    }

    public Method getMethod() {
        long addr = unsafe.getAddress(this.address + METHOD_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.methodCache, addr)) {
            this.methodCache = Method.getOrCreate(addr);
        }
        return this.methodCache;
    }

    public void setMethod(@Nullable Method method) {
        unsafe.putAddress(this.address + METHOD_OFFSET, method == null ? 0L : method.address);
    }

    public void markForDeoptimization(boolean inc_recompile_counts) {
        unsafe.putInt(this.address + MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET, inc_recompile_counts ? 1 : 2);
    }

    public int getMarkForDeoptimizationStatus() {
        return unsafe.getInt(this.address + MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET);
    }

    public boolean update_recompile_counts(){
        return this.getMarkForDeoptimizationStatus()!= 2;
    }

    public boolean isMarkedForDeoptimization() {
        return this.getMarkForDeoptimizationStatus() != 0;
    }

    public boolean makeNotEntrant() {
        return false;
    }

    public void clearICCallsites() {
//        ResourceMark rm;
        RelocIterator iter=new RelocIterator(this);
        while(iter.next()) {
            if (iter.type() == RelocInfo.Type.VIRTUAL_CALL_TYPE) {
                //CompiledIC* ic = CompiledIC_at(&iter);
                //ic->set_to_clean(false);
            }
        }
    }

    public long getVerifiedEntryPoint() {
        return 0L;
    }

    public long constsBegin() {
        return 0L;
    }

    public long constsEnd() {
        return 0L;
    }

    public boolean constsContains(long addr) {
        return this.constsBegin() <= addr && addr < this.constsEnd();
    }

    public int constsSize() {
        return (int) (this.constsEnd() - this.constsBegin());
    }

    public long stubBegin() {
        return 0L;
    }

    public long stubEnd() {
        return 0L;
    }

    public boolean stubContains(long addr) {
        return this.stubBegin() <= addr && addr < this.stubEnd();
    }

    public int stubSize() {
        return (int) (this.stubEnd() - this.stubBegin());
    }

    public long instsBegin() {
        return this.codeBegin();
    }

    public long instsEnd() {
        return this.stubEnd();
    }

    // Returns true if a given address is in the 'insts' section. The method
    // insts_contains_inclusive() is end-inclusive.
    public boolean instsContains(long addr) {
        return instsBegin() <= addr && addr < instsEnd();
    }

    public boolean instsContainsInclusive(long addr) {
        return this.instsBegin() <= addr && addr <= instsEnd();
    }

    public int instsSize() {
        return (int) (instsEnd() - instsBegin());
    }

    public static final class States {
        public static final int
                not_installed = -1, // in construction, only the owner doing the construction is allowed to advance state
                in_use = 0,  // executable nmethod
                not_used = 1,  // not entrant, but revivable
                not_entrant = 2,  // marked for deoptimization but activations may still exist,will be transformed to zombie when all activations are gone
                unloaded = 3,  // there should be no activations, should not be called, will be
                zombie = 4;// transformed to zombie by the sweeper, when not "locked in vm".
    }
}
