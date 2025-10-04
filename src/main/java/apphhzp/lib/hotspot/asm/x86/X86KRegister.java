package apphhzp.lib.hotspot.asm.x86;

import apphhzp.lib.hotspot.asm.AbstractRegister;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.VMRegImpl;

public class X86KRegister extends AbstractRegister {
    public X86KRegister(int value) {
        super(value);
    }

    @Override
    public VMReg as_VMReg() {
        return VMRegImpl.as_VMReg((encoding() << 1) + X86ConcreteRegisterImpl.max_xmm);
    }

    // derived registers, offsets, and addresses
    public X86KRegister successor() {
        return new X86KRegister(encoding() + 1);
    }

    // accessors
    public int encoding(){
        if (!is_valid()){
            throw new RuntimeException("invalid register ("+this.value+")");
        }
        return this.value;
    }

    public boolean is_valid(){
        return 0 <= this.value && this.value < X86KRegisterImpl.number_of_registers;
    }
    private static final String[] names =new String[]{
            "k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7"
    };
    public String name(){
        return is_valid() ? names[encoding()] : "knoreg";
    }
}
