package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.interpreter.Bytecodes.Code.*;
import static apphhzp.lib.hotspot.interpreter.Bytecodes.Flags.*;
import static apphhzp.lib.hotspot.utilities.BasicType.*;

public class Bytecodes {

    @SuppressWarnings("unused")
    public static class Code {
        public static final int _illegal = -1,

        // Java bytecodes
        _nop = 0, // 0x00
                _aconst_null = 1, // 0x01
                _iconst_m1 = 2, // 0x02
                _iconst_0 = 3, // 0x03
                _iconst_1 = 4, // 0x04
                _iconst_2 = 5, // 0x05
                _iconst_3 = 6, // 0x06
                _iconst_4 = 7, // 0x07
                _iconst_5 = 8, // 0x08
                _lconst_0 = 9, // 0x09
                _lconst_1 = 10, // 0x0a
                _fconst_0 = 11, // 0x0b
                _fconst_1 = 12, // 0x0c
                _fconst_2 = 13, // 0x0d
                _dconst_0 = 14, // 0x0e
                _dconst_1 = 15, // 0x0f
                _bipush = 16, // 0x10
                _sipush = 17, // 0x11
                _ldc = 18, // 0x12
                _ldc_w = 19, // 0x13
                _ldc2_w = 20, // 0x14
                _iload = 21, // 0x15
                _lload = 22, // 0x16
                _fload = 23, // 0x17
                _dload = 24, // 0x18
                _aload = 25, // 0x19
                _iload_0 = 26, // 0x1a
                _iload_1 = 27, // 0x1b
                _iload_2 = 28, // 0x1c
                _iload_3 = 29, // 0x1d
                _lload_0 = 30, // 0x1e
                _lload_1 = 31, // 0x1f
                _lload_2 = 32, // 0x20
                _lload_3 = 33, // 0x21
                _fload_0 = 34, // 0x22
                _fload_1 = 35, // 0x23
                _fload_2 = 36, // 0x24
                _fload_3 = 37, // 0x25
                _dload_0 = 38, // 0x26
                _dload_1 = 39, // 0x27
                _dload_2 = 40, // 0x28
                _dload_3 = 41, // 0x29
                _aload_0 = 42, // 0x2a
                _aload_1 = 43, // 0x2b
                _aload_2 = 44, // 0x2c
                _aload_3 = 45, // 0x2d
                _iaload = 46, // 0x2e
                _laload = 47, // 0x2f
                _faload = 48, // 0x30
                _daload = 49, // 0x31
                _aaload = 50, // 0x32
                _baload = 51, // 0x33
                _caload = 52, // 0x34
                _saload = 53, // 0x35
                _istore = 54, // 0x36
                _lstore = 55, // 0x37
                _fstore = 56, // 0x38
                _dstore = 57, // 0x39
                _astore = 58, // 0x3a
                _istore_0 = 59, // 0x3b
                _istore_1 = 60, // 0x3c
                _istore_2 = 61, // 0x3d
                _istore_3 = 62, // 0x3e
                _lstore_0 = 63, // 0x3f
                _lstore_1 = 64, // 0x40
                _lstore_2 = 65, // 0x41
                _lstore_3 = 66, // 0x42
                _fstore_0 = 67, // 0x43
                _fstore_1 = 68, // 0x44
                _fstore_2 = 69, // 0x45
                _fstore_3 = 70, // 0x46
                _dstore_0 = 71, // 0x47
                _dstore_1 = 72, // 0x48
                _dstore_2 = 73, // 0x49
                _dstore_3 = 74, // 0x4a
                _astore_0 = 75, // 0x4b
                _astore_1 = 76, // 0x4c
                _astore_2 = 77, // 0x4d
                _astore_3 = 78, // 0x4e
                _iastore = 79, // 0x4f
                _lastore = 80, // 0x50
                _fastore = 81, // 0x51
                _dastore = 82, // 0x52
                _aastore = 83, // 0x53
                _bastore = 84, // 0x54
                _castore = 85, // 0x55
                _sastore = 86, // 0x56
                _pop = 87, // 0x57
                _pop2 = 88, // 0x58
                _dup = 89, // 0x59
                _dup_x1 = 90, // 0x5a
                _dup_x2 = 91, // 0x5b
                _dup2 = 92, // 0x5c
                _dup2_x1 = 93, // 0x5d
                _dup2_x2 = 94, // 0x5e
                _swap = 95, // 0x5f
                _iadd = 96, // 0x60
                _ladd = 97, // 0x61
                _fadd = 98, // 0x62
                _dadd = 99, // 0x63
                _isub = 100, // 0x64
                _lsub = 101, // 0x65
                _fsub = 102, // 0x66
                _dsub = 103, // 0x67
                _imul = 104, // 0x68
                _lmul = 105, // 0x69
                _fmul = 106, // 0x6a
                _dmul = 107, // 0x6b
                _idiv = 108, // 0x6c
                _ldiv = 109, // 0x6d
                _fdiv = 110, // 0x6e
                _ddiv = 111, // 0x6f
                _irem = 112, // 0x70
                _lrem = 113, // 0x71
                _frem = 114, // 0x72
                _drem = 115, // 0x73
                _ineg = 116, // 0x74
                _lneg = 117, // 0x75
                _fneg = 118, // 0x76
                _dneg = 119, // 0x77
                _ishl = 120, // 0x78
                _lshl = 121, // 0x79
                _ishr = 122, // 0x7a
                _lshr = 123, // 0x7b
                _iushr = 124, // 0x7c
                _lushr = 125, // 0x7d
                _iand = 126, // 0x7e
                _land = 127, // 0x7f
                _ior = 128, // 0x80
                _lor = 129, // 0x81
                _ixor = 130, // 0x82
                _lxor = 131, // 0x83
                _iinc = 132, // 0x84
                _i2l = 133, // 0x85
                _i2f = 134, // 0x86
                _i2d = 135, // 0x87
                _l2i = 136, // 0x88
                _l2f = 137, // 0x89
                _l2d = 138, // 0x8a
                _f2i = 139, // 0x8b
                _f2l = 140, // 0x8c
                _f2d = 141, // 0x8d
                _d2i = 142, // 0x8e
                _d2l = 143, // 0x8f
                _d2f = 144, // 0x90
                _i2b = 145, // 0x91
                _i2c = 146, // 0x92
                _i2s = 147, // 0x93
                _lcmp = 148, // 0x94
                _fcmpl = 149, // 0x95
                _fcmpg = 150, // 0x96
                _dcmpl = 151, // 0x97
                _dcmpg = 152, // 0x98
                _ifeq = 153, // 0x99
                _ifne = 154, // 0x9a
                _iflt = 155, // 0x9b
                _ifge = 156, // 0x9c
                _ifgt = 157, // 0x9d
                _ifle = 158, // 0x9e
                _if_icmpeq = 159, // 0x9f
                _if_icmpne = 160, // 0xa0
                _if_icmplt = 161, // 0xa1
                _if_icmpge = 162, // 0xa2
                _if_icmpgt = 163, // 0xa3
                _if_icmple = 164, // 0xa4
                _if_acmpeq = 165, // 0xa5
                _if_acmpne = 166, // 0xa6
                _goto = 167, // 0xa7
                _jsr = 168, // 0xa8
                _ret = 169, // 0xa9
                _tableswitch = 170, // 0xaa
                _lookupswitch = 171, // 0xab
                _ireturn = 172, // 0xac
                _lreturn = 173, // 0xad
                _freturn = 174, // 0xae
                _dreturn = 175, // 0xaf
                _areturn = 176, // 0xb0
                _return = 177, // 0xb1
                _getstatic = 178, // 0xb2
                _putstatic = 179, // 0xb3
                _getfield = 180, // 0xb4
                _putfield = 181, // 0xb5
                _invokevirtual = 182, // 0xb6
                _invokespecial = 183, // 0xb7
                _invokestatic = 184, // 0xb8
                _invokeinterface = 185, // 0xb9
                _invokedynamic = 186, // 0xba
                _new = 187, // 0xbb
                _newarray = 188, // 0xbc
                _anewarray = 189, // 0xbd
                _arraylength = 190, // 0xbe
                _athrow = 191, // 0xbf
                _checkcast = 192, // 0xc0
                _instanceof = 193, // 0xc1
                _monitorenter = 194, // 0xc2
                _monitorexit = 195, // 0xc3
                _wide = 196, // 0xc4
                _multianewarray = 197, // 0xc5
                _ifnull = 198, // 0xc6
                _ifnonnull = 199, // 0xc7
                _goto_w = 200, // 0xc8
                _jsr_w = 201, // 0xc9
                _breakpoint = 202, // 0xca

        number_of_java_codes = 203,

        // JVM bytecodes
        _fast_agetfield = number_of_java_codes,
                _fast_bgetfield = 204,
                _fast_cgetfield = 205,
                _fast_dgetfield = 206,
                _fast_fgetfield = 207,
                _fast_igetfield = 208,
                _fast_lgetfield = 209,
                _fast_sgetfield = 210,

        _fast_aputfield = 211,
                _fast_bputfield = 212,
                _fast_zputfield = 213,
                _fast_cputfield = 214,
                _fast_dputfield = 215,
                _fast_fputfield = 216,
                _fast_iputfield = 217,
                _fast_lputfield = 218,
                _fast_sputfield = 219,

        _fast_aload_0 = 220,
                _fast_iaccess_0 = 221,
                _fast_aaccess_0 = 222,
                _fast_faccess_0 = 223,

        _fast_iload = 224,
                _fast_iload2 = 225,
                _fast_icaload = 226,

        _fast_invokevfinal = 227,
                _fast_linearswitch = 228,
                _fast_binaryswitch = 229,

        // special handling of oop constants:
        _fast_aldc = 230,
                _fast_aldc_w = 231,

        _return_register_finalizer = 232,

        // special handling of signature-polymorphic methods:
        _invokehandle = 233,

        // These bytecodes are rewritten at CDS dump time, so that we can prevent them from being
        // rewritten at run time. This way, the ConstMethods can be placed in the CDS ReadOnly
        // section, and RewriteByteCodes/RewriteFrequentPairs can rewrite non-CDS bytecodes
        // at run time.
        //
        // Rewritten at CDS dump time to | Original bytecode
        // _invoke_virtual rewritten on sparc, will be disabled if UseSharedSpaces turned on.
        // ------------------------------+------------------
        _nofast_getfield = 234,          //  <- _getfield
                _nofast_putfield = 235,          //  <- _putfield
                _nofast_aload_0 = 236,          //  <- _aload_0
                _nofast_iload = 237,          //  <- _iload

        _shouldnotreachhere = 238,          // For debugging


        number_of_codes = 239;
        public final int code;
        private static final Code[] caches = new Code[number_of_codes + 5];

        public static Code of(int val) {
            if (val < _illegal || val >= number_of_codes) {
                throw new IllegalArgumentException("illegal code: " + val);
            }
            return caches[val + 1] == null ? caches[val + 1] = new Code(val) : caches[val + 1];
        }

        public static Code of(byte val) {
            return of(tran(val));
        }

        public static int tran(byte val) {
            return val & 0xff;
        }

        private Code(int code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "Bytecodes::Code(" + this.code + ")";
        }
    }

    public static class Flags {
        public static final int
                _bc_can_trap = 1 << 0,     // bytecode execution can trap or block
                _bc_can_rewrite = 1 << 1,     // bytecode execution has an alternate form

        // format bits (determined only by the format string):
        _fmt_has_c = 1 << 2,     // constant, such as sipush "bcc"
                _fmt_has_j = 1 << 3,     // constant pool cache index, such as getfield "bjj"
                _fmt_has_k = 1 << 4,     // constant pool index, such as ldc "bk"
                _fmt_has_i = 1 << 5,     // local index, such as iload
                _fmt_has_o = 1 << 6,     // offset, such as ifeq
                _fmt_has_nbo = 1 << 7,     // contains native-order field(s)
                _fmt_has_u2 = 1 << 8,     // contains double-byte field(s)
                _fmt_has_u4 = 1 << 9,     // contains quad-byte field
                _fmt_not_variable = 1 << 10,    // not of variable length (simple or wide)
                _fmt_not_simple = 1 << 11,    // either wide or variable length
                _all_fmt_bits = (_fmt_not_simple * 2 - _fmt_has_c),

        // Example derived format syndromes:
        _fmt_b = _fmt_not_variable,
                _fmt_bc = _fmt_b | _fmt_has_c,
                _fmt_bi = _fmt_b | _fmt_has_i,
                _fmt_bkk = _fmt_b | _fmt_has_k | _fmt_has_u2,
                _fmt_bJJ = _fmt_b | _fmt_has_j | _fmt_has_u2 | _fmt_has_nbo,
                _fmt_bo2 = _fmt_b | _fmt_has_o | _fmt_has_u2,
                _fmt_bo4 = _fmt_b | _fmt_has_o | _fmt_has_u4;
        public final int val;

        public Flags(int val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return "Bytecodes::Flags(" + this.val + ")";
        }
    }

    public static String[] _name = new String[number_of_codes];
    public static @RawCType("BasicType[]") int[] _result_type = new int[number_of_codes];
    public static byte[] _depth = new byte[number_of_codes];
    public static @RawCType("u_char[]") int[]     _lengths = new int[number_of_codes];
    public static @RawCType("Code[]") int[]        _java_code = new int[number_of_codes];
    public static @RawCType("char[]") int[]       _flags = new int[(1 << JVM.BitsPerByte) * 2]; // all second page for wide formats
    private static boolean _is_initialized = false;

    static {
        initialize();
    }

    private static void def(int code, String name, String format, String wide_format, int result_type, int depth, boolean can_trap) {
        def(code, name, format, wide_format, result_type, depth, can_trap, code);
    }


    private static void def(int code, String name, String format, String wide_format, int result_type, int depth, boolean can_trap, int java_code) {
        //assert(wide_format == null || format != null, "short form must exist if there's a wide form");
        int len = (format != null ? (int) format.length() : 0);
        int wlen = (wide_format != null ? (int) wide_format.length() : 0);
        _name[code] = name;
        _result_type[code] = result_type;
        _depth[code] = (byte) depth;
        _lengths[code] = (((wlen << 4) | (len & 0xF)) & 0xff);
        _java_code[code] = java_code;
        int bc_flags = 0;
        if (can_trap) bc_flags |= Flags._bc_can_trap;
        if (java_code != code) bc_flags |= Flags._bc_can_rewrite;
        _flags[code] = compute_flags(format, bc_flags);
        _flags[code + (1 << JVM.BitsPerByte)] = compute_flags(wide_format, bc_flags);
//        assert(is_defined(code)      == (format != null),      "");
//        assert(wide_is_defined(code) == (wide_format != null), "");
//        assert(length_for(code)      == len, "");
//        assert(wide_length_for(code) == wlen, "");
    }

    public static int raw_special_length_at(long bcp, long end) {
        int code = code_or_bp_at(bcp);
        if (code == _breakpoint) {
            return 1;
        } else {
            return special_length_at(code, bcp, end);
        }
    }

    public static int code_or_bp_at(long bcp) {
        return unsafe.getByte(bcp) & 0xff;
    }

    public static int special_length_at(int code, long bcp) {
        return special_length_at(code, bcp, 0);
    }

    public static int special_length_at(int code, long bcp, long end) {
        switch (code) {
            case _wide:
                if (end != 0L && bcp + 1 >= end) {
                    return -1; // don't read past end of code buffer
                }
                return wide_length_for(unsafe.getByte((bcp + 1)) & 0xff);
            case _tableswitch: {
                long aligned_bcp = JVM.alignUp(bcp + 1, 4);
                if (end != 0L && aligned_bcp + 3 * 4 >= end) {
                    return -1; // don't read past end of code buffer
                }
                // Promote calculation to signed 64 bits to do range checks, used by the verifier.
                long lo = unsafe.getInt(aligned_bcp + 1 * 4);
                long hi = unsafe.getInt(aligned_bcp + 2 * 4);
                long len = (aligned_bcp - bcp) + (3 + hi - lo + 1) * 4;
                // Only return len if it can be represented as a positive int and lo <= hi.
                // The caller checks for bytecode stream overflow.
                if (lo <= hi && len == (int) len) {
                    if (len <= 0) {
                        throw new RuntimeException("must be");
                    }
                    return (int) len;
                } else {
                    return -1;
                }
            }

            case _lookupswitch:      // fall through
            case _fast_binaryswitch: // fall through
            case _fast_linearswitch: {
                long aligned_bcp = JVM.alignUp(bcp + 1, 4);
                if (end != 0L && aligned_bcp + 2 * 4 >= end) {
                    return -1; // don't read past end of code buffer
                }
                long npairs = unsafe.getInt(aligned_bcp + 4);
                long len = (aligned_bcp - bcp) + (2 + 2 * npairs) * 4;
                // Only return len if it can be represented as a positive int and npairs >= 0.
                if (npairs >= 0 && len == (int) len) {
                    if (len <= 0) {
                        throw new RuntimeException("must be");
                    }
                    return (int) len;
                } else {
                    return -1;
                }
            }
            default:
                // Note: Length functions must return <=0 for invalid bytecodes.
                return 0;
        }
    }

    private static int compute_flags(String format, int more_flags) {
        if (format == null) return 0;  // not even more_flags
        int flags = more_flags;
        int fp = 0;
        switch (JVM.charAt(format,(fp))){
            case '\0':
                flags |= _fmt_not_simple; // but variable
                break;
            case 'b':
                flags |= _fmt_not_variable;  // but simple
                ++fp;  // skip 'b'
                break;
            case 'w':
                flags |= _fmt_not_variable | _fmt_not_simple;
                ++fp;  // skip 'w'
                if (JVM.charAt(format,fp) != 'b') {
                    throw new RuntimeException("wide format must start with 'wb'");
                }
                ++fp;  // skip 'b'
                break;
        }

        int has_nbo = 0, has_jbo = 0, has_size = 0;
        for (; ; ) {
            int this_flag;
            char fc = JVM.charAt(format,(fp++));
            switch (fc) {
                case '\0':  // end of string
                    if (flags != (char) flags) {
                        throw new RuntimeException("change _format_flags");
                    }
                    return flags;

                case '_':
                    continue;         // ignore these

                case 'j':
                    this_flag = _fmt_has_j;
                    has_jbo = 1;
                    break;
                case 'k':
                    this_flag = _fmt_has_k;
                    has_jbo = 1;
                    break;
                case 'i':
                    this_flag = _fmt_has_i;
                    has_jbo = 1;
                    break;
                case 'c':
                    this_flag = _fmt_has_c;
                    has_jbo = 1;
                    break;
                case 'o':
                    this_flag = _fmt_has_o;
                    has_jbo = 1;
                    break;

                // uppercase versions mark native byte order (from Rewriter)
                // actually, only the 'J' case happens currently
                case 'J':
                    this_flag = _fmt_has_j;
                    has_nbo = 1;
                    break;
                case 'K':
                    this_flag = _fmt_has_k;
                    has_nbo = 1;
                    break;
                case 'I':
                    this_flag = _fmt_has_i;
                    has_nbo = 1;
                    break;
                case 'C':
                    this_flag = _fmt_has_c;
                    has_nbo = 1;
                    break;
                case 'O':
                    this_flag = _fmt_has_o;
                    has_nbo = 1;
                    break;
                default:
                    throw new RuntimeException("bad char in format"); //guarantee(false, "bad char in format");
            }

            flags |= this_flag;

            if (has_jbo != 0 && has_nbo != 0) {
                throw new RuntimeException("mixed byte orders in format");
            }
            if (has_nbo != 0)
                flags |= _fmt_has_nbo;

            int this_size = 1;
            if (JVM.charAt(format,(fp)) == fc) {
                // advance beyond run of the same characters
                this_size = 2;
                while (JVM.charAt(format,(++fp)) == fc) this_size++;
                switch (this_size) {
                    case 2:
                        flags |= _fmt_has_u2;
                        break;
                    case 4:
                        flags |= _fmt_has_u4;
                        break;
                    default:
                        throw new RuntimeException("bad rep count in format");
                }
            }
            if (!(has_size == 0 ||                     // no field yet
                    this_size == has_size ||             // same size
                    this_size < has_size && JVM.charAt(format,(fp)) == '\0'// last field can be short
            )) {
                throw new RuntimeException("mixed field sizes in format");
            }
            has_size = this_size;
        }
    }

    public static void check(int code) {
        if (!is_defined(code)) {
            throw new IllegalArgumentException("illegal code: " + code);
        }
    }

    public static void wide_check(int code) {
        if (!wide_is_defined(code)) {
            throw new IllegalArgumentException("illegal code: " + code);
        }
    }

    public static boolean is_valid(int code) {
        return 0 <= code && code < number_of_codes;
    }

    public static boolean is_defined(int code) {
        return is_valid(code) && flags(code, false) != 0;
    }

    public static boolean wide_is_defined(int code) {
        return is_defined(code) && flags(code, true) != 0;
    }

    public static String name(int code) {
        check(code);
        return _name[code];
    }

    public static @RawCType("BasicType") int result_type(int code) {
        check(code);
        return _result_type[code];
    }

    public static int depth(int code) {
        check(code);
        return _depth[code];
    }

    public static int length_for(int code) {
        return is_valid(code) ? _lengths[code] & 0xF : -1;
    }

    public static int wide_length_for(int code) {
        return is_valid(code) ? _lengths[code] >> 4 : -1;
    }

    public static int non_breakpoint_code_at(Method method, long bcp) {
        if (method == null) {
            throw new IllegalArgumentException("must have the method for breakpoint conversion");
        }
        if (!method.contains(bcp)) {
            throw new IllegalArgumentException("must be valid bcp in method");
        }
        return method.orig_bytecode_at(method.bci_from(bcp));
    }

    public static boolean check_method(Method method, long bcp) {
        if (JVM.ENABLE_EXTRA_CHECK) {
            return method.contains(bcp);
        }
        return true;
    }

    public static boolean check_must_rewrite(int code) {
        if (!can_rewrite(code)) {
            throw new IllegalArgumentException("post-check only");
        }
        // Some codes are conditionally rewriting.  Look closely at them.
        switch (code) {
            case _aload_0:
                // Even if RewriteFrequentPairs is turned on,
                // the _aload_0 code might delay its rewrite until
                // a following _getfield rewrites itself.
                return false;
            case _lookupswitch:
                return false;  // the rewrite is not done by the interpreter
            case _new:
                // (Could actually look at the class here, but the profit would be small.)
                return false;  // the rewrite is not always done
            default:
                // No other special cases.
                return true;
        }
    }

    public static int code_at(Method method, long bcp) {
        if (!(method == null || check_method(method, bcp))) {
            throw new IllegalArgumentException("bcp must point into method");
        }
        int code = unsafe.getByte(bcp) & 0xff;
        if (!(code != _breakpoint || method != null)) {
            throw new IllegalArgumentException("need Method* to decode breakpoint");
        }
        return (code != _breakpoint) ? code : non_breakpoint_code_at(method, bcp);
    }

    public static int java_code_at(Method method, long bcp) {
        return java_code(code_at(method, bcp));
    }

    public static boolean can_trap(int code) {
        check(code);
        return has_all_flags(code, _bc_can_trap, false);
    }

    public static int java_code(int code) {
        check(code);
        return _java_code[code];
    }

    public static boolean can_rewrite(int code) {
        check(code);
        return has_all_flags(code, _bc_can_rewrite, false);
    }

    public static boolean must_rewrite(int code) {
        return can_rewrite(code) && check_must_rewrite(code);
    }

    public static boolean native_byte_order(int code) {
        check(code);
        return has_all_flags(code, _fmt_has_nbo, false);
    }

    public static boolean uses_cp_cache(int code) {
        check(code);
        return has_all_flags(code, _fmt_has_j, false);
    }

    // if 'end' is provided, it indicates the end of the code buffer which
    // should not be read past when parsing.
    public static int length_for_code_at(int code, long bcp) {
        int l = length_for(code);
        return l > 0 ? l : special_length_at(code, bcp);
    }

    public static int length_at(Method method, long bcp) {
        return length_for_code_at(code_at(method, bcp), bcp);
    }

    public static int java_length_at(Method method, long bcp) {
        return length_for_code_at(java_code_at(method, bcp), bcp);
    }

    public static boolean is_java_code(int code) {
        return 0 <= code && code < number_of_java_codes;
    }

    public static boolean is_store_into_local(int code) {
        return (_istore <= code && code <= _astore_3);
    }

    public static boolean is_const(int code) {
        return (_aconst_null <= code && code <= _ldc2_w);
    }

    public static boolean is_zero_const(int code) {
        return (code == _aconst_null || code == _iconst_0
                || code == _fconst_0 || code == _dconst_0);
    }

    public static boolean is_return(int code) {
        return (_ireturn <= code && code <= _return);
    }

    public static boolean is_invoke(int code) {
        return (_invokevirtual <= code && code <= _invokedynamic);
    }

    public static boolean has_receiver(int code) {
        if (!is_invoke(code)) {
            throw new IllegalArgumentException();
        }
        return code == _invokevirtual ||
                code == _invokespecial ||
                code == _invokeinterface;
    }

    public static boolean has_optional_appendix(int code) {
        return code == _invokedynamic || code == _invokehandle;
    }

    public static int flags(int code, boolean is_wide) {
        if (code != (code & 0xff)) {
            throw new IllegalArgumentException("must be a byte");
        }
        return _flags[code + (is_wide ? (1 << JVM.BitsPerByte) : 0)];
    }

    public static boolean has_all_flags(int code, int test_flags, boolean is_wide) {
        return (flags(code, is_wide) & test_flags) == test_flags;
    }

    private static void initialize() {
        if (_is_initialized) return;
        if (number_of_codes > 256) {
            throw new ExceptionInInitializerError("too many bytecodes");
        }
        // initialize bytecode tables - didn't use static array initializers
        // (such as {}) so we can do additional consistency checks and init-
        // code is independent of actual bytecode numbering.
        //
        // Note 1: NULL for the format string means the bytecode doesn't exist
        //         in that form.
        //
        // Note 2: The result type is T_ILLEGAL for bytecodes where the top of stack
        //         type after execution is not only determined by the bytecode itself.

        //  Java bytecodes
        //  bytecode               bytecode name           format   wide f.   result tp  stk traps
        def(_nop, "nop", "b", null, T_VOID, 0, false);
        def(_aconst_null, "aconst_null", "b", null, T_OBJECT, 1, false);
        def(_iconst_m1, "iconst_m1", "b", null, T_INT, 1, false);
        def(_iconst_0, "iconst_0", "b", null, T_INT, 1, false);
        def(_iconst_1, "iconst_1", "b", null, T_INT, 1, false);
        def(_iconst_2, "iconst_2", "b", null, T_INT, 1, false);
        def(_iconst_3, "iconst_3", "b", null, T_INT, 1, false);
        def(_iconst_4, "iconst_4", "b", null, T_INT, 1, false);
        def(_iconst_5, "iconst_5", "b", null, T_INT, 1, false);
        def(_lconst_0, "lconst_0", "b", null, T_LONG, 2, false);
        def(_lconst_1, "lconst_1", "b", null, T_LONG, 2, false);
        def(_fconst_0, "fconst_0", "b", null, T_FLOAT, 1, false);
        def(_fconst_1, "fconst_1", "b", null, T_FLOAT, 1, false);
        def(_fconst_2, "fconst_2", "b", null, T_FLOAT, 1, false);
        def(_dconst_0, "dconst_0", "b", null, T_DOUBLE, 2, false);
        def(_dconst_1, "dconst_1", "b", null, T_DOUBLE, 2, false);
        def(_bipush, "bipush", "bc", null, T_INT, 1, false);
        def(_sipush, "sipush", "bcc", null, T_INT, 1, false);
        def(_ldc, "ldc", "bk", null, T_ILLEGAL, 1, true);
        def(_ldc_w, "ldc_w", "bkk", null, T_ILLEGAL, 1, true);
        def(_ldc2_w, "ldc2_w", "bkk", null, T_ILLEGAL, 2, true);
        def(_iload, "iload", "bi", "wbii", T_INT, 1, false);
        def(_lload, "lload", "bi", "wbii", T_LONG, 2, false);
        def(_fload, "fload", "bi", "wbii", T_FLOAT, 1, false);
        def(_dload, "dload", "bi", "wbii", T_DOUBLE, 2, false);
        def(_aload, "aload", "bi", "wbii", T_OBJECT, 1, false);
        def(_iload_0, "iload_0", "b", null, T_INT, 1, false);
        def(_iload_1, "iload_1", "b", null, T_INT, 1, false);
        def(_iload_2, "iload_2", "b", null, T_INT, 1, false);
        def(_iload_3, "iload_3", "b", null, T_INT, 1, false);
        def(_lload_0, "lload_0", "b", null, T_LONG, 2, false);
        def(_lload_1, "lload_1", "b", null, T_LONG, 2, false);
        def(_lload_2, "lload_2", "b", null, T_LONG, 2, false);
        def(_lload_3, "lload_3", "b", null, T_LONG, 2, false);
        def(_fload_0, "fload_0", "b", null, T_FLOAT, 1, false);
        def(_fload_1, "fload_1", "b", null, T_FLOAT, 1, false);
        def(_fload_2, "fload_2", "b", null, T_FLOAT, 1, false);
        def(_fload_3, "fload_3", "b", null, T_FLOAT, 1, false);
        def(_dload_0, "dload_0", "b", null, T_DOUBLE, 2, false);
        def(_dload_1, "dload_1", "b", null, T_DOUBLE, 2, false);
        def(_dload_2, "dload_2", "b", null, T_DOUBLE, 2, false);
        def(_dload_3, "dload_3", "b", null, T_DOUBLE, 2, false);
        def(_aload_0, "aload_0", "b", null, T_OBJECT, 1, true); // rewriting in interpreter
        def(_aload_1, "aload_1", "b", null, T_OBJECT, 1, false);
        def(_aload_2, "aload_2", "b", null, T_OBJECT, 1, false);
        def(_aload_3, "aload_3", "b", null, T_OBJECT, 1, false);
        def(_iaload, "iaload", "b", null, T_INT, -1, true);
        def(_laload, "laload", "b", null, T_LONG, 0, true);
        def(_faload, "faload", "b", null, T_FLOAT, -1, true);
        def(_daload, "daload", "b", null, T_DOUBLE, 0, true);
        def(_aaload, "aaload", "b", null, T_OBJECT, -1, true);
        def(_baload, "baload", "b", null, T_INT, -1, true);
        def(_caload, "caload", "b", null, T_INT, -1, true);
        def(_saload, "saload", "b", null, T_INT, -1, true);
        def(_istore, "istore", "bi", "wbii", T_VOID, -1, false);
        def(_lstore, "lstore", "bi", "wbii", T_VOID, -2, false);
        def(_fstore, "fstore", "bi", "wbii", T_VOID, -1, false);
        def(_dstore, "dstore", "bi", "wbii", T_VOID, -2, false);
        def(_astore, "astore", "bi", "wbii", T_VOID, -1, false);
        def(_istore_0, "istore_0", "b", null, T_VOID, -1, false);
        def(_istore_1, "istore_1", "b", null, T_VOID, -1, false);
        def(_istore_2, "istore_2", "b", null, T_VOID, -1, false);
        def(_istore_3, "istore_3", "b", null, T_VOID, -1, false);
        def(_lstore_0, "lstore_0", "b", null, T_VOID, -2, false);
        def(_lstore_1, "lstore_1", "b", null, T_VOID, -2, false);
        def(_lstore_2, "lstore_2", "b", null, T_VOID, -2, false);
        def(_lstore_3, "lstore_3", "b", null, T_VOID, -2, false);
        def(_fstore_0, "fstore_0", "b", null, T_VOID, -1, false);
        def(_fstore_1, "fstore_1", "b", null, T_VOID, -1, false);
        def(_fstore_2, "fstore_2", "b", null, T_VOID, -1, false);
        def(_fstore_3, "fstore_3", "b", null, T_VOID, -1, false);
        def(_dstore_0, "dstore_0", "b", null, T_VOID, -2, false);
        def(_dstore_1, "dstore_1", "b", null, T_VOID, -2, false);
        def(_dstore_2, "dstore_2", "b", null, T_VOID, -2, false);
        def(_dstore_3, "dstore_3", "b", null, T_VOID, -2, false);
        def(_astore_0, "astore_0", "b", null, T_VOID, -1, false);
        def(_astore_1, "astore_1", "b", null, T_VOID, -1, false);
        def(_astore_2, "astore_2", "b", null, T_VOID, -1, false);
        def(_astore_3, "astore_3", "b", null, T_VOID, -1, false);
        def(_iastore, "iastore", "b", null, T_VOID, -3, true);
        def(_lastore, "lastore", "b", null, T_VOID, -4, true);
        def(_fastore, "fastore", "b", null, T_VOID, -3, true);
        def(_dastore, "dastore", "b", null, T_VOID, -4, true);
        def(_aastore, "aastore", "b", null, T_VOID, -3, true);
        def(_bastore, "bastore", "b", null, T_VOID, -3, true);
        def(_castore, "castore", "b", null, T_VOID, -3, true);
        def(_sastore, "sastore", "b", null, T_VOID, -3, true);
        def(_pop, "pop", "b", null, T_VOID, -1, false);
        def(_pop2, "pop2", "b", null, T_VOID, -2, false);
        def(_dup, "dup", "b", null, T_VOID, 1, false);
        def(_dup_x1, "dup_x1", "b", null, T_VOID, 1, false);
        def(_dup_x2, "dup_x2", "b", null, T_VOID, 1, false);
        def(_dup2, "dup2", "b", null, T_VOID, 2, false);
        def(_dup2_x1, "dup2_x1", "b", null, T_VOID, 2, false);
        def(_dup2_x2, "dup2_x2", "b", null, T_VOID, 2, false);
        def(_swap, "swap", "b", null, T_VOID, 0, false);
        def(_iadd, "iadd", "b", null, T_INT, -1, false);
        def(_ladd, "ladd", "b", null, T_LONG, -2, false);
        def(_fadd, "fadd", "b", null, T_FLOAT, -1, false);
        def(_dadd, "dadd", "b", null, T_DOUBLE, -2, false);
        def(_isub, "isub", "b", null, T_INT, -1, false);
        def(_lsub, "lsub", "b", null, T_LONG, -2, false);
        def(_fsub, "fsub", "b", null, T_FLOAT, -1, false);
        def(_dsub, "dsub", "b", null, T_DOUBLE, -2, false);
        def(_imul, "imul", "b", null, T_INT, -1, false);
        def(_lmul, "lmul", "b", null, T_LONG, -2, false);
        def(_fmul, "fmul", "b", null, T_FLOAT, -1, false);
        def(_dmul, "dmul", "b", null, T_DOUBLE, -2, false);
        def(_idiv, "idiv", "b", null, T_INT, -1, true);
        def(_ldiv, "ldiv", "b", null, T_LONG, -2, true);
        def(_fdiv, "fdiv", "b", null, T_FLOAT, -1, false);
        def(_ddiv, "ddiv", "b", null, T_DOUBLE, -2, false);
        def(_irem, "irem", "b", null, T_INT, -1, true);
        def(_lrem, "lrem", "b", null, T_LONG, -2, true);
        def(_frem, "frem", "b", null, T_FLOAT, -1, false);
        def(_drem, "drem", "b", null, T_DOUBLE, -2, false);
        def(_ineg, "ineg", "b", null, T_INT, 0, false);
        def(_lneg, "lneg", "b", null, T_LONG, 0, false);
        def(_fneg, "fneg", "b", null, T_FLOAT, 0, false);
        def(_dneg, "dneg", "b", null, T_DOUBLE, 0, false);
        def(_ishl, "ishl", "b", null, T_INT, -1, false);
        def(_lshl, "lshl", "b", null, T_LONG, -1, false);
        def(_ishr, "ishr", "b", null, T_INT, -1, false);
        def(_lshr, "lshr", "b", null, T_LONG, -1, false);
        def(_iushr, "iushr", "b", null, T_INT, -1, false);
        def(_lushr, "lushr", "b", null, T_LONG, -1, false);
        def(_iand, "iand", "b", null, T_INT, -1, false);
        def(_land, "land", "b", null, T_LONG, -2, false);
        def(_ior, "ior", "b", null, T_INT, -1, false);
        def(_lor, "lor", "b", null, T_LONG, -2, false);
        def(_ixor, "ixor", "b", null, T_INT, -1, false);
        def(_lxor, "lxor", "b", null, T_LONG, -2, false);
        def(_iinc, "iinc", "bic", "wbiicc", T_VOID, 0, false);
        def(_i2l, "i2l", "b", null, T_LONG, 1, false);
        def(_i2f, "i2f", "b", null, T_FLOAT, 0, false);
        def(_i2d, "i2d", "b", null, T_DOUBLE, 1, false);
        def(_l2i, "l2i", "b", null, T_INT, -1, false);
        def(_l2f, "l2f", "b", null, T_FLOAT, -1, false);
        def(_l2d, "l2d", "b", null, T_DOUBLE, 0, false);
        def(_f2i, "f2i", "b", null, T_INT, 0, false);
        def(_f2l, "f2l", "b", null, T_LONG, 1, false);
        def(_f2d, "f2d", "b", null, T_DOUBLE, 1, false);
        def(_d2i, "d2i", "b", null, T_INT, -1, false);
        def(_d2l, "d2l", "b", null, T_LONG, 0, false);
        def(_d2f, "d2f", "b", null, T_FLOAT, -1, false);
        def(_i2b, "i2b", "b", null, T_BYTE, 0, false);
        def(_i2c, "i2c", "b", null, T_CHAR, 0, false);
        def(_i2s, "i2s", "b", null, T_SHORT, 0, false);
        def(_lcmp, "lcmp", "b", null, T_VOID, -3, false);
        def(_fcmpl, "fcmpl", "b", null, T_VOID, -1, false);
        def(_fcmpg, "fcmpg", "b", null, T_VOID, -1, false);
        def(_dcmpl, "dcmpl", "b", null, T_VOID, -3, false);
        def(_dcmpg, "dcmpg", "b", null, T_VOID, -3, false);
        def(_ifeq, "ifeq", "boo", null, T_VOID, -1, false);
        def(_ifne, "ifne", "boo", null, T_VOID, -1, false);
        def(_iflt, "iflt", "boo", null, T_VOID, -1, false);
        def(_ifge, "ifge", "boo", null, T_VOID, -1, false);
        def(_ifgt, "ifgt", "boo", null, T_VOID, -1, false);
        def(_ifle, "ifle", "boo", null, T_VOID, -1, false);
        def(_if_icmpeq, "if_icmpeq", "boo", null, T_VOID, -2, false);
        def(_if_icmpne, "if_icmpne", "boo", null, T_VOID, -2, false);
        def(_if_icmplt, "if_icmplt", "boo", null, T_VOID, -2, false);
        def(_if_icmpge, "if_icmpge", "boo", null, T_VOID, -2, false);
        def(_if_icmpgt, "if_icmpgt", "boo", null, T_VOID, -2, false);
        def(_if_icmple, "if_icmple", "boo", null, T_VOID, -2, false);
        def(_if_acmpeq, "if_acmpeq", "boo", null, T_VOID, -2, false);
        def(_if_acmpne, "if_acmpne", "boo", null, T_VOID, -2, false);
        def(_goto, "goto", "boo", null, T_VOID, 0, false);
        def(_jsr, "jsr", "boo", null, T_INT, 0, false);
        def(_ret, "ret", "bi", "wbii", T_VOID, 0, false);
        def(_tableswitch, "tableswitch", "", null, T_VOID, -1, false); // may have backward branches
        def(_lookupswitch, "lookupswitch", "", null, T_VOID, -1, false); // rewriting in interpreter
        def(_ireturn, "ireturn", "b", null, T_INT, -1, true);
        def(_lreturn, "lreturn", "b", null, T_LONG, -2, true);
        def(_freturn, "freturn", "b", null, T_FLOAT, -1, true);
        def(_dreturn, "dreturn", "b", null, T_DOUBLE, -2, true);
        def(_areturn, "areturn", "b", null, T_OBJECT, -1, true);
        def(_return, "return", "b", null, T_VOID, 0, true);
        def(_getstatic, "getstatic", "bJJ", null, T_ILLEGAL, 1, true);
        def(_putstatic, "putstatic", "bJJ", null, T_ILLEGAL, -1, true);
        def(_getfield, "getfield", "bJJ", null, T_ILLEGAL, 0, true);
        def(_putfield, "putfield", "bJJ", null, T_ILLEGAL, -2, true);
        def(_invokevirtual, "invokevirtual", "bJJ", null, T_ILLEGAL, -1, true);
        def(_invokespecial, "invokespecial", "bJJ", null, T_ILLEGAL, -1, true);
        def(_invokestatic, "invokestatic", "bJJ", null, T_ILLEGAL, 0, true);
        def(_invokeinterface, "invokeinterface", "bJJ__", null, T_ILLEGAL, -1, true);
        def(_invokedynamic, "invokedynamic", "bJJJJ", null, T_ILLEGAL, 0, true);
        def(_new, "new", "bkk", null, T_OBJECT, 1, true);
        def(_newarray, "newarray", "bc", null, T_OBJECT, 0, true);
        def(_anewarray, "anewarray", "bkk", null, T_OBJECT, 0, true);
        def(_arraylength, "arraylength", "b", null, T_INT, 0, true);
        def(_athrow, "athrow", "b", null, T_VOID, -1, true);
        def(_checkcast, "checkcast", "bkk", null, T_OBJECT, 0, true);
        def(_instanceof, "instanceof", "bkk", null, T_INT, 0, true);
        def(_monitorenter, "monitorenter", "b", null, T_VOID, -1, true);
        def(_monitorexit, "monitorexit", "b", null, T_VOID, -1, true);
        def(_wide, "wide", "", null, T_VOID, 0, false);
        def(_multianewarray, "multianewarray", "bkkc", null, T_OBJECT, 1, true);
        def(_ifnull, "ifnull", "boo", null, T_VOID, -1, false);
        def(_ifnonnull, "ifnonnull", "boo", null, T_VOID, -1, false);
        def(_goto_w, "goto_w", "boooo", null, T_VOID, 0, false);
        def(_jsr_w, "jsr_w", "boooo", null, T_INT, 0, false);
        def(_breakpoint, "breakpoint", "", null, T_VOID, 0, true);

        //  JVM bytecodes
        //  bytecode               bytecode name           format   wide f.   result tp  stk traps  std code

        def(_fast_agetfield, "fast_agetfield", "bJJ", null, T_OBJECT, 0, true, _getfield);
        def(_fast_bgetfield, "fast_bgetfield", "bJJ", null, T_INT, 0, true, _getfield);
        def(_fast_cgetfield, "fast_cgetfield", "bJJ", null, T_CHAR, 0, true, _getfield);
        def(_fast_dgetfield, "fast_dgetfield", "bJJ", null, T_DOUBLE, 0, true, _getfield);
        def(_fast_fgetfield, "fast_fgetfield", "bJJ", null, T_FLOAT, 0, true, _getfield);
        def(_fast_igetfield, "fast_igetfield", "bJJ", null, T_INT, 0, true, _getfield);
        def(_fast_lgetfield, "fast_lgetfield", "bJJ", null, T_LONG, 0, true, _getfield);
        def(_fast_sgetfield, "fast_sgetfield", "bJJ", null, T_SHORT, 0, true, _getfield);

        def(_fast_aputfield, "fast_aputfield", "bJJ", null, T_OBJECT, 0, true, _putfield);
        def(_fast_bputfield, "fast_bputfield", "bJJ", null, T_INT, 0, true, _putfield);
        def(_fast_zputfield, "fast_zputfield", "bJJ", null, T_INT, 0, true, _putfield);
        def(_fast_cputfield, "fast_cputfield", "bJJ", null, T_CHAR, 0, true, _putfield);
        def(_fast_dputfield, "fast_dputfield", "bJJ", null, T_DOUBLE, 0, true, _putfield);
        def(_fast_fputfield, "fast_fputfield", "bJJ", null, T_FLOAT, 0, true, _putfield);
        def(_fast_iputfield, "fast_iputfield", "bJJ", null, T_INT, 0, true, _putfield);
        def(_fast_lputfield, "fast_lputfield", "bJJ", null, T_LONG, 0, true, _putfield);
        def(_fast_sputfield, "fast_sputfield", "bJJ", null, T_SHORT, 0, true, _putfield);

        def(_fast_aload_0, "fast_aload_0", "b", null, T_OBJECT, 1, true, _aload_0);
        def(_fast_iaccess_0, "fast_iaccess_0", "b_JJ", null, T_INT, 1, true, _aload_0);
        def(_fast_aaccess_0, "fast_aaccess_0", "b_JJ", null, T_OBJECT, 1, true, _aload_0);
        def(_fast_faccess_0, "fast_faccess_0", "b_JJ", null, T_OBJECT, 1, true, _aload_0);

        def(_fast_iload, "fast_iload", "bi", null, T_INT, 1, false, _iload);
        def(_fast_iload2, "fast_iload2", "bi_i", null, T_INT, 2, false, _iload);
        def(_fast_icaload, "fast_icaload", "bi_", null, T_INT, 0, false, _iload);

        // Faster method invocation.
        def(_fast_invokevfinal, "fast_invokevfinal", "bJJ", null, T_ILLEGAL, -1, true, _invokevirtual);

        def(_fast_linearswitch, "fast_linearswitch", "", null, T_VOID, -1, false, _lookupswitch);
        def(_fast_binaryswitch, "fast_binaryswitch", "", null, T_VOID, -1, false, _lookupswitch);

        def(_return_register_finalizer, "return_register_finalizer", "b", null, T_VOID, 0, true, _return);

        def(_invokehandle, "invokehandle", "bJJ", null, T_ILLEGAL, -1, true, _invokevirtual);

        def(_fast_aldc, "fast_aldc", "bj", null, T_OBJECT, 1, true, _ldc);
        def(_fast_aldc_w, "fast_aldc_w", "bJJ", null, T_OBJECT, 1, true, _ldc_w);

        def(_nofast_getfield, "nofast_getfield", "bJJ", null, T_ILLEGAL, 0, true, _getfield);
        def(_nofast_putfield, "nofast_putfield", "bJJ", null, T_ILLEGAL, -2, true, _putfield);

        def(_nofast_aload_0, "nofast_aload_0", "b", null, T_OBJECT, 1, true, _aload_0);
        def(_nofast_iload, "nofast_iload", "bi", null, T_INT, 1, false, _iload);

        def(_shouldnotreachhere, "_shouldnotreachhere", "b", null, T_VOID, 0, false);

        // compare can_trap information for each bytecode with the
        // can_trap information for the corresponding base bytecode
        // (if a rewritten bytecode can trap, so must the base bytecode)
//  #ifdef ASSERT
//        { for (int i = 0; i < number_of_codes; i++) {
//            if (is_defined(i)) {
//                Code code = cast(i);
//                Code java = java_code(code);
//                if (can_trap(code) && !can_trap(java))
//                    fatal("%s can trap => %s can trap, too", name(code), name(java));
//            }
//        }
//        }
//  #endif

        // initialization successful
        _is_initialized = true;
    }
}
