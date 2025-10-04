package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

public class X86NativeMovConstRegPatching extends X86NativeMovConstReg{
    public X86NativeMovConstRegPatching(long addr) {
        super(addr);
    }
    public static X86NativeMovConstRegPatching nativeMovConstRegPatching_at(@RawCType("address")long address) {
        X86NativeMovConstRegPatching test = new X86NativeMovConstRegPatching(address - X86NativeMovConstReg.Intel_specific_constants.instruction_offset);
        if (JVM.ENABLE_EXTRA_CHECK) {
            test.verify();
        }

        return test;
    }
}
