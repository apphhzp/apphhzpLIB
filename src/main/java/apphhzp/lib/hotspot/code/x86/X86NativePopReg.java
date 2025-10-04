package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.hotspot.asm.x86.X86Register;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class X86NativePopReg extends X86NativeInstruction{
    public X86NativePopReg(long addr) {
        super(addr);
    }
    public static final class Intel_specific_constants {
        public static final int
        instruction_code            = 0x58,
        instruction_size            =    1,
        instruction_offset          =    0,
        data_offset                 =    1,
        next_instruction_offset     =    1;
    }

    // Insert a pop instruction
    public static void insert(@RawCType("address")long code_pos, X86Register reg){
        if (!(reg.encoding() < 8)){
            throw new UnsupportedOperationException("no space for REX");
        }
        unsafe.putByte(code_pos, (byte) ((Intel_specific_constants.instruction_code | reg.encoding())&0xff));
        //ICache::invalidate_range(code_pos, instruction_size);
    }
}
