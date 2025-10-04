package apphhzp.lib.hotspot.asm;

import apphhzp.lib.helfy.JVM;

public class ConcreteRegisterImpl {
    public static final int number_of_registers= JVM.intConstant("ConcreteRegisterImpl::number_of_registers");
    protected ConcreteRegisterImpl() {
        throw new UnsupportedOperationException("ShouldNotReachHere");
    }
}
