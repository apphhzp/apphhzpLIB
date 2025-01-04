package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.classfile.ClassFileParser;
import apphhzp.lib.hotspot.classfile.VMClasses;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.compiler.CompLevel;
import apphhzp.lib.hotspot.memory.ReferenceType;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.method.ConstMethod;
import apphhzp.lib.hotspot.oop.method.Method;
import apphhzp.lib.hotspot.runtime.Thread;
import apphhzp.lib.hotspot.stream.AllFieldStream;

import javax.annotation.Nullable;

import java.util.Objects;

import static apphhzp.lib.ClassHelper.unsafe;

public class InstanceKlass extends Klass {
    public static final Type TYPE = JVM.type("InstanceKlass");
    public static final int SIZE = TYPE.size;
    public static final long CONSTANTS_OFFSET = TYPE.offset("_constants");
    public static final long INNER_CLASSES_OFFSET=TYPE.offset("_inner_classes");
    public static final long SOURCE_DEBUG_EXTENSION_OFFSET=TYPE.offset("_source_debug_extension");
    public static final long NONSTATIC_FIELD_SIZE_OFFSET=TYPE.offset("_nonstatic_field_size");
    public static final long STATIC_FIELD_SIZE_OFFSET=TYPE.offset("_static_field_size");
    public static final long NONSTATIC_OOP_MAP_SIZE_OFFSET=TYPE.offset("_nonstatic_oop_map_size");
    public static final long ITABLE_LEN_OFFSET=TYPE.offset("_itable_len");
    public static final long STATIC_OOP_FIELD_COUNT_OFFSET=TYPE.offset("_static_oop_field_count");
    public static final long FIELDS_COUNT_OFFSET = TYPE.offset("_java_fields_count");
    public static final long IDNUM_ALLOCATED_COUNT_OFFSET=TYPE.offset("_idnum_allocated_count");
    public static final long IS_MARKED_DEPENDENT_OFFSET=TYPE.offset("_is_marked_dependent");
    public static final long INIT_STATE_OFFSET = TYPE.offset("_init_state");
    public static final long REFERENCE_TYPE_OFFSET=TYPE.offset("_reference_type");
    public static final long MISC_FLAGS_OFFSET = TYPE.offset("_misc_flags");
    public static final long INIT_THREAD_OFFSET = TYPE.offset("_init_thread");
    public static final long OOP_MAP_CACHE_OFFSET=TYPE.offset("_oop_map_cache");
    public static final long OSR_NMETHOD_HEAD_OFFSET = TYPE.offset("_osr_nmethods_head");
    public static final long BREAKPOINTS_OFFSET = JVM.isJVMTISupported ? TYPE.offset("_breakpoints") : -1;
    public static final long METHODS_OFFSET = TYPE.offset("_methods");
    public static final long DEFAULT_METHODS_OFFSET = TYPE.offset("_default_methods");
    public static final long LOCAL_INTERFACES_OFFSET = TYPE.offset("_local_interfaces");
    public static final long TRANSITIVE_INTERFACES_OFFSET = TYPE.offset("_transitive_interfaces");
    public static final long METHOD_ORDERING_OFFSET=TYPE.offset("_method_ordering");
    public static final long DEFAULT_VTABLE_INDICES_OFFSET=TYPE.offset("_default_vtable_indices");
    public static final long FIELDS_OFFSET = TYPE.offset("_fields");
    private ConstantPool constantPoolCache;
    private U2Array innerClassesCache;
    private NMethod headCache;
    private VMTypeArray<Method> methodsCache;
    private VMTypeArray<Method> defaultMethodsCache;
    private VMTypeArray<InstanceKlass> localInterfacesCache;
    private VMTypeArray<InstanceKlass> transitiveInterfacesCache;
    private BreakpointInfo breakpointInfoCache;
    private IntArray methodOrderingCache;
    private IntArray defaultVTableIndicesCache;
    private U2Array fieldsCache;

    public static InstanceKlass getOrCreate(long addr) {
        Klass klass = Klass.getOrCreate(addr);
        if (klass.isInstanceKlass()) {
            return (InstanceKlass) klass;
        }
        throw new IllegalArgumentException("Need a InstanceKlass pointer!");
    }

    public static InstanceKlass create(){
        if (!(JVM.usingSharedSpaces||JVM.dumpSharedSpaces)){
            throw new IllegalStateException("only for CDS");
        }
        long addr=unsafe.allocateMemory(SIZE);
        unsafe.setMemory(addr, SIZE, (byte)0);
        return new InstanceKlass(addr);
    }

    protected InstanceKlass(long addr) {
        super(addr);
    }

//    public static InstanceKlass allocate_instance_klass(ClassFileParser parser) {
//        int size = InstanceKlass.size(parser.vtable_size(),
//                parser.itable_size(),
//                nonstatic_oop_map_size(parser.total_oop_map_count()),
//                parser.is_interface());
//
//        Symbol  class_name = parser.class_name();
//        if (class_name==null){
//            throw new IllegalStateException("invariant");
//        }
//        ClassLoaderData loader_data = parser.loader_data();
//        if (loader_data == null){
//            throw new IllegalStateException("invariant");
//        }
//        InstanceKlass ik;
//
//        // Allocation
//        if (ReferenceType.REF_NONE == parser.reference_type()) {
//            if (class_name.equals(Symbol.getVMSymbol("java/lang/Class"))) {
//                // mirror
//                ik = new (loader_data, size, THREAD) InstanceMirrorKlass(parser);
//            } else if (is_class_loader(class_name, parser)) {
//                // class loader
//                ik = new (loader_data, size, THREAD) InstanceClassLoaderKlass(parser);
//            } else {
//                // normal
//                ik = new (loader_data, size, THREAD) InstanceKlass(parser, InstanceKlass::_kind_other);
//            }
//        } else {
//            // reference
//            ik = new (loader_data, size, THREAD) InstanceRefKlass(parser);
//        }
//        return ik;
//    }
//
//    public static int size(int vtable_length, int itable_length,
//                    int nonstatic_oop_map_size,
//                    boolean is_interface) {
//        return (int) JVM.alignUp(SIZE/JVM.wordSize +
//                vtable_length +
//                itable_length +
//                nonstatic_oop_map_size +
//                (is_interface ? JVM.oopSize/JVM.wordSize : 0),1);
//    }
//
//    public static int nonstatic_oop_map_size(int oop_map_count) {
//        return oop_map_count * OopMapBlock.size_in_words();
//    }
//
//    public static  boolean is_class_loader( Symbol class_name,
//                                    ClassFileParser parser) {
//        if (class_name==null){
//            throw new IllegalArgumentException("invariant");
//        }
//        if (class_name.equals(Symbol.getVMSymbol("java/lang/ClassLoader"))){
//            return true;
//        }
//        if (VMClasses.classKlass().is_loaded()) {
//            Klass  super_klass = parser.super_klass();
//            if (super_klass != null) {
//                if (super_klass.is_subtype_of(VMClasses.classLoaderKlass())) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    public static InstanceKlass create(ClassFileParser parser, int kind){
//        return create(parser,kind,KlassID.InstanceKlassID);
//    }
//
//    public static InstanceKlass create(ClassFileParser parser, int kind,int /*KlassID*/ id){
//
//    }

    @Override
    public boolean isInstanceKlass() {
        return true;
    }

    @Override
    public InstanceKlass asInstanceKlass() {
        return this;
    }

    public ConstantPool getConstantPool() {
        long addr = unsafe.getAddress(this.address + CONSTANTS_OFFSET);
        if (!isEqual(this.constantPoolCache, addr)) {
            this.constantPoolCache = ConstantPool.getOrCreate(addr);
        }
        return this.constantPoolCache;
    }

    public void setConstantPool(ConstantPool pool) {
        unsafe.putAddress(this.address + CONSTANTS_OFFSET, pool.address);
        VMTypeArray<Method> methods = this.getMethods();
        for (Method method : methods) {
            method.getConstMethod().setConstantPool(pool);
        }
        methods = this.getDefaultMethods();
        if (methods != null) {
            for (Method method : methods) {
                method.getConstMethod().setConstantPool(pool);
            }
        }
    }

    public U2Array getInnerClasses() {
        long addr = unsafe.getAddress(this.address + INNER_CLASSES_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.innerClassesCache,addr)){
            this.innerClassesCache = new U2Array(addr);
        }
        return this.innerClassesCache;
    }

    public void setInnerClasses(@Nullable U2Array innerClasses) {
        this.innerClassesCache=null;
        unsafe.putAddress(this.address+INNER_CLASSES_OFFSET,innerClasses==null?0L:innerClasses.address);
    }

    public String getSourceDebugExtension() {
        return JVM.getStringRef(this.address + SOURCE_DEBUG_EXTENSION_OFFSET);
    }

    public void setSourceDebugExtension(String extension) {
        JVM.putStringRef(this.address + SOURCE_DEBUG_EXTENSION_OFFSET,extension);
    }

    public int getNonstaticFieldSize() {
        return unsafe.getInt(this.address + NONSTATIC_FIELD_SIZE_OFFSET);
    }

    public void setNonstaticFieldSize(int size) {
        unsafe.putInt(this.address + NONSTATIC_FIELD_SIZE_OFFSET,size);
    }

    public int getStaticFieldSize() {
        return unsafe.getInt(this.address + STATIC_FIELD_SIZE_OFFSET);
    }

    public void setStaticFieldSize(int size) {
        unsafe.putInt(this.address + STATIC_FIELD_SIZE_OFFSET,size);
    }

    public int getNonstaticOopMapSize() {
        return unsafe.getInt(this.address + NONSTATIC_OOP_MAP_SIZE_OFFSET);
    }

    public void setNonstaticOopMapSize(int size) {
        unsafe.putInt(this.address+NONSTATIC_OOP_MAP_SIZE_OFFSET,size);
    }

    public int getITableLen() {
        return unsafe.getInt(this.address + ITABLE_LEN_OFFSET);
    }

    public void setITableLen(int len) {
        unsafe.putInt(this.address + ITABLE_LEN_OFFSET,len);
    }

    public int getStaticOopFieldCount(){
        return unsafe.getShort(this.address + STATIC_OOP_FIELD_COUNT_OFFSET)&0xffff;
    }

    public void setStaticOopFieldCount(int count) {
        unsafe.putShort(this.address + STATIC_OOP_FIELD_COUNT_OFFSET,(short)(count&0xffff));
    }

    public int getFieldsCount() {
        return unsafe.getShort(this.address + FIELDS_COUNT_OFFSET) & 0xffff;
    }

    public void setFieldsCount(int cnt) {
        unsafe.putShort(this.address + FIELDS_COUNT_OFFSET, (short) (cnt & 0xffff));
    }

    public int getIDNumAllocatedCount(){
        return unsafe.getShort(this.address + IDNUM_ALLOCATED_COUNT_OFFSET)&0xffff;
    }

    public void setIDNumAllocatedCount(int count) {
        unsafe.putShort(this.address+IDNUM_ALLOCATED_COUNT_OFFSET,(short)(count&0xffff));
    }

    public boolean isMarkedDependent(){
        return unsafe.getByte(this.address+IS_MARKED_DEPENDENT_OFFSET)!=0;
    }

    public void setIsMarkedDependent(boolean mark) {
        unsafe.putByte(this.address+IS_MARKED_DEPENDENT_OFFSET,(byte)(mark?1:0));
    }

    public int getInitState() {
        return unsafe.getByte(this.address + INIT_STATE_OFFSET) & 0xff;
    }

    public void setInitState(int state) {
        unsafe.putByte(this.address + INIT_STATE_OFFSET, (byte) (state & 0xff));
    }

    public static final int classState_allocated = JVM.intConstant("InstanceKlass::allocated");
    public static final int classState_loaded = JVM.intConstant("InstanceKlass::loaded");
    public static final int classState_linked = JVM.intConstant("InstanceKlass::linked");
    public static final int classState_being_initialized = JVM.intConstant("InstanceKlass::being_initialized");
    public static final int classState_fully_initialized = JVM.intConstant("InstanceKlass::fully_initialized");
    public static final int classState_initialization_error = JVM.intConstant("InstanceKlass::initialization_error");

    public boolean is_loaded() {
        return this.getInitState() >= classState_loaded;
    }

    public boolean is_linked() {
        return this.getInitState() >= classState_linked;
    }

    public boolean is_initialized() {
        return this.getInitState() == classState_fully_initialized;
    }

    public boolean is_not_initialized() {
        return this.getInitState() < classState_being_initialized;
    }

    public boolean is_being_initialized() {
        return this.getInitState() == classState_being_initialized;
    }

    public boolean is_in_error_state() {
        return this.getInitState() == classState_initialization_error;
    }

    public ReferenceType getReferenceType(){
        return ReferenceType.of(unsafe.getByte(this.address+REFERENCE_TYPE_OFFSET)&0xff);
    }

    public void setReferenceType(int type) {
        unsafe.putByte(this.address+REFERENCE_TYPE_OFFSET,(byte)(type&0xff));
    }

    public int getMiscFlags() {
        return unsafe.getShort(this.address + MISC_FLAGS_OFFSET) & 0xffff;
    }

    public void setMiscFlags(int flags) {
        unsafe.putShort(this.address + MISC_FLAGS_OFFSET, (short) (flags & 0xffff));
    }

    @Nullable
    public Thread getInitThread() {
        long addr = unsafe.getAddress(this.address + INIT_THREAD_OFFSET);
        if (addr == 0L) {
            return null;
        }
        return Thread.getOrCreate(addr);
    }

    public void setInitThread(@Nullable Thread thread){
        unsafe.putAddress(this.address+INIT_THREAD_OFFSET,thread==null?0L:thread.address);
    }

    @Nullable
    public NMethod getOsrNMethodHead() {
        long addr = unsafe.getAddress(this.address + OSR_NMETHOD_HEAD_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.headCache, addr)) {
            this.headCache = new NMethod(addr);
        }
        return this.headCache;
    }

    public void setOsrNMethodHead(@Nullable NMethod nMethod) {
        unsafe.putAddress(this.address + OSR_NMETHOD_HEAD_OFFSET, nMethod == null ? 0L : nMethod.address);
    }

//    void InstanceKlass::add_osr_nmethod(nmethod* n) {
//        assert_lock_strong(CompiledMethod_lock);
//#ifndef PRODUCT
//        nmethod* prev = lookup_osr_nmethod(n->method(), n->osr_entry_bci(), n->comp_level(), true);
//        assert(prev == NULL || !prev->is_in_use() COMPILER2_PRESENT(|| StressRecompilation),
//        "redundant OSR recompilation detected. memory leak in CodeCache!");
//#endif
//        // only one compilation can be active
//        assert(n->is_osr_method(), "wrong kind of nmethod");
//        n->set_osr_link(osr_nmethods_head());
//        set_osr_nmethods_head(n);
//        // Raise the highest osr level if necessary
//        n->method()->set_highest_osr_comp_level(MAX2(n->method()->highest_osr_comp_level(), n->comp_level()));
//
//        // Get rid of the osr methods for the same bci that have lower levels.
//        for (int l = CompLevel_limited_profile; l < n->comp_level(); l++) {
//            nmethod *inv = lookup_osr_nmethod(n->method(), n->osr_entry_bci(), l, true);
//            if (inv != NULL && inv->is_in_use()) {
//                inv->make_not_entrant();
//            }
//        }
//    }

    public boolean remove_osr_nmethod(NMethod n) {
        if (!n.isOsrMethod()) {
            throw new IllegalArgumentException("wrong kind of nmethod");
        }
        NMethod last = null;
        NMethod cur = this.getOsrNMethodHead();
        int max_level = CompLevel.NONE.id;
        Method m = n.getMethod();
        boolean found = false;
        while (cur != null && !cur.equals(n)) {
            if (Objects.equals(m, cur.getMethod())) {
                max_level = Math.max(max_level, cur.getCompLevel().id);
            }
            last = cur;
            cur = cur.getNext();
        }
        NMethod next = null;
        if (Objects.equals(cur, n)) {
            found = true;
            next = cur.getNext();
            if (last == null) {
                this.setOsrNMethodHead(next);
            } else {
                last.setNext(next);
            }
        }
        n.setNext(null);
        cur = next;
        while (cur != null) {
            if (Objects.equals(m, cur.getMethod())) {
                max_level = Math.max(max_level, cur.getCompLevel().id);
            }
            cur = cur.getNext();
        }
        m.setHighestOsrCompLevel(CompLevel.of(max_level));
        return found;
    }

    public int markOsrNMethods(Method m) {
        NMethod osr = this.getOsrNMethodHead();
        int found = 0;
        while (osr != null) {
            if (isEqual(osr.getMethod(), m.address)) {
                osr.markForDeoptimization(true);
                found++;
            }
            osr = osr.getNext();
        }
        return found;
    }

//    public NMethod lookup_osr_nmethod(Method m, int bci, CompLevel comp_level, boolean match_level){
//        NMethod osr = this.getOsrNMethodHead();
//        NMethod best = null;
//        while (osr != null) {
//            if (osr.getMethod() == m &&
//                    (bci == JVM.invocationEntryBci || osr.getEntryBci() == bci)) {
//                if (match_level) {
//                    if (osr.getCompLevel()== comp_level) {
//                        return osr;
//                    }
//                } else {
//                    if (best == null || (osr.getCompLevel().id > best.getCompLevel().id)) {
//                        if (osr.getCompLevel() == CompilationPolicy::highest_compile_level()) {
//                            // Found the best possible - return it.
//                            return osr;
//                        }
//                        best = osr;
//                    }
//                }
//            }
//            osr = osr.getNext();
//        }
//
//
//        if (best != null && best.getCompLevel().id>=comp_level.id) {
//            return best;
//        }
//        return null;
//    }

    public Method getMethod(String name, String desc) {
        ConstMethod tmp;
        for (Method method : this.getMethods()) {
            tmp = method.getConstMethod();
            if (tmp.getName().toString().equals(name) && tmp.getSignature().toString().equals(desc)) {
                return method;
            }
        }
        if (this.getDefaultMethods() != null) {
            for (Method method : this.getMethods()) {
                tmp = method.getConstMethod();
                if (tmp.getName().toString().equals(name) && tmp.getSignature().toString().equals(desc)) {
                    return method;
                }
            }
        }
        return null;
    }

    public VMTypeArray<Method> getMethods() {
        long addr = unsafe.getAddress(this.address + METHODS_OFFSET);
        if (!isEqual(this.methodsCache, addr)) {
            this.methodsCache = new VMTypeArray<>(addr, Method.class, Method::getOrCreate);
        }
        return this.methodsCache;
    }

    @Nullable
    public VMTypeArray<Method> getDefaultMethods() {
        long addr = unsafe.getAddress(this.address + DEFAULT_METHODS_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.defaultMethodsCache, addr)) {
            this.defaultMethodsCache = new VMTypeArray<>(addr, Method.class, Method::getOrCreate);
        }
        return this.defaultMethodsCache;
    }

    public VMTypeArray<InstanceKlass> getLocalInterfaces() {
        long addr = unsafe.getAddress(this.address + LOCAL_INTERFACES_OFFSET);
        if (!isEqual(this.localInterfacesCache, addr)) {
            this.localInterfacesCache = new VMTypeArray<>(addr, InstanceKlass.class, InstanceKlass::getOrCreate);
        }
        return this.localInterfacesCache;
    }

    public void setLocalInterfaces(VMTypeArray<InstanceKlass> localInterfaces) {
        this.localInterfacesCache = null;
        unsafe.putAddress(this.address+LOCAL_INTERFACES_OFFSET,localInterfaces.address);
    }

    //All interfaces
    public VMTypeArray<InstanceKlass> getTransitiveInterfaces() {
        long addr = unsafe.getAddress(this.address + TRANSITIVE_INTERFACES_OFFSET);
        if (!isEqual(this.transitiveInterfacesCache, addr)) {
            this.transitiveInterfacesCache = new VMTypeArray<>(addr, InstanceKlass.class, InstanceKlass::getOrCreate);
        }
        return this.transitiveInterfacesCache;
    }

    public void setTransitiveInterfaces(VMTypeArray<InstanceKlass> transitiveInterfaces) {
        this.transitiveInterfacesCache = null;
        unsafe.putAddress(this.address+TRANSITIVE_INTERFACES_OFFSET,transitiveInterfaces.address);
    }

    public U2Array getFields() {
        long addr = unsafe.getAddress(this.address + FIELDS_OFFSET);
        if (!isEqual(this.fieldsCache, addr)) {
            this.fieldsCache = new U2Array(addr);
        }
        return this.fieldsCache;
    }

    public void setFields(U2Array fields) {
        this.fieldsCache = null;
        unsafe.putAddress(this.address+FIELDS_OFFSET,fields.address);
    }

    @Override
    public void setAccessible() {
        super.setAccessible();
        for (Method method : this.getMethods()) {
            method.setAccessible();
        }
        if (this.getDefaultMethods() != null) {
            for (Method method : this.getDefaultMethods()) {
                method.setAccessible();
            }
        }
        for(AllFieldStream stream=new AllFieldStream(this);!stream.done();stream.next()){
            stream.field().setAccessible();
        }
        this.updateReflectionData();
    }

    @Nullable
    public BreakpointInfo getBreakpointInfo() {
        if (!JVM.isJVMTISupported) {
            return null;
        }
        long addr = unsafe.getAddress(this.address + BREAKPOINTS_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.breakpointInfoCache, addr)) {
            this.breakpointInfoCache = BreakpointInfo.of(addr);
        }
        return this.breakpointInfoCache;
    }

    public void setBreakpointInfo(@Nullable BreakpointInfo info) {
        if (JVM.isJVMTISupported) {
            unsafe.putAddress(this.address + BREAKPOINTS_OFFSET, info == null ? 0L : info.address);
        }
    }

    public void setIsContended(boolean value)        {
        if (value) {
            this.setMiscFlags(this.getMiscFlags()|MiscFlags.IS_CONTENDED);
        } else {
            this.setMiscFlags(this.getMiscFlags()&~MiscFlags.IS_CONTENDED);
        }
    }

    @Override
    public boolean isAssignableFrom(Klass klass) {
        if (super.isAssignableFrom(klass)) {
            return true;
        }
        for (InstanceKlass klass1 : this.getLocalInterfaces()) {
            if (klass1.isAssignableFrom(klass)) {
                return true;
            }
        }
        return false;
    }

    public int getSizeHelper(){
        return Klass.layout_helper_to_size_helper(this.getLayout());
    }

    public int nonstatic_oop_map_count(){
        return this.getNonstaticOopMapSize()/OopMapBlock.size_in_words();
    }

    @Override
    public String toString() {
        return "Instance" + super.toString();
    }

    public static final class MiscFlags {
        public static final int REWRITTEN = JVM.intConstant("InstanceKlass::_misc_rewritten");
        public static final int HAS_NONSTATIC_FIELDS = JVM.intConstant("InstanceKlass::_misc_has_nonstatic_fields");
        public static final int SHOULD_VERIFY_CLASS = JVM.intConstant("InstanceKlass::_misc_should_verify_class");
        public static final int IS_CONTENDED = JVM.intConstant("InstanceKlass::_misc_is_contended");
        public static final int HAS_NONSTATIC_CONCRETE_METHODS = JVM.intConstant("InstanceKlass::_misc_has_nonstatic_concrete_methods");
        public static final int DECLARES_NONSTATIC_CONCRETE_METHODS = JVM.intConstant("InstanceKlass::_misc_declares_nonstatic_concrete_methods");
        public static final int HAS_BEEN_REDEFINED = JVM.intConstant("InstanceKlass::_misc_has_been_redefined");
        public static final int IS_SCRATCH_CLASS = JVM.intConstant("InstanceKlass::_misc_is_scratch_class");
        public static final int IS_SHARED_BOOT_CLASS = JVM.intConstant("InstanceKlass::_misc_is_shared_boot_class");
        public static final int IS_SHARED_PLATFORM_CLASS = JVM.intConstant("InstanceKlass::_misc_is_shared_platform_class");
        public static final int IS_SHARED_APP_CLASS = JVM.intConstant("InstanceKlass::_misc_is_shared_app_class");
    }
}