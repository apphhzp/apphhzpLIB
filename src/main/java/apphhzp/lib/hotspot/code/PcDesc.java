package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** PcDescs map a physical PC (given as offset from start of nmethod) to
 * the corresponding source scope and byte code index.*/
public class PcDesc extends JVMObject {
    public static final Type TYPE = JVM.type("PcDesc");
    public static final int SIZE = TYPE.size;
    public static final long PC_OFFSET_OFFSET = TYPE.offset("_pc_offset");
    public static final long SCOPE_DECODE_OFFSET_OFFSET = TYPE.offset("_scope_decode_offset");
    public static final long OBJ_DECODE_OFFSET_OFFSET = TYPE.offset("_obj_decode_offset");
    public static final long FLAGS_OFFSET = TYPE.offset("_flags");
    public static final int PCDESC_reexecute = JVM.intConstant("PcDesc::PCDESC_reexecute"),
            PCDESC_is_method_handle_invoke = JVM.intConstant("PcDesc::PCDESC_is_method_handle_invoke"),
            PCDESC_return_oop = JVM.intConstant("PcDesc::PCDESC_return_oop"),
            PCDESC_rethrow_exception = 1 << 3,
            PCDESC_has_ea_local_in_scope = 1 << 4,
            PCDESC_arg_escape = 1 << 5,
            PCDESC_is_optimized_linkToNative = 1 << 6;
    public static final int
            // upper and lower exclusive limits real offsets:
            lower_offset_limit = -1,
            upper_offset_limit = -1 >>> 1;

    public PcDesc(long addr) {
        super(addr);
        if (addr==0L){

        }
    }

    public void set_flag(int mask, boolean z) {
        unsafe.putInt(this.address + FLAGS_OFFSET, z ? (flags() | mask) : (flags() & ~mask));
    }

    public int flags() {
        return unsafe.getInt(this.address + FLAGS_OFFSET);
    }

    /**
     * offset from start of nmethod
     */
    public int pc_offset() {
        return unsafe.getInt(this.address + PC_OFFSET_OFFSET);
    }

    /**
     * offset for scope in nmethod
     */
    public int scope_decode_offset() {
        return unsafe.getInt(this.address + SCOPE_DECODE_OFFSET_OFFSET);
    }

    public int obj_decode_offset() {
        return unsafe.getInt(this.address + OBJ_DECODE_OFFSET_OFFSET);
    }

    public void set_pc_offset(int x) {
        unsafe.putInt(this.address + PC_OFFSET_OFFSET, x);
    }

    public void set_scope_decode_offset(int x) {
        unsafe.putInt(this.address + SCOPE_DECODE_OFFSET_OFFSET, x);
    }

    public void set_obj_decode_offset(int x) {
        unsafe.putInt(this.address + OBJ_DECODE_OFFSET_OFFSET, x);
    }

    // Flags
    public boolean rethrow_exception() {
        return (flags() & PCDESC_rethrow_exception) != 0;
    }

    public void set_rethrow_exception(boolean z) {
        set_flag(PCDESC_rethrow_exception, z);
    }

    public boolean should_reexecute() {
        return (flags() & PCDESC_reexecute) != 0;
    }

    public void set_should_reexecute(boolean z) {
        set_flag(PCDESC_reexecute, z);
    }

    /** Does pd refer to the same information as pd?*/
    public boolean is_same_info(PcDesc pd) {
        return scope_decode_offset() == pd.scope_decode_offset() &&
                obj_decode_offset() == pd.obj_decode_offset() &&
                flags() == pd.flags();
    }

    public boolean is_method_handle_invoke() {
        return (flags() & PCDESC_is_method_handle_invoke) != 0;
    }

    public void set_is_method_handle_invoke(boolean z) {
        set_flag(PCDESC_is_method_handle_invoke, z);
    }

    public boolean is_optimized_linkToNative() {
        return (flags() & PCDESC_is_optimized_linkToNative) != 0;
    }

    public void set_is_optimized_linkToNative(boolean z) {
        set_flag(PCDESC_is_optimized_linkToNative, z);
    }

    public boolean return_oop() {
        return (flags() & PCDESC_return_oop) != 0;
    }

    public void set_return_oop(boolean z) {
        set_flag(PCDESC_return_oop, z);
    }

    /** Indicates if there are objects in scope that, based on escape analysis, are local to the
     * compiled method or local to the current thread, i.e. NoEscape or ArgEscape*/
    public boolean has_ea_local_in_scope() {
        return (flags() & PCDESC_has_ea_local_in_scope) != 0;
    }

    public void set_has_ea_local_in_scope(boolean z) {
        set_flag(PCDESC_has_ea_local_in_scope, z);
    }

    /** Indicates if this pc descriptor is at a call site where objects that do not escape the
     * current thread are passed as arguments.*/
    public boolean arg_escape() {
        return (flags() & PCDESC_arg_escape) != 0;
    }

    public void set_arg_escape(boolean z) {
        set_flag(PCDESC_arg_escape, z);
    }

    public @RawCType("address")long real_pc(CompiledMethod code){
        return code.code_begin() + pc_offset();
    }
    public void print_on(PrintStream st, CompiledMethod code) {
        st.println("PcDesc(pc=0x"+Long.toHexString(real_pc(code))+" offset="+pc_offset()+" bits="+flags()+"):");
        for (ScopeDesc sd = code.scope_desc_at(real_pc(code));
             sd != null;
             sd = sd.sender()) {
            sd.print_on(st);
        }
    }
}
