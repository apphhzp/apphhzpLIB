package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.*;
import apphhzp.lib.hotspot.oops.Metadata;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.runtime.Frame;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public abstract class CompiledMethod extends CodeBlob {
    public static final Type TYPE = JVM.type("CompiledMethod");
    public static final int SIZE = TYPE.size;
    public static final long MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET = JVM.computeOffset(JVM.intSize, CodeBlob.SIZE);
    //BitFields
    public static final long HAS_UNSAFE_ACCESS_OFFSET = JVM.computeOffset(JVM.intSize, MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET + JVM.intSize);
    public static final long HAS_METHOD_HANDLE_INVOKES_OFFSET = HAS_UNSAFE_ACCESS_OFFSET;
    public static final long HAS_WIDE_VECTORS_OFFSET = HAS_UNSAFE_ACCESS_OFFSET;
    public static final long METHOD_OFFSET = TYPE.offset("_method");
    public static final long SCOPES_DATA_BEGIN_OFFSET = TYPE.offset("_scopes_data_begin");
    public static final long DEOPT_HANDLER_BEGIN_OFFSET = TYPE.offset("_deopt_handler_begin");
    public static final long DEOPT_MH_HANDLER_BEGIN_OFFSET = TYPE.offset("_deopt_mh_handler_begin");
    public static final long PC_DESC_CONTAINER_OFFSET = JVM.computeOffset(JVM.oopSize, DEOPT_MH_HANDLER_BEGIN_OFFSET + JVM.oopSize);
    public static final long EXCEPTION_CACHE_OFFSET = TYPE.offset("_exception_cache");
    public static final long GC_DATA_OFFSET = JVM.computeOffset(JVM.oopSize, EXCEPTION_CACHE_OFFSET + JVM.oopSize);
    public final PcDescContainer _pc_desc_container;
    public static final class MarkForDeoptimizationStatus {
        public static final int not_marked = 0,
                deoptimize = 1,
                deoptimize_noupdate = 2;
    }

    static {
        JVM.assertOffset(METHOD_OFFSET, JVM.computeOffset(JVM.oopSize, HAS_UNSAFE_ACCESS_OFFSET));
        JVM.assertOffset(EXCEPTION_CACHE_OFFSET, JVM.computeOffset(JVM.oopSize, PC_DESC_CONTAINER_OFFSET + PcDescCache.SIZE));
        JVM.assertOffset(SIZE, JVM.computeOffset(JVM.oopSize, GC_DATA_OFFSET + JVM.oopSize));
    }

    private Method methodCache;

    public CompiledMethod(long addr, Type type) {
        super(addr, type);
        _pc_desc_container=new PcDescContainer(addr+PC_DESC_CONTAINER_OFFSET);
    }

    @Override
    public boolean is_compiled() {
        return true;
    }


    public @RawCType("T*") long gc_data() {
        return unsafe.getAddress(this.address + GC_DATA_OFFSET);
    }

    public void set_gc_data(@RawCType("T*") long gc_data) {
        unsafe.putAddress(this.address + GC_DATA_OFFSET, gc_data);
    }

    public boolean has_unsafe_access() {
        return (PlatformInfo.isLittleEndian()? unsafe.getByte(this.address+HAS_UNSAFE_ACCESS_OFFSET)&1 :unsafe.getByte(this.address+HAS_UNSAFE_ACCESS_OFFSET)&0b10000000)!=0;
    }

    public void set_has_unsafe_access(boolean z) {
        if (PlatformInfo.isLittleEndian()){
            unsafe.putByte(this.address+HAS_UNSAFE_ACCESS_OFFSET, (byte) ((unsafe.getByte(this.address+HAS_UNSAFE_ACCESS_OFFSET)&~1)|(z?1:0)));
        }else {
            unsafe.putByte(this.address+HAS_UNSAFE_ACCESS_OFFSET, (byte) ((unsafe.getByte(this.address+HAS_UNSAFE_ACCESS_OFFSET)&~0b10000000)|(z?0b10000000:0)));
        }
    }

    public boolean has_method_handle_invokes() {
        return (PlatformInfo.isLittleEndian()? unsafe.getByte(this.address+HAS_METHOD_HANDLE_INVOKES_OFFSET)&0b10 :unsafe.getByte(this.address+HAS_METHOD_HANDLE_INVOKES_OFFSET)&0b1000000)!=0;
    }

    public void set_has_method_handle_invokes(boolean z) {
        if (PlatformInfo.isLittleEndian()){
            unsafe.putByte(this.address+HAS_METHOD_HANDLE_INVOKES_OFFSET, (byte) ((unsafe.getByte(this.address+HAS_METHOD_HANDLE_INVOKES_OFFSET)&~0b10)|(z?0b10:0)));
        }else {
            unsafe.putByte(this.address+HAS_METHOD_HANDLE_INVOKES_OFFSET, (byte) ((unsafe.getByte(this.address+HAS_METHOD_HANDLE_INVOKES_OFFSET)&~0b1000000)|(z?0b1000000:0)));
        }
    }

    public boolean has_wide_vectors() {
        return (PlatformInfo.isLittleEndian()? unsafe.getByte(this.address+HAS_WIDE_VECTORS_OFFSET)&0b100 :unsafe.getByte(this.address+HAS_WIDE_VECTORS_OFFSET)&0b100000)!=0;
    }

    public void set_has_wide_vectors(boolean z) {
        if (PlatformInfo.isLittleEndian()){
            unsafe.putByte(this.address+HAS_WIDE_VECTORS_OFFSET, (byte) ((unsafe.getByte(this.address+HAS_WIDE_VECTORS_OFFSET)&~0b100)|(z?0b100:0)));
        }else {
            unsafe.putByte(this.address+HAS_WIDE_VECTORS_OFFSET, (byte) ((unsafe.getByte(this.address+HAS_WIDE_VECTORS_OFFSET)&~0b100000)|(z?0b100000:0)));
        }
    }
    public abstract boolean is_in_use();
    public abstract int comp_level();
    public abstract int compile_id();
    public abstract boolean make_not_used();
    public abstract boolean make_not_entrant();
    public abstract boolean make_entrant();
    public abstract @RawCType("address")long entry_point();
    public abstract boolean make_zombie();
    public abstract boolean is_osr_method();
    public abstract int osr_entry_bci();


    public Method method() {
        long addr = unsafe.getAddress(this.address + METHOD_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.methodCache, addr)) {
            this.methodCache = Method.getOrCreate(addr);
        }
        return this.methodCache;
    }

    public boolean is_native_method() {
        Method method = method();
        return method != null && method.is_native();
    }

    public boolean is_java_method() {
        Method method = method();
        return method != null && !method.is_native();
    }

    public boolean  is_marked_for_deoptimization(){
        return unsafe.getInt(this.address+MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET) != MarkForDeoptimizationStatus.not_marked;
    }

    public boolean update_recompile_counts(){
        // Update recompile counts when either the update is explicitly requested (deoptimize)
        // or the nmethod is not marked for deoptimization at all (not_marked).
        // The latter happens during uncommon traps when deoptimized nmethod is made not entrant.
        return unsafe.getInt(this.address+MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET) != MarkForDeoptimizationStatus.deoptimize_noupdate;
    }

    /**tells whether frames described by this nmethod can be deoptimized<br>
     * note: native wrappers cannot be deoptimized.*/
    public boolean can_be_deoptimized(){
        return is_java_method();
    }

    public abstract Oop oop_at(int index);
    public abstract Metadata metadata_at(int index);

    public @RawCType("address")long scopes_data_begin(){
        return unsafe.getAddress(this.address+SCOPES_DATA_BEGIN_OFFSET);
    }
    public abstract @RawCType("address")long scopes_data_end();
    public int scopes_data_size(){
        return (int) (scopes_data_end() - scopes_data_begin());
    }

    public abstract PcDesc scopes_pcs_begin();
    public abstract PcDesc scopes_pcs_end();
    public int scopes_pcs_size(){
        return (int) (scopes_pcs_end().address -scopes_pcs_begin().address);
    }

    public @RawCType("address") long insts_begin() {
        return code_begin();
    }

    public @RawCType("address") long insts_end() {
        return stub_begin();
    }

    /** Returns true if a given address is in the 'insts' section. The method insts_contains_inclusive() is end-inclusive.*/
    public boolean insts_contains(@RawCType("address") long addr) {
        return insts_begin() <= addr && addr < insts_end();
    }

    public boolean insts_contains_inclusive(@RawCType("address") long addr) {
        return insts_begin() <= addr && addr <= insts_end();
    }

    public int insts_size() {
        return (int) (insts_end() - insts_begin());
    }

    public abstract @RawCType("address") long consts_begin();

    public abstract @RawCType("address") long consts_end();

    public boolean consts_contains(@RawCType("address") long addr) {
        return consts_begin() <= addr && addr < consts_end();
    }

    public int consts_size() {
        return (int) (consts_end() - consts_begin());
    }

    public abstract @RawCType("address") long stub_begin();

    public abstract @RawCType("address") long stub_end() ;

    public boolean stub_contains(@RawCType("address") long addr) {
        return stub_begin() <= addr && addr < stub_end();
    }

    public int stub_size() {
        return (int) (stub_end() - stub_begin());
    }

    public abstract @RawCType("address") long handler_table_begin();
    public abstract @RawCType("address") long handler_table_end();
    public boolean handler_table_contains(@RawCType("address") long addr){
        return handler_table_begin() <= addr && addr < handler_table_end();
    }
    public int handler_table_size() {
        return (int) (handler_table_end() - handler_table_begin());
    }

    public abstract @RawCType("address") long exception_begin();

    public abstract @RawCType("address") long nul_chk_table_begin();
    public abstract @RawCType("address") long nul_chk_table_end();
    public boolean nul_chk_table_contains(@RawCType("address") long addr){
        return nul_chk_table_begin() <= addr && addr < nul_chk_table_end();
    }
    public int nul_chk_table_size(){
        return (int) (nul_chk_table_end() - nul_chk_table_begin());
    }

    public ExceptionCache exception_cache(){
        long addr=unsafe.getAddress(this.address+EXCEPTION_CACHE_OFFSET);
        if (addr==0L){
            return null;
        }
        return new ExceptionCache(addr);
    }

    // ScopeDesc retrieval operation
    public PcDesc pc_desc_at(@RawCType("address") long pc)   { return find_pc_desc(pc, false); }
    // pc_desc_near returns the first PcDesc at or after the given pc.
    public PcDesc pc_desc_near(@RawCType("address") long pc) { return find_pc_desc(pc, true); }

    public PcDesc find_pc_desc(@RawCType("address") long pc, boolean approximate) {
        return _pc_desc_container.find_pc_desc(pc, approximate,new PcDescSearch(code_begin(), scopes_pcs_begin(), scopes_pcs_end()));
    }

    public ScopeDesc scope_desc_at(@RawCType("address")long pc) {
        PcDesc pd = pc_desc_at(pc);
        if (pd==null){
            throw new IllegalStateException("scope must be present");
        }
        return new ScopeDesc(this, pd);
    }

    public ScopeDesc scope_desc_near(@RawCType("address")long pc) {
        PcDesc pd = pc_desc_near(pc);
        if (pd==null){
            throw new IllegalStateException("scope must be present");
        }
        return new ScopeDesc(this, pd);
    }

    public @RawCType("address") long deopt_mh_handler_begin(){
        return unsafe.getAddress(this.address+DEOPT_MH_HANDLER_BEGIN_OFFSET);
    }

    public @RawCType("address") long deopt_handler_begin(){
        return unsafe.getAddress(this.address+DEOPT_HANDLER_BEGIN_OFFSET) ;
    }

    public abstract @RawCType("address")long get_original_pc(Frame fr);

    public boolean is_method_handle_return(@RawCType("address")long return_pc) {
        if (!has_method_handle_invokes())  return false;
        PcDesc pd = pc_desc_at(return_pc);
        if (pd == null)
            return false;
        return pd.is_method_handle_invoke();
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

    public NativeCallWrapper call_wrapper_at(long call) {
        throw new UnsupportedOperationException();
    }

    public boolean is_deopt_pc(@RawCType("address")long pc) { return is_deopt_entry(pc) || is_deopt_mh_entry(pc); }

    // When using JVMCI the address might be off by the size of a call instruction.
    public boolean is_deopt_entry(@RawCType("address")long pc) {
        if (JVM.includeJVMCI){
            return pc == deopt_handler_begin() ||
                    (is_compiled_by_jvmci() && pc == (deopt_handler_begin() + NativeCall.instruction_size()));
        }
        return pc == deopt_handler_begin();
    }

    public boolean is_deopt_mh_entry(@RawCType("address")long pc) {
        if (JVM.includeJVMCI){
            return pc == deopt_mh_handler_begin() ||
                    (is_compiled_by_jvmci() && pc == (deopt_mh_handler_begin() + NativeCall.instruction_size()));
        }
        return pc == deopt_mh_handler_begin();
    }

    // Return the original PC for the given PC if:
    // (a) the given PC belongs to a nmethod and
    // (b) it is a deopt PC
    public static @RawCType("address")long get_deopt_original_pc(Frame fr) {
        if (fr.cb() == null)
            return 0L;

        CompiledMethod cm = fr.cb().as_compiled_method_or_null();
        if (cm != null && cm.is_deopt_pc(fr.pc()))
            return cm.get_original_pc(fr);
        return 0L;
    }

    public abstract boolean is_unloading();
    public abstract @RawCType("address")long verified_entry_point();
}
