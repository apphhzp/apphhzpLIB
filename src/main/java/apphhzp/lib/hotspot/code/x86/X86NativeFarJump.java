package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

public class X86NativeFarJump extends X86NativeInstruction{
    public @RawCType("address")long jump_destination(){
        X86NativeMovConstReg  mov = X86NativeMovConstReg.nativeMovConstReg_at(addr_at(0));
        return mov.data();
    }
    public X86NativeFarJump(long addr) {
        super(addr);
    }
    public void verify() {
        if (is_far_jump()) {
            X86NativeMovConstReg mov = X86NativeMovConstReg.nativeMovConstReg_at(addr_at(0));
            X86NativeInstruction jmp = nativeInstruction_at(mov.next_instruction_address());
            if (jmp.is_jump_reg()) return;
        }
        throw new RuntimeException("not a jump instruction");
    }
    public static  X86NativeFarJump nativeFarJump_at(@RawCType("address")long address) {
        X86NativeFarJump jump = new X86NativeFarJump(address);
        if (JVM.ENABLE_EXTRA_CHECK) {
            jump.verify();
        }
        return jump;
    }
}
