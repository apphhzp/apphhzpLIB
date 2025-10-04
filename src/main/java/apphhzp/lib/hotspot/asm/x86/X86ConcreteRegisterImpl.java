package apphhzp.lib.hotspot.asm.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.asm.ConcreteRegisterImpl;

public final class X86ConcreteRegisterImpl extends ConcreteRegisterImpl {
    public static final int max_gpr= PlatformInfo.isX86_64()?X86RegisterImpl.number_of_registers<<1:X86RegisterImpl.number_of_registers;
    public static final int max_fpr=max_gpr + 2 * X86FloatRegisterImpl.number_of_registers;
    public static final int max_xmm=max_fpr +
            X86XMMRegisterImpl.max_slots_per_register * X86XMMRegisterImpl.number_of_registers;
    public static final int max_kpr=max_xmm +
            X86KRegisterImpl.max_slots_per_register * X86KRegisterImpl.number_of_registers;
    private X86ConcreteRegisterImpl() {}
}
