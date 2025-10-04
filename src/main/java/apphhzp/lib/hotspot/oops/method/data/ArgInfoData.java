package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

public class ArgInfoData extends ArrayData{
    public ArgInfoData(DataLayout data) {
        super(data);
        if (!(data.tag() == DataLayout.arg_info_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }
    public boolean is_ArgInfoData(){ return true; }

    public int number_of_args() {
        return array_len();
    }

    public @RawCType("uint")int arg_modified(int arg) {
        return array_uint_at(arg);
    }

    public void set_arg_modified(int arg, @RawCType("uint")int val) {
        array_set_int_at(arg, val);
    }
    public void print_data_on(PrintStream st, String extra){
        print_shared(st, "ArgInfoData", extra);
        int nargs = number_of_args();
        for (int i = 0; i < nargs; i++) {
            st.printf("  0x%x", arg_modified(i));
        }
        st.println();
    }

}
