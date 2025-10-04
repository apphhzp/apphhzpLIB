package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A CounterData corresponds to a simple counter.*/
public class CounterData extends BitData{
    protected  static final int
        count_off=0,
                counter_cell_count=count_off+1;
    public CounterData(DataLayout layout) {
        super(layout);
    }

    public boolean is_CounterData(){ return true; }

    public static int static_cell_count() {
        return counter_cell_count;
    }

    public int cell_count() {
        return static_cell_count();
    }

    // Direct accessor
    public int count() {
        @RawCType("intptr_t")long raw_data = intptr_at(count_off);
        if (raw_data > 2147483647L) {
            raw_data = 2147483647L;
        } else if (raw_data < -2147483648) {
            raw_data = -2147483648;
        }
        return (int)(raw_data);
    }

    // Code generation support
    public static @RawCType("ByteSize")int count_offset() {
        return cell_offset(count_off);
    }
    public static @RawCType("ByteSize")int counter_data_size() {
        return cell_offset(counter_cell_count);
    }

    public void set_count(int count) {
        set_int_at(count_off, count);
    }

    @Override
    public void print_data_on(PrintStream st, String extra) {
        print_shared(st, "CounterData", extra);
        st.println("count("+Integer.toUnsignedString(count())+")");
    }
}
