package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.utilities.BasicHashtableEntry;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.helfy.JVM.*;

public class AdapterHandlerEntry extends BasicHashtableEntry {
    public static final Type TYPE = JVM.type("AdapterHandlerEntry");
    public static final int SIZE = TYPE.size;
    public static final long FINGERPRINT_OFFSET=computeOffset(oopSize,BasicHashtableEntry.NEXT_OFFSET+ oopSize);
    public static final long I2C_ENTRY_OFFSET = computeOffset(oopSize,FINGERPRINT_OFFSET+oopSize);
    public static final long C2I_ENTRY_OFFSET = computeOffset(oopSize,I2C_ENTRY_OFFSET + oopSize);
    public static final long C2I_UNVERIFIED_ENTRY_OFFSET = computeOffset( oopSize,C2I_ENTRY_OFFSET + oopSize);
    public static final long C2I_NO_CLINIT_CHECK_ENTRY_OFFSET =computeOffset(oopSize, C2I_UNVERIFIED_ENTRY_OFFSET + oopSize);
    public static final long SAVED_CODE_OFFSET=JVM.includeAssert?computeOffset(oopSize,C2I_NO_CLINIT_CHECK_ENTRY_OFFSET+ oopSize):-1;
    public static final long SAVED_CODE_LENGTH_OFFSET=JVM.includeAssert?computeOffset(intSize,SAVED_CODE_OFFSET+oopSize):-1;
    static {
        if (includeAssert){
            JVM.assertOffset(SIZE, computeOffset(Math.max(intSize,oopSize),SAVED_CODE_LENGTH_OFFSET+intSize));
        }else{
            JVM.assertOffset(SIZE, computeOffset(Math.max(intSize,oopSize),C2I_NO_CLINIT_CHECK_ENTRY_OFFSET+oopSize));
        }
    }

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

    @Override
    public String toString() {
        return "AdapterHandlerEntry@0x"+Long.toHexString(this.address);
    }
}
