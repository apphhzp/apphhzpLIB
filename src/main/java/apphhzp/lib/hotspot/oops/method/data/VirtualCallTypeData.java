package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A VirtualCallTypeData is used to access profiling information about
 * a virtual call for which we collect type information about
 * arguments and return value.*/
public class VirtualCallTypeData extends VirtualCallData {
    // entries for arguments if any
    private TypeStackSlotEntries _args;
    // entry for return type if any
    private ReturnTypeEntry _ret;

    private int cell_count_global_offset() {
        return VirtualCallData.static_cell_count() + TypeEntriesAtCall.cell_count_local_offset();
    }

    // number of cells not counting the header
    private int cell_count_no_header() {
        return uint_at(cell_count_global_offset());
    }

    private void check_number_of_arguments(int total){
        if (!(number_of_arguments() == total)){
            throw new RuntimeException("should be set in DataLayout::initialize");
        }
    }

    public VirtualCallTypeData(DataLayout layout) {
        super(layout);
        if (!(layout.tag() == DataLayout.virtual_call_type_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
        _args=new TypeStackSlotEntries(VirtualCallData.static_cell_count()+TypeEntriesAtCall.header_cell_count(), number_of_arguments());
        _ret=new ReturnTypeEntry(cell_count() - ReturnTypeEntry.static_cell_count());
        _args.set_profile_data(this);
        _ret.set_profile_data(this);
    }

    public TypeStackSlotEntries args(){
        if (!has_arguments()){
            throw new IllegalStateException("no profiling of arguments");
        }
        return _args;
    }

    public ReturnTypeEntry ret() {
        if (!has_return()){
            throw new IllegalStateException("no profiling of return value");
        }
        return _ret;
    }

    public boolean is_VirtualCallTypeData(){ return true; }

    public static int static_cell_count() {
        return -1;
    }

    public static int compute_cell_count(BytecodeStream stream) {
        return VirtualCallData.static_cell_count() + TypeEntriesAtCall.compute_cell_count(stream);
    }

    public int cell_count() {
        return VirtualCallData.static_cell_count() +
                TypeEntriesAtCall.header_cell_count() +
                int_at_unchecked(cell_count_global_offset());
    }

    public int number_of_arguments() {
        return cell_count_no_header() / TypeStackSlotEntries.per_arg_count();
    }

    public void set_argument_type(int i, Klass k){
        if (!has_arguments()){
            throw new IllegalStateException("no arguments!");
        }
        @RawCType("intptr_t")long current = _args.type(i);
        _args.set_type(i, TypeEntries.with_status(k, current));
    }

    public void set_return_type(Klass k) {
        if (!has_return()){
            throw new IllegalStateException("no return!");
        }
        @RawCType("intptr_t")long current = _ret.type();
        _ret.set_type(TypeEntries.with_status(k, current));
    }

    // An entry for a return value takes less space than an entry for an
    // argument, so if the remainder of the number of cells divided by
    // the number of cells for an argument is not null, a return value
    // is profiled in this object.
    public boolean has_return() {
        boolean res = (cell_count_no_header() % TypeStackSlotEntries.per_arg_count()) != 0;
        if (!(!res || TypeEntriesAtCall.return_profiling_enabled())){
            throw new RuntimeException("no profiling of return values");
        }
        return res;
    }

    // An entry for a return value takes less space than an entry for an
    // argument so if the number of cells exceeds the number of cells
    // needed for an argument, this object contains type information for
    // at least one argument.
    public boolean has_arguments() {
        boolean res = cell_count_no_header() >= TypeStackSlotEntries.per_arg_count();
        if (!(!res || TypeEntriesAtCall.arguments_profiling_enabled())){
            throw new RuntimeException("no profiling of arguments");
        }
        return res;
    }
    public static @RawCType("ByteSize")int args_data_offset() {
        return cell_offset(VirtualCallData.static_cell_count()) + TypeEntriesAtCall.args_data_offset();
    }

    public @RawCType("ByteSize")int argument_type_offset(int i) {
        return _args.type_offset(i);
    }

    public @RawCType("ByteSize")int return_type_offset() {
        return _ret.type_offset();
    }

    public static void initialize(DataLayout dl, int cell_count) {
        TypeEntriesAtCall.initialize(dl, VirtualCallData.static_cell_count(), cell_count);
    }

    public void print_data_on(PrintStream st, String extra){
        super.print_data_on(st, extra);
        if (has_arguments()) {
            tab(st, 0,true);
            st.print("argument types");
            _args.print_data_on(st);
        }
        if (has_return()) {
            tab(st, 0,true);
            st.print("return type");
            _ret.print_data_on(st);
        }
    }
}
