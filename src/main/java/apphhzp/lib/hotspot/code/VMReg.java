package apphhzp.lib.hotspot.code;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.runtime.x86.X86VMReg;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.hotspot.code.VMRegImpl.BAD_REG;
import static apphhzp.lib.hotspot.code.VMRegImpl.stack0;

public abstract class VMReg {
    private final int value;
    public String  name() {
        if (is_reg()) {
            return VMRegImpl.regName( value());
        } else if (!is_valid()) {
            return "BAD";
        } else {
            // shouldn't really be called with stack
            return "STACKED REG";
        }
    }
    public static VMReg create(int value){
        if (PlatformInfo.isX86()){
            return new X86VMReg(value);
        }else {
            throw new RuntimeException("Unsupported platform");
        }
    }
    protected VMReg(int value) {
        this.value = value;
    }
    public boolean is_valid()  { return (this.value) != BAD_REG; }
    public boolean is_stack()  { return  this.value >= stack0.value; }
    public boolean is_reg() { return is_valid() && !is_stack();}

    // This really ought to check that the register is "real" in the sense that
    // we don't try and get the VMReg number of a physical register that doesn't
    // have an expressible part. That would be pd specific code
    public VMReg next() {
        if (!((is_reg() && value() < stack0.value() - 1) || is_stack())){
            throw new RuntimeException("must be");
        }
        return VMReg.create(value() + 1);
    }
    public VMReg next(int i) {
        if (!((is_reg() && value() < stack0.value() - i) || is_stack())){
            throw new RuntimeException("must be");
        }
        return VMReg.create(value() + i);
    }
    public VMReg prev() {
        if (!((is_stack() && value() > stack0.value()) || (is_reg() && value() != 0))){
            throw new RuntimeException("must be");
        }
        return VMReg.create(value() - 1);
    }

    public @RawCType("intptr_t")int value(){
        return this.value;
    }

    public VMReg bias(int offset) {
        if (!is_stack()){
            throw new RuntimeException("must be");
        }
        VMReg res = VMRegImpl.stack2reg(reg2stack() + offset);
        if (!res.is_stack()){
            throw new RuntimeException("must be");
        }
        return res;
    }


    public @RawCType("uintptr_t")int reg2stack() {
        if (!is_stack()){
            throw new RuntimeException("Not a stack-based register");
        }
        return value() - stack0.value();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==this){
            return true;
        }
        if (obj instanceof VMReg vmReg){
            return vmReg.value==this.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    public void print_on(PrintStream out) {
        if( is_reg() ) {
            out.print(VMRegImpl.regName(value));
        } else if (is_stack()) {
            int stk = value() - stack0.value();
            out.print("["+stk*4+"]");
        } else {
            out.print("BAD!");
        }
    }

    @Override
    public String toString() {
        if( is_reg() ) {
            return "VMReg@"+VMRegImpl.regName(value);
        } else if (is_stack()) {
            int stk = value() - stack0.value();
            return "VMReg@["+stk*4+"]";
        } else {
            return "VMReg@BAD!";
        }
    }
}
