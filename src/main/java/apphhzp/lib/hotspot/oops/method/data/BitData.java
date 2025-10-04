package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/**A BitData holds a flag or two in its header.*/
public class BitData extends ProfileData {
    protected static final int null_seen_flag = JVM.includeJVMCI ? JVM.intConstant("BitData::null_seen_flag") : DataLayout.first_flag;
    protected static final int  // bytecode threw any exception
            exception_seen_flag = JVM.includeJVMCI ? JVM.intConstant("BitData::exception_seen_flag") : -1;
    protected static final int bit_cell_count = 0;  // no additional data fields needed.

    public BitData(DataLayout layout) {
        super(layout);
    }

    public boolean is_BitData() {
        return true;
    }

    public static int static_cell_count() {
        return bit_cell_count;
    }

    public int cell_count() {
        return static_cell_count();
    }

    // Accessor

    // The null_seen flag bit is specially known to the interpreter.
    // Consulting it allows the compiler to avoid setting up null_check traps.
    public boolean null_seen() {
        return flag_at(null_seen_flag);
    }

    public void set_null_seen() {
        set_flag_at(null_seen_flag);
    }

    // true if an exception was thrown at the specific BCI
    public boolean exception_seen() {
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return flag_at(exception_seen_flag);
    }

    public void set_exception_seen() {
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        set_flag_at(exception_seen_flag);
    }

    // Code generation support
    public static int null_seen_byte_constant() {
        return flag_number_to_constant(null_seen_flag);
    }

    public static @RawCType("ByteSize") int bit_data_size() {
        return cell_offset(bit_cell_count);
    }

    @Override
    public void print_data_on(PrintStream st, String extra) {
        print_shared(st, "BitData", extra);
        st.println();
    }
}
