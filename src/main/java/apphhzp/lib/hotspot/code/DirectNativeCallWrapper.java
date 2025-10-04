package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.runtime.SharedRuntime;
import apphhzp.lib.hotspot.util.RawCType;

public class DirectNativeCallWrapper extends NativeCallWrapper{
    private  NativeCall _call;
    public DirectNativeCallWrapper(NativeCall addr) {
        if (true){
            throw new UnsupportedOperationException("Incomplete");
        }
        _call=addr;
    }

    @Override
    public @RawCType("address") long destination() {
        return _call.destination();
    }

    @Override
    public @RawCType("address") long instruction_address() {
        return _call.instruction_address();
    }

    @Override
    public @RawCType("address") long next_instruction_address() {
        return _call.next_instruction_address();
    }

    @Override
    public @RawCType("address") long return_address() {
        return _call.return_address();
    }

    @Override
    public long get_resolve_call_stub(boolean is_optimized) {
        if (is_optimized) {
            return SharedRuntime.get_resolve_opt_virtual_call_stub();
        }
        return SharedRuntime.get_resolve_virtual_call_stub();
    }

    public void set_destination_mt_safe(@RawCType("address")long dest) {
        throw new UnsupportedOperationException("TODO");
    }

//    public void set_to_interpreted(Method method, CompiledICInfo& info) {
//        CompiledDirectStaticCall* csc = CompiledDirectStaticCall::at(instruction_address());
//        {
//            csc->set_to_interpreted(method, info.entry());
//        }
//    }

    public void verify() {
        // make sure code pattern is actually a call imm32 instruction
        _call.verify();
        _call.verify_alignment();
    }

    public void verify_resolve_call(@RawCType("address")long dest) {
        CodeBlob db = CodeCache.findBlobUnsafe(dest);
        if (!(db != null && !db.is_adapter_blob())){
            throw new RuntimeException("must use stub!");
        }
    }

    public boolean is_call_to_interpreted(@RawCType("address")long dest) {
        CodeBlob cb = CodeCache.find_blob(_call.instruction_address());
        return cb.contains(dest);
    }

    public boolean is_safe_for_patching() { return false; }

//    public NativeInstruction get_load_instruction(virtual_call_Relocation* r) {
//        return nativeMovConstReg_at(r->cached_value());
//    }

    public @RawCType("void*")long get_data(NativeInstruction instruction) {
        return ((NativeMovConstReg)instruction).data();
    }

    public void set_data(NativeInstruction instruction, @RawCType("intptr_t")long data) {
        ((NativeMovConstReg) instruction).set_data(data);
    }
}
