package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

// destination is rbx or rax
// mov rbx, [rip + offset]
public class X86NativeLoadGot extends X86NativeInstruction{
    private static final boolean has_rex = PlatformInfo.isX86_64();
    private static final int rex_size = PlatformInfo.isX86_64()?1:0;

    public X86NativeLoadGot(long addr) {
        super(addr);
    }

    private static final class Intel_specific_constants {
        public static final int
        rex_prefix = 0x48,
        rex_b_prefix = 0x49,
        instruction_code = 0x8b,
        modrm_rbx_code = 0x1d,
        modrm_rax_code = 0x05,
        instruction_length = 6 + rex_size,
        offset_offset = 2 + rex_size;
    };

    private int rip_offset() { return int_at(Intel_specific_constants.offset_offset); }
    private @RawCType("address")long return_address() { return addr_at(Intel_specific_constants.instruction_length); }
    private @RawCType("address")long got_address() { return return_address() + rip_offset(); }

    private void report_and_fail(){
        System.err.printf("Addr: 0x"+Long.toHexString(instruction_address())+" Code: %x %x %x\n",
                (has_rex ? ubyte_at(0) : 0), ubyte_at(rex_size), ubyte_at(rex_size + 1));
        throw new RuntimeException("not a indirect rip mov to rbx");
    }
    private @RawCType("address")long instruction_address() { return addr_at(0); }
    public void verify(){
        if (has_rex) {
            int rex = ubyte_at(0);
            if (rex != Intel_specific_constants.rex_prefix && rex != Intel_specific_constants.rex_b_prefix) {
                report_and_fail();
            }
        }

        int inst = ubyte_at(rex_size);
        if (inst != Intel_specific_constants.instruction_code) {
            report_and_fail();
        }
        int modrm = ubyte_at(rex_size + 1);
        if (modrm != Intel_specific_constants.modrm_rbx_code && modrm != Intel_specific_constants.modrm_rax_code) {
            report_and_fail();
        }
    }
    public @RawCType("address")long next_instruction_address(){
        return return_address();
    }
    public @RawCType("intptr_t")long data(){
        return unsafe.getAddress(got_address());
    }
    public void set_data(@RawCType("intptr_t")long data) {
        @RawCType("intptr_t*")long addr = got_address();
        unsafe.putAddress(addr,data);
    }

    public static X86NativeLoadGot nativeLoadGot_at(@RawCType("address")long addr) {
        X86NativeLoadGot load = new X86NativeLoadGot(addr);
        if (JVM.ENABLE_EXTRA_CHECK) {
            load.verify();
        }
        return load;
    }
}
