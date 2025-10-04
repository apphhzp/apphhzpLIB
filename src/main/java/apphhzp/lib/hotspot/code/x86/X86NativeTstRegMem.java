package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.hotspot.cpu.x86.X86Assembler;

public class X86NativeTstRegMem extends X86NativeInstruction{

    public static final class Intel_specific_constants {
        public static final int
        instruction_rex_prefix_mask = 0xF0,
        instruction_rex_prefix      = X86Assembler.Prefix.REX,
        instruction_rex_b_prefix    = X86Assembler.Prefix.REX_B,
        instruction_code_memXregl   = 0x85,
        modrm_mask                  = 0x38, // select reg from the ModRM byte
        modrm_reg                   = 0x00  // rax
        ;
    }
    public X86NativeTstRegMem(long addr) {
        super(addr);
    }
}
