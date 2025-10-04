package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.oops.method.Method;

public class Bytecode_field extends Bytecode_member_ref {
    public Bytecode_field(Method method, int bci) {
        super(method, bci);
        verify();
    }

    // Testers
    public boolean is_getfield() {
        return java_code() == Bytecodes.Code._getfield;
    }

    public boolean is_putfield() {
        return java_code() == Bytecodes.Code._putfield;
    }

    public boolean is_getstatic() {
        return java_code() == Bytecodes.Code._getstatic;
    }

    public boolean is_putstatic() {
        return java_code() == Bytecodes.Code._putstatic;
    }

    public boolean is_getter() {
        return is_getfield() || is_getstatic();
    }

    public boolean is_static() {
        return is_getstatic() || is_putstatic();
    }

    public boolean is_valid() {
        return is_getfield() ||
                is_putfield() ||
                is_getstatic() ||
                is_putstatic();
    }

    public void verify(){
        if (!is_valid()){
            throw new RuntimeException("check field");
        }
    }
}
