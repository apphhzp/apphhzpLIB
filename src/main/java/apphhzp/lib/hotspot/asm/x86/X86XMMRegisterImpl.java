package apphhzp.lib.hotspot.asm.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.asm.AbstractRegisterImpl;

public final class X86XMMRegisterImpl extends AbstractRegisterImpl {
    public static final int number_of_registers = PlatformInfo.isX86_64()?32:8,
    max_slots_per_register = 16;   // 512-bit
    private X86XMMRegisterImpl() {}
}
