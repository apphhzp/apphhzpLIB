package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/**Type entry used for return from a call. A single cell to record the type.*/
public class ReturnTypeEntry extends TypeEntries {
    private static final int cell_count = 1;

    public ReturnTypeEntry(int base_off) {
        super(base_off);
    }
    public void post_initialize() {
        set_type(type_none());
    }

    public @RawCType("intptr_t")long type(){
        return _pd.intptr_at(_base_off);
    }

    public void set_type(@RawCType("intptr_t")long k) {
        _pd.set_intptr_at(_base_off, k);
    }

    public static int static_cell_count() {
        return cell_count;
    }

    public static @RawCType("ByteSize")int size() {
        return cell_count * DataLayout.cell_size;
    }

    public @RawCType("ByteSize")int type_offset() {
        return (int) (DataLayout.CELLS_OFFSET+ (long) _base_off *DataLayout.cell_size);
    }


    public void print_data_on(PrintStream st) {
        _pd.tab(st,0);
        print_klass(st, type());
        st.println();
    }
}
