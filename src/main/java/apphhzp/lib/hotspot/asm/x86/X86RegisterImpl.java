package apphhzp.lib.hotspot.asm.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.asm.RegisterImpl;

public final class X86RegisterImpl extends RegisterImpl {
    public static final int number_of_byte_registers = PlatformInfo.isX86_64() ? 16 : 4,
            max_slots_per_register = PlatformInfo.isX86_64() ? 2 : 1;
    private X86RegisterImpl() {}
    // The implementation of integer registers for the ia32 architecture
    public static X86Register as_Register(int encoding) {
        return new X86Register(encoding);
    }
}
