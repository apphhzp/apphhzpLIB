package apphhzp.lib.hotspot.code.reloc;

import apphhzp.lib.hotspot.code.RelocInfo;
import apphhzp.lib.hotspot.code.RelocIterator;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public abstract class Relocation {
    private RelocIterator _binding;
    private RelocInfo.Type _rtype;

    protected Relocation(RelocInfo.Type rtype) {
        if (true){
            throw new UnsupportedOperationException("Incomplete");
        }
        _binding = null;
        _rtype = rtype;
    }

    protected RelocIterator binding() {
        if (_binding == null) {
            throw new IllegalStateException("must be bound");
        }
        return _binding;
    }

    protected void set_binding(RelocIterator b) {
        if (_binding != null) {
            throw new IllegalStateException("must be unbound");
        }
        _binding = b;
        if (_binding == null) {
            throw new IllegalStateException("must now be bound");
        }
    }

    public static boolean is_short(int x) {
        return x == (short) x;
    }

    public static @RawCType("short*") long add_short(@RawCType("short*") long p, int x) {
        unsafe.putShort(p++, (short) x);
        return p;
    }

    public static @RawCType("short*") long add_jint(@RawCType("short*") long p, int x) {
        unsafe.putShort(p++, (short) RelocInfo.data0_from_int(x));
        unsafe.putShort(p++, (short) RelocInfo.data1_from_int(x));
        return p;
    }

    public static @RawCType("short*") long add_var_int(@RawCType("short*") long p, int x) {   // add a variable-width int
        if (is_short(x)) p = add_short(p, x);
        else p = add_jint(p, x);
        return p;
    }

    public static @RawCType("short*") long pack_1_int_to(@RawCType("short*") long p, int x0) {
        // Format is one of:  [] [x] [Xx]
        if (x0 != 0) p = add_var_int(p, x0);
        return p;
    }

    protected int unpack_1_int() {
        if (datalen() > 2) {
            throw new IllegalStateException("too much data");
        }
        return RelocInfo.jint_data_at(0, data(), datalen());
    }

    protected @RawCType("short*") long pack_2_ints_to(@RawCType("short*") long p, int x0, int x1) {
        // Format is one of:  [] [x y?] [Xx Y?y]
        if (x0 == 0 && x1 == 0) {
            // no halfwords needed to store zeroes
        } else if (is_short(x0) && is_short(x1)) {
            // 1-2 halfwords needed to store shorts
            p = add_short(p, x0);
            if (x1 != 0) p = add_short(p, x1);
        } else {
            // 3-4 halfwords needed to store jints
            p = add_jint(p, x0);
            p = add_var_int(p, x1);
        }
        return p;
    }
//    protected @RawCType("short*") long unpack_2_ints(jint& x0, jint& x1) {
//        int    dlen = datalen();
//        short* dp  = data();
//        if (dlen <= 2) {
//            x0 = relocInfo::short_data_at(0, dp, dlen);
//            x1 = relocInfo::short_data_at(1, dp, dlen);
//        } else {
//            assert(dlen <= 4, "too much data");
//            x0 = relocInfo::jint_data_at(0, dp, dlen);
//            x1 = relocInfo::jint_data_at(2, dp, dlen);
//        }
//    }

    protected @RawCType("short*") long data() {
        return binding().data();
    }

    protected int datalen() {
        return binding().datalen();
    }

    protected int format() {
        return binding().format();
    }

    public long addr() {
        return binding().addr();
    }

    public CompiledMethod code() {
        return binding().code();
    }

    public boolean addr_in_const() {
        return binding().addr_in_const();
    }

    public RelocInfo.Type type() { return _rtype; }

    // is it a call instruction?
    public abstract boolean is_call();

    // is it a data movement instruction?
    public abstract boolean is_data();

    // some relocations can compute their own values
    public abstract @RawCType("address") long  value();

    // all relocations are able to reassert their values
    public abstract void set_value(@RawCType("address") long x);

    public abstract boolean clear_inline_cache();
}
