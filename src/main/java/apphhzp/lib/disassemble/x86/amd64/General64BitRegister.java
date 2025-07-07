package apphhzp.lib.disassemble.x86.amd64;

import apphhzp.lib.disassemble.x86.General32BitRegister;
import apphhzp.lib.disassemble.x86.GeneralRegister;

import static apphhzp.lib.disassemble.x86.General32BitRegister.*;

public enum General64BitRegister implements GeneralRegister {
    rAX(eAX),
    rCX(eCX),
    rDX(eDX),
    rBX(eBX),
    rSP(eSP),
    rBP(eBP),
    rSI(eSI),
    rDI(eDI),
    r8(true,r8d),
    r9(true,r9d),
    r10(true,r10d),
    r11(true,r11d),
    r12(true,r12d),
    r13(true,r13d),
    r14(true,r14d),
    r15(true,r15d);
    public final  boolean is64Only;
    public final General32BitRegister child;
    General64BitRegister(General32BitRegister child){
        this(false,child);
    }
    General64BitRegister(boolean is64Only, General32BitRegister child){
        this.is64Only = is64Only;
        this.child = child;
    }
}
