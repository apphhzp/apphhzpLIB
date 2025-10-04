package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

// Handles all kinds of jump on Intel. Long/far, conditional/unconditional
public class X86NativeGeneralJump extends X86NativeInstruction{

    public static final class Intel_specific_constants {
        public static final int
        // Constants does not apply, since the lengths and offsets depends on the actual jump
        // used
        // Instruction codes:
        //   Unconditional jumps: 0xE9    (rel32off), 0xEB (rel8off)
        //   Conditional jumps:   0x0F8x  (rel32off), 0x7x (rel8off)
        unconditional_long_jump  = 0xe9,
        unconditional_short_jump = 0xeb,
        instruction_size = 5;
    };

    public @RawCType("address")long instruction_address(){
        return addr_at(0);
    }
    public @RawCType("address")long jump_destination(){
        int op_code = ubyte_at(0);
        boolean is_rel32off = (op_code == 0xE9 || op_code == 0x0F);
        int  offset  = (op_code == 0x0F)  ? 2 : 1;
        int  length  = offset + ((is_rel32off) ? 4 : 1);

        if (is_rel32off) {
            return addr_at(0) + length + int_at(offset);
        } else {
            return addr_at(0) + length + sbyte_at(offset);
        }
    }
    public X86NativeGeneralJump(long addr) {
        super(addr);
    }

    public void verify() {
        if (!(new X86NativeInstruction(this.address).is_jump() ||
                new X86NativeInstruction(this.address).is_cond_jump())){
            throw new RuntimeException("not a general jump instruction");
        }
    }

    public static X86NativeGeneralJump nativeGeneralJump_at(@RawCType("address")long address) {
        X86NativeGeneralJump jump = new X86NativeGeneralJump(address);
        if (JVM.ENABLE_EXTRA_CHECK){
            jump.verify();
        }
        return jump;
    }

    public static void insert_unconditional(@RawCType("address")long code_pos, @RawCType("address")long entry) {
        @RawCType("intptr_t")long disp = entry - (code_pos + 1 + 4);
        if (PlatformInfo.isX86_64()){
            if (!(disp == (long)(int)disp)){
                throw new RuntimeException("must be 32-bit offset");
            }
        }
        unsafe.putByte(code_pos, (byte) Intel_specific_constants.unconditional_long_jump);
        unsafe.putInt((code_pos+1), (int) disp);
        //ICache::invalidate_range(code_pos, instruction_size);
    }
}
