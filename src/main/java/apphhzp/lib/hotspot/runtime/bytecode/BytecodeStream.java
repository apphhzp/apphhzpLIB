package apphhzp.lib.hotspot.runtime.bytecode;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

public class BytecodeStream extends BaseBytecodeStream {
    private @RawCType("Bytecodes::Code") int _code;

    public BytecodeStream(Method method) {
        super(method);
    }

    public BytecodeStream(Method method, int bci) {
        super(method);
        set_start(bci);
    }

    // Iteration
    public @RawCType("Bytecodes::Code") int next() {
        @RawCType("Bytecodes::Code") int raw_code, code;
        // set reading position
        _bci = _next_bci;
        if (is_last_bytecode()) {
            // indicate end of bytecode stream
            raw_code = code = Bytecodes.Code._illegal;
        } else {
            // get bytecode
            @RawCType("address") long bcp = this.bcp();
            raw_code = Bytecodes.code_at(_method, bcp);
            code = Bytecodes.java_code(raw_code);
            // set next bytecode position
            //
            // note that we cannot advance before having the
            // tty bytecode otherwise the stepping is wrong!
            // (carefull: length_for(...) must be used first!)
            int len = Bytecodes.length_for(code);
            if (len == 0) len = Bytecodes.length_at(_method, bcp);
            if (len <= 0 || (_bci > _end_bci - len) || (_bci - len >= _next_bci)) {
                raw_code = code = Bytecodes.Code._illegal;
            } else {
                _next_bci += len;
                if (!(_bci < _next_bci)){
                    throw new RuntimeException("length must be > 0");
                }
                // set attributes
                _is_wide = false;
                // check for special (uncommon) cases
                if (code == Bytecodes.Code._wide) {
                    raw_code = ClassHelperSpecial.unsafe.getByte(bcp + 1) & 0xff;
                    code = raw_code;  // wide BCs are always Java-normal
                    _is_wide = true;
                }
                if (!Bytecodes.is_java_code(code)){
                    throw new RuntimeException("sanity check");
                }
            }
        }
        _raw_code = raw_code;
        _code = code;
        return _code;
    }

    public @RawCType("Bytecodes::Code") int code() {
        return _code;
    }

    // Unsigned indices, widening
    public int get_index() {
        return is_wide() ? bytecode().get_index_u2(raw_code(), true) : get_index_u1();
    }

    // Get an unsigned 2-byte index, swapping the bytes if necessary.
    public int get_index_u2() {
        assert_raw_stream(false);
        return bytecode().get_index_u2(raw_code(), false);
    }

    // Get an unsigned 2-byte index in native order.
    public int get_index_u2_cpcache() {
        assert_raw_stream(false);
        return bytecode().get_index_u2_cpcache(raw_code());
    }

    public int get_index_u4() {
        assert_raw_stream(false);
        return bytecode().get_index_u4(raw_code());
    }

    public boolean has_index_u4() {
        return bytecode().has_index_u4(raw_code());
    }
}
