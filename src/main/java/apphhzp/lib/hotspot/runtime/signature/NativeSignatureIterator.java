package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

public abstract class NativeSignatureIterator extends SignatureIterator {
    private Method _method;
    // We need separate JNI and Java offset values because in 64 bit mode,
    // the argument offsets are not in sync with the Java stack.
    // For example a long takes up 1 "C" stack entry but 2 Java stack entries.
    private int          _offset;                // The java stack offset
    private int          _prepended;             // number of prepended JNI parameters (1 JNIEnv, plus 1 mirror if static)
    private int          _jni_offset;            // the current parameter offset, starting with 0
    public NativeSignatureIterator(Method method){
        super(method.signature());
        _method = method;
        _offset = 0;
        _jni_offset = 0;
        final int JNIEnv_words = 1;
        final int mirror_words = 1;
        _prepended = !is_static() ? JNIEnv_words : JNIEnv_words + mirror_words;
    }


    public Method method()          { return _method; }
    public int          offset()          { return _offset; }
    public int      jni_offset()          { return _jni_offset + _prepended; }
    public boolean      is_static()          { return method().is_static(); }
    public abstract void pass_int();
    public abstract void pass_long();
    public abstract void pass_object();  // objects, arrays, inlines
    public abstract void pass_float();
    public void pass_byte()             { pass_int(); };
    public void pass_short()            { pass_int(); };
    public void pass_double()           {
        if (JVM.isLP64){
            throw new UnsupportedOperationException("Should be overridden");
        }
        pass_long();
    }
    public void iterate() { iterate(new Fingerprinter(method()).fingerprint()); }

    // iterate() calls the 3 virtual methods according to the following invocation syntax:
    //
    // {pass_int | pass_long | pass_object}
    //
    // Arguments are handled from left to right (receiver first, if any).
    // The offset() values refer to the Java stack offsets but are 0 based and increasing.
    // The java_offset() values count down to 0, and refer to the Java TOS.
    // The jni_offset() values increase from 1 or 2, and refer to C arguments.
    // The method's return type is ignored.

    public void iterate(@RawCType("uint64_t")long fingerprint) {
        set_fingerprint(fingerprint);
        if (!is_static()) {
            // handle receiver (not handled by iterate because not in signature)
            pass_object(); _jni_offset++; _offset++;
        }
        do_parameters_on(this);
    }

    @Override
    protected void do_type(int type) {
        if (type == BasicType.T_BYTE || type == BasicType.T_BOOLEAN) {
            pass_byte();
            _jni_offset++;
            _offset++;
        } else if (type == BasicType.T_CHAR || type == BasicType.T_SHORT) {
            pass_short();
            _jni_offset++;
            _offset++;
        } else if (type == BasicType.T_INT) {
            pass_int();
            _jni_offset++;
            _offset++;
        } else if (type == BasicType.T_FLOAT) {
            pass_float();
            _jni_offset++;
            _offset++;
        } else if (type == BasicType.T_DOUBLE) {
            int jni_offset = JVM.isLP64 ? 1 : 2;
            pass_double();
            _jni_offset += jni_offset;
            _offset += 2;
        } else if (type == BasicType.T_LONG) {
            int jni_offset = JVM.isLP64 ? 1 : 2;
            pass_long();
            _jni_offset += jni_offset;
            _offset += 2;
        } else if (type == BasicType.T_ARRAY || type == BasicType.T_OBJECT) {
            pass_object();
            _jni_offset++;
            _offset++;
        } else {
            throw new RuntimeException("ShouldNotReachHere");
        }
    }
}
