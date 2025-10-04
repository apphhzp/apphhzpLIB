package apphhzp.lib.hotspot.asm.x86;

import apphhzp.lib.hotspot.asm.AbstractRegisterImpl;

public final class X86KRegisterImpl extends AbstractRegisterImpl {
    public static final int number_of_registers = 8,
    // opmask registers are 64bit wide on both 32 and 64 bit targets.
    // thus two slots are reserved per register.
    max_slots_per_register = 2;
    private X86KRegisterImpl() {}
}
