package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class X86NativeIllegalInstruction extends X86NativeInstruction{

    public X86NativeIllegalInstruction(long addr) {
        super(addr);
    }

    public static final class Intel_specific_constants {
        public static final int
        instruction_code            = 0x0B0F,    // Real byte order is: 0x0F, 0x0B
        instruction_size            =    2,
        instruction_offset          =    0,
        next_instruction_offset     =    2;
    };

    // Insert illegal opcode as specific address
    public static void insert(@RawCType("address")long code_pos){
        unsafe.putShort(code_pos, (short) Intel_specific_constants.instruction_code);
        //ICache::invalidate_range(code_pos, instruction_size);
    }
}
