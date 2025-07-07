package apphhzp.lib.hotspot.code;

public abstract class NativeCall {
    protected long address;
    public abstract long destination();
    public abstract long instruction_address();
    public abstract long next_instruction_address();
    public abstract long return_address();

}
