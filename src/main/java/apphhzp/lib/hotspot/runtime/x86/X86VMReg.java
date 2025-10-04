package apphhzp.lib.hotspot.runtime.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.asm.x86.*;
import apphhzp.lib.hotspot.code.VMReg;

import static apphhzp.lib.helfy.JVM.is_even;

public class X86VMReg extends VMReg {
    public X86VMReg(int value) {
        super(value);
    }
    public boolean is_Register() {
        return (value()&0xffffffffL) < (X86ConcreteRegisterImpl.max_gpr&0xffffffffL);
    }

    public boolean is_FloatRegister() {
        return value() >= X86ConcreteRegisterImpl.max_gpr && value() < X86ConcreteRegisterImpl.max_fpr;
    }

    public boolean is_XMMRegister() {
        int uarch_max_xmm = X86ConcreteRegisterImpl.max_xmm;
        if (JVM.isLP64 &&JVM.getFlag("UseAVX").getIntx() < 3) {
            int half_xmm = (X86XMMRegisterImpl.max_slots_per_register * X86XMMRegisterImpl.number_of_registers) / 2;
            uarch_max_xmm -= half_xmm;
        }
        return (value() >= X86ConcreteRegisterImpl.max_fpr && value() < uarch_max_xmm);
    }

    public boolean is_KRegister() {
        if (JVM.getFlag("UseAVX").getIntx() > 2) {
            return value() >= X86ConcreteRegisterImpl.max_xmm && value() < X86ConcreteRegisterImpl.max_kpr;
        } else {
            return false;
        }
    }

    public X86Register as_Register() {
        if (!is_Register()){
            throw new RuntimeException("must be");
        }
        // Yuk
        return PlatformInfo.isX86_64()? new X86Register(value() >> 1):new X86Register(value());
    }

    public X86FloatRegister as_FloatRegister() {
        if (!(is_FloatRegister() && is_even(value()))){
            throw new RuntimeException("must be");
        }
        // Yuk
        return new X86FloatRegister((value() - X86ConcreteRegisterImpl.max_gpr) >> 1);
    }

    public X86XMMRegister as_XMMRegister() {
        if (!(is_XMMRegister() && is_even(value()))){
            throw new RuntimeException("must be");
        }
        // Yuk
        return new X86XMMRegister((value() - X86ConcreteRegisterImpl.max_fpr) >> 4);
    }

    public X86KRegister as_KRegister() {
        if (!is_KRegister()){
            throw new RuntimeException("must be");
        }
        // Yuk
        return new X86KRegister((value() - X86ConcreteRegisterImpl.max_xmm) >> 1);
    }

    public boolean is_concrete() {
        if (!is_reg()){
            throw new RuntimeException("must be");
        }
        if ((!PlatformInfo.isX86_64())&&is_Register()){
            return true;
        }
        // Do not use is_XMMRegister() here as it depends on the UseAVX setting.
        if (value() >= X86ConcreteRegisterImpl.max_fpr && value() < X86ConcreteRegisterImpl.max_xmm) {
            int base = value() - X86ConcreteRegisterImpl.max_fpr;
            return base % X86XMMRegisterImpl.max_slots_per_register == 0;
        } else {
            return is_even(value());   // General, float, and K registers are all two slots wide
        }
    }
}
