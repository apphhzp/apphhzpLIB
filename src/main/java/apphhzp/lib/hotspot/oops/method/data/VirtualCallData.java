package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A VirtualCallData is used to access profiling information about a
 * virtual call.  For now, it has nothing more than a ReceiverTypeData.*/
public class VirtualCallData extends ReceiverTypeData {
    public VirtualCallData(DataLayout layout) {
        super(layout);
        if (!(layout.tag() == DataLayout.virtual_call_data_tag ||
                layout.tag() == DataLayout.virtual_call_type_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }
    public boolean is_VirtualCallData(){
        return true; }

    public static int static_cell_count() {
        // At this point we could add more profile state, e.g., for arguments.
        // But for now it's the same size as the base record type.
        return ReceiverTypeData.static_cell_count();
    }

    public int cell_count() {
        return static_cell_count();
    }

    // Direct accessors
    public static @RawCType("ByteSize")int virtual_call_data_size() {
        return cell_offset(static_cell_count());
    }

    public void print_data_on(PrintStream st, String extra){
        print_shared(st, "VirtualCallData", extra);
        print_receiver_data_on(st);
    }
}
