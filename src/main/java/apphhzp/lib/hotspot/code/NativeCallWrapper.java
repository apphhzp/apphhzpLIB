package apphhzp.lib.hotspot.code;

public abstract class NativeCallWrapper {
    public abstract long/*address*/ destination();
    public abstract long/*address*/ instruction_address();
    public abstract long/*address*/ next_instruction_address();
    public abstract long/*address*/ return_address();
    //public abstract long/*address*/ get_resolve_call_stub(boolean is_optimized);
    //public abstract void set_destination_mt_safe(long/*address*/ dest);
    //public abstract void set_to_interpreted(Method method, CompiledICInfo& info);
    //public abstract void verify();
    //public abstract void verify_resolve_call(long/*address*/ dest);

    //public abstract boolean is_call_to_interpreted(long/*address*/ dest);
    //public abstract boolean is_safe_for_patching();

    //public abstract NativeInstruction* get_load_instruction(virtual_call_Relocation* r);

    //public abstract void *get_data(NativeInstruction* instruction);
    //public abstract void set_data(NativeInstruction* instruction, intptr_t data) ;
}
