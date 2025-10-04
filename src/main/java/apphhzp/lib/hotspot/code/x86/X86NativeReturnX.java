package apphhzp.lib.hotspot.code.x86;

public class X86NativeReturnX extends X86NativeInstruction{

    public static final class Intel_specific_constants {
        public static final int
        instruction_code            = 0xC2,
        instruction_size            =    2,
        instruction_offset          =    0,
        next_instruction_offset     =    2;
    };
    public X86NativeReturnX(long addr) {
        super(addr);
    }
}
