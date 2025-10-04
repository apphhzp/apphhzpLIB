package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.Deoptimization;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A ProfileData object is created to refer to a section of profiling
 * data in a structured way.*/
public abstract class ProfileData {
    // This is a pointer to a section of profiling data.
    private DataLayout _data;
    private static final int
            tab_width_one = 16,
            tab_width_two = 36;

    protected DataLayout data() {
        return _data;
    }

    protected static final int cell_size = DataLayout.cell_size;

    public ProfileData(DataLayout data) {
        _data = data;
    }

    // How many cells are in this?
    public abstract int cell_count();

    // Return the size of this data.
    public int size_in_bytes() {
        return DataLayout.compute_size_in_bytes(cell_count());
    }

    // Low-level accessors for underlying data
    protected void set_intptr_at(int index, @RawCType("intptr_t") long value) {
        if (!(0 <= index && index < cell_count())) {
            throw new IndexOutOfBoundsException("oob");
        }
        data().set_cell_at(index, value);
    }


    protected @RawCType("intptr_t") long intptr_at(int index) {
        if (!(0 <= index && index < cell_count())) {
            throw new IndexOutOfBoundsException("oob");
        }
        return data().cell_at(index);
    }

    protected void set_uint_at(int index, @RawCType("uint") int value) {
        set_intptr_at(index, value & 0xffffffffL);
    }

    protected @RawCType("uint") int uint_at(int index) {
        return (int) (intptr_at(index) & 0xffffffffL);
    }

    protected void set_int_at(int index, int value) {
        set_intptr_at(index, value);
    }

    protected int int_at(int index) {
        return (int) intptr_at(index);
    }

    protected int int_at_unchecked(int index) {
        return (int) data().cell_at(index);
    }

    protected void set_oop_at(int index, OopDesc value) {
        set_intptr_at(index, value.address);
    }

    protected OopDesc oop_at(int index) {
        return OopDesc.of(intptr_at(index));
    }

    protected void set_flag_at(int flag_number) {
        data().set_flag_at(flag_number);
    }

    protected boolean flag_at(int flag_number) {
        return data().flag_at(flag_number);
    }

    // two convenient imports for use by subclasses:
    protected static @RawCType("ByteSize") int cell_offset(int index) {
        return (int) (DataLayout.CELLS_OFFSET + (index) * cell_size);
    }

    protected static int flag_number_to_constant(int flag_number) {
        return DataLayout.flag_number_to_constant(flag_number);
    }

    public @RawCType("u2")int bci() {
        return data().bci();
    }

    public @RawCType("address")long dp() {
        return _data.address;
    }

    public int trap_state() {
        return data().trap_state();
    }

    public void set_trap_state(int new_state) {
        data().set_trap_state(new_state);
    }

    // Type checking
    public boolean is_BitData() {
        return false;
    }

    public boolean is_CounterData() {
        return false;
    }

    public boolean is_JumpData() {
        return false;
    }

    public boolean is_ReceiverTypeData() {
        return false;
    }

    public boolean is_VirtualCallData() {
        return false;
    }

    public boolean is_RetData() {
        return false;
    }

    public boolean is_BranchData() {
        return false;
    }

    public boolean is_ArrayData() {
        return false;
    }

    public boolean is_MultiBranchData() {
        return false;
    }

    public boolean is_ArgInfoData() {
        return false;
    }

    public boolean is_CallTypeData() {
        return false;
    }

    public boolean is_VirtualCallTypeData() {
        return false;
    }

    public boolean is_ParametersTypeData() {
        return false;
    }

    public boolean is_SpeculativeTrapData() {
        return false;
    }

//    public String print_data_on_helper( MethodData md)  {
//        DataLayout* dp  = md->extra_data_base();
//        DataLayout* end = md->args_data_limit();
//        stringStream ss;
//        for (;; dp = MethodData::next_extra(dp)) {
//            assert(dp < end, "moved past end of extra data");
//            switch(dp->tag()) {
//                case DataLayout::speculative_trap_data_tag:
//                    if (dp->bci() == bci()) {
//                        SpeculativeTrapData* data = new SpeculativeTrapData(dp);
//                        int trap = data->trap_state();
//                        char buf[100];
//                        ss.print("trap/");
//                        data->method()->print_short_name(&ss);
//                        ss.print("(%s) ", Deoptimization::format_trap_state(buf, sizeof(buf), trap));
//                    }
//                    break;
//                case DataLayout::bit_data_tag:
//                    break;
//                case DataLayout::no_tag:
//                case DataLayout::arg_info_data_tag:
//                    return ss.as_string();
//                break;
//                default:
//                    fatal("unexpected tag %d", dp->tag());
//            }
//        }
//        return NULL;
//    }
//
//    public void print_data_on(PrintStream st,  MethodData md) {
//        print_data_on(st, print_data_on_helper(md));
//    }
//
//    void ProfileData::print_shared(outputStream* st, const char* name, const char* extra) const {
//        st->print("bci: %d", bci());
//        st->fill_to(tab_width_one);
//        st->print("%s", name);
//        tab(st);
//        int trap = trap_state();
//        if (trap != 0) {
//            char buf[100];
//            st->print("trap(%s) ", Deoptimization::format_trap_state(buf, sizeof(buf), trap));
//        }
//        if (extra != NULL) {
//            st->print("%s", extra);
//        }
//        int flags = data()->flags();
//        if (flags != 0) {
//            st->print("flags(%d) ", flags);
//        }
//    }
//
//    void ProfileData::tab(outputStream* st, bool first) const {
//        st->fill_to(first ? tab_width_one : tab_width_two);
//    }

    public void release_set_intptr_at(int index, @RawCType("intptr_t")long value) {
        if (!(0 <= index && index < cell_count())){
            throw new IndexOutOfBoundsException("oob");
        }
        data().release_set_cell_at(index, value);
    }

    public void release_set_uint_at(int index, @RawCType("uint")int value) {
        release_set_intptr_at(index,  value&0xffffffffL);
    }

    public void release_set_int_at(int index, int value) {
        release_set_intptr_at(index,  value);
    }

    public void print_data_on(PrintStream st){
        this.print_data_on(st,(String) null);
    }
    public abstract void print_data_on(PrintStream st, String extra);

    public void print_data_on(PrintStream st, MethodData md){
        print_data_on(st, print_data_on_helper(md));
    }

    public @RawCType("char*")String print_data_on_helper(MethodData md){
        DataLayout dp  = md.extra_data_base();
        DataLayout end = md.args_data_limit();
        StringBuilder ss=new StringBuilder();
        for (;; dp = MethodData.next_extra(dp)){
            if (!(dp.address < end.address)){
                throw new RuntimeException("moved past end of extra data");
            }
            int tag = dp.tag();
            if (tag == DataLayout.speculative_trap_data_tag) {
                if (dp.bci() == bci()) {
                    SpeculativeTrapData data = new SpeculativeTrapData(dp);
                    int trap = data.trap_state();
                    ss.append("trap/");
                    data.method().print_short_name(ss);
                    ss.append("(").append(Deoptimization.format_trap_state(trap)).append(") ");
                }
            } else if (tag == DataLayout.bit_data_tag) {
            } else if (tag == DataLayout.no_tag || tag == DataLayout.arg_info_data_tag) {
                return ss.toString();
            } else {
                throw new RuntimeException("unexpected tag " + dp.tag());
            }
        }
    }
    public void print_shared(PrintStream st, String name, String extra){
        String tmp_str="bci: "+bci();
        st.print(tmp_str);
        int tmp=tab_width_one-tmp_str.length();
        while (tmp>0){
            st.print(" ");
            tmp--;
        }
        st.print(name);
        tab(st,Math.max(tmp_str.length(),tab_width_one)+name.length());
        int trap = trap_state();
        if (trap != 0) {
            st.printf("trap(%s) ", Deoptimization.format_trap_state(trap));
        }
        if (extra != null){
            st.printf("%s", extra);
        }
        int flags = data().flags();
        if (flags != 0) {
            st.printf("flags(%d) ", flags);
        }
    }

    public void tab(PrintStream st,int cnt){
        tab(st,cnt,false);
    }

    public void tab(PrintStream st,int cnt, boolean first){
        int tmp=(first ? tab_width_one : tab_width_two)-cnt;
        while (tmp>0){
            st.print(" ");
            --tmp;
        }
    }
}
