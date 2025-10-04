package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.cpu.x86.X86Assembler;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

// An interface for accessing/manipulating native leal instruction of form:
//        leal reg, [reg + offset]
public class X86NativeLoadAddress extends X86NativeMovRegMem {
    private static final boolean has_rex= PlatformInfo.isX86_64();
    private static final int rex_size=PlatformInfo.isX86_64()?1:0;

    public static final class Intel_specific_constants {
        public static final int
        instruction_prefix_wide             = X86Assembler.Prefix.REX_W,
        instruction_prefix_wide_extended    = X86Assembler.Prefix.REX_WB,
        lea_instruction_code                = 0x8D,
        mov64_instruction_code              = 0xB8;
    };

    public void verify(){
        // make sure code pattern is actually a mov [reg+offset], reg instruction
        @RawCType("u_char")int test_byte = unsafe.getByte(instruction_address())&0xff;
        if(JVM.isLP64){
            if ( (test_byte == Intel_specific_constants.instruction_prefix_wide ||
                    test_byte == Intel_specific_constants.instruction_prefix_wide_extended) ) {
                test_byte = unsafe.getByte(instruction_address() + 1)&0xff;
            }
        }
        if (JVM.isLP64){
            if (!((test_byte == Intel_specific_constants.lea_instruction_code) || (test_byte == Intel_specific_constants.mov64_instruction_code) )) {
                throw new RuntimeException("not a lea reg, [reg+offs] instruction");
            }
        }else {
            if (!((test_byte == Intel_specific_constants.lea_instruction_code))) {
                throw new RuntimeException("not a lea reg, [reg+offs] instruction");
            }
        }
    }
    public void print(PrintStream tty){
        tty.printf("0x"+Long.toHexString(instruction_address())+": lea [reg + %x], reg\n", offset());
    }

    public X86NativeLoadAddress(long addr) {
        super(addr);
    }

    public static X86NativeLoadAddress nativeLoadAddress_at (@RawCType("address")long address) {
        X86NativeLoadAddress test = new X86NativeLoadAddress(address - X86NativeMovRegMem.Intel_specific_constants.instruction_offset);
        if (JVM.ENABLE_EXTRA_CHECK){
            test.verify();
        }
        return test;
    }
}
