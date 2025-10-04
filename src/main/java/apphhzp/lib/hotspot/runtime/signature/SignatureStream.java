package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.util.CString;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;
import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.utilities.BasicType.is_java_primitive;
import static apphhzp.lib.hotspot.utilities.BasicType.is_reference_type;

public class SignatureStream {
    private final Symbol _signature;
    private int _begin;
    private int _end;
    private int _limit;
    private int _array_prefix;  // count of '[' before the array element descr
    private @RawCType("BasicType") int _type;
    private int _state;
    private Symbol _previous_name;    // cache the previously looked up symbol to avoid lookups
    private @RawCType("GrowableArray<Symbol*>*") List<Symbol> _names; // symbols created while parsing that need to be dereferenced
    private static final int _s_field = 0, _s_method = 1, _s_method_return = 3;
    public static final @RawCType("enum FailureMode") int  ReturnNull=0, NCDFError=1, CachedOrNull=2 ;

    public SignatureStream(Symbol signature) {
        this(signature, true);
    }

    public SignatureStream(Symbol signature, boolean is_method) {
        if (!(!is_method || signature.char_at(0) == '(')) {
            throw new IllegalArgumentException("method signature required");
        }
        _signature = signature;
        _limit = signature.getLength();
        int oz = (is_method ? _s_method : _s_field);
        _state = oz;
        _begin = _end = oz; // skip first '(' in method signatures
        _array_prefix = 0;  // just for definiteness

        // assigning java/lang/Object to _previous_name means we can
        // avoid a number of NULL checks in the parser
        _previous_name = Symbol.getVMSymbol("java/lang/Object");
        _names = null;
        next();
    }

    private void set_done() {
        _state |= -2;   // preserve s_method bit
        if (!is_done()) {
            throw new IllegalArgumentException("Unable to set state to done");
        }
    }

    private int scan_type(@RawCType("BasicType") int type) {
        @RawCType("u1*") long base = _signature.bytes();
        int end = _end;
        int limit = _limit;
        @RawCType("u1*") long tem;
        if (type == BasicType.T_OBJECT) {
            tem = CString.memchr(base + end, ';', limit - end);
            return tem == 0L ? limit : (int) (tem + 1 - base);
        } else if (type == BasicType.T_ARRAY) {
            while ((end < limit) && ((char) (unsafe.getByte(base + end) & 0xff) == '[')) {
                end++;
            }
            _array_prefix = end - _end;  // number of '[' chars just skipped
            if (Signature.has_envelope((char) (unsafe.getByte(base + end) & 0xff))) {
                tem = CString.memchr(base + end, ';', limit - end);
                return tem == 0L ? limit : (int) (tem + 1 - base);
            }
            // Skipping over a single character for a primitive type.
            if (!is_java_primitive(BasicType.charToBasicType((char) (unsafe.getByte(base + end) & 0xff)))) {
                throw new RuntimeException("only primitives expected");
            }
            return end + 1;
        }// Skipping over a single character for a primitive type (or void).
        if (is_reference_type(type)) {
            throw new RuntimeException("only primitives or void expected");
        }
        return end + 1;
    }

    public boolean at_return_type() {
        return _state == (int) _s_method_return;
    }

    public boolean is_done() {
        return _state < 0;
    }

    public boolean is_reference() {
        return is_reference_type(_type);
    }

    public boolean is_array() {
        return _type == BasicType.T_ARRAY;
    }

    public boolean is_primitive() {
        return is_java_primitive(_type);
    }

    public @RawCType("BasicType") int type() {
        return _type;
    }

    public @RawCType("u1*") long raw_bytes() {
        return _signature.bytes() + _begin;
    }

    public int raw_length() {
        return _end - _begin;
    }

    public int raw_symbol_begin() {
        return _begin + (has_envelope() ? 1 : 0);
    }

    public int raw_symbol_end() {
        return _end - (has_envelope() ? 1 : 0);
    }

    public char raw_char_at(int i) {
        if (i >= _limit) {
            throw new IllegalArgumentException("index for raw_char_at is over the limit");
        }
        return _signature.char_at(i);
    }

    public boolean has_envelope() {
        if (!Signature.has_envelope(_signature.char_at(_begin))) {
            return false;
        }
        // this should always be true, but let's test it:
        if (_signature.char_at(_end - 1) != ';') {
            throw new RuntimeException("signature envelope has no semi-colon at end");
        }
        return true;
    }

    // return the symbol for chars in symbol_begin()..symbol_end()
    public Symbol as_symbol() {
        return find_symbol();
    }

    public void next() {
        final Symbol sig = _signature;
        int len = _limit;
        if (_end >= len) {
            set_done();
            return;
        }
        _begin = _end;
        int ch = sig.char_at(_begin);
        if (ch == ')') {
            if (_state != _s_method) {
                throw new RuntimeException("must be in method");
            }
            _state = _s_method_return;
            _begin = ++_end;
            if (_end >= len) {
                set_done();
                return;
            }
            ch = sig.char_at(_begin);
        }
        @RawCType("BasicType") int bt = BasicType.charToBasicType((char) ch);
        _type = bt;
        _end = scan_type(bt);
    }

    private static final int jl_len = 10, object_len = 6, jl_object_len = jl_len + object_len;
    private static final @RawCType("char[]") long jl_str;

    static {
        jl_str = unsafe.allocateMemory(11);
        unsafe.setMemory(jl_str, 11, (byte) '\0');
        char[] s = "java/lang/".toCharArray();
        for (int i = 0, len = s.length; i < len; i++) {
            unsafe.putByte(jl_str + i, (byte) (s[i] & 0xff));
        }
    }

    private Symbol find_symbol() {
        // Create a symbol from for string _begin _end
        int begin = raw_symbol_begin();
        int end = raw_symbol_end();

        @RawCType("char*") long symbol_chars = _signature.bytes() + begin;
        int len = end - begin;

        // Quick check for common symbols in signatures
        //assert(signature_symbols_sane(), "incorrect signature sanity check");
        if (len == jl_object_len && CString.memcmp(symbol_chars, jl_str, jl_len) == 0) {
            if (CString.memcmp("String", symbol_chars + jl_len, object_len) == 0) {
                return Symbol.getVMSymbol("java/lang/String");
            } else if (CString.memcmp("Object", symbol_chars + jl_len, object_len) == 0) {
                return Symbol.getVMSymbol("java/lang/Object");
            }
        }

        Symbol name = _previous_name;
        if (name.equals(symbol_chars, len)) {
            return name;
        }

        // Save names for cleaning up reference count at the end of
        // SignatureStream scope.
        name = Symbol.newSymbol(symbol_chars, len);

        // Only allocate the GrowableArray for the _names buffer if more than
        // one name is being processed in the signature.
        if (!_previous_name.is_permanent()) {
            if (_names == null) {
                _names = new ArrayList<>(10);
            }
            _names.add(_previous_name);
        }
        _previous_name = name;
        return name;
    }

    public void skip_to_return_type() {
        while (!at_return_type()) {
            next();
        }
    }

    public int array_prefix_length() {
        return _type == BasicType.T_ARRAY ? _array_prefix : 0;
    }

    private int skip_whole_array_prefix() {
        if (_type != BasicType.T_ARRAY) {
            throw new RuntimeException("must be");
        }
        // we are stripping all levels of T_ARRAY,
        // so we must decode the next character
        int whole_array_prefix = _array_prefix;
        int new_begin = _begin + whole_array_prefix;
        _begin = new_begin;
        int ch = _signature.char_at(new_begin);
        @RawCType("BasicType") int bt = BasicType.charToBasicType((char) ch);
        //assert(ch == type2char(bt), "bad signature char %c/%d", ch, ch);
        _type = bt;
        if (!(bt != BasicType.T_VOID && bt != BasicType.T_ARRAY)) {
            throw new RuntimeException("bad signature type");
        }
        // Don't bother to re-scan, since it won't change the value of _end.
        return whole_array_prefix;
    }

    public int skip_array_prefix(int max_skip_length) {
        if (_type != BasicType.T_ARRAY) {
            return 0;
        }
        if (_array_prefix > max_skip_length) {
            // strip some but not all levels of T_ARRAY
            _array_prefix -= max_skip_length;
            _begin += max_skip_length;
            return max_skip_length;
        }
        return skip_whole_array_prefix();
    }

    public int skip_array_prefix() {
        if (_type != BasicType.T_ARRAY) {
            return 0;
        }
        return skip_whole_array_prefix();
    }

    public Klass as_klass(ClassLoader class_loader, ProtectionDomain protection_domain,
                                    @RawCType("FailureMode")int failure_mode) {
        if (!is_reference()) {
            return null;
        }
        Symbol name = as_symbol();
        Klass k;
        if (failure_mode == ReturnNull) {
            // Note:  SD::resolve_or_null returns NULL for most failure modes,
            // but not all.  Circularity errors, invalid PDs, etc., throw.
            try{
                k=Klass.asKlass(Class.forName(name.toString().replace('/','.'),true,class_loader));
            }catch (Throwable t){
                k=null;
            }
        } else if (failure_mode == CachedOrNull) {
            Class<?> cls=ClassHelperSpecial.findLoadedClass(class_loader,name.toString().replace('/','.'));
            k = cls==null?null:Klass.asKlass(cls);
            return k;
        } else {
            // The only remaining failure mode is NCDFError.
            // The test here allows for an additional mode CNFException
            // if callers need to request the reflective error instead.
            boolean throw_error = (failure_mode == NCDFError);
            //k = SystemDictionary::resolve_or_fail(name, class_loader, protection_domain, throw_error, CHECK_NULL);
            try{
                k=Klass.asKlass(Class.forName(name.toString().replace('/','.'),true,class_loader));
            }catch (Throwable t){
                if (throw_error){
                    throwOriginalException(t);
                }
                k=null;
            }
        }
        return k;
    }

    public Class<?> as_java_mirror(ClassLoader class_loader, ProtectionDomain protection_domain,
                                        @RawCType("FailureMode")int failure_mode) {
        if (!is_reference()){
            return BasicType.java_mirror(type());
        }
        Klass klass = as_klass(class_loader, protection_domain, failure_mode);
        if (klass == null) {
            return null;
        }
        return klass.asClass();
    }
}
