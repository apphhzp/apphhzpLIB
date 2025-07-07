package apphhzp.lib.disassemble.x86;

public enum General32BitRegister implements GeneralRegister{
    eAX,
    eCX,
    eDX,
    eBX,
    eSP,
    eBP,
    eSI,
    eDI,
    r8d(true),
    r9d(true),
    r10d(true),
    r11d(true),
    r12d(true),
    r13d(true),
    r14d(true),
    r15d(true);
    public final boolean is64Only;
    General32BitRegister() {
        this(false);
    }

    General32BitRegister(boolean is64Only) {
        this.is64Only = is64Only;
    }
}
