package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** Entries in a ProfileData object to record types: it can either be
 * none (no profile), unknown (conflicting profile data) or a klass if
 * a single one is seen. Whether a null reference was seen is also
 * recorded. No counter is associated with the type and a single type
 * is tracked (unlike VirtualCallData).*/
public abstract class TypeEntries {
    // A single cell is used to record information for a type:
    // - the cell is initialized to 0
    // - when a type is discovered it is stored in the cell
    // - bit zero of the cell is used to record whether a null reference
    // was encountered or not
    // - bit 1 is set to record a conflict in the type information

    public  static final int null_seen = 1,
                type_mask = ~null_seen,
                type_unknown = 2,
                status_bits = null_seen | type_unknown,
                type_klass_mask = ~status_bits;

    // what to initialize a cell to
    public static @RawCType("intptr_t")long type_none() {
        return 0;
    }

    // null seen = bit 0 set?
    public static boolean was_null_seen(@RawCType("intptr_t")long v) {
        return (v & null_seen) != 0;
    }

    // conflicting type information = bit 1 set?
    public static boolean is_type_unknown(@RawCType("intptr_t")long v) {
        return (v & type_unknown) != 0;
    }

    // not type information yet = all bits cleared, ignoring bit 0?
    public static boolean is_type_none(@RawCType("intptr_t")long v) {
        return (v & type_mask) == 0;
    }

    // recorded type: cell without bit 0 and 1
    public static @RawCType("intptr_t")long klass_part(@RawCType("intptr_t")long v) {
        @RawCType("intptr_t")long r = v & type_klass_mask;
        return r;
    }

    // type recorded
    public static Klass valid_klass(@RawCType("intptr_t")long k) {
        if (!is_type_none(k) &&
                !is_type_unknown(k)) {
            Klass res = Klass.getOrCreate(klass_part(k));
            if (res == null){
                throw new RuntimeException("invalid");
            }
            return res;
        } else {
            return null;
        }
    }

    public static @RawCType("intptr_t")long with_status(@RawCType("intptr_t")long k, @RawCType("intptr_t")long in) {
        return k | (in & status_bits);
    }

    public static @RawCType("intptr_t")long with_status(Klass k, @RawCType("intptr_t")long in) {
        return with_status(k.address, in);
    }
// ProfileData object these entries are part of
    protected ProfileData _pd;
    // offset within the ProfileData object where the entries start
    protected final int _base_off;

    protected TypeEntries(int base_off){
        //_pd(NULL), _base_off(base_off)
        _pd=null;
        _base_off = base_off;
    }

    protected void set_intptr_at(int index, @RawCType("intptr_t")long value) {
        _pd.set_intptr_at(index, value);
    }

    protected @RawCType("intptr_t")long intptr_at(int index) {
        return _pd.intptr_at(index);
    }

    public void set_profile_data(ProfileData pd) {
        _pd = pd;
    }
    public void print_klass(PrintStream st,@RawCType("intptr_t")long k) {
        if (is_type_none(k)) {
            st.print("none");
        } else if (is_type_unknown(k)) {
            st.print("unknown");
        } else {
            valid_klass(k).print_value_on(st);
        }
        if (was_null_seen(k)) {
            st.print(" (null seen)");
        }
    }
}
