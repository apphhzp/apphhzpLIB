package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.*;
import apphhzp.lib.hotspot.compiler.ImmutableOopMap;
import apphhzp.lib.hotspot.compiler.ImmutableOopMapPair;
import apphhzp.lib.hotspot.compiler.ImmutableOopMapSet;
import apphhzp.lib.hotspot.interpreter.Bytecode_field;
import apphhzp.lib.hotspot.interpreter.Bytecode_invoke;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.Metadata;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.runtime.Frame;
import apphhzp.lib.hotspot.util.RawCType;

import javax.annotation.Nullable;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.code.blob.CompiledMethod.States.*;

/** nmethods (native methods) are the compiled code versions of Java methods.<br><br>

 An nmethod contains:<br>
  - header                  (the nmethod structure)<br>
  [Relocation]<br>
  - relocation information<br>
  - constant part           (doubles, longs and floats used in nmethod)<br>
  - oop table<br>
  [Code]<br>
  - code body<br>
  - exception handler<br>
  - stub code<br>
  [Debugging information]<br>
  - oop array<br>
  - data array<br>
  - pcs<br>
  [Exception handler table]<br>
  - handler entry point array<br>
  [Implicit Null Pointer exception table]<br>
  - implicit null table array<br>
  [Speculations]<br>
  - encoded speculations array<br>
  [JVMCINMethodData]<br>
  - meta data for JVMCI compiled nmethod*/
public class NMethod extends CompiledMethod {
    public static final Type TYPE = JVM.type("nmethod");
    public static final int SIZE = TYPE.size;
    public static final long ENTRY_BCI_OFFSET = TYPE.offset("_entry_bci");
    public static final long OSR_LINK_OFFSET = TYPE.offset("_osr_link");
    public static final long ENTRY_POINT_OFFSET=TYPE.offset("_entry_point");
    public static final long VERIFIED_ENTRY_POINT_OFFSET = TYPE.offset("_verified_entry_point");
    public static final long OSR_ENTRY_POINT_OFFSET=TYPE.offset("_osr_entry_point");
    public static final long EXCEPTION_OFFSET_OFFSET = TYPE.offset("_exception_offset");
    public static final long UNWIND_HANDLER_OFFSET_OFFSET=JVM.computeOffset(JVM.intSize,EXCEPTION_OFFSET_OFFSET+JVM.intSize);
    public static final long CONSTS_OFFSET_OFFSET = TYPE.offset("_consts_offset");
    public static final long STUB_OFFSET_OFFSET = TYPE.offset("_stub_offset");
    public static final long OOPS_OFFSET_OFFSET = TYPE.offset("_oops_offset");
    public static final long METADATA_OFFSET_OFFSET=TYPE.offset("_metadata_offset");
    public static final long SCOPES_PCS_OFFSET_OFFSET=TYPE.offset("_scopes_pcs_offset");
    public static final long DEPENDENCIES_OFFSET_OFFSET=TYPE.offset("_dependencies_offset");
    public static final long NATIVE_INVOKERS_OFFSET_OFFSET=JVM.computeOffset(JVM.intSize,DEPENDENCIES_OFFSET_OFFSET+JVM.intSize);
    public static final long HANDLER_TABLE_OFFSET_OFFSET=TYPE.offset("_handler_table_offset");
    public static final long NUL_CHK_TABLE_OFFSET_OFFSET=TYPE.offset("_nul_chk_table_offset");
    public static final long SPECULATIONS_OFFSET_OFFSET=JVM.includeJVMCI?JVM.computeOffset(JVM.intSize,NUL_CHK_TABLE_OFFSET_OFFSET+JVM.intSize):-1;
    public static final long JVMCI_DATA_OFFSET_OFFSET=JVM.includeJVMCI?JVM.computeOffset(JVM.intSize,SPECULATIONS_OFFSET_OFFSET+JVM.intSize):-1;
    public static final long NMETHOD_END_OFFSET_OFFSET=TYPE.offset("_nmethod_end_offset");
    public static final long ORIG_PC_OFFSET_OFFSET=TYPE.offset("_orig_pc_offset");
    public static final long COMPILE_ID_OFFSET=TYPE.offset("_compile_id");
    public static final long COMP_LEVEL_OFFSET = TYPE.offset("_comp_level");
    public static final long STATE_OFFSET = TYPE.offset("_state");
    public static final long LOCK_COUNT_OFFSET = TYPE.offset("_lock_count");
    public static final long STACK_TRAVERSAL_MARK_OFFSET = TYPE.offset("_stack_traversal_mark");

    static {
        JVM.assertOffset(CONSTS_OFFSET_OFFSET,JVM.computeOffset(JVM.intSize,UNWIND_HANDLER_OFFSET_OFFSET+JVM.intSize));
        JVM.assertOffset(HANDLER_TABLE_OFFSET_OFFSET,JVM.computeOffset(JVM.intSize,NATIVE_INVOKERS_OFFSET_OFFSET+JVM.intSize));
        if (JVMCI_DATA_OFFSET_OFFSET!=-1){
            JVM.assertOffset(NMETHOD_END_OFFSET_OFFSET,JVM.computeOffset(JVM.intSize,JVMCI_DATA_OFFSET_OFFSET+JVM.intSize));
        }
    }
    private NMethod nextCache;

    public NMethod(long addr) {
        super(addr, TYPE);
    }

    private int code_offset(){
        return (int) (code_begin() - header_begin());
    }

    @Override
    public boolean is_nmethod() {
        return true;
    }

    public boolean is_osr_method(){
        return unsafe.getInt(this.address+ENTRY_BCI_OFFSET)!= JVM.invocationEntryBci;
    }

    public int comp_level() {
        return unsafe.getInt(this.address + COMP_LEVEL_OFFSET);
    }

    /** Compiler task identification.  Note that all OSR methods
     * are numbered in an independent sequence if CICountOSR is true,
     * and native method wrappers are also numbered independently if
     * CICountNative is true.*/
    @Override
    public int compile_id() {
        return unsafe.getInt(this.address+COMPILE_ID_OFFSET);
    }

    @Nullable
    public String compile_kind(){
        if (is_osr_method()){
            return "osr";
        }
        if(method()!=null && is_native_method()){
            return "c2n";
        }
        return null;
    }

    @Override
    public boolean make_not_used() {
        return make_not_entrant();
    }

    @Override
    public boolean make_not_entrant() {
        if (method().is_method_handle_intrinsic()){
            throw new IllegalStateException("Cannot make MH intrinsic not entrant");
        }
        return make_not_entrant_or_zombie(not_entrant);
    }

    @Override
    public boolean make_entrant() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    public int get_state() {
        return unsafe.getByte(this.address + STATE_OFFSET)&0xff;
    }

    public int lock_count() {
        return unsafe.getInt(this.address + LOCK_COUNT_OFFSET);
    }

    public void set_lock_count(int count) {
        unsafe.putInt(this.address + LOCK_COUNT_OFFSET, count);
    }

    /** When true is returned, it is unsafe to remove this nmethod even if
     *  it is a zombie, since the VM or the ServiceThread might still be
     *  using it.*/
    @Override
    public boolean is_locked_by_vm() {
        return this.lock_count() > 0;
    }


    /**
     * Common functionality for both make_not_entrant and make_zombie
     */
    public boolean make_not_entrant_or_zombie(int state) {
        return false;
//        assert(state == zombie || state == not_entrant, "must be zombie or not_entrant");
//
//        if (Atomic::load(&_state) >= state) {
//            // Avoid taking the lock if already in required state.
//            // This is safe from races because the state is an end-state,
//            // which the nmethod cannot back out of once entered.
//            // No need for fencing either.
//            return false;
//        }
//
//        // Make sure the nmethod is not flushed.
//        nmethodLocker nml(this);
//        // This can be called while the system is already at a safepoint which is ok
//        NoSafepointVerifier nsv;
//
//        // during patching, depending on the nmethod state we must notify the GC that
//        // code has been unloaded, unregistering it. We cannot do this right while
//        // holding the CompiledMethod_lock because we need to use the CodeCache_lock. This
//        // would be prone to deadlocks.
//        // This flag is used to remember whether we need to later lock and unregister.
//        bool nmethod_needs_unregister = false;
//
//        {
//            // Enter critical section.  Does not block for safepoint.
//            MutexLocker ml(CompiledMethod_lock->owned_by_self() ? NULL : CompiledMethod_lock, Mutex::_no_safepoint_check_flag);
//
//            // This logic is equivalent to the logic below for patching the
//            // verified entry point of regular methods. We check that the
//            // nmethod is in use to ensure that it is invalidated only once.
//            if (is_osr_method() && is_in_use()) {
//                // this effectively makes the osr nmethod not entrant
//                invalidate_osr_method();
//            }
//
//            if (Atomic::load(&_state) >= state) {
//            // another thread already performed this transition so nothing
//            // to do, but return false to indicate this.
//            return false;
//        }
//
//            // The caller can be calling the method statically or through an inline
//            // cache call.
//            if (!is_osr_method() && !is_not_entrant()) {
//                NativeJump::patch_verified_entry(entry_point(), verified_entry_point(),
//                        SharedRuntime::get_handle_wrong_method_stub());
//            }
//
//            if (is_in_use() && update_recompile_counts()) {
//                // It's a true state change, so mark the method as decompiled.
//                // Do it only for transition from alive.
//                inc_decompile_count();
//            }
//
//            // If the state is becoming a zombie, signal to unregister the nmethod with
//            // the heap.
//            // This nmethod may have already been unloaded during a full GC.
//            if ((state == zombie) && !is_unloaded()) {
//                nmethod_needs_unregister = true;
//            }
//
//            // Must happen before state change. Otherwise we have a race condition in
//            // nmethod::can_convert_to_zombie(). I.e., a method can immediately
//            // transition its state from 'not_entrant' to 'zombie' without having to wait
//            // for stack scanning.
//            if (state == not_entrant) {
//                mark_as_seen_on_stack();
//                OrderAccess::storestore(); // _stack_traversal_mark and _state
//            }
//
//            // Change state
//            if (!try_transition(state)) {
//                // If the transition fails, it is due to another thread making the nmethod more
//                // dead. In particular, one thread might be making the nmethod unloaded concurrently.
//                // If so, having patched in the jump in the verified entry unnecessarily is fine.
//                // The nmethod is no longer possible to call by Java threads.
//                // Incrementing the decompile count is also fine as the caller of make_not_entrant()
//                // had a valid reason to deoptimize the nmethod.
//                // Marking the nmethod as seen on stack also has no effect, as the nmethod is now
//                // !is_alive(), and the seen on stack value is only used to convert not_entrant
//                // nmethods to zombie in can_convert_to_zombie().
//                return false;
//            }
//
//            // Log the transition once
//            log_state_change();
//
//            // Remove nmethod from method.
//            unlink_from_method();
//
//        } // leave critical region under CompiledMethod_lock
//
//#if INCLUDE_JVMCI
//        // Invalidate can't occur while holding the Patching lock
//        JVMCINMethodData* nmethod_data = jvmci_nmethod_data();
//        if (nmethod_data != NULL) {
//            nmethod_data->invalidate_nmethod_mirror(this);
//        }
//#endif
//
//#ifdef ASSERT
//        if (is_osr_method() && method() != NULL) {
//            // Make sure osr nmethod is invalidated, i.e. not on the list
//            bool found = method()->method_holder()->remove_osr_nmethod(this);
//            assert(!found, "osr nmethod should have been invalidated");
//        }
//#endif
//
//        // When the nmethod becomes zombie it is no longer alive so the
//        // dependencies must be flushed.  nmethods in the not_entrant
//        // state will be flushed later when the transition to zombie
//        // happens or they get unloaded.
//        if (state == zombie) {
//            {
//                // Flushing dependencies must be done before any possible
//                // safepoint can sneak in, otherwise the oops used by the
//                // dependency logic could have become stale.
//                MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
//                if (nmethod_needs_unregister) {
//                    Universe::heap()->unregister_nmethod(this);
//                }
//                flush_dependencies(/*delete_immediately*/true);
//            }
//
//#if INCLUDE_JVMCI
//            // Now that the nmethod has been unregistered, it's
//            // safe to clear the HotSpotNmethod mirror oop.
//            if (nmethod_data != NULL) {
//                nmethod_data->clear_nmethod_mirror(this);
//            }
//#endif
//
//            // Clear ICStubs to prevent back patching stubs of zombie or flushed
//            // nmethods during the next safepoint (see ICStub::finalize), as well
//            // as to free up CompiledICHolder resources.
//            {
//                CompiledICLocker ml(this);
//                clear_ic_callsites();
//            }
//
//            // zombie only - if a JVMTI agent has enabled the CompiledMethodUnload
//            // event and it hasn't already been reported for this nmethod then
//            // report it now. The event may have been reported earlier if the GC
//            // marked it for unloading). JvmtiDeferredEventQueue support means
//            // we no longer go to a safepoint here.
//            post_compiled_method_unload();
//
//#ifdef ASSERT
//            // It's no longer safe to access the oops section since zombie
//            // nmethods aren't scanned for GC.
//            _oops_are_stale = true;
//#endif
//            // the Method may be reclaimed by class unloading now that the
//            // nmethod is in zombie state
//            set_method(NULL);
//        } else {
//            assert(state == not_entrant, "other cases may need to be handled differently");
//        }
//
//        if (TraceCreateZombies && state == zombie) {
//            ResourceMark m;
//            tty->print_cr("nmethod <" INTPTR_FORMAT "> %s code made %s", p2i(this), this->method() ? this->method()->name_and_sig_as_C_string() : "null", (state == not_entrant) ? "not entrant" : "zombie");
//        }
//
//        NMethodSweeper::report_state_change(this);
//        return true;
    }

    /** Support for oops in scopes and relocs:<br>
     * Note: index 0 is reserved for null.*/
    @Override
    public Oop oop_at(int index) {
        if (index == 0) {
            return null;
        }
        return new Oop(oop_addr_at(index));
    }
    /** Support for meta data in scopes and relocs:<br>
     * Note: index 0 is reserved for null.*/
    @Override
    public Metadata metadata_at(int index) {
        return index == 0 ? null: Metadata.getMetadata(unsafe.getAddress(metadata_addr_at(index)));
    }

    public @RawCType("Metadata**") long metadata_addr_at(int index){
        // relocation indexes are biased by 1 (because 0 is reserved)
        //Actually, when index==metadata_count(), JVM will crash.The latest version of Hotspot JVM does not seem to fix this problem.
        if (!(index > 0 && index <= metadata_count())){
            throw new IndexOutOfBoundsException("must be a valid non-zero index");
        }
        return metadata_begin()+ (long) (index - 1) *JVM.oopSize;
    }

    public @RawCType("oop*") long oop_addr_at(int index){
        // relocation indexes are biased by 1 (because 0 is reserved)
        //Actually, when index==oops_count(), JVM will crash.
        if (!(index > 0 && index < oops_count())){
            throw new IndexOutOfBoundsException("must be a valid non-zero index");
        }
//        assert(!_oops_are_stale, "oops are stale");
        return oops_begin()+ (long) (index - 1) *JVM.oopSize;
    }

    @Override
    public @RawCType("address") long scopes_data_end() {
        return header_begin() + unsafe.getInt(this.address+SCOPES_PCS_OFFSET_OFFSET);
    }

    @Override
    public PcDesc scopes_pcs_begin() {
        return new PcDesc(header_begin() + unsafe.getInt(this.address+SCOPES_PCS_OFFSET_OFFSET));
    }

    @Override
    public PcDesc scopes_pcs_end() {
        return new PcDesc(header_begin() + unsafe.getInt(this.address+DEPENDENCIES_OFFSET_OFFSET));
    }

    public @RawCType("address")long dependencies_begin(){
        return header_begin() + unsafe.getInt(this.address+DEPENDENCIES_OFFSET_OFFSET);
    }
    public @RawCType("address")long dependencies_end(){
        return header_begin() + unsafe.getInt(this.address+NATIVE_INVOKERS_OFFSET_OFFSET);
    }

    public @RawCType("RuntimeStub**") long native_invokers_begin(){
        return (header_begin() + unsafe.getInt(this.address+NATIVE_INVOKERS_OFFSET_OFFSET));
    }

    public @RawCType("RuntimeStub**") long native_invokers_end(){
        return (header_begin() + unsafe.getInt(this.address+HANDLER_TABLE_OFFSET_OFFSET));
    }

    @Override
    public @RawCType("address") long consts_begin() {
        return header_begin() + unsafe.getInt(this.address+CONSTS_OFFSET_OFFSET);
    }

    @Override
    public @RawCType("address") long consts_end() {
        return code_begin();
    }

    @Override
    public @RawCType("address") long stub_begin() {
        return header_begin() + unsafe.getInt(this.address+STUB_OFFSET_OFFSET);
    }

    @Override
    public @RawCType("address") long stub_end() {
        return header_begin() + unsafe.getInt(this.address+OOPS_OFFSET_OFFSET);
    }

    public @RawCType("oop*")long oops_begin(){
        return (header_begin() + unsafe.getInt(this.address+OOPS_OFFSET_OFFSET));
    }
    public @RawCType("oop*")long oops_end(){
        return (header_begin() + unsafe.getInt(this.address+METADATA_OFFSET_OFFSET));
    }

    public @RawCType("Metadata**")long metadata_begin(){
        return (header_begin() + unsafe.getInt(this.address+METADATA_OFFSET_OFFSET));
    }
    public @RawCType("Metadata**")long metadata_end(){
        return unsafe.getAddress(this.address+SCOPES_DATA_BEGIN_OFFSET);
    }

    @Override
    public @RawCType("address") long handler_table_begin() {
        return header_begin() + unsafe.getInt(this.address+HANDLER_TABLE_OFFSET_OFFSET);
    }

    @Override
    public @RawCType("address") long handler_table_end() {
        return header_begin() + unsafe.getInt(this.address+NUL_CHK_TABLE_OFFSET_OFFSET);
    }

    @Override
    public @RawCType("address") long exception_begin() {
        return header_begin() + unsafe.getInt(this.address+EXCEPTION_OFFSET_OFFSET);
    }

    @Override
    public long nul_chk_table_begin() {
        return header_begin() + unsafe.getInt(this.address+NUL_CHK_TABLE_OFFSET_OFFSET);
    }

    @Override
    public long nul_chk_table_end() {
        if (!JVM.includeJVMCI){
            return header_begin() + unsafe.getInt(this.address+NMETHOD_END_OFFSET_OFFSET);
        }
        return header_begin() + unsafe.getInt(this.address+SPECULATIONS_OFFSET_OFFSET);
    }

    public @RawCType("address")long speculations_begin(){
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return header_begin() + unsafe.getInt(this.address+SPECULATIONS_OFFSET_OFFSET);
    }
    public @RawCType("address")long speculations_end(){
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return header_begin() + unsafe.getInt(this.address+JVMCI_DATA_OFFSET_OFFSET);
    }
    public @RawCType("address")long jvmci_data_begin(){
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return header_begin() + unsafe.getInt(this.address+JVMCI_DATA_OFFSET_OFFSET);
    }
    public @RawCType("address")long jvmci_data_end(){
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return header_begin() + unsafe.getInt(this.address+NMETHOD_END_OFFSET_OFFSET);
    }

    public @RawCType("address")long unwind_handler_begin(){
        int val=unsafe.getInt(this.address+UNWIND_HANDLER_OFFSET_OFFSET);
        return val != -1 ? (header_begin() + val) : 0L;
    }

    // Sizes
    public int oops_size(){
        return (int) (oops_end() - oops_begin());
    }
    public int metadata_size(){
        return (int) (metadata_end() - metadata_begin());
    }
    public int dependencies_size (){
        return (int) (dependencies_end() -dependencies_begin ());
    }

    public int speculations_size(){
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return (int) (speculations_end () -            speculations_begin ());
    }
    public int jvmci_data_size(){
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return (int) (jvmci_data_end   () -            jvmci_data_begin   ());
    }

    public int oops_count(){
        if (!(oops_size() % JVM.oopSize == 0)){
            throw new RuntimeException();
        }
        return (oops_size() / JVM.oopSize) + 1;
    }
    public int metadata_count(){
        if (!(metadata_size() % JVM.wordSize==0)){
            throw new RuntimeException();
        }
        return (metadata_size() / JVM.wordSize)+1;
    }

    public int total_size() {
        return consts_size() +
                insts_size() +
                stub_size() +
                scopes_data_size() +
                scopes_pcs_size() +
                handler_table_size() +
                nul_chk_table_size();
    }

    // Containment
    public boolean oops_contains (Oop addr){
        return oops_begin() <= addr.address && addr.address < oops_end();
    }
    public boolean metadata_contains(@RawCType("Metadata**")long addr){
        return metadata_begin     () <= addr && addr < metadata_end     ();
    }
    public boolean scopes_data_contains(@RawCType("address")long addr){
        return scopes_data_begin  () <= addr && addr < scopes_data_end  ();
    }
    public boolean scopes_pcs_contains(PcDesc addr){
        return scopes_pcs_begin().address <= addr.address && addr.address < scopes_pcs_end().address;
    }

    // entry points

    /**entry point with class check*/
    public @RawCType("address") long entry_point(){
        // normal entry point
        return unsafe.getAddress(this.address+ENTRY_POINT_OFFSET);
    }

    @Override
    public boolean make_zombie() {
        return false;
    }

    /** entry point without class check*/
    public @RawCType("address") long verified_entry_point(){
        // if klass is correct
        return unsafe.getAddress(this.address+VERIFIED_ENTRY_POINT_OFFSET);
    }

    // flag accessing and manipulation
    public boolean is_not_installed(){
        return this.get_state() == States.not_installed;
    }
    public boolean is_in_use(){ return this.get_state() <= States.in_use; }
    public boolean is_alive(){ return this.get_state() < unloaded; }
    public boolean is_not_entrant(){ return this.get_state() == not_entrant; }
    public boolean is_zombie(){ return this.get_state() == zombie; }
    public boolean is_unloaded(){ return this.get_state() == unloaded; }

    // Sweeper support
    public @RawCType("int64_t")long stack_traversal_mark(){
        if (TYPE.field("_stack_traversal_mark").typeName.equals("int64_t")){
            return unsafe.getLong(this.address+STACK_TRAVERSAL_MARK_OFFSET);
        }
        //In some versions of JVM, this field is of type long.
        return JVM.getCLevelLong(this.address+STACK_TRAVERSAL_MARK_OFFSET);
    }
    public void set_stack_traversal_mark(@RawCType("int64_t")long l){
        if (TYPE.field("_stack_traversal_mark").typeName.equals("int64_t")){
            unsafe.putLong(this.address+STACK_TRAVERSAL_MARK_OFFSET,l);
            return;
        }
        //In some versions of JVM, this field is of type long.
        JVM.putCLevelLong(this.address+STACK_TRAVERSAL_MARK_OFFSET,l);
    }

    // On-stack replacement support
    public int osr_entry_bci(){
        if (!is_osr_method()){
            throw new IllegalStateException("wrong kind of nmethod");
        }
        return unsafe.getInt(this.address+ENTRY_BCI_OFFSET);
    }
    public @RawCType("address")long  osr_entry(){
        if (!is_osr_method()){
            throw new IllegalStateException("wrong kind of nmethod");
        }
        return unsafe.getAddress(this.address+OSR_ENTRY_POINT_OFFSET);
    }
    void  invalidate_osr_method(){
        //TODO
        throw new UnsupportedOperationException();
    }
    @Nullable
    public NMethod osr_link(){
        long addr=unsafe.getAddress(this.address+OSR_LINK_OFFSET);
        if (addr==0){
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=new NMethod(addr);
        }
        return  this.nextCache;
    }
    public void set_osr_link(NMethod n){
        unsafe.putAddress(this.address+OSR_LINK_OFFSET,n==null?0:n.address);
    }

    public NativeCallWrapper call_wrapper_at(long call) {
        return new DirectNativeCallWrapper(NativeCall.create(call));
    }

    // Helper used by both find_pc_desc methods.
    public static boolean match_desc(PcDesc pc, int pc_offset, boolean approximate) {
        if (!approximate) {
            return pc.pc_offset() == pc_offset;
        } else {
            return new PcDesc(pc.address-PcDesc.SIZE).pc_offset() < pc_offset && pc_offset <= pc.pc_offset();
        }
    }

    public @RawCType("address*")long orig_pc_addr(Frame fr) {
        return (fr.unextended_sp() + unsafe.getInt(this.address+ORIG_PC_OFFSET_OFFSET));
    }

    // Accessor/mutator for the original pc of a frame before a frame was deopted.
    public @RawCType("address")long get_original_pc(Frame fr) {
        return unsafe.getAddress(orig_pc_addr(fr));
    }
    public void    set_original_pc( Frame fr, @RawCType("address")long pc) {
        unsafe.putAddress(orig_pc_addr(fr),pc);
    }

    public @RawCType("address")long call_instruction_address(@RawCType("address")long pc){
        if (NativeCall.is_call_before(pc)) {
            NativeCall ncall = NativeCall.nativeCall_before(pc);
            return ncall.instruction_address();
        }
        return 0L;
    }
    public boolean is_unloading() {
        throw new UnsupportedOperationException();
    }

    // Return a the last scope in (begin..end]
    public ScopeDesc scope_desc_in(@RawCType("address")long begin, @RawCType("address")long end) {
        PcDesc p = pc_desc_near(begin+1);
        if (p != null && p.real_pc(this) <= end) {
            return new ScopeDesc(this, p);
        }
        return null;
    }
    public void print_code_comment_on(PrintStream st, int column, @RawCType("address")long begin, @RawCType("address")long end) {
//        ImplicitExceptionTable implicit_table(this);
//        int pc_offset = (int) (begin - code_begin());
//        int cont_offset = implicit_table.continuation_offset(pc_offset);
        boolean oop_map_required = false;
//        if (cont_offset != 0) {
//            st.move_to(column, 6, 0);
//            if (pc_offset == cont_offset) {
//                st->print("; implicit exception: deoptimizes");
//                oop_map_required = true;
//            } else {
//                st->print("; implicit exception: dispatches to " INTPTR_FORMAT, p2i(code_begin() + cont_offset));
//            }
//        }

        // Find an oopmap in (begin, end].  We use the odd half-closed
        // interval so that oop maps and scope descs which are tied to the
        // byte after a call are printed with the call itself.  OopMaps
        // associated with implicit exceptions are printed with the implicit
        // instruction.
        @RawCType("address")long base = code_begin();
        ImmutableOopMapSet oms = oop_maps();
        if (oms != null) {
            for (int i = 0, imax = oms.count(); i < imax; i++) {
                ImmutableOopMapPair pair = oms.pair_at(i);
                ImmutableOopMap om = pair.get_from(oms);
                @RawCType("address")long pc = base + pair.pc_offset();
                if (pc >= begin) {
                    boolean is_implicit_deopt;
                    if (JVM.includeJVMCI){
                        is_implicit_deopt = false;///implicit_table.continuation_offset(pair->pc_offset()) == (uint) pair->pc_offset();
                    }else {
                        is_implicit_deopt = false;
                    }
                    if (is_implicit_deopt ? pc == begin : pc > begin && pc <= end) {
                        //st.move_to(column, 6, 0);
                        st.print("; ");
                        om.print_on(st);
                        oop_map_required = false;
                    }
                }
                if (pc > end) {
                    break;
                }
            }
        }
        if (oop_map_required){
            throw new RuntimeException("missed oopmap");
        }

        //Thread* thread = Thread::current();

        // Print any debug info present at this pc.
        ScopeDesc sd  = scope_desc_in(begin, end);
        if (sd != null) {
            //st.move_to(column, 6, 0);
            if (sd.bci() == JVM.invocationEntryBci) {
                st.print(";*synchronization entry");
            } else if (sd.bci() == -2) {
                st.print(";* method exit (unlocked if synchronized)");
            } else if (sd.bci() == -3) {
                st.print(";* unwind (locked if synchronized)");
            } else if (sd.bci() == -4) {
                st.print(";* unwind (unlocked if synchronized)");
            } else if (sd.bci() == -5) {
                st.print(";* unknown");
            } else if (sd.bci() == -6) {
                st.print(";* invalid frame state");
            } else {
                if (sd.method() == null) {
                    st.print("method is NULL");
                } else if (sd.method().is_native()) {
                    st.print("method is native");
                } else {
                    @RawCType("Bytecodes::Code")int bc = sd.method().java_code_at(sd.bci());
                    st.printf(";*%s", Bytecodes.name(bc));
                    switch (bc) {
                        case Bytecodes.Code._invokevirtual:
                        case Bytecodes.Code._invokespecial:
                        case Bytecodes.Code._invokestatic:
                        case Bytecodes.Code._invokeinterface:
                        {
                            Bytecode_invoke invoke=new Bytecode_invoke((sd.method()), sd.bci());
                            st.print(" ");
                            if (invoke.name() != null) {
                                st.print(invoke.name());
                            } else {
                                st.print("<UNKNOWN>");
                            }
                            break;
                        }
                        case Bytecodes.Code._getfield:
                        case Bytecodes.Code._putfield:
                        case Bytecodes.Code._getstatic:
                        case Bytecodes.Code._putstatic:
                        {
                            Bytecode_field field=new Bytecode_field((sd.method()), sd.bci());
                            st.print(" ");
                            if (field.name() != null)
                                st.print(field.name());
                            else
                                st.print("<UNKNOWN>");
                        }
                        default:
                            break;
                    }
                }
                st.printf(" {reexecute=%d rethrow=%d return_oop=%d}", sd.should_reexecute()?1:0, sd.rethrow_exception()?1:0, sd.return_oop()?1:0);
            }

            // Print all scopes
            for (;sd != null; sd = sd.sender()) {
                //st.move_to(column, 6, 0);
                st.print("; -");
                if (sd.should_reexecute()) {
                    st.print(" (reexecute)");
                }
                if (sd.method() == null) {
                    st.print("method is NULL");
                } else {
                    sd.method().print_short_name(st);
                }
                int lineno = sd.method().line_number_from_bci(sd.bci());
                if (lineno != -1) {
                    st.printf("@%d (line %d)", sd.bci(), lineno);
                } else {
                    st.printf("@%d", sd.bci());
                }
                st.println();
            }
        }

        // Print relocation information
        // Prevent memory leak: allocating without ResourceMark.
//        String str = reloc_string_for(begin, end);
//        if (str != NULL) {
//            if (sd != NULL) st.cr();
//            st.move_to(column, 6, 0);
//            st.print(";   {%s}", str);
//        }
    }
}