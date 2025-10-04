package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.runtime.signature.ReferenceArgumentCount;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;


/** Type entries used for arguments passed at a call and parameters on
 * method entry. 2 cells per entry: one for the type encoded as in
 * TypeEntries and one initialized with the stack slot where the
 * profiled object is to be found so that the interpreter can locate
 * it quickly.*/
public class TypeStackSlotEntries extends TypeEntries {
    private static final int
        stack_slot_entry=0,
                type_entry=1,
                per_arg_cell_count=2;

    // offset of cell for stack slot for entry i within ProfileData object
    private int stack_slot_offset(int i) {
        return _base_off + stack_slot_local_offset(i);
    }

    private final int _number_of_entries;

    // offset of cell for type for entry i within ProfileData object
    private int type_offset_in_cells(int i)  {
        return _base_off + type_local_offset(i);
    }
    public TypeStackSlotEntries(int base_off, int nb_entries) {
        super(base_off);
        _number_of_entries = nb_entries;
    }
    public int number_of_entries() { return _number_of_entries; }

    // offset of cell for stack slot for entry i within this block of cells for a TypeStackSlotEntries
    public static int stack_slot_local_offset(int i) {
        return i * per_arg_cell_count + stack_slot_entry;
    }

    // offset of cell for type for entry i within this block of cells for a TypeStackSlotEntries
    public static int type_local_offset(int i) {
        return i * per_arg_cell_count + type_entry;
    }

    // stack slot for entry i
    public @RawCType("uint")int stack_slot(int i) {
        if (!(i >= 0 && i < _number_of_entries)){
            throw new IndexOutOfBoundsException("oob");
        }
        return _pd.uint_at(stack_slot_offset(i));
    }

    // set stack slot for entry i
    public void set_stack_slot(int i, @RawCType("uint")int num) {
        if (!(i >= 0 && i < _number_of_entries)){
            throw new IndexOutOfBoundsException("oob");
        }
        _pd.set_uint_at(stack_slot_offset(i), num);
    }

    // type for entry i
    public  @RawCType("intptr_t")long type(int i) {
        if (!(i >= 0 && i < _number_of_entries)){
            throw new IndexOutOfBoundsException("oob");
        }
        return _pd.intptr_at(type_offset_in_cells(i));
    }

    // set type for entry i
    public void set_type(int i, @RawCType("intptr_t")long k) {
        if (!(i >= 0 && i < _number_of_entries)){
            throw new IndexOutOfBoundsException("oob");
        }
        _pd.set_intptr_at(type_offset_in_cells(i), k);
    }

    public static @RawCType("ByteSize")int per_arg_size() {
        return (per_arg_cell_count * DataLayout.cell_size);
    }

    public static int per_arg_count() {
        return per_arg_cell_count;
    }

    public @RawCType("ByteSize")int type_offset(int i) {
        return (int) (DataLayout.CELLS_OFFSET+ (type_offset_in_cells(i))*DataLayout.cell_size);
    }
    public static int compute_cell_count(Symbol signature, boolean include_receiver, int max) {
        // Parameter profiling include the receiver
        int args_count = include_receiver ? 1 : 0;
        ReferenceArgumentCount rac=new ReferenceArgumentCount(signature);
        args_count += rac.count();
        args_count = Math.min(args_count, max);
        return args_count * per_arg_cell_count;
    }

    public void print_data_on(PrintStream st){
        for (int i = 0; i < _number_of_entries; i++) {
            _pd.tab(st,0);
            st.print(i+": stack("+Integer.toUnsignedString(stack_slot(i))+") ");
            print_klass(st, type(i));
            st.println();
        }
    }

}
