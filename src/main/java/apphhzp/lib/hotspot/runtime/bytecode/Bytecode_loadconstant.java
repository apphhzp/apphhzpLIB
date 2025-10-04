package apphhzp.lib.hotspot.runtime.bytecode;

import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantTag;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

public class Bytecode_loadconstant extends Bytecode{
    private Method _method;

    private int raw_index(){
        @RawCType("Bytecodes::Code")int rawc = code();
        if (rawc == Bytecodes.Code._wide){
            throw new RuntimeException("verifier prevents this");
        }
        if (Bytecodes.java_code(rawc) == Bytecodes.Code._ldc) {
            return get_index_u1(rawc);
        } else {
            return get_index_u2(rawc, false);
        }
    }
    public Bytecode_loadconstant(Method method, int bci) {
        super(method,  method.bcp_from(bci));
        this._method = method;
        verify();
    }

    public void verify() {
        if (_method==null){
            throw new IllegalArgumentException("must supply method");
        }
        @RawCType("Bytecodes::Code")int stdc = Bytecodes.java_code(code());
        if (!(stdc == Bytecodes.Code._ldc ||
                stdc == Bytecodes.Code._ldc_w ||
                stdc == Bytecodes.Code._ldc2_w)){
            throw new IllegalArgumentException("load constant");
        }
    }

    public boolean has_cache_index(){ return code() >= Bytecodes.Code.number_of_java_codes; }

    public int pool_index() {               // index into constant pool
        int index = raw_index();
        if (has_cache_index()) {
            return _method.constants().object_to_cp_index(index);
        }
        return index;
    }
    public int cache_index() {             // index into reference cache (or -1 if none)
        return has_cache_index() ? raw_index() : -1;
    }

    public @RawCType("BasicType")int result_type() {        // returns the result type of the ldc
        int index = pool_index();
        return _method.constants().basic_type_for_constant_at(index);
    }
    public Object resolve_constant(){
        if (_method==null){
            throw new RuntimeException("must supply method to resolve constant");
        }
        int index = raw_index();
        ConstantPool constants = _method.constants();
        if (has_cache_index()) {
            return constants.resolve_cached_constant_at(index);
        } else if (constants.tag_at(index)==ConstantTag.Dynamic) {
            return constants.resolve_possibly_cached_constant_at(index);
        } else {
            return constants.resolve_constant_at(index);
        }
    }
}
