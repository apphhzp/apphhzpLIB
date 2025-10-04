package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.cpu.x86.X86Assembler;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

// An interface for accessing/manipulating native moves of the form:
//      mov[b/w/l/q] [reg + offset], reg   (instruction_code_reg2mem)
//      mov[b/w/l/q] reg, [reg+offset]     (instruction_code_mem2reg
//      mov[s/z]x[w/b/q] [reg + offset], reg
//      fld_s  [reg+offset]
//      fld_d  [reg+offset]
//      fstp_s [reg + offset]
//      fstp_d [reg + offset]
//      mov_literal64  scratch,<pointer> ; mov[b/w/l/q] 0(scratch),reg | mov[b/w/l/q] reg,0(scratch)
//
// Warning: These routines must be able to handle any instruction sequences
// that are generated as a result of the load/store byte,word,long
// macros.  For example: The load_unsigned_byte instruction generates
// an xor reg,reg inst prior to generating the movb instruction.  This
// class must skip the xor instruction.
public class X86NativeMovRegMem extends X86NativeInstruction{
    public X86NativeMovRegMem(long addr) {
        super(addr);
    }


    public static final class Intel_specific_constants {
        public static final int
        instruction_prefix_wide_lo          = X86Assembler.Prefix.REX,
        instruction_prefix_wide_hi          = X86Assembler.Prefix.REX_WRXB,
        instruction_code_xor                = 0x33,
        instruction_extended_prefix         = 0x0F,
        instruction_code_mem2reg_movslq     = 0x63,
        instruction_code_mem2reg_movzxb     = 0xB6,
        instruction_code_mem2reg_movsxb     = 0xBE,
        instruction_code_mem2reg_movzxw     = 0xB7,
        instruction_code_mem2reg_movsxw     = 0xBF,
        instruction_operandsize_prefix      = 0x66,
        instruction_code_reg2mem            = 0x89,
        instruction_code_mem2reg            = 0x8b,
        instruction_code_reg2memb           = 0x88,
        instruction_code_mem2regb           = 0x8a,
        instruction_code_float_s            = 0xd9,
        instruction_code_float_d            = 0xdd,
        instruction_code_long_volatile      = 0xdf,
        instruction_code_xmm_ss_prefix      = 0xf3,
        instruction_code_xmm_sd_prefix      = 0xf2,
        instruction_code_xmm_code           = 0x0f,
        instruction_code_xmm_load           = 0x10,
        instruction_code_xmm_store          = 0x11,
        instruction_code_xmm_lpd            = 0x12,

        instruction_code_lea                = 0x8d,

        instruction_VEX_prefix_2bytes       = X86Assembler.Prefix.VEX_2bytes,
        instruction_VEX_prefix_3bytes       = X86Assembler.Prefix.VEX_3bytes,
        instruction_EVEX_prefix_4bytes      = X86Assembler.Prefix.EVEX_4bytes,

        instruction_offset                  = 0,
        data_offset                         = 2,
        next_instruction_offset             = 4;
    };

    // helper
    public int instruction_start(){
        int off = 0;
        @RawCType("u_char")int instr_0 = ubyte_at(off);

        // See comment in Assembler::locate_operand() about VEX prefixes.
        if (instr_0 == Intel_specific_constants.instruction_VEX_prefix_2bytes) {
            if (!JVM.isLP64){
                if (!((0xC0 & ubyte_at(1)) == 0xC0)){
                    throw new RuntimeException("shouldn't have LDS and LES instructions");
                }
            }
            return 2;
        }
        if (instr_0 == Intel_specific_constants.instruction_VEX_prefix_3bytes) {
            if (!JVM.isLP64){
                if (!((0xC0 & ubyte_at(1)) == 0xC0)){
                    throw new RuntimeException("shouldn't have LDS and LES instructions");
                }
            }
            return 3;
        }
        if (instr_0 == Intel_specific_constants.instruction_EVEX_prefix_4bytes) {
            return 4;
        }

        // First check to see if we have a (prefixed or not) xor
        if (instr_0 >= Intel_specific_constants.instruction_prefix_wide_lo && // 0x40
                instr_0 <= Intel_specific_constants.instruction_prefix_wide_hi) { // 0x4f
            off++;
            instr_0 = ubyte_at(off);
        }

        if (instr_0 == Intel_specific_constants.instruction_code_xor) {
            off += 2;
            instr_0 = ubyte_at(off);
        }

        // Now look for the real instruction and the many prefix/size specifiers.

        if (instr_0 == Intel_specific_constants.instruction_operandsize_prefix ) {  // 0x66
            off++; // Not SSE instructions
            instr_0 = ubyte_at(off);
        }

        if ( instr_0 == Intel_specific_constants.instruction_code_xmm_ss_prefix || // 0xf3
                instr_0 == Intel_specific_constants.instruction_code_xmm_sd_prefix) { // 0xf2
            off++;
            instr_0 = ubyte_at(off);
        }

        if ( instr_0 >= Intel_specific_constants.instruction_prefix_wide_lo && // 0x40
                instr_0 <= Intel_specific_constants.instruction_prefix_wide_hi) { // 0x4f
            off++;
            instr_0 = ubyte_at(off);
        }


        if (instr_0 == Intel_specific_constants.instruction_extended_prefix ) {  // 0x0f
            off++;
        }

        return off;
    }

    public @RawCType("address")long instruction_address() {
        return addr_at(instruction_start());
    }

    public int num_bytes_to_end_of_patch() {
        return patch_offset() + 4;
    }

    public int offset(){
        return int_at(patch_offset());
    }

    public void set_offset(int x) {
        set_int_at(patch_offset(), x);
    }

    public void add_offset_in_bytes(int add_offset) {
        int patch_off = patch_offset();
        set_int_at(patch_off, int_at(patch_off) + add_offset);
    }
    public void print (PrintStream tty){
        tty.printf("0x"+Long.toHexString(instruction_address())+": mov reg, [reg + %x]\n",offset());
    }

    private int patch_offset(){
        int off = Intel_specific_constants.data_offset + instruction_start();
        @RawCType("u_char")int mod_rm = unsafe.getByte(instruction_address() + 1)&0xff;
        // nnnn(r12|rsp) isn't coded as simple mod/rm since that is
        // the encoding to use an SIB byte. Which will have the nnnn
        // field off by one byte
        if ((mod_rm & 7) == 0x4) {
            off++;
        }
        return off;
    }

    public void verify() {
        // make sure code pattern is actually a mov [reg+offset], reg instruction
        @RawCType("u_char")int test_byte = unsafe.getByte(instruction_address())&0xff;
        switch (test_byte) {
            case Intel_specific_constants.instruction_code_reg2memb:  // 0x88 movb a, r
            case Intel_specific_constants.instruction_code_reg2mem:   // 0x89 movl a, r (can be movq in 64bit)
            case Intel_specific_constants.instruction_code_mem2regb:  // 0x8a movb r, a
            case Intel_specific_constants.instruction_code_mem2reg:   // 0x8b movl r, a (can be movq in 64bit)
                break;

            case Intel_specific_constants.instruction_code_mem2reg_movslq: // 0x63 movsql r, a
            case Intel_specific_constants.instruction_code_mem2reg_movzxb: // 0xb6 movzbl r, a (movzxb)
            case Intel_specific_constants.instruction_code_mem2reg_movzxw: // 0xb7 movzwl r, a (movzxw)
            case Intel_specific_constants.instruction_code_mem2reg_movsxb: // 0xbe movsbl r, a (movsxb)
            case Intel_specific_constants.instruction_code_mem2reg_movsxw: // 0xbf  movswl r, a (movsxw)
                break;

            case Intel_specific_constants.instruction_code_float_s:   // 0xd9 fld_s a
            case Intel_specific_constants.instruction_code_float_d:   // 0xdd fld_d a
            case Intel_specific_constants.instruction_code_xmm_load:  // 0x10 movsd xmm, a
            case Intel_specific_constants.instruction_code_xmm_store: // 0x11 movsd a, xmm
            case Intel_specific_constants.instruction_code_xmm_lpd:   // 0x12 movlpd xmm, a
                break;

            case Intel_specific_constants.instruction_code_lea:       // 0x8d lea r, a
                break;

            default:
                throw new RuntimeException("not a mov [reg+offs], reg instruction");
        }
    }

    public static X86NativeMovRegMem nativeMovRegMem_at(@RawCType("address")long address) {
        X86NativeMovRegMem test = new X86NativeMovRegMem(address - X86NativeMovRegMem.Intel_specific_constants.instruction_offset);
        if (JVM.ENABLE_EXTRA_CHECK) {
            test.verify();
        }
        return test;
    }
}
