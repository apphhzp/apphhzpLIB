package apphhzp.lib.hotspot.asm.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.asm.AbstractRegister;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.VMRegImpl;

public class X86Register extends AbstractRegister {
    public X86Register(int value) {
        super(value);
    }

    public X86Register successor() {
        return new X86Register(encoding() + 1);
    }

    public VMReg as_VMReg() {
        if(this.value==-1){
            return VMRegImpl.Bad();
        }
        return PlatformInfo.isX86_64()?VMRegImpl.as_VMReg(encoding() << 1 ):VMRegImpl.as_VMReg(encoding() );
    }

    // accessors
    public int encoding() {
        if (!is_valid()){
            throw new RuntimeException("invalid register");
        }
        return this.value;
    }

    public boolean is_valid() {
        return 0 <=  this.value && this.value < X86RegisterImpl.number_of_registers;
    }

    public boolean has_byte_register() {
        return 0 <= this.value && this.value < X86RegisterImpl.number_of_byte_registers;
    }

    private static final String[] names = PlatformInfo.isX86_64()?
            new String[]{
                    "rax", "rcx", "rdx", "rbx", "rsp", "rbp", "rsi", "rdi",
                    "r8",  "r9",  "r10", "r11", "r12", "r13", "r14", "r15"}:
            new String[]{
                    "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi"
    };
    public String name() {
        return is_valid() ? names[encoding()] : "noreg";
    }
}
