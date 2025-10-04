package apphhzp.lib.hotspot.code;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.runtime.x86.X86VMRegImpl;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/**{@code typedef VMRegImpl* VMReg;}*/
public class VMRegImpl {
    public static final Type TYPE = JVM.type("VMRegImpl");
    public static final long stack0_address = TYPE.global("stack0");
    public static final long regName_address = TYPE.global("regName[0]");
    public static final int BAD_REG = -1;
    public static final VMReg stack0 = VMReg.create((int) unsafe.getAddress(stack0_address));
    // Names for registers
    public static final int register_count = JVM.intConstant("ConcreteRegisterImpl::number_of_registers");
    // VMRegs are 4 bytes wide on all platforms
    public static final int stack_slot_size = JVM.intConstant("VMRegImpl::stack_slot_size");
    public static final int slots_per_word = JVM.wordSize / stack_slot_size;
    protected VMRegImpl(){
        throw new UnsupportedOperationException("ShouldNotReachHere");
    }

    public static VMReg as_VMReg(int val) {
        return as_VMReg(val,false);
    }
    public static VMReg as_VMReg(int val, boolean bad_ok) {
        if (!(val > BAD_REG || bad_ok)){
            throw new RuntimeException("invalid");
        }
        return VMReg.create(val);
    }

    // Convert register numbers to stack slots and vice versa
    public static VMReg stack2reg( int idx ) {
        return VMReg.create(stack0.value() + idx);
    }

    public static String regName(int index) {
        if (index < 0 || index >= register_count) {
            throw new IndexOutOfBoundsException(index);
        }
        return JVM.getStringRef(regName_address + (long) index * JVM.oopSize);
    }

    public static VMReg Bad() {
        return VMReg.create(BAD_REG);
    }

    public static VMReg vmStorageToVMReg(int type, int index){
        if (PlatformInfo.isX86()){
            return X86VMRegImpl.vmStorageToVMReg(type, index);
        }else {
            throw new RuntimeException("unsupported platform");
        }
    }
}
