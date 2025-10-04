package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.signature.ArgumentSizeComputer;

public class Bytecode_invoke extends Bytecode_member_ref{
    // Constructor that skips verification
    protected Bytecode_invoke(Method method, int bci, boolean unused){
        super(method, bci);
    }
    public Bytecode_invoke(Method method, int bci){
        super(method,bci);
        verify();
    }

    public void verify() {
        if (!is_valid()){
            throw new RuntimeException("check invoke");
        }
        if (cpcache() == null){
            throw new RuntimeException("do not call this from verifier or rewriter");
        }
    }

    // Attributes
    //public Method static_target();                  // "specified" method   (from constant pool)

    // Testers
    public boolean is_invokeinterface()                 { return invoke_code() == Bytecodes.Code._invokeinterface; }
    public boolean is_invokevirtual()                   { return invoke_code() == Bytecodes.Code._invokevirtual; }
    public boolean is_invokestatic()                    { return invoke_code() == Bytecodes.Code._invokestatic; }
    public boolean is_invokespecial()                   { return invoke_code() == Bytecodes.Code._invokespecial; }
    public boolean is_invokedynamic()                   { return invoke_code() == Bytecodes.Code._invokedynamic; }
    public boolean is_invokehandle()                    { return invoke_code() == Bytecodes.Code._invokehandle; }

    public boolean has_receiver()                       { return !is_invokestatic() && !is_invokedynamic(); }

    public boolean is_valid()                           { return is_invokeinterface() ||
            is_invokevirtual()   ||
            is_invokestatic()    ||
            is_invokespecial()   ||
            is_invokedynamic()   ||
            is_invokehandle(); }

    public boolean has_appendix(){
        return cpcache_entry().has_appendix();
    }

    public int size_of_parameters() {
        ArgumentSizeComputer asc=new ArgumentSizeComputer(signature());
        return asc.size() + (has_receiver() ? 1 : 0);
    }
    // Helper to skip verification.   Used is_valid() to check if the result is really an invoke
    private Bytecode_invoke Bytecode_invoke_check(Method method, int bci){
        return new Bytecode_invoke(method, bci, false);
    }
}
