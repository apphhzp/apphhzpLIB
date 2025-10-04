package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.compiler.CompLevel;
import apphhzp.lib.hotspot.interpreter.BytecodeTracer;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.interpreter.InterpreterOopMap;
import apphhzp.lib.hotspot.interpreter.OopMapCache;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.method.data.MethodData;
import apphhzp.lib.hotspot.prims.MethodHandles;
import apphhzp.lib.hotspot.prims.VMIntrinsics;
import apphhzp.lib.hotspot.runtime.AdapterHandlerEntry;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;
import apphhzp.lib.hotspot.runtime.signature.SignatureTypePrinter;
import apphhzp.lib.hotspot.stream.CompressedLineNumberReadStream;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.HashMap;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.helfy.JVM.oopSize;
import static apphhzp.lib.hotspot.oops.method.Method.Flags.*;

public class Method extends Metadata {
    public static final Type TYPE = JVM.type("Method");
    public static final int SIZE = TYPE.size;
    public static final long CONSTMETHOD_OFFSET = TYPE.offset("_constMethod");
    public static final long METHOD_DATA_OFFSET = TYPE.offset("_method_data");
    public static final long METHODCOUNTERS_OFFSET = TYPE.offset("_method_counters");
    public static final long ADAPTER_OFFSET = METHODCOUNTERS_OFFSET + JVM.oopSize;
    public static final long ACC_FLAGS_OFFSET = TYPE.offset("_access_flags");
    public static final long VTABLE_INDEX_OFFSET = TYPE.offset("_vtable_index");
    public static final long INTRINSIC_ID_OFFSET = TYPE.offset("_intrinsic_id");
    public static final long FLAGS_OFFSET = TYPE.offset("_flags");
    public static final long TRACE_FLAGS_OFFSET = JVM.includeJFR ? JVM.computeOffset(2, FLAGS_OFFSET + 2) : -1;
    public static final long COMPILED_INVOCATION_COUNT_OFFSET = JVM.product ? -1 : JVM.computeOffset(8, JVM.includeJFR ? TRACE_FLAGS_OFFSET + 2 : FLAGS_OFFSET + 2);
    public static final long I2I_ENTRY_OFFSET = TYPE.offset("_i2i_entry");
    public static final long FROM_COMPILED_ENTRY_OFFSET = TYPE.offset("_from_compiled_entry");
    public static final long CODE_OFFSET = TYPE.offset("_code");
    public static final long FROM_INTERPRETED_OFFSET = TYPE.offset("_from_interpreted_entry");

    static {
        if (COMPILED_INVOCATION_COUNT_OFFSET != -1) {
            JVM.assertOffset(I2I_ENTRY_OFFSET, JVM.computeOffset(JVM.oopSize, COMPILED_INVOCATION_COUNT_OFFSET + 8));
        } else if (TRACE_FLAGS_OFFSET != -1) {
            JVM.assertOffset(I2I_ENTRY_OFFSET, JVM.computeOffset(JVM.oopSize, TRACE_FLAGS_OFFSET + 2));
        }
    }

    private static final HashMap<Long, Method> CACHE = new HashMap<>();

    private ConstMethod constMethodCache;
    private CompiledMethod codeCache;
    private MethodCounters countersCache;
    private AdapterHandlerEntry adapterCache;
    private MethodData dataCache;

    public static Method getOrCreate(long addr) {
        if (addr == 0L) {
            throw new IllegalArgumentException("The pointer is NULL(0L)!");
        }
        Method re = CACHE.get(addr);
        if (re != null) {
            return re;
        }
        CACHE.put(addr, re = new Method(addr));
        return re;
    }

    public static void clearCacheMap() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private Method(long addr) {
        super(addr);
    }

    public ConstMethod constMethod() {
        long addr = unsafe.getAddress(this.address + CONSTMETHOD_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.constMethodCache, addr)) {
            this.constMethodCache = new ConstMethod(addr);
        }
        return this.constMethodCache;
    }

    public void set_constMethod(ConstMethod val) {
        unsafe.putAddress(this.address + CONSTMETHOD_OFFSET, val.address);
    }

    @Nullable
    public MethodData getMethodData() {
        long addr = unsafe.getAddress(this.address + METHOD_DATA_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.dataCache, addr)) {
            this.dataCache = new MethodData(addr);
        }
        return this.dataCache;
    }

    @Nullable
    public MethodCounters getCounters() {
        long addr = unsafe.getAddress(this.address + METHODCOUNTERS_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.countersCache, addr)) {
            this.countersCache = new MethodCounters(addr);
        }
        return this.countersCache;
    }

    public void setCounters(@Nullable MethodCounters counters) {
        unsafe.putAddress(this.address + METHODCOUNTERS_OFFSET, counters == null ? 0L : counters.address);
    }

    @Nullable
    public AdapterHandlerEntry getAdapter() {
        long addr = unsafe.getAddress(this.address + ADAPTER_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.adapterCache, addr)) {
            this.adapterCache = new AdapterHandlerEntry(addr);
        }
        return this.adapterCache;
    }

    public void setAdapter(@Nullable AdapterHandlerEntry adapter) {
        unsafe.putAddress(this.address + ADAPTER_OFFSET, adapter == null ? 0L : adapter.address);
    }

    public AccessFlags access_flags() {
        return AccessFlags.getOrCreate(unsafe.getInt(this.address + ACC_FLAGS_OFFSET));
    }

    public void set_access_flags(int flags) {
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }

    public boolean is_public(){ return access_flags().isPublic();      }
    public boolean is_private(){ return access_flags().isPrivate();     }
    public boolean is_protected(){ return access_flags().isProtected();   }
    public boolean is_package_private(){ return !is_public() && !is_private() && !is_protected(); }
    public boolean is_static(){ return access_flags().isStatic();      }
    public boolean is_final(){ return access_flags().isFinal();       }
    public boolean is_synchronized(){ return access_flags().isSynchronized();}
    public boolean is_native(){ return access_flags().isNative();      }
    public boolean is_abstract(){ return access_flags().isAbstract();    }
    public boolean is_synthetic(){ return access_flags().isSynthetic();   }

    // RedefineClasses() support:
    public boolean is_old()                                { return access_flags().isOld(); }
    public boolean is_obsolete()                           { return access_flags().isObsolete(); }
    public boolean is_deleted()                            { return access_flags().isDeleted(); }
    public boolean on_stack()                              { return access_flags().onStack(); }

    public @RawCType("ConstMethod::MethodType")int method_type() {
        return this.constMethod().method_type();
    }
    public boolean is_overpass(){
        return this.method_type()==ConstMethod.MethodType.OVERPASS;
    }

    public void setAccessible() {
        int flags = unsafe.getInt(this.address + ACC_FLAGS_OFFSET);
        flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        flags |= Opcodes.ACC_PUBLIC;
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }

    public int intrinsic_id() {
        return unsafe.getShort(this.address + INTRINSIC_ID_OFFSET) & 0xffff;
    }

    public void set_intrinsic_id(@RawCType("vmIntrinsicID")int id){
        unsafe.putShort(this.address+INTRINSIC_ID_OFFSET, (short) (id&0xffff));
    }

    /**Test if this method is an internal MH primitive method.*/
    public boolean is_method_handle_intrinsic() {
        int id = this.intrinsic_id();
        return MethodHandles.is_signature_polymorphic(id) && MethodHandles.is_signature_polymorphic_intrinsic(id);
    }

    public int getFlags() {
        return unsafe.getShort(this.address + FLAGS_OFFSET) & 0xffff;
    }

    public void setFlags(int flags) {
        unsafe.putShort(this.address + FLAGS_OFFSET, (short) (flags & 0xffff));
    }

    public long interpreter_entry() {
        return unsafe.getAddress(this.address + I2I_ENTRY_OFFSET);
    }

    public void set_interpreter_entry(long addr) {
        unsafe.putAddress(this.address + I2I_ENTRY_OFFSET, addr);
        unsafe.putAddress(this.address + FROM_INTERPRETED_OFFSET, addr);
    }

    public long getFromCompiledEntry() {
        return unsafe.getAddress(this.address + FROM_COMPILED_ENTRY_OFFSET);
    }

    public void setFromCompiledEntry(long addr) {
        unsafe.putAddress(this.address + FROM_COMPILED_ENTRY_OFFSET, addr);
    }

    public long getFromInterpretedEntry() {
        return unsafe.getAddress(this.address + FROM_INTERPRETED_OFFSET);
    }

    public void setFromInterpretedEntry(long addr) {
        unsafe.putAddress(this.address + FROM_INTERPRETED_OFFSET, addr);
    }

    public InstanceKlass method_holder() {
        return this.constMethod().constants().pool_holder();
    }

    @Nullable
    public CompiledMethod code() {
        long addr = unsafe.getAddress(this.address + CODE_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.codeCache, addr)) {
            this.codeCache=new NMethod(addr);;
        }
        return this.codeCache;
    }

    public void setCompiledMethod(@Nullable CompiledMethod method) {
        unsafe.putAddress(this.address + CODE_OFFSET, method == null ? 0L : method.address);
    }

    public int code_size(){ return this.constMethod().getCodeSize(); }

    /** method size in <b>words<b/> */
    public int method_size(){
        return SIZE/JVM.wordSize + ( is_native() ? 2 : 0 );
    }

    public long get_i2c_entry() {
        AdapterHandlerEntry entry = getAdapter();
        if (entry == null) {
            throw new IllegalStateException("must have");
        }
        return entry.getI2CEntry();
    }

    public long get_c2i_entry() {
        AdapterHandlerEntry entry = getAdapter();
        if (entry == null) {
            throw new IllegalStateException("must have");
        }
        return entry.getC2IEntry();
    }

    public long get_c2i_unverified_entry() {
        AdapterHandlerEntry entry = getAdapter();
        if (entry == null) {
            throw new IllegalStateException("must have");
        }
        return entry.getC2IUnverifiedEntry();
    }

    public long get_c2i_no_clinit_check_entry() {
        AdapterHandlerEntry entry = getAdapter();
        if (entry == null) {
            throw new IllegalStateException("must have");
        }
        return entry.getC2INoClinitCheckEntry();
    }

    public ConstantPool constants() {
        return this.constMethod().constants();
    }

    public void setConstantPool(ConstantPool pool){
        this.constMethod().set_constants(pool);
    }

    public Symbol name() {
        return constants().symbol_at(name_index());
    }

    public int name_index() {
        return constMethod().getNameIndex();
    }

    public void set_name_index(int index) {
        constMethod().setNameIndex(index);
    }

    // signature
    public Symbol signature() {
        return constants().symbol_at(signature_index());
    }

    public int signature_index() {
        return constMethod().getSignatureIndex();
    }

    public void set_signature_index(int index) {
        constMethod().setSignatureIndex(index);
    }

    // generics support
    @Nullable
    public Symbol generic_signature() {
        int idx = generic_signature_index();
        return ((idx != 0) ? this.constants().symbol_at(idx) : null);
    }

    public int generic_signature_index() {
        return constMethod().getGenericSignatureIndex();
    }

    public void set_generic_signature_index(int index) {
        constMethod().setGenericSignatureIndex(index);
    }

    public @RawCType("AnnotationArray*") U1Array annotations(){
        return this.constMethod().method_annotations();
    }
    public @RawCType("AnnotationArray*") U1Array parameter_annotations()  {
        return this.constMethod().parameter_annotations();
    }
    public @RawCType("AnnotationArray*") U1Array annotation_default()   {
        return this.constMethod().default_annotations();
    }
    public @RawCType("AnnotationArray*") U1Array type_annotations()       {
        return this.constMethod().type_annotations();
    }

    public @RawCType("Bytecodes::Code") int java_code_at(int bci){
        return Bytecodes.java_code_at(this, bcp_from(bci));
    }
    public @RawCType("Bytecodes::Code") int code_at(int bci){
        return Bytecodes.code_at(this, bcp_from(bci));
    }


    public long bcp_from(int bci){
        if (!((is_native() && bci == 0) || (!is_native() && 0 <= bci && bci < code_size()))){
            throw new IllegalArgumentException("illegal bci: "+bci+" for "+(is_native()?"native" : "non-native")+" method");
        }
        long bcp = code_base() + bci;
        if (!(is_native() && bcp == code_base() || contains(bcp))){
            throw new IllegalArgumentException("bcp doesn't belong to this method");
        }
        return bcp;
    }

    public @RawCType("address")long bcp_from(@RawCType("address")long bcp){
        if (is_native() && bcp == 0L) {
            return code_base();
        } else {
            return bcp;
        }
    }

    public int bci_from(@RawCType("address") long bcp) {
        boolean isNative = this.access_flags().isNative();
        if (isNative && bcp == 0) {
            return 0;
        }
        // Do not have a ResourceMark here because AsyncGetCallTrace stack walking code
        // may call this after interrupting a nested ResourceMark.
        if (!(isNative && bcp == code_base() || contains(bcp))) {
            throw new IllegalArgumentException("bcp doesn't belong to this method. bcp: 0x" + Long.toHexString(bcp));
        }
        return (int) (bcp - code_base());
    }

    public int orig_bytecode_at(int bci) {
        if (!JVM.includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        BreakpointInfo bp = this.method_holder().getBreakpointInfo();
        for (; bp != null; bp = bp.getNext()) {
            if (bp.match(this, bci)) {
                return bp.getOrigBytecode();
            }
        }
        return Bytecodes.Code._shouldnotreachhere;
    }

    public void set_orig_bytecode_at(int bci, @RawCType("Bytecodes::Code")int code) {
        if (!JVM.includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        if (code==Bytecodes.Code._breakpoint){
            throw new IllegalArgumentException("cannot patch breakpoints this way");
        }
        BreakpointInfo bp = this.method_holder().getBreakpointInfo();
        for (; bp != null; bp = bp.getNext()) {
            if (bp.match(this, bci)) {
                bp.setOrigBytecode(code);
                // and continue, in case there is more than one
            }
        }
    }

    public @RawCType("u2") int number_of_breakpoints() {
        if (!JVM.includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        MethodCounters mcs = this.getCounters();
        if (mcs == null) {
            return 0;
        } else {
            return mcs.getNumberOfBreakpoints();
        }
    }
    public void incr_number_of_breakpoints() {
        if (!JVM.includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            mcs.setNumberOfBreakpoints(mcs.getNumberOfBreakpoints()+1);
        }
    }
    public void decr_number_of_breakpoints() {
        if (!JVM.includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            mcs.setNumberOfBreakpoints(mcs.getNumberOfBreakpoints()-1);
        }
    }
    // Initialization only
    public void clear_number_of_breakpoints() {
        if (!JVM.includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            mcs.setNumberOfBreakpoints(0);
        }
    }

    public int highest_comp_level() {
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            return mcs.getHighestCompLevel();
        } else {
            return CompLevel.NONE.id;
        }
    }

    public int highest_osr_comp_level()  {
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            return mcs.getHighestOsrCompLevel();
        } else {
            return CompLevel.NONE.id;
        }
    }

    public void set_highest_comp_level(int level) {
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            mcs.setHighestCompLevel(level);
        }
    }

    public void set_highest_osr_comp_level(int level) {
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            mcs.setHighestOsrCompLevel(level);
        }
    }

    public static boolean caller_sensitive(int flags) {
        return (flags & CALLER_SENSITIVE) != 0;
    }

    public static boolean force_inline(int flags) {
        return (flags & FORCE_INLINE) != 0;
    }

    public static boolean dont_inline(int flags) {
        return (flags & DONT_INLINE) != 0;
    }

    public static boolean is_hidden(int flags) {
        return (flags & HIDDEN) != 0;
    }

    public void set_code(long code) {
        this.constMethod().set_code(code);
    }

    public long code_base() {
        return this.constMethod().code_base();
    }

    public boolean contains(long bcp) {
        return this.constMethod().contains(bcp);
    }

    // index into InstanceKlass methods() array
    // note: also used by jfr
    public @RawCType("u2") int method_idnum()            { return this.constMethod().getMethodIdnum(); }
    public void set_method_idnum(@RawCType("u2")int idnum)   { this.constMethod().setMethodIdnum(idnum); }

    public @RawCType("u2") int orig_method_idnum()           { return this.constMethod().getOrigMethodIdnum(); }
    public void set_orig_method_idnum(@RawCType("u2") int idnum)   { this.constMethod().setOrigMethodIdnum(idnum); }

    public int  verifier_max_stack()                {
        return this.constMethod().getMaxStack();
    }
    public int           max_stack()                {
        return this.constMethod().getMaxStack() + extra_stack_entries();
    }
    public void      set_max_stack(int size)              {
        this.constMethod().setMaxStack(size);
    }

    public static final int // How many extra stack entries for invokedynamic
            extra_stack_entries_for_jsr292 = 1;
    public static int extra_stack_entries() {
        return extra_stack_entries_for_jsr292;
    }

    // max locals
    public int  max_locals() {
        return this.constMethod().getMaxLocals(); }
    public void set_max_locals(int size){
        this.constMethod().setMaxLocals(size);
    }


    // Count of times method was exited via exception while interpreting
    public void interpreter_throwout_increment() {
        if (!JVM.usingServerCompiler){
            throw new UnsupportedOperationException();
        }
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            mcs.setInterpreterThrowoutCount(mcs.getInterpreterThrowoutCount()+1);
        }
    }


    public int  interpreter_throwout_count(){
        MethodCounters mcs = this.getCounters();
        if (mcs == null) {
            return 0;
        } else {
            return mcs.getInterpreterThrowoutCount();
        }
    }

    public @RawCType("BasicType") int result_type(){
        return this.constMethod().result_type();
    }
    public boolean is_returning_oop(){
        @RawCType("BasicType") int r = result_type();
        return BasicType.is_reference_type(r);
    }
    public boolean is_returning_fp(){
        @RawCType("BasicType") int r = result_type();
        return (r == BasicType.T_FLOAT || r == BasicType.T_DOUBLE);
    }

    public int  size_of_parameters()                { return this.constMethod().size_of_parameters(); }
    public void set_size_of_parameters(int size)          { this.constMethod().set_size_of_parameters(size); }

    public boolean has_stackmap_table() {
        return this.constMethod().has_stackmap_table();
    }

    public U1Array stackmap_data() {
        return this.constMethod().stackmap_data();
    }

    public void set_stackmap_data(U1Array sd) {
        this.constMethod().set_stackmap_data(sd);
    }

    public boolean is_default_method(){
        return this.method_holder() != null &&
                this.method_holder().isInterface() &&
                !is_abstract() && !is_private();
    }

    public void print_name(PrintStream st) {
        st.printf("%s ", is_static() ? "static" : "virtual");
        if (JVM.getFlag("WizardMode").getBool()) {
            st.printf("%s.", method_holder().internal_name());
            st.print(name());
            st.print(signature());
        } else {
            SignatureTypePrinter sig=new SignatureTypePrinter(signature(), st);
            sig.print_returntype();
            st.printf(" %s.", method_holder().internal_name());
            st.print(name());
            st.print("(");
            sig.print_parameters();
            st.print(")");
        }
    }

    public void print_short_name(PrintStream st) {
        st.printf(" %s::", method_holder().external_name());
        st.print(name());
        if (JVM.getFlag("WizardMode").getBool()) {
            st.print(signature());
        } else if (MethodHandles.is_signature_polymorphic(intrinsic_id())) {
            MethodHandles.print_as_basic_type_signature_on(st, signature());
        }
    }
    public void print_short_name(StringBuilder st) {
        st.append(" ").append(method_holder().external_name()).append("::");
        st.append(name());
        if (JVM.getFlag("WizardMode").getBool()) {
            st.append(signature());
        } else if (MethodHandles.is_signature_polymorphic(intrinsic_id())) {
            MethodHandles.print_as_basic_type_signature_on(st, signature());
        }
    }

    public  boolean has_linenumber_table(){
        return constMethod().has_linenumber_table();
    }

    public @RawCType("u_char*") long compressed_linenumber_table() {
        return constMethod().compressed_linenumber_table();
    }

    public int line_number_from_bci(int bci){
        int best_bci  =  0;
        int best_line = -1;
        if (bci == JVM.invocationEntryBci) {
            bci = 0;
        }
        if (0 <= bci && bci < code_size() && has_linenumber_table()) {
            // The line numbers are a short array of 2-tuples [start_pc, line_number].
            // Not necessarily sorted and not necessarily one-to-one.
            CompressedLineNumberReadStream stream=new CompressedLineNumberReadStream(compressed_linenumber_table());
            while (stream.readPair()) {
                if (stream.bci() == bci) {
                    // perfect match
                    return stream.line();
                } else {
                    // update best_bci/line
                    if (stream.bci() < bci && stream.bci() >= best_bci) {
                        best_bci  = stream.bci();
                        best_line = stream.line();
                    }
                }
            }
        }
        return best_line;
    }


    // Return bci if it appears to be a valid bcp
    // Return -1 otherwise.
    // Used by profiling code, when invalid data is a possibility.
    // The caller is responsible for validating the Method* itself.
    public int validate_bci_from_bcp(@RawCType("address")long bcp){
        // keep bci as -1 if not a valid bci
        int bci = -1;
        if (bcp == 0 || bcp == code_base()) {
            // code_size() may return 0 and we allow 0 here
            // the method may be native
            bci = 0;
        } else if (contains(bcp)) {
            bci = (int) (bcp - code_base());
        }
        // Assert that if we have dodged any asserts, bci is negative.
        if (!(bci == -1 || bci == bci_from(bcp_from(bci)))){
            throw new RuntimeException("sane bci if >=0");
        }
        return bci;
    }



    public void print_codes_on(PrintStream st) {
        print_codes_on(0, code_size(), st);
    }

    public void print_codes_on(int from, int to, PrintStream st){
        BytecodeStream s =new BytecodeStream(this);
        s.set_interval(from, to);
        BytecodeTracer.set_closure(BytecodeTracer.std_closure());
        while (s.next() >= 0) {
            BytecodeTracer.trace(this, s.bcp(), st);
        }
    }
    // Test if this method is an MH adapter frame generated by Java code.
    // Cf. java/lang/invoke/InvokerBytecodeGenerator
    public boolean is_compiled_lambda_form() {
        return intrinsic_id() == VMIntrinsics.compiledLambdaForm;
    }

    public final static class Flags {
        public static final int CALLER_SENSITIVE = JVM.intConstant("Method::_caller_sensitive");
        public static final int FORCE_INLINE = JVM.intConstant("Method::_force_inline");
        public static final int DONT_INLINE = JVM.intConstant("Method::_dont_inline");
        public static final int HIDDEN = JVM.intConstant("Method::_hidden");
        public static final int HAS_INJECTED_PROFILE = 1 << 4;
        public static final int INTRINSIC_CANDIDATE = 1 << 5;
        public static final int RESERVED_STACK_ACCESS = 1 << 6;
        public static final int SCOPED = 1 << 7;
    }

    public final static class  VtableIndexFlag {
        // Valid vtable indexes are non-negative (>= 0).
        // These few negative values are used as sentinels.
        public static final int itable_index_max        = -10, // first itable index, growing downward
        pending_itable_index    = -9,  // itable index will be assigned
        invalid_vtable_index    = -4,  // distinct from any valid vtable index
        garbage_vtable_index    = -3,  // not yet linked; no vtable layout yet
        nonvirtual_vtable_index = -2;   // there is no need for vtable dispatch
        // 6330203 Note:  Do not use -1, which was overloaded with many meanings.
    }

    // exception handler table
    public boolean has_exception_handler()
    { return constMethod().has_exception_handler(); }
//    int exception_table_length()
//    { return getConstMethod().exception_table_length(); }
//    ExceptionTableElement exception_table_start()
//    { return getConstMethod().exception_table_start(); }

    public void mask_for(int bci, InterpreterOopMap mask) {
        // Only GC uses the OopMapCache during thread stack root scanning
        // any other uses generate an oopmap but do not save it in the cache.
//        if (Universe.getCollectedHeap().isGCActive()) {
//            getHolder().mask_for(this, bci, mask);
//        } else {
            OopMapCache.compute_one_oop_map(this, bci, mask);
        //}
        //return;
    }

    public Symbol klass_name(){
        return method_holder().name();
    }


    public boolean has_jsrs() {
        return access_flags().hasJsrs();
    };
    public void set_has_jsrs() {
        set_access_flags(access_flags().flags|AccessFlags.JVM_ACC_HAS_JSRS);
    }

    // returns true if the method has any monitors.
    public boolean has_monitors(){
        return is_synchronized() || access_flags().hasMonitorBytecodes();
    }
    public boolean has_monitor_bytecodes(){
        return access_flags().hasMonitorBytecodes();
    }

    public void set_has_monitor_bytecodes(){
        set_access_flags(access_flags().flags|AccessFlags.JVM_ACC_HAS_MONITOR_BYTECODES);
    }

    public boolean has_valid_initializer_flags() {
        return (is_static() ||
                method_holder().major_version() < 51);
    }


    public boolean is_static_initializer() {
        // For classfiles version 51 or greater, ensure that the clinit method is
        // static.  Non-static methods with the name "<clinit>" are not static
        // initializers. (older classfiles exempted for backward compatibility)
        return name().equals(Symbol.getVMSymbol("<clinit>"))&&
                has_valid_initializer_flags();
    }

    public boolean is_object_initializer() {
        return name().equals(Symbol.getVMSymbol("<init>"));
    }

    // monitor matching. This returns a conservative estimate of whether the monitorenter/monitorexit bytecodes
    // propererly nest in the method. It might return false, even though they actually nest properly, since the info.
    // has not been computed yet.
    public boolean guaranteed_monitor_matching(){ return access_flags().isMonitorMatching(); }
    public void set_guaranteed_monitor_matching(){
        set_access_flags(access_flags().flags|AccessFlags.JVM_ACC_MONITOR_MATCH);
    }

    // checks method and its method holder
    public boolean is_final_method(){
        return is_final_method(method_holder().getAccessFlags());
    }
    public boolean is_final_method(AccessFlags class_access_flags){
        // or "does_not_require_vtable_entry"
        // default method or overpass can occur, is not final (reuses vtable entry)
        // private methods in classes get vtable entries for backward class compatibility.
        if (is_overpass() || is_default_method())  return false;
        return is_final() || class_access_flags.isFinal();
    }

    public boolean valid_vtable_index(){
        return unsafe.getInt(this.address+VTABLE_INDEX_OFFSET) >= VtableIndexFlag.nonvirtual_vtable_index;
    }
    public boolean has_vtable_index(){
        return unsafe.getInt(this.address+VTABLE_INDEX_OFFSET) >= 0; }
    public int  vtable_index(){
        return unsafe.getInt(this.address+VTABLE_INDEX_OFFSET);
    }
    public boolean valid_itable_index(){ return unsafe.getInt(this.address+VTABLE_INDEX_OFFSET) <= VtableIndexFlag.pending_itable_index; }
    public boolean has_itable_index() { return unsafe.getInt(this.address+VTABLE_INDEX_OFFSET) <= VtableIndexFlag.itable_index_max; }
    public int  itable_index() {
        if (!valid_itable_index()){
            throw new IllegalStateException();
        }
        return VtableIndexFlag.itable_index_max - unsafe.getInt(this.address+VTABLE_INDEX_OFFSET);
    }

    public boolean can_be_statically_bound(AccessFlags class_access_flags){
        if (is_final_method(class_access_flags))  return true;
//#ifdef ASSERT
//        ResourceMark rm;
//        bool is_nonv = (vtable_index() == nonvirtual_vtable_index);
//        if (class_access_flags.is_interface()) {
//            assert(is_nonv == is_static() || is_nonv == is_private(),
//            "nonvirtual unexpected for non-static, non-private: %s",
//                    name_and_sig_as_C_string());
//        }
//#endif
        if (!(valid_vtable_index() || valid_itable_index())){
            throw new RuntimeException("method must be linked before we ask this question");
        }
        return vtable_index() == VtableIndexFlag.nonvirtual_vtable_index;
    }

    public boolean can_be_statically_bound() {
        return can_be_statically_bound(method_holder().getAccessFlags());
    }

    public boolean can_be_statically_bound(InstanceKlass context) {
        return (method_holder().equals(context)) && can_be_statically_bound();
    }

    public boolean needs_clinit_barrier(){
        return is_static() && !method_holder().is_initialized();
    }

    public Method get_new_method(){
        InstanceKlass holder = method_holder();
        Method new_method = holder.methodWithIdNum(orig_method_idnum());
        if (new_method == null){
            throw new RuntimeException("method_with_idnum() should not be NULL");
        }
        if (this.equals(new_method)){
            throw new RuntimeException("sanity check");
        }
        return new_method;
    }

//    public char* name_and_sig_as_C_string() {
//        return name_and_sig_as_C_string(constants().pool_holder(), name(), signature());
//    }
//
//    public char* name_and_sig_as_C_string(char* buf, int size) {
//        return name_and_sig_as_C_string(constants().pool_holder(), name(), signature(), buf, size);
//    }
//
//    public char* name_and_sig_as_C_string(Klass klass, Symbol method_name, Symbol signature) {
//        const char* klass_name = klass->external_name();
//        int klass_name_len  = (int)strlen(klass_name);
//        int method_name_len = method_name->utf8_length();
//        int len             = klass_name_len + 1 + method_name_len + signature->utf8_length();
//        char* dest          = NEW_RESOURCE_ARRAY(char, len + 1);
//        strcpy(dest, klass_name);
//        dest[klass_name_len] = '.';
//        strcpy(&dest[klass_name_len + 1], method_name->as_C_string());
//        strcpy(&dest[klass_name_len + 1 + method_name_len], signature->as_C_string());
//        dest[len] = 0;
//        return dest;
//    }
//
//    public byte[] name_and_sig_as_C_string(Klass klass, Symbol method_name, Symbol signature, byte[] buf, int size) {
//        Symbol klass_name = klass.name();
//        klass_name.as_klass_external_name(buf, size);
//        int len = (int) CString.strlen(buf);
//
//        if (len < size - 1) {
//            buf[len++] = '.';
//
//            method_name.as_C_string(&(buf[len]), size - len);
//            len = (int)CString.strlen(buf);
//            signature.as_C_string(&(buf[len]), size - len);
//        }
//
//        return buf;
//    }
// Inlined elements
    private @RawCType("address*")long native_function_addr(){
        if (!this.is_native()){
            throw new RuntimeException("must be native");
        }
        return (this.address+SIZE);
    }
    private @RawCType("address*")long signature_handler_addr(){
        return native_function_addr() + oopSize;
    }
    public @RawCType("address")long native_function(){
        return unsafe.getAddress(native_function_addr());
    }
    // signature handler (used for native methods only)
    public @RawCType("address")long signature_handler(){
        return unsafe.getAddress(signature_handler_addr());
    }

    @Override
    public String toString() {
        return "Method@0x"+Long.toHexString(this.address);
    }
}
