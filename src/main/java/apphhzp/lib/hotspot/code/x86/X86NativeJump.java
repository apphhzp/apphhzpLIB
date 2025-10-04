package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class X86NativeJump extends X86NativeInstruction{
    public X86NativeJump(long addr) {
        super(addr);
    }

    public static final class Intel_specific_constants {
        public static final int
        instruction_code            = 0xe9,
        instruction_size            =    5,
        instruction_offset          =    0,
        data_offset                 =    1,
        next_instruction_offset     =    5;
    };

    public @RawCType("address")long instruction_address(){ return addr_at(Intel_specific_constants.instruction_offset); }
    public @RawCType("address")long next_instruction_address(){ return addr_at(Intel_specific_constants.next_instruction_offset); }
    public @RawCType("address")long jump_destination(){
        @RawCType("address")long dest = (int_at(Intel_specific_constants.data_offset)+next_instruction_address());
        // 32bit used to encode unresolved jmp as jmp -1
        // 64bit can't produce this so it used jump to self.
        // Now 32bit and 64bit use jump to self as the unresolved address
        // which the inline cache code (and relocs) know about

        // return -1 if jump to self
        dest = (dest == this.address) ? -1L : dest;
        return dest;
    }

    public void verify() {
        if ((unsafe.getByte(instruction_address())&0xff) != Intel_specific_constants.instruction_code) {
            // far jump
            X86NativeMovConstReg mov = X86NativeMovConstReg.nativeMovConstReg_at(instruction_address());
            X86NativeInstruction jmp = nativeInstruction_at(mov.next_instruction_address());
            if (!jmp.is_jump_reg()) {
                throw new RuntimeException("not a jump instruction");
            }
        }
    }

    public static X86NativeJump nativeJump_at(@RawCType("address")long address) {
        X86NativeJump jump = new X86NativeJump(address - Intel_specific_constants.instruction_offset);
        if (JVM.ENABLE_EXTRA_CHECK){
            jump.verify();
        }
        return jump;
    }

    public void  set_jump_destination(@RawCType("address")long dest)  {
        @RawCType("intptr_t")long val = dest - next_instruction_address();
        if (dest == -1L) {
            val = -5; // jump to self
        }
        if (PlatformInfo.isX86_64()){
            if (!((Math.abs(val)  & 0xFFFFFFFF00000000L) == 0 || dest == -1L)){
                throw new RuntimeException("must be 32bit offset or -1");
            }
        }

        set_int_at(Intel_specific_constants.data_offset, (int) val);
    }

}
