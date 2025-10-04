package apphhzp.lib.hotspot.runtime.bytecode;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class Bytecode {
    protected @RawCType("address") long _bcp;
    protected @RawCType("Bytecodes::Code") int _code;

    // Address computation
    protected @RawCType("address") long addr_at(int offset) {
        return _bcp + offset;
    }

    protected @RawCType("u_char") int byte_at(int offset) {
        return unsafe.getByte(addr_at(offset))&0xff;
    }

    protected @RawCType("address") long aligned_addr_at(int offset) {
        return JVM.alignUp(addr_at(offset), 4);
    }

    // Word access:
    protected int     get_Java_u2_at     (int offset){
        return Bytes.get_Java_u2(addr_at(offset));
    }
    protected int     get_Java_u4_at     (int offset){
        return Bytes.get_Java_u4(addr_at(offset));
    }
    protected int     get_aligned_Java_u4_at(int offset){ return Bytes.get_Java_u4(aligned_addr_at(offset)); }
    protected int     get_native_u2_at   (int offset){ return Bytes.get_native_u2(addr_at(offset)); }
    protected int     get_native_u4_at   (int offset){ return Bytes.get_native_u4(addr_at(offset)); }

    public Bytecode(Method method, @RawCType("address")long bcp){
        //: _bcp(bcp), _code(Bytecodes::code_at(method, addr_at(0)))
        _bcp=bcp;
        _code= Bytecodes.code_at(method,addr_at(0));
        if (method==null){
            throw new IllegalArgumentException("this form requires a valid Method*");
        }
    }

    // Attributes
    public @RawCType("address") long bcp()                            { return _bcp; }
    public int instruction_size()                   { return Bytecodes.length_for_code_at(_code, bcp()); }

    public @RawCType("Bytecodes::Code") int code()                   { return _code; }
    public @RawCType("Bytecodes::Code") int java_code()              { return Bytecodes.java_code(code()); }
    public @RawCType("Bytecodes::Code") int invoke_code()            { return (code() == Bytecodes.Code._invokehandle) ? code() : java_code(); }

    // Static functions for parsing bytecodes in place.
    public int get_index_u1(@RawCType("Bytecodes::Code") int bc) {
        assert_same_format_as(bc); assert_index_size(1, bc);
        return unsafe.getByte(addr_at(1))&0xff;
    }
    public int get_index_u2(@RawCType("Bytecodes::Code") int bc) {
        return get_index_u2(bc,false);
    }
    public int get_index_u2(@RawCType("Bytecodes::Code") int bc, boolean is_wide) {
        assert_same_format_as(bc, is_wide); assert_index_size(2, bc, is_wide);
        @RawCType("address")long p = addr_at(is_wide ? 2 : 1);
        if (can_use_native_byte_order(bc, is_wide)) {
            return Bytes.get_native_u2(p);
        } else {
            return Bytes.get_Java_u2(p);
        }
    }
    public int get_index_u1_cpcache(@RawCType("Bytecodes::Code") int bc) {
        assert_same_format_as(bc); assert_index_size(1, bc);
        return unsafe.getByte(addr_at(1))&0xff + ConstantPool.CPCACHE_INDEX_TAG;
    }
    public int get_index_u2_cpcache(@RawCType("Bytecodes::Code") int bc) {
        assert_same_format_as(bc); assert_index_size(2, bc); assert_native_index(bc);
        return Bytes.get_native_u2(addr_at(1)) + ConstantPool.CPCACHE_INDEX_TAG;
    }
    public int get_index_u4(@RawCType("Bytecodes::Code") int bc) {
        assert_same_format_as(bc); assert_index_size(4, bc);
        if (!(can_use_native_byte_order(bc))){
            throw new RuntimeException();
        }
        return Bytes.get_native_u4(addr_at(1));
    }
    public boolean has_index_u4(@RawCType("Bytecodes::Code") int bc) {
        return bc == Bytecodes.Code._invokedynamic;
    }

    public int get_offset_s2(@RawCType("Bytecodes::Code") int bc) {
        assert_same_format_as(bc); assert_offset_size(2, bc);
        return (short) Bytes.get_Java_u2(addr_at(1));
    }
    public int get_offset_s4(@RawCType("Bytecodes::Code") int bc) {
        assert_same_format_as(bc); assert_offset_size(4, bc);
        return Bytes.get_Java_u4(addr_at(1));
    }

    public int get_constant_u1(int offset, @RawCType("Bytecodes::Code") int bc) {
        assert_same_format_as(bc); assert_constant_size(1, offset, bc);
        return unsafe.getByte(addr_at(offset));
    }

    public int get_constant_u2(int offset, @RawCType("Bytecodes::Code") int bc) {
        return get_constant_u2(offset,bc,false);
    }
    public int get_constant_u2(int offset, @RawCType("Bytecodes::Code") int bc, boolean is_wide) {
        assert_same_format_as(bc, is_wide); assert_constant_size(2, offset, bc, is_wide);
        return unsafe.getShort(addr_at(offset));
    }

    // These are used locally and also from bytecode streams.
//    public void assert_same_format_as(Bytecodes::Code testbc, boolean is_wide = false) const NOT_DEBUG_RETURN;
//    public static void assert_index_size(int required_size, Bytecodes::Code bc, boolean is_wide = false) NOT_DEBUG_RETURN;
//    public static void assert_offset_size(int required_size, Bytecodes::Code bc, boolean is_wide = false) NOT_DEBUG_RETURN;
//    public static void assert_constant_size(int required_size, int where, Bytecodes::Code bc, boolean is_wide = false) NOT_DEBUG_RETURN;
//    public static void assert_native_index(Bytecodes::Code bc, boolean is_wide = false) NOT_DEBUG_RETURN;
    public void assert_same_format_as(@RawCType("Bytecodes::Code") int testbc){
        assert_same_format_as(testbc,false);
    }
    public void assert_same_format_as(@RawCType("Bytecodes::Code") int testbc, boolean is_wide){
        if (!JVM.ENABLE_EXTRA_CHECK){
            return;
        }
        @RawCType("Bytecodes::Code")int thisbc = (byte_at(0));
        if (thisbc == Bytecodes.Code._breakpoint){
            return;  // let the assertion fail silently
        }
        if (is_wide){
            if (thisbc != Bytecodes.Code._wide){
                throw new RuntimeException("expected a wide instruction");
            }
            thisbc = (byte_at(1));
            if (thisbc == Bytecodes.Code._breakpoint)  return;
        }
        int thisflags = Bytecodes.flags(testbc, is_wide) & Bytecodes.Flags._all_fmt_bits;
        int testflags = Bytecodes.flags(thisbc, is_wide) & Bytecodes.Flags._all_fmt_bits;
        if (thisflags != testflags){
            throw new RuntimeException(String.format("assert_same_format_as(%d) failed on bc=%d%s; %d != %d",
                    (int)testbc, (int)thisbc, (is_wide?"/wide":""), testflags, thisflags));
        }
    }
    public static void assert_index_size(int size, @RawCType("Bytecodes::Code") int bc){
        assert_index_size(size,bc,false);
    }
    public static void assert_index_size(int size, @RawCType("Bytecodes::Code") int bc, boolean is_wide) {
        if (!JVM.ENABLE_EXTRA_CHECK){
            return;
        }
        int have_fmt = (Bytecodes.flags(bc, is_wide)
                  & (Bytecodes.Flags._fmt_has_u2 | Bytecodes.Flags._fmt_has_u4 |
                Bytecodes.Flags._fmt_not_simple |
                // Not an offset field:
                Bytecodes.Flags._fmt_has_o));
        int need_fmt = -1;
        switch (size) {
            case 1: need_fmt = 0;                      break;
            case 2: need_fmt = Bytecodes.Flags._fmt_has_u2; break;
            case 4: need_fmt = Bytecodes.Flags._fmt_has_u4; break;
        }
        if (is_wide)  need_fmt |= Bytecodes.Flags._fmt_not_simple;
        if (have_fmt != need_fmt) {
            throw new RuntimeException(String.format("assert_index_size %d: bc=%d%s %d != %d", size, bc, (is_wide?"/wide":""), have_fmt, need_fmt));
        }
    }
    public static void assert_offset_size(int size, @RawCType("Bytecodes::Code") int bc){
        assert_offset_size(size,bc,false);
    }
    public static void assert_offset_size(int size, @RawCType("Bytecodes::Code") int bc, boolean is_wide) {
        if (!JVM.ENABLE_EXTRA_CHECK){
            return;
        }
        int have_fmt = Bytecodes.flags(bc, is_wide) & Bytecodes.Flags._all_fmt_bits;
        int need_fmt = switch (size) {
            case 2 -> Bytecodes.Flags._fmt_bo2;
            case 4 -> Bytecodes.Flags._fmt_bo4;
            default -> -1;
        };
        if (is_wide){
            need_fmt |= Bytecodes.Flags._fmt_not_simple;
        }
        if (have_fmt != need_fmt) {
            throw new RuntimeException(String.format("assert_offset_size %d: bc=%d%s %d != %d", size, bc, (is_wide?"/wide":""), have_fmt, need_fmt));
        }
    }

    public static void assert_constant_size(int size, int where, @RawCType("Bytecodes::Code") int bc){
        assert_constant_size(size,where,bc,false);
    }

    public static void assert_constant_size(int size, int where, @RawCType("Bytecodes::Code") int bc, boolean is_wide) {
        if (!JVM.ENABLE_EXTRA_CHECK){
            return;
        }
        int have_fmt = Bytecodes.flags(bc, is_wide) & (Bytecodes.Flags._all_fmt_bits
                // Ignore any 'i' field (for iinc):
                & ~Bytecodes.Flags._fmt_has_i);
        int need_fmt = -1;
        switch (size) {
            case 1: need_fmt = Bytecodes.Flags._fmt_bc;                          break;
            case 2: need_fmt = Bytecodes.Flags._fmt_bc | Bytecodes.Flags._fmt_has_u2; break;
        }
        if (is_wide)  need_fmt |= Bytecodes.Flags._fmt_not_simple;
        int length = is_wide ? Bytecodes.wide_length_for(bc) : Bytecodes.length_for(bc);
        if (have_fmt != need_fmt){
            throw new RuntimeException(String.format("assert_constant_size %d @%d: bc=%d%s %d != %d%n", size, where, bc, (is_wide?"/wide":""), have_fmt, need_fmt));
        }
        if (where + size != length){
            throw new RuntimeException(String.format("assert_constant_size oob %d @%d: bc=%d%s %d != %d%n", size, where, bc, (is_wide?"/wide":""), have_fmt, need_fmt));
        }
    }
    public static void assert_native_index(@RawCType("Bytecodes::Code") int bc) {
        assert_native_index(bc,false);
    }
    public static void assert_native_index(@RawCType("Bytecodes::Code") int bc, boolean is_wide) {
        if ((Bytecodes.flags(bc, is_wide) & Bytecodes.Flags._fmt_has_nbo) == 0){
            throw new RuntimeException("native index");
        }
    }
    public static boolean can_use_native_byte_order(@RawCType("Bytecodes::Code") int bc) {
        return can_use_native_byte_order(bc,false);
    }
    public static boolean can_use_native_byte_order(@RawCType("Bytecodes::Code") int bc, boolean is_wide) {
        return (!JVM.is_Java_byte_ordering_different() || Bytecodes.native_byte_order(bc /*, is_wide*/));
    }
}
