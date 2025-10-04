package apphhzp.lib.hotspot.runtime.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.asm.x86.X86Register;
import apphhzp.lib.hotspot.asm.x86.X86XMMRegister;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.VMRegImpl;

public class X86VMRegImpl extends VMRegImpl{
    public static final int INTEGER_TYPE=0,
             VECTOR_TYPE= 1,
             X87_TYPE= 2,
             STACK_TYPE= 3;
    private X86VMRegImpl(){}
    public static VMReg vmStorageToVMReg(int type, int index) {
        return switch (type) {
            case INTEGER_TYPE -> new X86Register(index).as_VMReg();
            case VECTOR_TYPE -> new X86XMMRegister(index).as_VMReg();
            case STACK_TYPE -> VMRegImpl.stack2reg(JVM.isLP64 ? index * 2 : index);// numbering on x64 goes per 64-bits
            default -> VMRegImpl.Bad();
        };
    }
}
