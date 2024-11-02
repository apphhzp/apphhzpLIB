package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class AdapterHandlerEntry extends JVMObject {
    public static final Type TYPE = JVM.type("AdapterHandlerEntry");
    public static final int SIZE = TYPE.size;
    public static final long I2C_ENTRY_OFFSET = JVM.oopSize;
    public static final long C2I_ENTRY_OFFSET = I2C_ENTRY_OFFSET + JVM.oopSize;
    public static final long C2I_UNVERIFIED_ENTRY_OFFSET = C2I_ENTRY_OFFSET + JVM.oopSize;
    public static final long C2I_NO_CLINIT_CHECK_ENTRY_OFFSET = C2I_UNVERIFIED_ENTRY_OFFSET + JVM.oopSize;

    public AdapterHandlerEntry(long addr) {
        super(addr);
    }

    public long getI2CEntry() {
        return unsafe.getAddress(this.address + I2C_ENTRY_OFFSET);
    }

    public long getC2IEntry() {
        return unsafe.getAddress(this.address + C2I_ENTRY_OFFSET);
    }

    public long getC2IUnverifiedEntry() {
        return unsafe.getAddress(this.address + C2I_UNVERIFIED_ENTRY_OFFSET);
    }

    public long getC2INoClinitCheckEntry() {
        return unsafe.getAddress(this.address + C2I_NO_CLINIT_CHECK_ENTRY_OFFSET);
    }

    public void setI2CEntry(long i2c_entry) {
        unsafe.putAddress(this.address + I2C_ENTRY_OFFSET, i2c_entry);
    }

    public void setC2IEntry(long c2i_entry) {
        unsafe.putAddress(this.address + C2I_ENTRY_OFFSET, c2i_entry);
    }

    public void setC2IUnverifiedEntry(long c2i_unverified_entry) {
        unsafe.putAddress(this.address + C2I_UNVERIFIED_ENTRY_OFFSET, c2i_unverified_entry);
    }

    public void setC2INoClinitCheckEntry(long c2i_no_clinit_check_entry) {
        unsafe.putAddress(this.address + C2I_NO_CLINIT_CHECK_ENTRY_OFFSET, c2i_no_clinit_check_entry);
    }

}
