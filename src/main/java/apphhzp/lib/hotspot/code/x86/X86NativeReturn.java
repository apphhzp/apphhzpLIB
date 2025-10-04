package apphhzp.lib.hotspot.code.x86;

public class X86NativeReturn extends X86NativeInstruction{

    public static final class Intel_specific_constants {
        public static final int
        instruction_code            = 0xC3,
        instruction_size            =    1,
        instruction_offset          =    0,
        next_instruction_offset     =    1;
    }
    public X86NativeReturn(long addr) {
        super(addr);
    }
}
