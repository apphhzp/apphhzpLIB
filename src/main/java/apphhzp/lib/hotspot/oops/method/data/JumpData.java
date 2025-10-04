package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A JumpData is used to access profiling information for a direct
 * branch.  It is a counter, used for counting the number of branches,
 * plus a data displacement, used for realigning the data pointer to
 * the corresponding target bci.*/
public class JumpData extends ProfileData{
    protected static final int  taken_off_set=0,
            displacement_off_set=1,
            jump_cell_count=2;

    public JumpData(DataLayout layout) {
        super(layout);
        if (!(layout.tag() == DataLayout.jump_data_tag ||
                layout.tag() == DataLayout.branch_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }
    protected void set_displacement(int displacement) {
        set_int_at(displacement_off_set, displacement);
    }

    public boolean is_JumpData(){ return true; }

    public static int static_cell_count() {
        return jump_cell_count;
    }

    public  int cell_count() {
        return static_cell_count();
    }

    // Direct accessor
    public @RawCType("uint")int taken() {
        return uint_at(taken_off_set);
    }

    public void set_taken(@RawCType("uint")int cnt) {
        set_uint_at(taken_off_set, cnt);
    }

    // Saturating counter
    public @RawCType("uint")int inc_taken() {
        @RawCType("uint")int cnt = taken() + 1;
        // Did we wrap? Will compiler screw us??
        if (cnt == 0) cnt--;
        set_uint_at(taken_off_set, cnt);
        return cnt;
    }

    public int displacement() {
        return int_at(displacement_off_set);
    }

    // Code generation support
    public static @RawCType("ByteSize")int taken_offset() {
        return cell_offset(taken_off_set);
    }

    public static @RawCType("ByteSize")int displacement_offset() {
        return cell_offset(displacement_off_set);
    }
    public void print_data_on(PrintStream st, String extra){
        print_shared(st, "JumpData", extra);
        st.println("taken("+Integer.toUnsignedString(taken())+") displacement("+displacement()+")");
    }
}
