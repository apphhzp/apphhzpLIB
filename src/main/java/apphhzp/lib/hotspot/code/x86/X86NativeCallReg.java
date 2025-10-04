package apphhzp.lib.hotspot.code.x86;

public class X86NativeCallReg extends X86NativeInstruction{

    public static final class Intel_specific_constants {
        public static final int
        instruction_code            = 0xFF,
        instruction_offset          =    0,
        return_address_offset_norex =    2,
        return_address_offset_rex   =    3;
    };

    public int next_instruction_offset(){
        if (ubyte_at(0) == Intel_specific_constants.instruction_code) {
            return Intel_specific_constants.return_address_offset_norex;
        } else {
            return Intel_specific_constants.return_address_offset_rex;
        }
    }
    public X86NativeCallReg(long addr) {
        super(addr);
    }
}
