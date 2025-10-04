package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.interpreter.Bytecode_lookupswitch;
import apphhzp.lib.hotspot.interpreter.Bytecode_tableswitch;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A MultiBranchData is used to access profiling information for
 * a multi-way branch (*switch bytecodes).  It consists of a series
 * of (count, displacement) pairs, which count the number of times each
 * case was taken and specify the data displacment for each branch target.*/
public class MultiBranchData extends ArrayData{

    protected static final int
        default_count_off_set=0,
                default_disaplacement_off_set=1,
                case_array_start=2
    ;
    protected static final int
        relative_count_off_set=0,
                relative_displacement_off_set=1,
                per_case_cell_count=2
    ;

    public MultiBranchData(DataLayout data) {
        super(data);
        if (!(data.tag() == DataLayout.multi_branch_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }

    protected void set_default_displacement(int displacement) {
        array_set_int_at(default_disaplacement_off_set, displacement);
    }
    protected void set_displacement_at(int index, int displacement) {
        array_set_int_at(case_array_start +
                        index * per_case_cell_count +
                        relative_displacement_off_set,
                displacement);
    }

    public boolean is_MultiBranchData() { return true; }

    public static int compute_cell_count(BytecodeStream stream){
        int cell_count = 0;
        if (stream.code() == Bytecodes.Code._tableswitch) {
            Bytecode_tableswitch sw=new Bytecode_tableswitch(stream.method(), stream.bcp());
            cell_count = 1 + per_case_cell_count * (1 + sw.length()); // 1 for default
        } else {
            Bytecode_lookupswitch sw=new Bytecode_lookupswitch(stream.method(), stream.bcp());
            cell_count = 1 + per_case_cell_count * (sw.number_of_pairs() + 1); // 1 for default
        }
        return cell_count;
    }

    public int number_of_cases() {
        int alen = array_len() - 2; // get rid of default case here.
        if (!(alen % per_case_cell_count == 0)){
            throw new RuntimeException("must be even");
        }
        return (alen / per_case_cell_count);
    }

    public @RawCType("uint")int default_count() {
        return array_uint_at(default_count_off_set);
    }
    public int default_displacement() {
        return array_int_at(default_disaplacement_off_set);
    }

    public @RawCType("uint")int count_at(int index) {
        return array_uint_at(case_array_start +
                index * per_case_cell_count +
                relative_count_off_set);
    }
    public  int displacement_at(int index) {
        return array_int_at(case_array_start +
                index * per_case_cell_count +
                relative_displacement_off_set);
    }

    // Code generation support
    public static @RawCType("ByteSize")int default_count_offset() {
        return array_element_offset(default_count_off_set);
    }
    public static @RawCType("ByteSize")int default_displacement_offset() {
        return array_element_offset(default_disaplacement_off_set);
    }
    public static @RawCType("ByteSize")int case_count_offset(int index) {
        return case_array_offset() +
                (per_case_size() * index) +
                relative_count_offset();
    }
    public static @RawCType("ByteSize")int case_array_offset() {
        return array_element_offset(case_array_start);
    }
    public static @RawCType("ByteSize")int per_case_size() {
        return (per_case_cell_count) * cell_size;
    }
    public static @RawCType("ByteSize")int relative_count_offset() {
        //noinspection ConstantValue
        return (relative_count_off_set) * cell_size;
    }
    public static @RawCType("ByteSize")int relative_displacement_offset() {
        return (relative_displacement_off_set) * cell_size;
    }
    public void print_data_on(PrintStream st, String extra) {
        print_shared(st, "MultiBranchData", extra);
        st.println("default_count("+Integer.toUnsignedString(default_count())+") displacement("+default_displacement()+")");
        int cases = number_of_cases();
        for (int i = 0; i < cases; i++) {
            tab(st,0);
            st.println("count("+Integer.toUnsignedString(count_at(i))+") displacement("+displacement_at(i)+")");
        }
    }

}
