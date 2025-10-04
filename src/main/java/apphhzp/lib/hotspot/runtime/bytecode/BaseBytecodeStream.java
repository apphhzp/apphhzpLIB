package apphhzp.lib.hotspot.runtime.bytecode;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class BaseBytecodeStream {
    protected Method _method;                       // read from method directly

    // reading position
    protected int _bci;                          // bci if current bytecode
    protected int _next_bci;                     // bci of next bytecode
    protected int _end_bci;                      // bci after the current iteration interval

    // last bytecode read
    protected @RawCType("Bytecodes::Code") int _raw_code;
    protected boolean _is_wide;
    protected boolean _is_raw;                       // false in 'cooked' BytecodeStream

    // Construction
    protected BaseBytecodeStream(Method method) {
        this._method=method;
        this.set_interval(0, _method.code_size());
        this._is_raw = false;
    }

    public void set_interval(int beg_bci, int end_bci) {
        // iterate over the interval [beg_bci, end_bci)
        if (!(0 <= beg_bci && beg_bci <= method().code_size())){
            throw new RuntimeException("illegal beg_bci");
        }
        if (!(0 <= end_bci && end_bci <= method().code_size())){
            throw new RuntimeException("illegal end_bci");
        }
        // setup of iteration pointers
        _bci = beg_bci;
        _next_bci = beg_bci;
        _end_bci = end_bci;
    }

    public void set_start(int beg_bci) {
        set_interval(beg_bci, _method.code_size());
    }

    public boolean is_raw() {
        return _is_raw;
    }

    // Stream attributes
    public Method method() {
        return _method;
    }

    public int bci() {
        return _bci;
    }

    public int next_bci() {
        return _next_bci;
    }

    public int end_bci() {
        return _end_bci;
    }

    public @RawCType("Bytecodes::Code") int raw_code() {
        return _raw_code;
    }

    public boolean is_wide() {
        return _is_wide;
    }

    public int instruction_size() {
        return (_next_bci - _bci);
    }

    public boolean is_last_bytecode() {
        return _next_bci >= _end_bci;
    }

    public @RawCType("address") long bcp() {
        return method().code_base() + _bci;
    }

    public Bytecode bytecode() {
        return new Bytecode(_method, bcp());
    }

    // State changes
    public void set_next_bci(int bci) {
        if (!(0 <= bci && bci <= method().code_size())){
            throw new RuntimeException("illegal bci");
        }
        _next_bci = bci;
    }

    // Bytecode-specific attributes
    public int dest() {
        return bci() + bytecode().get_offset_s2(raw_code());
    }

    public int dest_w() {
        return bci() + bytecode().get_offset_s4(raw_code());
    }

    // One-byte indices.
    public int get_index_u1() {
        assert_raw_index_size(1);
        return unsafe.getByte(bcp() + 1)&0xff;
    }

    protected void assert_raw_index_size(int size) {
        if (!JVM.ENABLE_EXTRA_CHECK){
            return;
        }
        if (raw_code() == Bytecodes.Code._invokedynamic && is_raw()) {
            // in raw mode, pretend indy is "bJJ__"
            if (size != 2){
                throw new RuntimeException("raw invokedynamic instruction has 2-byte index only");
            }
        } else {
            Bytecode.assert_index_size(size, raw_code(), is_wide());
        }
    }

    protected void assert_raw_stream(boolean want_raw) {
        if (!JVM.ENABLE_EXTRA_CHECK){
            return;
        }
        if (want_raw) {
            if (!is_raw()){
                throw new RuntimeException("this function only works on raw streams");
            }
        } else {
            if (is_raw()){
                throw new RuntimeException("this function only works on non-raw streams");
            }
        }
    }
}
