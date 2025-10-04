package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.NativeInstruction;
import apphhzp.lib.hotspot.cpu.x86.X86Assembler;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.code.x86.X86NativeInstruction.Intel_specific_constants.nop_instruction_code;

public class X86NativeInstruction extends JVMObject implements NativeInstruction {

    public X86NativeInstruction(long addr) {
        super(addr);
    }

    public static final class Intel_specific_constants {
        public static final int
                nop_instruction_code = 0x90,
                nop_instruction_size = 1;
    }


    public boolean is_nop() {
        return ubyte_at(0) == nop_instruction_code;
    }

    public boolean is_call(){
        return ubyte_at(0) == X86NativeCall.Intel_specific_constants.instruction_code;
    }

    public boolean is_call_reg(){
        return ubyte_at(0) == X86NativeCallReg.Intel_specific_constants.instruction_code ||
                (ubyte_at(1) == X86NativeCallReg.Intel_specific_constants.instruction_code &&
                        (ubyte_at(0) == X86Assembler.Prefix.REX || ubyte_at(0) == X86Assembler.Prefix.REX_B));
    }

    public boolean is_illegal(){
        return (short)int_at(0) == (short)X86NativeIllegalInstruction.Intel_specific_constants.instruction_code;
    }

    public boolean is_return(){
        return ubyte_at(0) == X86NativeReturn.Intel_specific_constants.instruction_code ||
                ubyte_at(0) == X86NativeReturnX.Intel_specific_constants.instruction_code;
    }

    public boolean is_jump(){
        return ubyte_at(0) == X86NativeJump.Intel_specific_constants.instruction_code ||
                ubyte_at(0) == 0xEB; /* short jump */
    }

    public boolean is_jump_reg(){
        int pos = 0;
        if (ubyte_at(0) == X86Assembler.Prefix.REX_B) {
            pos = 1;
        }
        return ubyte_at(pos) == 0xFF && (ubyte_at(pos + 1) & 0xF0) == 0xE0;
    }

    public boolean is_far_jump(){
        return is_mov_literal64();
    }

    public boolean is_cond_jump(){
        return (int_at(0) & 0xF0FF) == 0x800F /* long jump */ ||
                (ubyte_at(0) & 0xF0) == 0x70;  /* short jump */
    }

    public boolean is_safepoint_poll(){
        final int test_offset;
        if (PlatformInfo.isX86_64()){
            final boolean has_rex_prefix = ubyte_at(0) == X86NativeTstRegMem.Intel_specific_constants.instruction_rex_b_prefix;
            test_offset= has_rex_prefix ? 1 : 0;
        }else {
            test_offset = 0;
        }
        final boolean is_test_opcode = ubyte_at(test_offset) == X86NativeTstRegMem.Intel_specific_constants.instruction_code_memXregl;
        final boolean is_rax_target = (ubyte_at(test_offset + 1) & X86NativeTstRegMem.Intel_specific_constants.modrm_mask) == X86NativeTstRegMem.Intel_specific_constants.modrm_reg;
        return is_test_opcode && is_rax_target;
    }

    public boolean is_mov_literal64(){
        if (PlatformInfo.isX86_64()){
            return ((ubyte_at(0) == X86Assembler.Prefix.REX_W || ubyte_at(0) == X86Assembler.Prefix.REX_WB) &&
                    (ubyte_at(1) & (0xff ^ X86NativeMovConstReg.Intel_specific_constants.register_mask)) == 0xB8);
        }else {
            return false;
        }
    }


    protected @RawCType("address") long addr_at(int offset) {
        return (this.address) + offset;
    }

    protected @RawCType("s_char") byte sbyte_at(int offset) {
        return unsafe.getByte(addr_at(offset));
    }

    protected @RawCType("u_char") int ubyte_at(int offset) {
        return unsafe.getByte(addr_at(offset))&0xff;
    }

    protected int int_at(int offset) {
        return unsafe.getInt(addr_at(offset));
    }

    protected @RawCType("intptr_t") long ptr_at(int offset) {
        return unsafe.getAddress(addr_at(offset));
    }

    protected Oop oop_at(int offset) {
        return new Oop(addr_at(offset));
    }


    protected void set_char_at(int offset, @RawCType("char") byte c) {
        unsafe.putByte(addr_at(offset), (byte) (c&0xff));
        //wrote(offset);
    }

    protected void set_int_at(int offset, int i) {
        unsafe.putInt(addr_at(offset),i);
        //wrote(offset);
    }

    protected void set_ptr_at(int offset, @RawCType("intptr_t") long ptr) {
        unsafe.putAddress(addr_at(offset),ptr);
        //wrote(offset);
    }

    protected void set_oop_at(int offset, OopDesc o) {
        unsafe.putAddress(addr_at(offset),o.address);
        //wrote(offset);
    }

    public static X86NativeInstruction nativeInstruction_at(@RawCType("address")long address) {
        return new X86NativeInstruction(address);
    }
}