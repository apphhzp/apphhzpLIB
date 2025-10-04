package apphhzp.lib.hotspot.asm;

import apphhzp.lib.hotspot.code.VMReg;

public abstract class AbstractRegister {
    protected final int value;
    public AbstractRegister(final int value) {
        this.value = value;
    }
    protected int value(){
        return value;
    }
    public abstract VMReg as_VMReg();
}
