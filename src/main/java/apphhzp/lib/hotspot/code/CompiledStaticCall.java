package apphhzp.lib.hotspot.code;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

public abstract class CompiledStaticCall {

    // Code

    // Returns NULL if CodeBuffer::expand fails
    //public static @RawCType("address")long emit_to_interp_stub(CodeBuffer &cbuf, address mark = NULL);
    public static int to_interp_stub_size(){
        if (PlatformInfo.isX86()){
            if (JVM.isLP64){
                return 15;// movq (1+1+8); jmp (1+4)
            }else {
                return 10;// movl; jmp
            }
        }else {
            throw new UnsupportedOperationException();
        }
    }
    public static int to_trampoline_stub_size(){
        if (PlatformInfo.isX86()){
            // x86 doesn't use trampolines.
            return 0;
        }else {
            throw new UnsupportedOperationException();
        }
    }
    public static int reloc_to_interp_stub(){
        if (PlatformInfo.isX86()){
            return 4; // 3 in emit_to_interp_stub + 1 in emit_call
        }else {
            throw new UnsupportedOperationException();
        }
    }

    // Compute entry point given a method
    public static void compute_entry(Method m, boolean caller_is_nmethod, StaticCallInfo info){
        CompiledMethod m_code = m.code();
        info._callee = m;
        if (m_code != null && m_code.is_in_use() && !m_code.is_unloading()) {
            info._to_interpreter = false;
            info._entry  = m_code.verified_entry_point();
        } else {
            // Callee is interpreted code.  In any case entering the interpreter
            // puts a converter-frame on the stack to save arguments.
            if (m.is_method_handle_intrinsic()){
                throw new RuntimeException("Compiled code should never call interpreter MH intrinsics");
            }
            info._to_interpreter = true;
            info._entry      = m.get_c2i_entry();
        }
    }


    // Clean static call (will force resolving on next use)
    public abstract @RawCType("address")long destination();
    public boolean set_to_clean(){
        return set_to_clean(true);
    }
    // Clean static call (will force resolving on next use)
    public boolean set_to_clean(boolean in_use){
        // Reset call site
        set_destination_mt_safe(resolve_call_stub());
        // Do not reset stub here:  It is too expensive to call find_stub.
        // Instead, rely on caller (nmethod::clear_inline_caches) to clear
        // both the call and its stub.
        return true;
    }

    // Set state. The entry must be the same, as computed by compute_entry.
    // Computation and setting is split up, since the actions are separate during
    // a OptoRuntime::resolve_xxx.
    public void set(StaticCallInfo info){
        // Updating a cache to the wrong entry can cause bugs that are very hard
        // to track down - if cache entry gets invalid - we just clean it. In
        // this way it is always the same code path that is responsible for
        // updating and resolving an inline cache
        if (!is_clean()){
            throw new RuntimeException("do not update a call entry - use clean");
        }
        if (info._to_interpreter) {
            // Call to interpreted code
            set_to_interpreted(info.callee(), info.entry());
        } else {
            set_to_compiled(info.entry());
        }
    }

    // State
    public boolean is_clean(){
        return  destination() == resolve_call_stub();
    }
    public  boolean is_call_to_compiled(){
        return CodeCache.contains(destination());
    }
    public abstract boolean is_call_to_interpreted();

    public abstract @RawCType("address")long instruction_address();

    protected abstract @RawCType("address")long resolve_call_stub();
    protected abstract void set_destination_mt_safe(@RawCType("address")long dest) ;
    protected abstract void set_to_interpreted(Method callee, @RawCType("address")long entry);
    protected abstract String name();

    protected void set_to_compiled(@RawCType("address")long entry){
        // Call to compiled code
        if (!(CodeCache.contains(entry))){
            throw new RuntimeException("wrong entry point");
        }
        set_destination_mt_safe(entry);
    }
}
