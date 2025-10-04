package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A RetData is used to access profiling information for a ret bytecode.
 * It is composed of a count of the number of times that the ret has
 * been executed, followed by a series of triples of the form
 * (bci, count, di) which count the number of times that some bci was the
 * target of the ret and cache a corresponding data displacement.*/
public class RetData extends CounterData {
    protected static final int
        bci0_offset = counter_cell_count,
                count0_offset=bci0_offset+1,
                displacement0_offset=count0_offset+1,
                ret_row_cell_count = (displacement0_offset + 1) - bci0_offset;

    protected void set_bci(@RawCType("uint")int row, int bci) {
        if (!(Integer.compareUnsigned(row,row_limit())<0)){
            throw new IndexOutOfBoundsException("oob");
        }
        set_int_at(bci0_offset + row * ret_row_cell_count, bci);
    }
    protected void set_bci_count(@RawCType("uint")int row, @RawCType("uint")int count) {
        if (!(Integer.compareUnsigned(row,row_limit())<0)){
            throw new IndexOutOfBoundsException("oob");
        }
        set_uint_at(count0_offset + row * ret_row_cell_count, count);
    }
    protected void set_bci_displacement(@RawCType("uint")int row, int disp) {
        set_int_at(displacement0_offset + row * ret_row_cell_count, disp);
    }


    protected void release_set_bci(@RawCType("uint")int row, int bci) {
        if (!(Integer.compareUnsigned(row,row_limit())<0)){
            throw new IndexOutOfBoundsException("oob");
        }
        // 'release' when setting the bci acts as a valid flag for other
        // threads wrt bci_count and bci_displacement.
        release_set_int_at(bci0_offset + row * ret_row_cell_count, bci);
    }

    public RetData(DataLayout layout) {
        super(layout);
        if (!(layout.tag() == DataLayout.ret_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }

    public boolean is_RetData() { return true; }

    public static final   int
        no_bci = -1 // value of bci when bci1/2 are not in use.
    ;

    public static int static_cell_count() {
        return (int) (counter_cell_count +  (JVM.getFlag("BciProfileWidth").getIntx()&0xffffffffL) * ret_row_cell_count);
    }

    public int cell_count() {
        return static_cell_count();
    }

    public static @RawCType("uint")int row_limit() {
        return (int) (JVM.getFlag("BciProfileWidth").getIntx()&0xffffffffL);
    }
    public static int bci_cell_index(@RawCType("uint")int row) {
        return bci0_offset + row * ret_row_cell_count;
    }
    public static int bci_count_cell_index(@RawCType("uint")int row) {
        return count0_offset + row * ret_row_cell_count;
    }
    public static int bci_displacement_cell_index(@RawCType("uint")int row) {
        return displacement0_offset + row * ret_row_cell_count;
    }

    // Direct accessors
    public int bci(@RawCType("uint")int row) {
        return int_at(bci_cell_index(row));
    }
    public @RawCType("uint")int bci_count(@RawCType("uint")int row) {
        return uint_at(bci_count_cell_index(row));
    }
    public int bci_displacement(@RawCType("uint")int row) {
        return int_at(bci_displacement_cell_index(row));
    }

    // Interpreter Runtime support
    public @RawCType("address")long fixup_ret(int return_bci, MethodData h_mdo){
        // First find the mdp which corresponds to the return bci.
        @RawCType("address")long mdp = h_mdo.bci_to_dp(return_bci);

        // Now check to see if any of the cache slots are open.
        for (@RawCType("uint")int row = 0;Integer.compareUnsigned(row,row_limit()) < 0; row++) {
            if (bci(row) == no_bci) {
                set_bci_displacement(row, (int) (mdp - dp()));
                set_bci_count(row, DataLayout.counter_increment);
                // Barrier to ensure displacement is written before the bci; allows
                // the interpreter to read displacement without fear of race condition.
                release_set_bci(row, return_bci);
                break;
            }
        }
        return mdp;
    }

    // Code generation support
    public static @RawCType("ByteSize")int bci_offset(@RawCType("uint")int row) {
        return cell_offset(bci_cell_index(row));
    }
    public static @RawCType("ByteSize")int bci_count_offset(@RawCType("uint")int row) {
        return cell_offset(bci_count_cell_index(row));
    }
    public static @RawCType("ByteSize")int bci_displacement_offset(@RawCType("uint")int row) {
        return cell_offset(bci_displacement_cell_index(row));
    }


    public void print_data_on(PrintStream st, String extra) {
        print_shared(st, "RetData", extra);
        @RawCType("uint")int row;
        int entries = 0;
        for (row = 0; row < row_limit(); row++) {
            if (bci(row) != no_bci)  entries++;
        }
        st.println("count("+Integer.toUnsignedString(count())+") entries("+Integer.toUnsignedString(entries)+")");
        for (row = 0; row < row_limit(); row++) {
            if (bci(row) != no_bci) {
                tab(st,0);
                st.println("bci("+bci(row)+": count("+Integer.toUnsignedString(bci_count(row))+") displacement("+bci_displacement(row)+"))");
            }
        }
    }
}
