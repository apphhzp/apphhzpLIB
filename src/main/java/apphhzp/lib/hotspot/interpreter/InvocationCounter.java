package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class InvocationCounter extends JVMObject {
    public static final int count_increment = JVM.intConstant("InvocationCounter::count_increment");
    public static final int count_shift = JVM.intConstant("InvocationCounter::count_shift");

    public InvocationCounter(long addr) {
        super(addr);
    }

    public int getCount() {
        return unsafe.getInt(this.address) >>> count_shift;
    }

    public void setCount(int count) {
        unsafe.putInt(this.address, count << count_shift | (this.carry() ? 1 : 0));
    }

    public boolean carry() {
        return (unsafe.getInt(this.address) & 1) != 0;
    }

    public void setCarry(boolean carry) {
        unsafe.putInt(this.address, this.getCount() << count_shift | (carry ? 1 : 0));
    }

    public int get() {
        return unsafe.getInt(this.address);
    }

    public void set(int count, boolean carry) {
        unsafe.putInt(this.address, count << count_shift | (carry ? 1 : 0));
    }

    @Override
    public String toString() {
        return "InvocationCounter0x"+Long.toHexString(this.address);
    }
}
