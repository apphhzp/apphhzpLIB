package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.compiler.CompLevel;
import apphhzp.lib.hotspot.oops.MethodData;
import apphhzp.lib.hotspot.oops.method.Method;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class NMethod extends CompiledMethod {
    public static final Type TYPE = JVM.type("nmethod");
    public static final int SIZE = TYPE.size;
    public static final long ENTRY_BCI_OFFSET = TYPE.offset("_entry_bci");
    public static final long OSR_LINK_OFFSET = TYPE.offset("_osr_link");
    public static final long VERIFIED_ENTRY_POINT_OFFSET = TYPE.offset("_verified_entry_point");
    public static final long CONSTS_OFFSET_OFFSET = TYPE.offset("_consts_offset");
    public static final long STUB_OFFSET_OFFSET = TYPE.offset("_stub_offset");
    public static final long OOPS_OFFSET_OFFSET = TYPE.offset("_oops_offset");
    public static final long COMP_LEVEL_OFFSET = TYPE.offset("_comp_level");
    public static final long STATE_OFFSET = TYPE.offset("_state");
    public static final long LOCK_COUNT_OFFSET = TYPE.offset("_lock_count");
    public static final long STACK_TRAVERSAL_MARK_OFFSET = TYPE.offset("_stack_traversal_mark");
    private NMethod nextCache;

    public NMethod(long addr) {
        super(addr, TYPE);
    }

    public int getEntryBci() {
        return unsafe.getInt(this.address + ENTRY_BCI_OFFSET);
    }

    @Nullable
    public NMethod getNext() {
        long addr = unsafe.getAddress(this.address + OSR_LINK_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.nextCache, addr)) {
            this.nextCache = new NMethod(addr);
        }
        return this.nextCache;
    }

    public void setNext(@Nullable NMethod nMethod) {
        unsafe.putAddress(this.address + OSR_LINK_OFFSET, nMethod == null ? 0L : nMethod.address);
    }

    @Override
    public long getVerifiedEntryPoint() {
        return unsafe.getLong(this.address + VERIFIED_ENTRY_POINT_OFFSET);
    }

    public void setVerifiedEntryPoint(long verifiedEntryPoint) {
        unsafe.putAddress(this.address + VERIFIED_ENTRY_POINT_OFFSET, verifiedEntryPoint);
    }

    @Override
    public long constsBegin() {
        return this.address + unsafe.getInt(this.address+CONSTS_OFFSET_OFFSET);
    }

    @Override
    public long constsEnd() {
        return super.codeBegin();
    }

    @Override
    public long stubBegin() {
        return this.address+ unsafe.getInt(this.address+STUB_OFFSET_OFFSET);
    }

    @Override
    public long stubEnd() {
        return this.address + unsafe.getInt(this.address+OOPS_OFFSET_OFFSET);
    }

    public CompLevel getCompLevel() {
        return CompLevel.of(unsafe.getInt(this.address + COMP_LEVEL_OFFSET));
    }

    public void setCompLevel(CompLevel level) {
        unsafe.putInt(this.address + COMP_LEVEL_OFFSET, level.id);
    }

    public byte getState() {
        return unsafe.getByte(this.address + STATE_OFFSET);
    }

    public int getLockCount() {
        return unsafe.getInt(this.address + LOCK_COUNT_OFFSET);
    }

    public void setLockCount(int count) {
        unsafe.putInt(this.address + LOCK_COUNT_OFFSET, count);
    }

    public long getStackTraversalMark() {
        return JVM.getCLevelLong(this.address+STACK_TRAVERSAL_MARK_OFFSET);
    }

    public void setStackTraversalMark(long mark){
        JVM.putCLevelLong(this.address+STACK_TRAVERSAL_MARK_OFFSET, mark);
    }

    public boolean isOsrMethod() {
        return this.getEntryBci() != JVM.invocationEntryBci;
    }

    @Override
    public boolean isZombie() {
        return this.getState() == States.zombie;
    }

    public boolean isNotInstalled() {
        return this.getState() == States.not_installed;
    }

    public boolean isInUse() {
        return this.getState() <= States.in_use;
    }

    public boolean isAlive() {
        return this.getState() < States.unloaded;
    }

    public boolean isUnloaded() {
        return this.getState() == States.unloaded;
    }

    @Override
    public boolean isLockedByVM() {
        return this.getLockCount() > 0;
    }

    public void invalidateOsrMethod() {
        if (this.getEntryBci() != JVM.invocationEntryBci) {
            throw new IllegalStateException("wrong kind of nmethod");
        }
        Method method = this.getMethod();
        if (method != null) {
            method.getHolder().removeOsrNMethod(this);
        }
    }

    @Override
    public boolean makeNotEntrant() {
        return false;//this.makeNotEntrantOrZombie(States.not_entrant);
    }

    public void unlinkFromMethod() {
        Method method = this.getMethod();
        if (method != null) {
            method.unlinkCode(this);
        }
    }

    public boolean tryTransition(int new_state_int) {
        byte new_state = (byte) new_state_int;
        for (;;) {
            byte old_state = this.getState();
            if (old_state >= new_state) {
                return false;
            }
            if (ClassHelper.compareAndSwapByte(null, this.address + STATE_OFFSET, old_state, new_state)) {
                return true;
            }
        }
    }

    public void inc_decompile_count() {
        if (!this.isCompiledByC2() && !this.isCompiledByJVMCI()) {
            return;
        }
        Method m = this.getMethod();
        if (m == null) return;
        MethodData mdo = m.getMethodData();
        if (mdo == null) return;
        mdo.incDecompileCount();
    }

    //This is unrealistic....

//    public boolean makeNotEntrantOrZombie(int state) {
//        if (state != States.zombie && state != States.not_entrant) {
//            throw new IllegalArgumentException("must be zombie or not_entrant");
//        }
//        if (this.getState() >= state) {
//            return false;
//        }
//        boolean nmethod_needs_unregister = false;
//        {
//            // Enter critical section.  Does not block for safepoint.
//            //MutexLocker ml(CompiledMethod_lock->owned_by_self() ? NULL : CompiledMethod_lock, Mutex::_no_safepoint_check_flag);
//
//            // This logic is equivalent to the logic below for patching the
//            // verified entry point of regular methods. We check that the
//            // nmethod is in use to ensure that it is invalidated only once.
//            if (this.isOsrMethod() && this.isInUse()) {
//                // this effectively makes the osr nmethod not entrant
//                this.invalidateOsrMethod();
//            }
//
//            if (this.getState() >= state) {
//                return false;
//            }
//
//            // The caller can be calling the method statically or through an inline
//            // cache call.
////            if (!this.isOsrMethod() && !is_not_entrant()) {
////                NativeJump::patch_verified_entry(entry_point(), verified_entry_point(),
////                        SharedRuntime::get_handle_wrong_method_stub());
////            }
//            if (this.isInUse() && update_recompile_counts()) {
//                inc_decompile_count();
//            }
//            if ((state == States.zombie) && !this.isUnloaded()) {
//                nmethod_needs_unregister = true;
//            }
////
//            if (state == States.not_entrant) {
//                this.markAsSeenOnStack();
//                //OrderAccess::storestore();
//            }
////
//            if (!tryTransition(state)) {
//                return false;
//            }
//
//
//            this.unlinkFromMethod();
//
//        }
//
////#if INCLUDE_JVMCI
////        JVMCINMethodData* nmethod_data = jvmci_nmethod_data();
////        if (nmethod_data != NULL) {
////            nmethod_data->invalidate_nmethod_mirror(this);
////        }
////#endif
//
//
//        if (state == States.zombie) {
//
//            // Flushing dependencies must be done before any possible
//            // safepoint can sneak in, otherwise the oops used by the
//            // dependency logic could have become stale.
////            if (nmethod_needs_unregister) {
////                Universe::heap()->unregister_nmethod(this);
////            }
////            flush_dependencies(true);
////            if (JVM.includeJVMCI) {
////                // Now that the nmethod has been unregistered, it's
////                // safe to clear the HotSpotNmethod mirror oop.
////                if (nmethod_data != NULL) {
////                    nmethod_data -> clear_nmethod_mirror(this);
////                }
////            }
////
////            // Clear ICStubs to prevent back patching stubs of zombie or flushed
////            // nmethods during the next safepoint (see ICStub::finalize), as well
////            // as to free up CompiledICHolder resources.
//            {
//                this.clearICCallsites();
//            }
//
//            // zombie only - if a JVMTI agent has enabled the CompiledMethodUnload
//            // event and it hasn't already been reported for this nmethod then
//            // report it now. The event may have been reported earlier if the GC
//            // marked it for unloading). JvmtiDeferredEventQueue support means
//            // we no longer go to a safepoint here.
//            //post_compiled_method_unload();
//
//
//            // the Method may be reclaimed by class unloading now that the
//            // nmethod is in zombie state
//            this.setMethod(null);
//        } else {
//            if (state != States.not_entrant) {
//                throw new IllegalStateException("other cases may need to be handled differently");
//            }
//        }
//
//        //NMethodSweeper::report_state_change(this);
//        return true;
//    }
}
