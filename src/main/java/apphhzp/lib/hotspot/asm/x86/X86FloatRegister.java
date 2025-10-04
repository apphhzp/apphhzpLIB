package apphhzp.lib.hotspot.asm.x86;

import apphhzp.lib.hotspot.asm.AbstractRegister;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.VMRegImpl;

public class X86FloatRegister extends AbstractRegister {
    public X86FloatRegister(int value) {
        super(value);
    }

    @Override
    public VMReg as_VMReg() {
        return VMRegImpl.as_VMReg((encoding() << 1) + X86ConcreteRegisterImpl.max_gpr);
    }

    public X86FloatRegister successor() {
        return new X86FloatRegister(encoding() + 1);
    }

    // accessors
    public int encoding() {
        if (!is_valid()){
            throw new RuntimeException("invalid register");
        }
        return this.value;
    }

    public boolean is_valid() {
        return 0 <= this.value &&  this.value < X86FloatRegisterImpl.number_of_registers;
    }
    private static final String[] names = new String[]{
        "st0", "st1", "st2", "st3", "st4", "st5", "st6", "st7"
    };
    public String name() {
        return is_valid() ? names[encoding()] : "noreg";
    }
}
