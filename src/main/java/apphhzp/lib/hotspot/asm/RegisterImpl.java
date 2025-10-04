package apphhzp.lib.hotspot.asm;

import apphhzp.lib.helfy.JVM;

public class RegisterImpl {
    public static final int number_of_registers= JVM.intConstant("RegisterImpl::number_of_registers");
    protected RegisterImpl() {
        throw new UnsupportedOperationException("ShouldNotReachHere");
    }
}
