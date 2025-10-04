package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.util.RawCType;

public abstract class NativeCallWrapper {
    public abstract @RawCType("address") long destination();
    public abstract @RawCType("address") long instruction_address();
    public abstract @RawCType("address") long next_instruction_address();
    public abstract @RawCType("address") long return_address();
    public abstract @RawCType("address") long get_resolve_call_stub(boolean is_optimized);
    public abstract void set_destination_mt_safe(@RawCType("address") long dest);
    //public abstract void set_to_interpreted(Method method, CompiledICInfo& info);
    public abstract void verify();
    public abstract void verify_resolve_call(@RawCType("address") long dest);

    public abstract boolean is_call_to_interpreted(@RawCType("address") long dest);
    public abstract boolean is_safe_for_patching();

    //public abstract NativeInstruction* get_load_instruction(virtual_call_Relocation* r);

    public abstract @RawCType("void*")long get_data(NativeInstruction instruction);
    public abstract void set_data(NativeInstruction instruction, @RawCType("intptr_t")long data);
}
