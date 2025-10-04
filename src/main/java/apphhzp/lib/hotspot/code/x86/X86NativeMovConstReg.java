package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.NativeMovConstReg;
import apphhzp.lib.hotspot.cpu.x86.X86Assembler;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class X86NativeMovConstReg extends X86NativeInstruction implements NativeMovConstReg {
    private static final boolean has_rex = PlatformInfo.isX86_64();
    private static final int rex_size = PlatformInfo.isX86_64()?1:0;

    public static final class Intel_specific_constants {
        public static final int
        instruction_code            = 0xB8,
        instruction_size            =    1 + rex_size + JVM.wordSize,
        instruction_offset          =    0,
        data_offset                 =    1 + rex_size,
        next_instruction_offset     =    instruction_size,
        register_mask               = 0x07;
    }

    public @RawCType("address")long instruction_address(){ return addr_at(Intel_specific_constants.instruction_offset); }
    public @RawCType("address")long next_instruction_address(){ return addr_at(Intel_specific_constants.next_instruction_offset); }
    public @RawCType("intptr_t")long data(){ return ptr_at(Intel_specific_constants.data_offset); }
    public void  set_data(@RawCType("intptr_t")long x){ set_ptr_at(Intel_specific_constants.data_offset, x); }
    public X86NativeMovConstReg(long addr) {
        super(addr);
    }
    public void  verify(){
        if (PlatformInfo.isX86_64()){
            // make sure code pattern is actually a mov reg64, imm64 instruction
            if ((ubyte_at(0) != X86Assembler.Prefix.REX_W && ubyte_at(0) != X86Assembler.Prefix.REX_WB) ||
                    (ubyte_at(1) & (0xff ^ Intel_specific_constants.register_mask)) != 0xB8) {
                print(System.err);
                throw new RuntimeException("not a REX.W[B] mov reg64, imm64");
            }
        }else {
            // make sure code pattern is actually a mov reg, imm32 instruction
            @RawCType("u_char")int test_byte = unsafe.getByte(instruction_address())&0xff;
            @RawCType("u_char")int test_byte_2 = test_byte & ( 0xff ^ Intel_specific_constants.register_mask);
            if (test_byte_2 != Intel_specific_constants.instruction_code) {
                throw new RuntimeException("not a mov reg, imm32");
            }
        }
    }
    public static X86NativeMovConstReg nativeMovConstReg_at(@RawCType("address")long address) {
        X86NativeMovConstReg test = new X86NativeMovConstReg(address - X86NativeMovConstReg.Intel_specific_constants.instruction_offset);
        if(JVM.ENABLE_EXTRA_CHECK) {
            test.verify();
        }
        return test;
    }

    public static X86NativeMovConstReg nativeMovConstReg_before(@RawCType("address")long address) {
        X86NativeMovConstReg test = new X86NativeMovConstReg(address - X86NativeMovConstReg.Intel_specific_constants.instruction_size - X86NativeMovConstReg.Intel_specific_constants.instruction_offset);
        if(JVM.ENABLE_EXTRA_CHECK) {
            test.verify();
        }
        return test;
    }


    public void print(PrintStream printStream) {
        printStream.println("0x"+Long.toHexString(instruction_address())+": mov reg, 0x"+Long.toHexString(data()));
    }
}
