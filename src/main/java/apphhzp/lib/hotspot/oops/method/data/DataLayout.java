package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.internalUnsafe;
import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class DataLayout extends JVMObject {
    public static final Type TYPE= JVM.type("DataLayout");
    public static final int SIZE=TYPE.size;
    public static final long TAG_OFFSET=TYPE.offset("_header._struct._tag");
    public static final long FLAGS_OFFSET=TYPE.offset("_header._struct._flags");
    public static final long BCI_OFFSET=TYPE.offset("_header._struct._bci");
    public static final long TRAPS_OFFSET=TYPE.offset("_header._struct._traps");
    public static final long CELLS_OFFSET=TYPE.offset("_cells[0]");
    public static final int cell_size=JVM.intConstant("DataLayout::cell_size");
    public static final int no_tag=JVM.intConstant("DataLayout::no_tag"),
            bit_data_tag=JVM.intConstant("DataLayout::bit_data_tag"),
            counter_data_tag=JVM.intConstant("DataLayout::counter_data_tag"),
            jump_data_tag=JVM.intConstant("DataLayout::jump_data_tag"),
            receiver_type_data_tag=JVM.intConstant("DataLayout::receiver_type_data_tag"),
            virtual_call_data_tag=JVM.intConstant("DataLayout::virtual_call_data_tag"),
            ret_data_tag=JVM.intConstant("DataLayout::ret_data_tag"),
            branch_data_tag=JVM.intConstant("DataLayout::branch_data_tag"),
            multi_branch_data_tag=JVM.intConstant("DataLayout::multi_branch_data_tag"),
            arg_info_data_tag=JVM.intConstant("DataLayout::arg_info_data_tag"),
            call_type_data_tag=JVM.intConstant("DataLayout::call_type_data_tag"),
            virtual_call_type_data_tag=JVM.intConstant("DataLayout::virtual_call_type_data_tag"),
            parameters_type_data_tag=JVM.intConstant("DataLayout::parameters_type_data_tag"),
            speculative_trap_data_tag=JVM.intConstant("DataLayout::speculative_trap_data_tag");
    public static final int
        // The trap state breaks down as [recompile:1 | reason:31].
        // This further breakdown is defined in deoptimization.cpp.
        // See Deoptimization::trap_state_reason for an assert that
        // trap_bits is big enough to hold reasons < Reason_RECORDED_LIMIT.
        //
        // The trap_state is collected only if ProfileTraps is true.
        trap_bits = 1+31,  // 31: enough to distinguish [0..Reason_RECORDED_LIMIT].
                trap_mask = -1,
                first_flag = 0;
    public static final int counter_increment=1;

    public DataLayout(long addr) {
        super(addr);
    }

    // Size computation
    public static int header_size_in_bytes() {
        return header_size_in_cells() * cell_size;
    }
    public static int header_size_in_cells() {
        return JVM.isLP64?1:2;
    }
    public static int compute_size_in_bytes(int cell_count) {
        return header_size_in_bytes() + cell_count * cell_size;
    }

    // Perform generic initialization of the data.  More specific
    // initialization occurs in overrides of ProfileData::post_initialize.
    public void initialize(@RawCType("u1")int tag, @RawCType("u2")int bci, int cell_count) {
        set_header(0);
        unsafe.putByte(this.address+TAG_OFFSET, (byte)(tag&0xff));
        unsafe.putShort(this.address+BCI_OFFSET,(short) (bci&0xffff));
        for (int i = 0; i < cell_count; i++) {
            set_cell_at(i, 0);
        }
        if (needs_array_len(tag)) {
            set_cell_at(ArrayData.array_len_off_set, cell_count - 1); // -1 for header.
        }
        if (tag == call_type_data_tag) {
            CallTypeData.initialize(this, cell_count);
        } else if (tag == virtual_call_type_data_tag) {
            VirtualCallTypeData.initialize(this, cell_count);
        }
    }


    // Accessors
    public @RawCType("u1") int tag() {
        return unsafe.getByte(this.address+TAG_OFFSET)&0xff;
    }

    // Return 32 bits of trap state.
    // The state tells if traps with zero, one, or many reasons have occurred.
    // It also tells whether zero or many recompilations have occurred.
    // The associated trap histogram in the MDO itself tells whether
    // traps are common or not.  If a BCI shows that a trap X has
    // occurred, and the MDO shows N occurrences of X, we make the
    // simplifying assumption that all N occurrences can be blamed
    // on that BCI.
    public @RawCType("uint")int trap_state() {
        return unsafe.getInt(this.address+TRAPS_OFFSET);
    }

    public void set_trap_state(@RawCType("uint")int new_state) {
        //assert(ProfileTraps, "used only under +ProfileTraps");
        @RawCType("uint")int old_flags = this.trap_state();
        unsafe.putInt(this.address+TRAPS_OFFSET,new_state | old_flags);
    }

    public @RawCType("u1") int flags() {
        return unsafe.getByte(this.address+FLAGS_OFFSET)&0xff;
    }

    public @RawCType("u2") int bci() {
        return unsafe.getShort(this.address+BCI_OFFSET)&0xffff;
    }

    public void set_header(@RawCType("u8")long value){
        unsafe.putLong(this.address+TAG_OFFSET,value);
    }
    public @RawCType("u8")long header(){
        return unsafe.getLong(this.address+TAG_OFFSET);
    }
    public void set_cell_at(int index, @RawCType("intptr_t")long value){
        unsafe.putAddress(this.address+CELLS_OFFSET+ (long) index *JVM.oopSize,value);
    }

    public @RawCType("intptr_t")long cell_at(int index){
        return unsafe.getAddress(this.address+CELLS_OFFSET+ (long) index *JVM.oopSize);// _cells[index];
    }

    public void set_flag_at(@RawCType("u1") int flag_number) {
        //_header._struct._flags |= (0x1 << flag_number);
        unsafe.putByte(this.address+FLAGS_OFFSET, (byte) (((unsafe.getByte(this.address+FLAGS_OFFSET)&0xff)|(0x1 << flag_number))&0xff));
    }
    public boolean flag_at(@RawCType("u1") int flag_number){
        return (unsafe.getByte(this.address+FLAGS_OFFSET) & (0x1 << flag_number)) != 0;
    }
    // Return a value which, when or-ed as a byte into _flags, sets the flag.
    public static @RawCType("u1") int flag_number_to_constant(@RawCType("u1") int flag_number) {
        return (0x1 << flag_number)&0xff;
    }
    // Return a value which, when or-ed as a word into _header, sets the flag.
    public static @RawCType("u8")long flag_mask_to_header_mask(@RawCType("uint")int byte_constant) {
        long addr=unsafe.allocateMemory(SIZE);
        DataLayout temp=new DataLayout(addr);
        temp.set_header(0);
        unsafe.putByte(addr+FLAGS_OFFSET, (byte) byte_constant);
        long re=temp.header();
        unsafe.freeMemory(addr);
        return re;
    }

    public int size_in_bytes(){
        int cells = cell_count();
        if (!(cells >= 0)){
            throw new RuntimeException("invalid number of cells");
        }
        return DataLayout.compute_size_in_bytes(cells);
    }
    public int cell_count() {
        int tag = tag();
        if (tag == DataLayout.bit_data_tag) {
            return BitData.static_cell_count();
        } else if (tag == DataLayout.counter_data_tag) {
            return CounterData.static_cell_count();
        } else if (tag == DataLayout.jump_data_tag) {
            return JumpData.static_cell_count();
        } else if (tag == DataLayout.receiver_type_data_tag) {
            return ReceiverTypeData.static_cell_count();
        } else if (tag == DataLayout.virtual_call_data_tag) {
            return VirtualCallData.static_cell_count();
        } else if (tag == DataLayout.ret_data_tag) {
            return RetData.static_cell_count();
        } else if (tag == DataLayout.branch_data_tag) {
            return BranchData.static_cell_count();
        } else if (tag == DataLayout.multi_branch_data_tag) {
            return ((new MultiBranchData(this)).cell_count());
        } else if (tag == DataLayout.arg_info_data_tag) {
            return ((new ArgInfoData(this)).cell_count());
        } else if (tag == DataLayout.call_type_data_tag) {
            return ((new CallTypeData(this)).cell_count());
        } else if (tag == DataLayout.virtual_call_type_data_tag) {
            return ((new VirtualCallTypeData(this)).cell_count());
        } else if (tag == DataLayout.parameters_type_data_tag) {
            return ((new ParametersTypeData(this)).cell_count());
        } else if (tag == DataLayout.speculative_trap_data_tag) {
            return SpeculativeTrapData.static_cell_count();
        }
        throw new RuntimeException("ShouldNotReachHere()");
    }
    // Some types of data layouts need a length field.
    private static boolean needs_array_len(@RawCType("u1")int tag) {
        return (tag == multi_branch_data_tag) || (tag == arg_info_data_tag) || (tag == parameters_type_data_tag);
    }

    public ProfileData data_in() {
        int tag = tag();
        if (tag == bit_data_tag) {
            return new BitData(this);
        } else if (tag == counter_data_tag) {
            return new CounterData(this);
        } else if (tag == jump_data_tag) {
            return new JumpData(this);
        } else if (tag == receiver_type_data_tag) {
            return new ReceiverTypeData(this);
        } else if (tag == virtual_call_data_tag) {
            return new VirtualCallData(this);
        } else if (tag == ret_data_tag) {
            return new RetData(this);
        } else if (tag == branch_data_tag) {
            return new BranchData(this);
        } else if (tag == multi_branch_data_tag) {
            return new MultiBranchData(this);
        } else if (tag == arg_info_data_tag) {
            return new ArgInfoData(this);
        } else if (tag == call_type_data_tag) {
            return new CallTypeData(this);
        } else if (tag == virtual_call_type_data_tag) {
            return new VirtualCallTypeData(this);
        } else if (tag == parameters_type_data_tag) {
            return new ParametersTypeData(this);
        } else if (tag == speculative_trap_data_tag) {
            return new SpeculativeTrapData(this);
        }
        throw new RuntimeException("ShouldNotReachHere()");
    }
    public void release_set_cell_at(int index, @RawCType("intptr_t")long value) {
        internalUnsafe.putAddressRelease(this.address+CELLS_OFFSET+ (long) index *JVM.oopSize,value);
    }
}
