package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.util.RawCType;

public interface NativeMovConstReg {
    @RawCType("intptr_t")long data();
    void set_data(@RawCType("intptr_t")long val);
}
