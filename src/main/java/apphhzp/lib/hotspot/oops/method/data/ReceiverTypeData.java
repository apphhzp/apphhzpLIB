package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A ReceiverTypeData is used to access profiling information about a
 * dynamic type check.  It consists of a counter which counts the total times
 * that the check is reached, and a series of (Klass*, count) pairs
 * which are used to store a type profile for the receiver of the check.*/
public class ReceiverTypeData extends CounterData {
    protected static final int
                // Description of the different counters
                // ReceiverTypeData for instanceof/checkcast/aastore:
                //   count is decremented for failed type checks
                //   JVMCI only: nonprofiled_count is incremented on type overflow
                // VirtualCallData for invokevirtual/invokeinterface:
                //   count is incremented on type overflow
                //   JVMCI only: nonprofiled_count is incremented on method overflow

                // JVMCI is interested in knowing the percentage of type checks involving a type not explicitly in the profile
                nonprofiled_count_off_set,
                receiver0_offset,
                count0_offset,
                receiver_type_row_cell_count;
    static {
        if (JVM.includeJVMCI){
            nonprofiled_count_off_set=counter_cell_count;
            receiver0_offset=nonprofiled_count_off_set+1;
        }else {
            nonprofiled_count_off_set=-1;
            receiver0_offset=counter_cell_count;
        }
        count0_offset=receiver0_offset+1;
        receiver_type_row_cell_count = (count0_offset + 1) - receiver0_offset;
    }

    public ReceiverTypeData(DataLayout layout){
        super(layout);
        if (!(layout.tag() == DataLayout.receiver_type_data_tag ||
                layout.tag() == DataLayout.virtual_call_data_tag ||
                layout.tag() == DataLayout.virtual_call_type_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }
    public boolean is_ReceiverTypeData()  { return true; }

    public static int static_cell_count() {
        return (int) (counter_cell_count +  (JVM.getFlag("TypeProfileWidth").getIntx()&0xffffffffL) * receiver_type_row_cell_count +(JVM.includeJVMCI?1:0));
    }

    public int cell_count()  {
        return static_cell_count();
    }

    // Direct accessors
    public static @RawCType("uint")int row_limit() {
        return (int)(JVM.getFlag("TypeProfileWidth").getIntx()&0xffffffffL);
    }
    public static int receiver_cell_index(@RawCType("uint")int row) {
        return receiver0_offset + row * receiver_type_row_cell_count;
    }
    public static int receiver_count_cell_index(@RawCType("uint")int row) {
        return count0_offset + row * receiver_type_row_cell_count;
    }

    public Klass receiver(@RawCType("uint")int row){
        if (!(Integer.compareUnsigned(row,row_limit()) < 0)){
            throw new IndexOutOfBoundsException("oob");
        }
        Klass recv = Klass.getOrCreate(intptr_at(receiver_cell_index(row)));
        return recv;
    }

    public void set_receiver(@RawCType("uint")int row, Klass k) {
        if (!(Integer.compareUnsigned(row,row_limit()) < 0)){
            throw new IndexOutOfBoundsException("oob");
        }
        set_intptr_at(receiver_cell_index(row),k==null?0L:k.address);
    }

    public @RawCType("uint")int receiver_count(@RawCType("uint")int row){
        if (!(Integer.compareUnsigned(row,row_limit()) < 0)){
            throw new IndexOutOfBoundsException("oob");
        }
        return uint_at(receiver_count_cell_index(row));
    }

    public void set_receiver_count(@RawCType("uint")int row, @RawCType("uint")int count) {
        if (!(Integer.compareUnsigned(row,row_limit()) < 0)){
            throw new IndexOutOfBoundsException("oob");
        }
        set_uint_at(receiver_count_cell_index(row), count);
    }

    public void clear_row(@RawCType("uint")int row) {
        if (!(Integer.compareUnsigned(row,row_limit()) < 0)){
            throw new IndexOutOfBoundsException("oob");
        }
        // Clear total count - indicator of polymorphic call site.
        // The site may look like as monomorphic after that but
        // it allow to have more accurate profiling information because
        // there was execution phase change since klasses were unloaded.
        // If the site is still polymorphic then MDO will be updated
        // to reflect it. But it could be the case that the site becomes
        // only bimorphic. Then keeping total count not 0 will be wrong.
        // Even if we use monomorphic (when it is not) for compilation
        // we will only have trap, deoptimization and recompile again
        // with updated MDO after executing method in Interpreter.
        // An additional receiver will be recorded in the cleaned row
        // during next call execution.
        //
        // Note: our profiling logic works with empty rows in any slot.
        // We do sorting a profiling info (ciCallProfile) for compilation.
        //
        set_count(0);
        set_receiver(row, null);
        set_receiver_count(row, 0);
        if (JVM.includeJVMCI){
            if (!this.is_VirtualCallData()) {
                // if this is a ReceiverTypeData for JVMCI, the nonprofiled_count
                // must also be reset (see "Description of the different counters" above)
                set_nonprofiled_count(0);
            }
        }
    }

    // Code generation support
    public static @RawCType("ByteSize")int receiver_offset(@RawCType("uint")int row) {
        return cell_offset(receiver_cell_index(row));
    }
    public static @RawCType("ByteSize")int receiver_count_offset(@RawCType("uint")int row) {
        return cell_offset(receiver_count_cell_index(row));
    }
    public static @RawCType("ByteSize")int nonprofiled_receiver_count_offset() {
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return cell_offset(nonprofiled_count_off_set);
    }
    public @RawCType("uint")int nonprofiled_count() {
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        return uint_at(nonprofiled_count_off_set);
    }
    public void set_nonprofiled_count(@RawCType("uint")int count) {
        if (!JVM.includeJVMCI){
            throw new UnsupportedOperationException();
        }
        set_uint_at(nonprofiled_count_off_set, count);
    }
    public static @RawCType("ByteSize")int receiver_type_data_size() {
        return cell_offset(static_cell_count());
    }

    public void print_receiver_data_on(PrintStream st){
        @RawCType("uint")int row;
        int entries = 0;
        for (row = 0; row < row_limit(); row++) {
            if (receiver(row) != null)
                entries++;
        }
        if (JVM.includeJVMCI){
            st.println("count("+Integer.toUnsignedString(count())+") nonprofiled_count("+Integer.toUnsignedString(nonprofiled_count())+
                    ") entries("+Integer.toUnsignedString(entries)+")");
        }else {
            st.println("count("+Integer.toUnsignedString(count())+") entries("+Integer.toUnsignedString(entries)+")");
        }
        int total = count();
        for (row = 0; row < row_limit(); row++) {
            if (receiver(row) != null) {
                total += receiver_count(row);
            }
        }
        for (row = 0; row < row_limit(); row++) {
            if (receiver(row) != null) {
                tab(st,0);
                receiver(row).print_value_on(st);
                st.printf("(%s %4.2f)\n",Integer.toUnsignedString(receiver_count(row)), (float)(receiver_count(row)&0xffffffffL) / (float) total);
            }
        }
    }
    public void print_data_on(PrintStream st, String  extra){
        print_shared(st, "ReceiverTypeData", extra);
        print_receiver_data_on(st);
    }
}
