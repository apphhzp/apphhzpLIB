package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.interpreter.Bytecode_invoke;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.Metadata;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class MethodData extends Metadata {
    public static final Type TYPE= JVM.type("MethodData");
    public static final int SIZE=TYPE.size;
    public static final long METHOD_OFFSET=TYPE.offset("_method");
    public static final long SIZE_OFFSET=TYPE.offset("_size");
    public static final long HINT_DI_OFFSET=JVM.computeOffset(JVM.intSize,SIZE_OFFSET+JVM.intSize);
    public static final long NOF_DECOMPILES_OFFSET=TYPE.offset("_compiler_counters._nof_decompiles");
    public static final long NOF_OVERFLOW_RECOMPILES_OFFSET=TYPE.offset("_compiler_counters._nof_overflow_recompiles");
    public static final long NOF_OVERFLOW_TRAPS_OFFSET=TYPE.offset("_compiler_counters._nof_overflow_traps");
    public static final long TRAP_HIST_ARRAY_OFFSET=TYPE.offset("_compiler_counters._trap_hist._array[0]");
    public static final long EFLAGS_OFFSET=TYPE.offset("_eflags");
    public static final long ARG_LOCAL_OFFSET=TYPE.offset("_arg_local");
    public static final long ARG_STACK_OFFSET=TYPE.offset("_arg_stack");
    public static final long ARG_RETURNED_OFFSET=TYPE.offset("_arg_returned");
    public static final long TENURE_TRAPS_OFFSET=TYPE.offset("_tenure_traps");
    public static final long INVOKE_MASK_OFFSET=TYPE.offset("_invoke_mask");
    public static final long BACKEDGE_MASK_OFFSET=TYPE.offset("_backedge_mask");
    public static final long JVMCI_IR_SIZE_OFFSET=JVM.includeJVMCI?TYPE.offset("_jvmci_ir_size"):-1;
    public static final long DATA_SIZE_OFFSET=TYPE.offset("_data_size");
    public static final long PARAMETERS_TYPE_DATA_DI_OFFSET=TYPE.offset("_parameters_type_data_di");
    public static final long DATA_OFFSET=TYPE.offset("_data[0]");
    // Whole-method sticky bits and flags
    public static final int
        _trap_hist_limit    = JVM.includeJVMCI?JVM.intConstant("Deoptimization::Reason_TRAP_HISTORY_LENGTH"):JVM.intConstant("Deoptimization::Reason_tenured"),
                _trap_hist_mask     = 0xff,
                _extra_data_count   = 4     // extra DataLayout headers, for trap history
    ; // Public flag values

    public static final int
        no_type_profile = 0,
                type_profile_jsr292 = 1,
                type_profile_all = 2;

    private Method methodCache;
    public final CompilerCounters compiler_counters;

    public MethodData(long addr) {
        super(addr);
        compiler_counters=new CompilerCounters(this);
    }

    public Method getMethod(){
        long addr=unsafe.getAddress(this.address+METHOD_OFFSET);
        if (!isEqual(this.methodCache,addr)){
            this.methodCache=Method.getOrCreate(addr);
        }
        return this.methodCache;
    }

    public @RawCType("uint")int inc_trap_count(int reason) {
        return compiler_counters.inc_trap_count(reason);
    }

    public @RawCType("uint")int overflow_trap_count() {
        return compiler_counters.overflow_trap_count();
    }
    public @RawCType("uint")int overflow_recompile_count() {
        return compiler_counters.overflow_recompile_count();
    }
    public @RawCType("uint")int inc_overflow_recompile_count() {
        return compiler_counters.inc_overflow_recompile_count();
    }
    public @RawCType("uint")int decompile_count() {
        return compiler_counters.decompile_count();
    }


    // Compiler-related counters.
    public static class CompilerCounters {
        private final MethodData owner;
        public static final int ARRAY_SIZE=JVM.includeJVMCI?2*_trap_hist_limit:_trap_hist_limit;
        public CompilerCounters(MethodData owner) {
            this.owner=owner;
        }

        // Return (uint)-1 for overflow.
        public @RawCType("uint")int trap_count(int reason){
            if (!(Integer.compareUnsigned(reason, ARRAY_SIZE)<0)){
                throw new IllegalArgumentException("oob");
            }
            return (int)(((unsafe.getByte(owner.address+TRAP_HIST_ARRAY_OFFSET+reason)&0xff)+1) & _trap_hist_mask) - 1;
        }

        public @RawCType("uint")int inc_trap_count(int reason){
            // Count another trap, anywhere in this method.
            if (!(reason >= 0)){
                throw new IllegalArgumentException("must be single trap");
            }
            if (!(Integer.compareUnsigned(reason, ARRAY_SIZE)<0)){
                throw new IllegalArgumentException("oob");
            }
            @RawCType("uint")int cnt1 = 1 + (unsafe.getByte(owner.address+TRAP_HIST_ARRAY_OFFSET+reason)&0xff);
            if ((cnt1 & _trap_hist_mask) != 0) {  // if no counter overflow...
                unsafe.putByte(owner.address+TRAP_HIST_ARRAY_OFFSET+reason, (byte)(cnt1&0xff));
                return cnt1;
            } else {
                @RawCType("uint")int val=unsafe.getInt(owner.address+NOF_OVERFLOW_TRAPS_OFFSET)+1;
                unsafe.putInt(owner.address+NOF_OVERFLOW_TRAPS_OFFSET,val);
                return _trap_hist_mask + val;
            }
        }

        public @RawCType("uint")int overflow_trap_count() {
            return unsafe.getInt(owner.address+NOF_OVERFLOW_TRAPS_OFFSET);
        }
        public @RawCType("uint")int overflow_recompile_count() {
            return unsafe.getInt(owner.address+NOF_OVERFLOW_RECOMPILES_OFFSET);//_nof_overflow_recompiles;
        }
        public @RawCType("uint")int inc_overflow_recompile_count() {
            int val=unsafe.getInt(owner.address+NOF_OVERFLOW_RECOMPILES_OFFSET)+1;
            unsafe.putInt(owner.address+NOF_OVERFLOW_RECOMPILES_OFFSET, val);
            return val;
        }
        public @RawCType("uint")int decompile_count() {
            return unsafe.getInt(owner.address+NOF_DECOMPILES_OFFSET);
        }
        public @RawCType("uint")int inc_decompile_count() {
            int val=unsafe.getInt(owner.address+NOF_DECOMPILES_OFFSET)+1;
            unsafe.putInt(owner.address+NOF_DECOMPILES_OFFSET, val);
            return val;
        }
    }

    // Helper for data_at
    public DataLayout limit_data_position() {
        return data_layout_at(unsafe.getInt(this.address+DATA_SIZE_OFFSET));
    }
    public boolean out_of_bounds(int data_index) {
        return data_index >= data_size();
    }

    // hint accessors
    public int      hint_di() {
        return unsafe.getInt(this.address+HINT_DI_OFFSET);
    }
    public void set_hint_di(int di)  {
        if (out_of_bounds(di)){
            throw new IndexOutOfBoundsException("hint_di out of bounds");
        }
        unsafe.putInt(this.address+HINT_DI_OFFSET,di);
    }
    public boolean is_valid(ProfileData current){
        return current != null; }
    public boolean is_valid(DataLayout  current){
        return current != null; }
    // What is the index of the first data entry?
    public int first_di(){
        return 0;
    }
    // Add a handful of extra data records, for trap tracking.
    public DataLayout extra_data_base()   { return limit_data_position(); }
    public DataLayout extra_data_limit()  { return new DataLayout(this.address + size_in_bytes()); }
    public DataLayout args_data_limit()   { return new DataLayout(this.address + size_in_bytes() -
            parameters_size_in_bytes()); }
    public int extra_data_size()           { return (int) (extra_data_limit().address - extra_data_base().address); }

    private static final int // data index for the area dedicated to parameters. -1 if no
    // parameter profiling.
      no_parameters = -2, parameters_uninitialized = -1 ;

    // Return pointer to area dedicated to parameters in MDO
    public ParametersTypeData parameters_type_data() {
        if (unsafe.getInt(this.address + PARAMETERS_TYPE_DATA_DI_OFFSET) == parameters_uninitialized){
            throw new RuntimeException("called too early");
        }
        return unsafe.getInt(this.address+PARAMETERS_TYPE_DATA_DI_OFFSET) != no_parameters ?
                (ParametersTypeData) data_layout_at(unsafe.getInt(this.address+PARAMETERS_TYPE_DATA_DI_OFFSET)).data_in() : null;
    }

    public int parameters_size_in_bytes() {
        ParametersTypeData param = parameters_type_data();
        return param == null ? 0 : param.size_in_bytes();
    }

    // My size
    public int size_in_bytes() {
        return unsafe.getInt(this.address+SIZE_OFFSET);
    }
    public int size(){
        return (int) JVM.align_metadata_size(JVM.alignUp(unsafe.getInt(this.address+SIZE_OFFSET), JVM.BytesPerWord)/JVM.BytesPerWord);
    }

    private static final int no_profile_data = -1, variable_cell_count = -2 ;

    // Compute the size of the profiling information corresponding to
    // the current bytecode.
    public static int compute_data_size(BytecodeStream stream) {
        int cell_count = bytecode_cell_count(stream.code());
        if (cell_count == no_profile_data) {
            return 0;
        }
        if (cell_count == variable_cell_count) {
            switch (stream.code()) {
                case Bytecodes.Code._lookupswitch:
                case Bytecodes.Code._tableswitch:
                    cell_count = MultiBranchData.compute_cell_count(stream);
                    break;
                case Bytecodes.Code._invokespecial:
                case Bytecodes.Code._invokestatic:
                case Bytecodes.Code._invokedynamic:
                    if (!(MethodData.profile_arguments() || MethodData.profile_return())){
                        throw new RuntimeException("should be collecting args profile");
                    }
                    if (profile_arguments_for_invoke(stream.method(), stream.bci()) ||
                            profile_return_for_invoke(stream.method(), stream.bci())) {
                        cell_count = CallTypeData.compute_cell_count(stream);
                    } else {
                        cell_count = CounterData.static_cell_count();
                    }
                    break;
                case Bytecodes.Code._invokevirtual:
                case Bytecodes.Code._invokeinterface: {
                    if (!(MethodData.profile_arguments() || MethodData.profile_return())){
                        throw new RuntimeException("should be collecting args profile");
                    }
                    if (profile_arguments_for_invoke(stream.method(), stream.bci()) ||
                            profile_return_for_invoke(stream.method(), stream.bci())) {
                        cell_count = VirtualCallTypeData.compute_cell_count(stream);
                    } else {
                        cell_count = VirtualCallData.static_cell_count();
                    }
                    break;
                }
                default:
                    throw new RuntimeException("unexpected bytecode for var length profile data");
            }
        }
        // Note:  cell_count might be zero, meaning that there is just
        //        a DataLayout header, with no extra cells.
        if (!(cell_count >= 0)){
            throw new RuntimeException("sanity");
        }
        return DataLayout.compute_size_in_bytes(cell_count);
    }

    private static int bytecode_cell_count(@RawCType("Bytecodes::Code")int code) {
        //TODO
//        if (CompilerConfig.is_c1_simple_only() && !JVM.getFlag("ProfileInterpreter").getBool()) {
//            return no_profile_data;
//        }
        switch (code) {
            case Bytecodes.Code._checkcast:
            case Bytecodes.Code._instanceof:
            case Bytecodes.Code._aastore:
                if (JVM.getFlag("TypeProfileCasts").getBool()) {
                    return ReceiverTypeData.static_cell_count();
                } else {
                    return BitData.static_cell_count();
                }
            case Bytecodes.Code._invokespecial:
            case Bytecodes.Code._invokestatic:
                if (MethodData.profile_arguments() || MethodData.profile_return()) {
                return variable_cell_count;
            } else {
                return CounterData.static_cell_count();
            }
            case Bytecodes.Code._goto:
            case Bytecodes.Code._goto_w:
            case Bytecodes.Code._jsr:
            case Bytecodes.Code._jsr_w:
                return JumpData.static_cell_count();
            case Bytecodes.Code._invokevirtual:
            case Bytecodes.Code._invokeinterface:
                if (MethodData.profile_arguments() || MethodData.profile_return()) {
                    return variable_cell_count;
                } else {
                    return VirtualCallData.static_cell_count();
                }
            case Bytecodes.Code._invokedynamic:
                if (MethodData.profile_arguments() || MethodData.profile_return()) {
                    return variable_cell_count;
                } else {
                    return CounterData.static_cell_count();
                }
            case Bytecodes.Code._ret:
                return RetData.static_cell_count();
            case Bytecodes.Code._ifeq:
            case Bytecodes.Code._ifne:
            case Bytecodes.Code._iflt:
            case Bytecodes.Code._ifge:
            case Bytecodes.Code._ifgt:
            case Bytecodes.Code._ifle:
            case Bytecodes.Code._if_icmpeq:
            case Bytecodes.Code._if_icmpne:
            case Bytecodes.Code._if_icmplt:
            case Bytecodes.Code._if_icmpge:
            case Bytecodes.Code._if_icmpgt:
            case Bytecodes.Code._if_icmple:
            case Bytecodes.Code._if_acmpeq:
            case Bytecodes.Code._if_acmpne:
            case Bytecodes.Code._ifnull:
            case Bytecodes.Code._ifnonnull:
                return BranchData.static_cell_count();
            case Bytecodes.Code._lookupswitch:
            case Bytecodes.Code._tableswitch:
                return variable_cell_count;
            default:
                return no_profile_data;
        }
    }


    // return the argument info cell
    public ArgInfoData arg_info() {
        DataLayout dp    = extra_data_base();
        DataLayout end   = args_data_limit();
        for (; dp.address < end.address; dp = next_extra(dp)) {
            if (dp.tag() == DataLayout.arg_info_data_tag)
                return new ArgInfoData(dp);
        }
        return null;
    }

    // Translate a bci to its corresponding data index (di).
    public @RawCType("address")long bci_to_dp(int bci) {
        DataLayout data = data_layout_before(bci);
        DataLayout prev = null;
        for ( ; is_valid(data); data = next_data_layout(data)) {
            if (data.bci() >= bci) {
                if (data.bci() == bci)  set_hint_di(dp_to_di(data.address));
                else if (prev != null)   set_hint_di(dp_to_di(prev.address));
                return data.address;
            }
            prev = data;
        }
        return limit_data_position().address;
    }
    public DataLayout data_layout_before(int bci) {
        // avoid SEGV on this edge case
        if (data_size() == 0)
            return null;
        DataLayout layout = data_layout_at(hint_di());
        if (layout.bci() <= bci)
            return layout;
        return data_layout_at(first_di());
    }

    // Location and size of data area
    public @RawCType("address")long data_base() {
        return this.address+DATA_OFFSET;
    }
    public int data_size() {
        return unsafe.getInt(this.address+DATA_SIZE_OFFSET);
    }

    // Helper for initialization
    public DataLayout data_layout_at(int data_index){
        if (!(data_index % JVM.oopSize == 0)){
            throw new IllegalArgumentException("unaligned");
        }
        return new DataLayout((this.address+DATA_OFFSET) + data_index);
    }

    // Iteration over data.
    public ProfileData next_data(ProfileData current) {
        int current_index = dp_to_di(current.dp());
        int next_index = current_index + current.size_in_bytes();
        ProfileData next = data_at(next_index);
        return next;
    }

    public DataLayout next_data_layout(DataLayout current) {
        int current_index = dp_to_di(current.address);
        int next_index = current_index + current.size_in_bytes();
        if (out_of_bounds(next_index)) {
            return null;
        }
        DataLayout next = data_layout_at(next_index);
        return next;
    }

    // Convert a dp (data pointer) to a di (data index).
    public int dp_to_di(@RawCType("address")long dp) {
        return (int) (dp - (this.address+DATA_OFFSET));
    }


    // Get the data at an arbitrary (sort of) data index.
    public ProfileData data_at(int data_index) {
        if (out_of_bounds(data_index)) {
            return null;
        }
        DataLayout data_layout = data_layout_at(data_index);
        return data_layout.data_in();
    }

    // Translate a bci to its corresponding data, or NULL.
    public ProfileData bci_to_data(int bci) {
        DataLayout data = data_layout_before(bci);
        for ( ; is_valid(data); data = next_data_layout(data)) {
            if (data.bci() == bci) {
                set_hint_di(dp_to_di(data.address));
                return data.data_in();
            } else if (data.bci() > bci) {
                break;
            }
        }
        return bci_to_extra_data(bci, null, false);
    }

    public static DataLayout next_extra(DataLayout dp) {
        int nb_cells = 0;
        int tag = dp.tag();
        if (tag == DataLayout.bit_data_tag || tag == DataLayout.no_tag) {
            nb_cells = BitData.static_cell_count();
        } else if (tag == DataLayout.speculative_trap_data_tag) {
            nb_cells = SpeculativeTrapData.static_cell_count();
        } else {
            throw new RuntimeException("unexpected tag " + dp.tag());
        }
        return new DataLayout(dp.address + DataLayout.compute_size_in_bytes(nb_cells));
    }

    // Translate a bci to its corresponding extra data, or NULL.
    public ProfileData bci_to_extra_data(int bci, Method m, boolean create_if_missing) {
        // This code assumes an entry for a SpeculativeTrapData is 2 cells
        if (!(2*DataLayout.compute_size_in_bytes(BitData.static_cell_count()) ==
                DataLayout.compute_size_in_bytes(SpeculativeTrapData.static_cell_count()))){
            throw new RuntimeException("code needs to be adjusted");
        }

        // Do not create one of these if method has been redefined.
        if (m != null && m.is_old()) {
            return null;
        }

        DataLayout dp  = extra_data_base();
        DataLayout end = args_data_limit();

        // Allocation in the extra data space has to be atomic because not
        // all entries have the same size and non atomic concurrent
        // allocation would result in a corrupted extra data space.
        {
            DataLayout[] tmp_arr=new DataLayout[]{dp};
            ProfileData result = bci_to_extra_data_helper(bci, m, tmp_arr, true);
            dp=tmp_arr[0];
            if (result != null) {
                return result;
            }
        }


        if (create_if_missing && Long.compareUnsigned(dp.address,end.address) < 0) {
            // Check again now that we have the lock. Another thread may
            // have added extra data entries.
            {
                DataLayout[] tmp_arr=new DataLayout[]{dp};
                ProfileData result = bci_to_extra_data_helper(bci, m, tmp_arr, false);
                dp=tmp_arr[0];
                if (result != null ||Long.compareUnsigned(dp.address,end.address) >= 0) {
                    return result;
                }
            }
            if (!(dp.tag() == DataLayout.no_tag || (dp.tag() == DataLayout.speculative_trap_data_tag && m != null))){
                throw new RuntimeException("should be free");
            }
            if (!(next_extra(dp).tag() == DataLayout.no_tag || next_extra(dp).tag() == DataLayout.arg_info_data_tag)){
                throw new RuntimeException("should be free or arg info");
            }
            @RawCType("u1")int tag = m == null ? DataLayout.bit_data_tag : DataLayout.speculative_trap_data_tag;
            // SpeculativeTrapData is 2 slots. Make sure we have room.
            if (m != null && next_extra(dp).tag() != DataLayout.no_tag) {
                return null;
            }
            long tmp_addr=unsafe.allocateMemory(DataLayout.SIZE);
            DataLayout temp=new DataLayout(tmp_addr);
            temp.initialize(tag, bci, 0);
            dp.set_header(temp.header());
            unsafe.freeMemory(tmp_addr);
            if (!(dp.tag() == tag)){
                throw new RuntimeException("sane");
            }
            if (!(dp.bci() == bci)){
                throw new RuntimeException("no concurrent allocation");
            }
            if (tag == DataLayout.bit_data_tag) {
                return new BitData(dp);
            } else {
                SpeculativeTrapData data = new SpeculativeTrapData(dp);
                data.set_method(m);
                return data;
            }
        }
        return null;
    }

    public ProfileData bci_to_extra_data_helper(int bci, Method m, @RawCType("DataLayout*&")DataLayout[] dp, boolean concurrent) {
        DataLayout end = args_data_limit();

        for (;; dp[0] = next_extra(dp[0])){
            if (!(Long.compareUnsigned(dp[0].address,end.address) < 0)){
                throw new RuntimeException("moved past end of extra data");
            }
            // No need for "Atomic::load_acquire" ops,
            // since the data structure is monotonic.
            int tag = dp[0].tag();
            if (tag == DataLayout.no_tag) {
                return null;
            } else if (tag == DataLayout.arg_info_data_tag) {
                dp[0] = end;
                return null; // ArgInfoData is at the end of extra data section.
            } else if (tag == DataLayout.bit_data_tag) {
                if (m == null && dp[0].bci() == bci) {
                    return new BitData(dp[0]);
                }
            } else if (tag == DataLayout.speculative_trap_data_tag) {
                if (m != null) {
                    SpeculativeTrapData data = new SpeculativeTrapData(dp[0]);
                    // data->method() may be null in case of a concurrent
                    // allocation. Maybe it's for the same method. Try to use that
                    // entry in that case.
                    if (dp[0].bci() == bci) {
                        if (data.method() == null) {
                            if (!concurrent) {
                                throw new RuntimeException("impossible because no concurrent allocation");
                            }
                            return null;
                        } else if (data.method().equals(m)) {
                            return data;
                        }
                    }
                }
            } else {
                throw new RuntimeException("unexpected tag " + dp[0].tag());
            }
        }
    }

    @Override
    public String toString() {
        return "MethodData@0x"+Long.toHexString(this.address);
    }

    public static boolean profile_jsr292(Method m, int bci) {
        if (m.is_compiled_lambda_form()) {
            return true;
        }
        Bytecode_invoke inv=new Bytecode_invoke(m , bci);
        return inv.is_invokedynamic() || inv.is_invokehandle();
    }

    public static boolean profile_unsafe(Method m, int bci) {
        Bytecode_invoke inv=new Bytecode_invoke(m , bci);
        if (inv.is_invokevirtual()) {
            Symbol klass = inv.klass();
            if (klass.toString().equals("jdk/internal/misc/Unsafe")||
                    klass.toString().equals("sun/misc/Unsafe") ||
                    klass.toString().equals("jdk/internal/misc/ScopedMemoryAccess")) {
                String name = inv.name().toString();
                if (name.startsWith("get") || name.startsWith("put")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean profile_memory_access(Method m, int bci) {
        Bytecode_invoke inv=new Bytecode_invoke(m , bci);
        if (inv.is_invokestatic()) {
            if (inv.klass().toString().equals("jdk/incubator/foreign/MemoryAccess")) {
                if (inv.name().toString().startsWith("get") || inv.name().toString().startsWith("set")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int profile_arguments_flag() {
        return (int) Long.remainderUnsigned(JVM.getFlag("TypeProfileLevel").getUIntx(),10);
    }

    public static boolean profile_arguments() {
        return profile_arguments_flag() > no_type_profile && profile_arguments_flag() <= type_profile_all;
    }

    public static boolean profile_arguments_jsr292_only() {
        return profile_arguments_flag() == type_profile_jsr292;
    }

    public static boolean profile_all_arguments() {
        return profile_arguments_flag() == type_profile_all;
    }

    public static boolean profile_arguments_for_invoke(Method m, int bci) {
        if (!profile_arguments()) {
            return false;
        }

        if (profile_all_arguments()) {
            return true;
        }

        if (profile_unsafe(m, bci)) {
            return true;
        }

        if (profile_memory_access(m, bci)) {
            return true;
        }

        if (!profile_arguments_jsr292_only()){
            throw new RuntimeException("inconsistent");
        }
        return profile_jsr292(m, bci);
    }

    public static int profile_return_flag() {
        return (int) Long.divideUnsigned(Long.remainderUnsigned(JVM.getFlag("TypeProfileLevel").getUIntx(),100),10);
    }

    public static boolean profile_return() {
        return profile_return_flag() > no_type_profile && profile_return_flag() <= type_profile_all;
    }

    public static boolean profile_return_jsr292_only() {
        return profile_return_flag() == type_profile_jsr292;
    }

    public static boolean profile_all_return() {
        return profile_return_flag() == type_profile_all;
    }

    public static boolean profile_return_for_invoke(Method m, int bci) {
        if (!profile_return()) {
            return false;
        }

        if (profile_all_return()) {
            return true;
        }
        if (!profile_return_jsr292_only()){
            throw new RuntimeException("inconsistent");
        }
        return profile_jsr292(m, bci);
    }

    public static int profile_parameters_flag() {
        return (int) Long.divideUnsigned(JVM.getFlag("TypeProfileLevel").getUIntx(),100);
    }

    public static boolean profile_parameters() {
        return profile_parameters_flag() > no_type_profile && profile_parameters_flag() <= type_profile_all;
    }

    public static boolean profile_parameters_jsr292_only() {
        return profile_parameters_flag() == type_profile_jsr292;
    }

    public static boolean profile_all_parameters() {
        return profile_parameters_flag() == type_profile_all;
    }

    public static boolean profile_parameters_for_method( Method m) {
        if (!profile_parameters()) {
            return false;
        }

        if (profile_all_parameters()) {
            return true;
        }
        if (!profile_parameters_jsr292_only()){
            throw new RuntimeException("inconsistent");
        }
        return m.is_compiled_lambda_form();
    }

}
