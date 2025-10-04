package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A ParametersTypeData is used to access profiling information about
 * types of parameters to a method*/
public class ParametersTypeData extends ArrayData {

    private TypeStackSlotEntries _parameters;

    private static int stack_slot_local_offset(int i) {
        assert_profiling_enabled();
        return array_start_off_set + TypeStackSlotEntries.stack_slot_local_offset(i);
    }

    private static int type_local_offset(int i) {
        assert_profiling_enabled();
        return array_start_off_set + TypeStackSlotEntries.type_local_offset(i);
    }

    private static boolean profiling_enabled(){
        return MethodData.profile_parameters();
    }
    private static void assert_profiling_enabled(){
        if (!profiling_enabled()){
            throw new RuntimeException("method parameters profiling should be on");
        }
    }
    public ParametersTypeData(DataLayout layout){
        super(layout);
        if (!(layout.tag() == DataLayout.parameters_type_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
        _parameters=new TypeStackSlotEntries(1,number_of_parameters());
        // Some compilers (VC++) don't want this passed in member initialization list
        _parameters.set_profile_data(this);
    }
    public int compute_cell_count(Method m) {
        if (!MethodData.profile_parameters_for_method(m)) {
            return 0;
        }
        int max = JVM.getFlag("TypeProfileParmsLimit").getIntx() == -1 ? 2147483647 : (int) JVM.getFlag("TypeProfileParmsLimit").getIntx();
        int obj_args = TypeStackSlotEntries.compute_cell_count(m.signature(), !m.is_static(), max);
        if (obj_args > 0) {
            return obj_args + 1; // 1 cell for array len
        }
        return 0;
    }
    public boolean is_ParametersTypeData() { return true; }

    public int number_of_parameters() {
        return array_len() / TypeStackSlotEntries.per_arg_count();
    }

    public TypeStackSlotEntries parameters() { return _parameters; }

    public @RawCType("uint")int stack_slot(int i) {
        return _parameters.stack_slot(i);
    }

    public void set_type(int i, Klass k) {
        @RawCType("intptr_t")long current = _parameters.type(i);
        _parameters.set_type(i, TypeEntries.with_status(k.address, current));
    }

    public static @RawCType("ByteSize")int stack_slot_offset(int i) {
        return cell_offset(stack_slot_local_offset(i));
    }

    public static @RawCType("ByteSize")int type_offset(int i) {
        return cell_offset(type_local_offset(i));
    }
    public void print_data_on(PrintStream st, String extra){
        st.print("parameter types"); // FIXME extra ignored?
        _parameters.print_data_on(st);
    }
}
