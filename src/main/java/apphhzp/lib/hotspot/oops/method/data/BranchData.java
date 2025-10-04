package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A BranchData is used to access profiling data for a two-way branch.
 * It consists of taken and not_taken counts as well as a data displacement
 * for the taken case.*/
public class BranchData extends JumpData {
    public BranchData(DataLayout layout) {
        super(layout);
        if (!(layout.tag() == DataLayout.branch_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }
    protected static final int
        not_taken_off_set = jump_cell_count,
                branch_cell_count=not_taken_off_set+1;

    @SuppressWarnings("RedundantMethodOverride")
    protected void set_displacement(int displacement) {
        set_int_at(displacement_off_set, displacement);
    }
    public boolean is_BranchData(){ return true; }

    public static int static_cell_count() {
        return branch_cell_count;
    }

    public int cell_count() {
        return static_cell_count();
    }

    // Direct accessor
    public @RawCType("uint")int not_taken() {
        return uint_at(not_taken_off_set);
    }

    public void set_not_taken(@RawCType("uint")int cnt) {
        set_uint_at(not_taken_off_set, cnt);
    }

    public @RawCType("uint")int inc_not_taken() {
        @RawCType("uint")int cnt = not_taken() + 1;
        // Did we wrap? Will compiler screw us??
        if (cnt == 0) cnt--;
        set_uint_at(not_taken_off_set, cnt);
        return cnt;
    }

    // Code generation support
    public static @RawCType("ByteSize")int not_taken_offset() {
        return cell_offset(not_taken_off_set);
    }
    public static @RawCType("ByteSize")int branch_data_size() {
        return cell_offset(branch_cell_count);
    }
    public void print_data_on(PrintStream st,String extra){
        print_shared(st, "BranchData", extra);
        st.println("taken("+Integer.toUnsignedString(taken())+") displacement("+displacement()+")");
        tab(st,0);
        st.println("not taken("+Integer.toUnsignedString(not_taken())+")");
    }
}
