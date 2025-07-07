package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.compiler.CompLevel;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.prims.VMIntrinsics;
import apphhzp.lib.hotspot.runtime.AdapterHandlerEntry;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.oops.method.Method.Flags.*;

public class Method extends MethodData {
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

    public ConstMethod getConstMethod() {
        long addr = unsafe.getAddress(this.address + CONSTMETHOD_OFFSET);
        if (!isEqual(this.constMethodCache, addr)) {
            this.constMethodCache = new ConstMethod(addr);
        }
        return this.constMethodCache;
    }

    public void setConstMethod(ConstMethod val) {
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

    public AccessFlags getAccessFlags() {
        return AccessFlags.getOrCreate(unsafe.getInt(this.address + ACC_FLAGS_OFFSET));
    }

    public void setAccessFlags(int flags) {
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }

    public boolean is_public(){ return getAccessFlags().isPublic();      }
    public boolean is_private(){ return getAccessFlags().isPrivate();     }
    public boolean is_protected(){ return getAccessFlags().isProtected();   }
    public boolean is_package_private(){ return !is_public() && !is_private() && !is_protected(); }
    public boolean is_static(){ return getAccessFlags().isStatic();      }
    public boolean is_final(){ return getAccessFlags().isFinal();       }
    public boolean is_synchronized(){ return getAccessFlags().isSynchronized();}
    public boolean is_native(){ return getAccessFlags().isNative();      }
    public boolean is_abstract(){ return getAccessFlags().isAbstract();    }
    public boolean is_synthetic(){ return getAccessFlags().isSynthetic();   }

    public void setAccessible() {
        int flags = unsafe.getInt(this.address + ACC_FLAGS_OFFSET);
        flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        flags |= Opcodes.ACC_PUBLIC;
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }

    public int getIntrinsicId() {
        return unsafe.getShort(this.address + INTRINSIC_ID_OFFSET) & 0xffff;
    }

    public void setIntrinsicId(int id) {
        unsafe.putShort(this.address + INTRINSIC_ID_OFFSET, (short) (id & 0xffff));
    }

    public boolean isMethodHandleIntrinsic() {
        int id = this.getIntrinsicId();
        return VMIntrinsics.isSignaturePolymorphic(id) && VMIntrinsics.isSignaturePolymorphicIntrinsic(id);
    }

    public int getFlags() {
        return unsafe.getShort(this.address + FLAGS_OFFSET) & 0xffff;
    }

    public void setFlags(int flags) {
        unsafe.putShort(this.address + FLAGS_OFFSET, (short) (flags & 0xffff));
    }

    public long getI2IEntry() {
        return unsafe.getAddress(this.address + I2I_ENTRY_OFFSET);
    }

    public void setI2IEntry(long addr) {
        unsafe.putAddress(this.address + I2I_ENTRY_OFFSET, addr);
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

    public InstanceKlass getHolder() {
        return this.getConstMethod().getConstantPool().getHolder();
    }

    @Nullable
    public CompiledMethod getCompiledMethod() {
        long addr = unsafe.getAddress(this.address + CODE_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.codeCache, addr)) {
            Type type = JVM.findDynamicTypeForAddress(addr, CompiledMethod.TYPE);
            if (type == CompiledMethod.TYPE) {
                this.codeCache = new CompiledMethod(addr);
            } else if (type == NMethod.TYPE) {
                this.codeCache = new NMethod(addr);
            } else {
                throw new NoSuchElementException("Unknown type:" + (type == null ? "null" : type.name));
            }
        }
        return this.codeCache;
    }

    public void setCompiledMethod(@Nullable CompiledMethod method) {
        unsafe.putAddress(this.address + CODE_OFFSET, method == null ? 0L : method.address);
    }

    public int code_size(){ return this.getConstMethod().getCodeSize(); }

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

    public ConstantPool getConstantPool() {
        return this.getConstMethod().getConstantPool();
    }

    public void setConstantPool(ConstantPool pool){
        this.getConstMethod().setConstantPool(pool);
    }

    public Symbol name() {
        return getConstantPool().symbol_at(name_index());
    }

    public int name_index() {
        return getConstMethod().getNameIndex();
    }

    public void set_name_index(int index) {
        getConstMethod().setNameIndex(index);
    }

    // signature
    public Symbol signature() {
        return getConstantPool().symbol_at(signature_index());
    }

    public int signature_index() {
        return getConstMethod().getSignatureIndex();
    }

    public void set_signature_index(int index) {
        getConstMethod().setSignatureIndex(index);
    }

    // generics support
    @Nullable
    public Symbol generic_signature() {
        int idx = generic_signature_index();
        return ((idx != 0) ? this.getConstantPool().symbol_at(idx) : null);
    }

    public int generic_signature_index() {
        return getConstMethod().getGenericSignatureIndex();
    }

    public void set_generic_signature_index(int index) {
        getConstMethod().setGenericSignatureIndex(index);
    }

    public @RawCType("AnnotationArray*") U1Array annotations(){
        return this.getConstMethod().method_annotations();
    }
    public @RawCType("AnnotationArray*") U1Array parameter_annotations()  {
        return this.getConstMethod().parameter_annotations();
    }
    public @RawCType("AnnotationArray*") U1Array annotation_default()   {
        return this.getConstMethod().default_annotations();
    }
    public @RawCType("AnnotationArray*") U1Array type_annotations()       {
        return this.getConstMethod().type_annotations();
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

    public int bci_from(@RawCType("address") long bcp) {
        boolean isNative = this.getAccessFlags().isNative();
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
        BreakpointInfo bp = this.getHolder().getBreakpointInfo();
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
        BreakpointInfo bp = this.getHolder().getBreakpointInfo();
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
        this.getConstMethod().set_code(code);
    }

    public long code_base() {
        return this.getConstMethod().code_base();
    }

    public boolean contains(long bcp) {
        return this.getConstMethod().contains(bcp);
    }

    // index into InstanceKlass methods() array
    // note: also used by jfr
    public @RawCType("u2") int method_idnum()            { return this.getConstMethod().getMethodIdnum(); }
    public void set_method_idnum(@RawCType("u2")int idnum)   { this.getConstMethod().setMethodIdnum(idnum); }

    public @RawCType("u2") int orig_method_idnum()           { return this.getConstMethod().getOrigMethodIdnum(); }
    public void set_orig_method_idnum(@RawCType("u2") int idnum)   { this.getConstMethod().setOrigMethodIdnum(idnum); }

    public int  verifier_max_stack()                {
        return this.getConstMethod().getMaxStack();
    }
    public int           max_stack()                {
        return this.getConstMethod().getMaxStack() + extra_stack_entries();
    }
    public void      set_max_stack(int size)              {
        this.getConstMethod().setMaxStack(size);
    }

    public static final int // How many extra stack entries for invokedynamic
            extra_stack_entries_for_jsr292 = 1;
    public static int extra_stack_entries() {
        return extra_stack_entries_for_jsr292;
    }

    // max locals
    public int  max_locals() {
        return this.getConstMethod().getMaxLocals(); }
    public void set_max_locals(int size){
        this.getConstMethod().setMaxLocals(size);
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
        return this.getConstMethod().result_type();
    }
    public boolean is_returning_oop(){
        @RawCType("BasicType") int r = result_type();
        return BasicType.is_reference_type(r);
    }
    public boolean is_returning_fp(){
        @RawCType("BasicType") int r = result_type();
        return (r == BasicType.T_FLOAT || r == BasicType.T_DOUBLE);
    }

    public int  size_of_parameters()                { return this.getConstMethod().size_of_parameters(); }
    public void set_size_of_parameters(int size)          { this.getConstMethod().set_size_of_parameters(size); }

    public boolean has_stackmap_table() {
        return this.getConstMethod().has_stackmap_table();
    }

    public U1Array stackmap_data() {
        return this.getConstMethod().stackmap_data();
    }

    public void set_stackmap_data(U1Array sd) {
        this.getConstMethod().set_stackmap_data(sd);
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
}
