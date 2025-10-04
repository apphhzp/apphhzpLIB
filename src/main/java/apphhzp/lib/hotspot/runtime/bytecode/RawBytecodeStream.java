package apphhzp.lib.hotspot.runtime.bytecode;

import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class RawBytecodeStream extends BaseBytecodeStream {
    public RawBytecodeStream(Method method) {
        super(method);
        this._is_raw = true;
    }

    // Iteration
    // Use raw_next() rather than next() for faster method reference
    public @RawCType("Bytecodes::Code") int raw_next() {
        @RawCType("Bytecodes::Code") int code;
        // set reading position
        _bci = _next_bci;
        if (is_last_bytecode()){
            throw new RuntimeException("caller should check is_last_bytecode()");
        }

        @RawCType("address") long bcp = this.bcp();
        code = Bytecodes.code_or_bp_at(bcp);

        // set next bytecode position
        int len = Bytecodes.length_for(code);
        if (len > 0 && (_bci <= _end_bci - len)) {
            if (!(code != Bytecodes.Code._wide && code != Bytecodes.Code._tableswitch
                    && code != Bytecodes.Code._lookupswitch)){
                throw new RuntimeException("can't be special bytecode");
            }
            _is_wide = false;
            _next_bci += len;
            if (_next_bci <= _bci) { // Check for integer overflow
                code = Bytecodes.Code._illegal;
            }
            _raw_code = code;
            return code;
        } else {
            return raw_next_special(code);
        }
    }

    public @RawCType("Bytecodes::Code") int raw_next_special(@RawCType("Bytecodes::Code") int code){
        if (is_last_bytecode()){
            throw new RuntimeException("should have been checked");
        }
        // set next bytecode position
        @RawCType("address")long bcp = this.bcp();
        @RawCType("address")long end = method().code_base() + end_bci();
        int len = Bytecodes.raw_special_length_at(bcp, end);
        // Very large tableswitch or lookupswitch size can cause _next_bci to overflow.
        if (len <= 0 || (_bci > _end_bci - len) || (_bci - len >= _next_bci)) {
            code = Bytecodes.Code._illegal;
        } else {
            _next_bci += len;
            // set attributes
            _is_wide = false;
            // check for special (uncommon) cases
            if (code == Bytecodes.Code._wide) {
                if (bcp + 1 >= end) {
                    code = Bytecodes.Code._illegal;
                } else {
                    code = unsafe.getByte(bcp+1)&0xff;
                    _is_wide = true;
                }
            }
        }
        _raw_code = code;
        return code;
    }
    // Unsigned indices, widening, with no swapping of bytes
    public int get_index() {
        return (is_wide()) ? get_index_u2_raw(bcp() + 2) : get_index_u1();
    }

    // Get an unsigned 2-byte index, with no swapping of bytes.
    public int get_index_u2() {
        if (is_wide()){
            throw new RuntimeException();
        }
        return get_index_u2_raw(bcp() + 1);
    }


    private int get_index_u2_raw(@RawCType("address") long p) {
        assert_raw_index_size(2);
        assert_raw_stream(true);
        return Bytes.get_Java_u2(p);
    }
}
