package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class X86NativeGotJump extends X86NativeInstruction{
    public X86NativeGotJump(long addr) {
        super(addr);
    }

    private static final class Intel_specific_constants {
        public static final int
        rex_prefix = 0x41,
        instruction_code = 0xff,
        modrm_code = 0x25,
        instruction_size = 6,
        rip_offset = 2;
    };

    private boolean has_rex()  { return ubyte_at(0) == Intel_specific_constants.rex_prefix; }
    private int rex_size()  { return has_rex() ? 1 : 0; }

    private @RawCType("address")long return_address()  { return addr_at(Intel_specific_constants.instruction_size + rex_size()); }
    private int got_offset()  { return int_at(Intel_specific_constants.rip_offset + rex_size()); }

    public @RawCType("address")long got_address(){ return return_address() + got_offset(); }
    public @RawCType("address")long next_instruction_address(){ return return_address(); }
    public boolean is_GotJump(){ return ubyte_at(rex_size()) == Intel_specific_constants.instruction_code; }
    public @RawCType("address")long destination(){
        @RawCType("address*")long got_entry = got_address();
        return unsafe.getAddress(got_entry);
    }
    public void set_jump_destination(@RawCType("address")long dest)  {
        @RawCType("address*")long got_entry =got_address();
        unsafe.putAddress(got_entry,dest);
    }
    public static X86NativeGotJump nativeGotJump_at(@RawCType("address")long addr) {
        X86NativeGotJump jump = new X86NativeGotJump(addr);
        if (JVM.ENABLE_EXTRA_CHECK){
            jump.verify();
        }
        return jump;
    }
    private @RawCType("address")long instruction_address(){
        return addr_at(0);
    }

    private void report_and_fail() {
        System.err.printf("Addr: 0x"+Long.toHexString(instruction_address()) + " Code: %x %x %x\n",
                (has_rex() ? ubyte_at(0) : 0), ubyte_at(rex_size()), ubyte_at(rex_size() + 1));
        throw new RuntimeException("not a indirect rip jump");
    }

    public void verify() {
        if (has_rex()) {
            int rex = ubyte_at(0);
            if (rex != Intel_specific_constants.rex_prefix) {
                report_and_fail();
            }
        }
        int inst = ubyte_at(rex_size());
        if (inst != Intel_specific_constants.instruction_code) {
            report_and_fail();
        }
        int modrm = ubyte_at(rex_size() + 1);
        if (modrm != Intel_specific_constants.modrm_code) {
            report_and_fail();
        }
    }
}
