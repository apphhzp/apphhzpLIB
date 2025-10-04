package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A ArrayData is a base class for accessing profiling data which does
 * not have a statically known size.  It consists of an array length
 * and an array start.*/
public class ArrayData extends ProfileData {
    protected static final int
        array_len_off_set=0,
                array_start_off_set=1;

    protected @RawCType("uint")int array_uint_at(int index) {
        int aindex = index + array_start_off_set;
        return uint_at(aindex);
    }
    protected int array_int_at(int index) {
        int aindex = index + array_start_off_set;
        return int_at(aindex);
    }
    protected OopDesc array_oop_at(int index) {
        int aindex = index + array_start_off_set;
        return oop_at(aindex);
    }
    protected void array_set_int_at(int index, int value) {
        int aindex = index + array_start_off_set;
        set_int_at(aindex, value);
    }

    // Code generation support for subclasses.
    protected static @RawCType("ByteSize")int array_element_offset(int index) {
        return cell_offset(array_start_off_set + index);
    }


    public ArrayData(DataLayout data) {
        super(data);
    }

    public boolean is_ArrayData() { return true; }

    @Override
    public void print_data_on(PrintStream st, String extra) {
        throw new AbstractMethodError();
    }

    public static int static_cell_count() {
        return -1;
    }

    public int array_len() {
        return int_at_unchecked(array_len_off_set);
    }

    public int cell_count() {
        return array_len() + 1;
    }

    // Code generation support
    public static @RawCType("ByteSize")int array_len_offset() {
        return cell_offset(array_len_off_set);
    }
    public static @RawCType("ByteSize")int array_start_offset() {
        return cell_offset(array_start_off_set);
    }
}
