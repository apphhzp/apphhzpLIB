package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.classfile.ModuleEntry;
import apphhzp.lib.hotspot.classfile.PackageEntry;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.interpreter.OopMapCache;
import apphhzp.lib.hotspot.memory.ReferenceType;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.method.ConstMethod;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.Thread;
import apphhzp.lib.hotspot.runtime.fieldDescriptor;
import apphhzp.lib.hotspot.stream.AllFieldStream;
import apphhzp.lib.hotspot.stream.JavaFieldStream;
import apphhzp.lib.hotspot.util.RawCType;

import javax.annotation.Nullable;
import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.helfy.JVM.oopSize;
import static apphhzp.lib.hotspot.oops.klass.InstanceKlass.ClassState.*;

public class InstanceKlass extends Klass {
    public static final Type TYPE = JVM.type("InstanceKlass");
    public static final int SIZE = TYPE.size;
    public static final long ANNOTATIONS_OFFSET=TYPE.offset("_annotations");
    public static final long PACKAGE_ENTRY_OFFSET=JVM.computeOffset(oopSize,ANNOTATIONS_OFFSET+oopSize);
    public static final long ARRAY_KLASSES_OFFSET=JVM.computeOffset(oopSize,PACKAGE_ENTRY_OFFSET+oopSize);
    public static final long CONSTANTS_OFFSET = TYPE.offset("_constants");
    public static final long INNER_CLASSES_OFFSET=TYPE.offset("_inner_classes");
    public static final long NEST_MEMBERS_OFFSET=JVM.computeOffset(oopSize,INNER_CLASSES_OFFSET+oopSize);
    public static final long NEST_HOST_OFFSET=JVM.computeOffset(oopSize,NEST_MEMBERS_OFFSET+oopSize);
    public static final long PERMITTED_SUBCLASSES_OFFSET=JVM.computeOffset(oopSize,NEST_HOST_OFFSET+oopSize);
    public static final long RECORD_COMPONENTS_OFFSET=JVM.computeOffset(oopSize,PERMITTED_SUBCLASSES_OFFSET+oopSize);
    public static final long SOURCE_DEBUG_EXTENSION_OFFSET=TYPE.offset("_source_debug_extension");
    public static final long NONSTATIC_FIELD_SIZE_OFFSET=TYPE.offset("_nonstatic_field_size");
    public static final long STATIC_FIELD_SIZE_OFFSET=TYPE.offset("_static_field_size");
    public static final long NONSTATIC_OOP_MAP_SIZE_OFFSET=TYPE.offset("_nonstatic_oop_map_size");
    public static final long ITABLE_LEN_OFFSET=TYPE.offset("_itable_len");
    public static final long NEST_HOST_INDEX_OFFSET=JVM.computeOffset(2,ITABLE_LEN_OFFSET+JVM.intSize);
    public static final long THIS_CLASS_INDEX_OFFSET=JVM.computeOffset(2,NEST_HOST_INDEX_OFFSET+2);
    public static final long STATIC_OOP_FIELD_COUNT_OFFSET=TYPE.offset("_static_oop_field_count");
    public static final long FIELDS_COUNT_OFFSET = TYPE.offset("_java_fields_count");
    public static final long IDNUM_ALLOCATED_COUNT_OFFSET=TYPE.offset("_idnum_allocated_count");
    public static final long IS_MARKED_DEPENDENT_OFFSET=TYPE.offset("_is_marked_dependent");
    public static final long INIT_STATE_OFFSET = TYPE.offset("_init_state");
    public static final long REFERENCE_TYPE_OFFSET=TYPE.offset("_reference_type");
    public static final long KIND_OFFSET=JVM.computeOffset(1,REFERENCE_TYPE_OFFSET+1);
    public static final long MISC_FLAGS_OFFSET = TYPE.offset("_misc_flags");
    public static final long INIT_THREAD_OFFSET = TYPE.offset("_init_thread");
    public static final long OOP_MAP_CACHE_OFFSET=TYPE.offset("_oop_map_cache");
    public static final long JNI_IDS_OFFSET=TYPE.offset("_jni_ids");
    public static final long METHODS_JMETHOD_IDS_OFFSET=TYPE.offset("_methods_jmethod_ids");
    public static final long OSR_NMETHOD_HEAD_OFFSET = TYPE.offset("_osr_nmethods_head");
    public static final long BREAKPOINTS_OFFSET = JVM.includeJVMTI ? TYPE.offset("_breakpoints") : -1;
    public static final long PREVIOUS_VERSIONS_OFFSET=JVM.includeJVMTI ?JVM.computeOffset(oopSize,BREAKPOINTS_OFFSET+oopSize):-1;
    public static final long CACHED_CLASS_FILE_OFFSET=JVM.includeJVMTI?JVM.computeOffset(oopSize,PREVIOUS_VERSIONS_OFFSET+oopSize):-1;
    public static final long JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET=JVM.includeJVMTI?JVM.computeOffset(oopSize,CACHED_CLASS_FILE_OFFSET+oopSize):-1;
    public static final long VERIFY_COUNT_OFFSET=JVM.product?-1:JVM.computeOffset(JVM.intSize,JVM.includeJVMTI?JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET+oopSize:OSR_NMETHOD_HEAD_OFFSET+oopSize);
    public static final long METHODS_OFFSET = TYPE.offset("_methods");
    public static final long DEFAULT_METHODS_OFFSET = TYPE.offset("_default_methods");
    public static final long LOCAL_INTERFACES_OFFSET = TYPE.offset("_local_interfaces");
    public static final long TRANSITIVE_INTERFACES_OFFSET = TYPE.offset("_transitive_interfaces");
    public static final long METHOD_ORDERING_OFFSET=TYPE.offset("_method_ordering");
    public static final long DEFAULT_VTABLE_INDICES_OFFSET=TYPE.offset("_default_vtable_indices");
    public static final long FIELDS_OFFSET = TYPE.offset("_fields");
    private PackageEntry packageCache;
    private ConstantPool constantPoolCache;
    private U2Array innerClassesCache;
    private U2Array nestMembersCache;
    private NMethod headCache;
    private VMTypeArray<Method> methodsCache;
    private VMTypeArray<Method> defaultMethodsCache;
    private VMTypeArray<InstanceKlass> localInterfacesCache;
    private VMTypeArray<InstanceKlass> transitiveInterfacesCache;
    private BreakpointInfo breakpointInfoCache;
    private IntArray methodOrderingCache;
    private IntArray defaultVTableIndicesCache;
    private U2Array fieldsCache;
    static {
        if (JVM.computeOffset(oopSize,ARRAY_KLASSES_OFFSET+oopSize)!=CONSTANTS_OFFSET){
            throw new AssertionError();
        }
        if (JVM.computeOffset(oopSize,RECORD_COMPONENTS_OFFSET+oopSize)!=SOURCE_DEBUG_EXTENSION_OFFSET){
            throw new AssertionError();
        }
        if (JVM.computeOffset(2,THIS_CLASS_INDEX_OFFSET+2)!=STATIC_OOP_FIELD_COUNT_OFFSET){
            throw new AssertionError();
        }
        if (VERIFY_COUNT_OFFSET!=-1){
            if (JVM.computeOffset(oopSize,VERIFY_COUNT_OFFSET+JVM.intSize)!=METHODS_OFFSET){
                throw new AssertionError();
            }
        }else if (JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET!=-1){
            if (JVM.computeOffset(oopSize,JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET+oopSize)!=METHODS_OFFSET){
                throw new AssertionError();
            }
        }
    }

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
    @Nullable
    public PackageEntry getPackage(){
        long addr=unsafe.getAddress(this.address+PACKAGE_ENTRY_OFFSET);
        if (addr==0){
            return null;
        }
        if (!isEqual(this.packageCache,addr)){
            this.packageCache=new PackageEntry(addr);
        }
        return this.packageCache;
    }

    public boolean in_unnamed_package(){ return (unsafe.getAddress(this.address+PACKAGE_ENTRY_OFFSET) == 0L); }

    public ConstantPool getConstantPool() {
        long addr = unsafe.getAddress(this.address + CONSTANTS_OFFSET);
        if (!JVMObject.isEqual(this.constantPoolCache, addr)) {
            this.constantPoolCache = ConstantPool.getOrCreate(addr);
        }
        return this.constantPoolCache;
    }

    public void setConstantPool(ConstantPool pool) {
        unsafe.putAddress(this.address + CONSTANTS_OFFSET, pool.address);
        VMTypeArray<Method> methods = this.getMethods();
        for (Method method : methods) {
            method.constMethod().set_constants(pool);
        }
        methods = this.getDefaultMethods();
        if (methods != null) {
            for (Method method : methods) {
                method.constMethod().set_constants(pool);
            }
        }
    }

    public Annotations annotations(){
        long addr=unsafe.getAddress(this.address+ANNOTATIONS_OFFSET);
        if (addr==0L){
            return null;
        }
        return new Annotations(addr);
    }

    public U2Array getInnerClasses() {
        long addr = unsafe.getAddress(this.address + INNER_CLASSES_OFFSET);
        if (!JVMObject.isEqual(this.innerClassesCache,addr)){
            this.innerClassesCache = new U2Array(addr);
        }
        return this.innerClassesCache;
    }

    public void setInnerClasses(@Nullable U2Array innerClasses) {
        this.innerClassesCache=null;
        unsafe.putAddress(this.address+INNER_CLASSES_OFFSET,innerClasses==null?0L:innerClasses.address);
    }

    public U2Array getNestMembers(){
        long addr=unsafe.getAddress(this.address+NEST_MEMBERS_OFFSET);
        if (!JVMObject.isEqual(this.nestMembersCache,addr)){
            this.nestMembersCache = new U2Array(addr);
        }
        return this.nestMembersCache;
    }

    public void setNestMembers(U2Array array){
        unsafe.putAddress(this.address+NEST_MEMBERS_OFFSET,array==null?0L:array.address);
    }

    public int getNestHostIndex(){
        return unsafe.getShort(this.address+NEST_HOST_INDEX_OFFSET)&0xffff;
    }

    public void setNestHostIndex(int index){
        unsafe.putShort(this.address+NEST_HOST_INDEX_OFFSET,(short)(index&0xffff));
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

    public int java_fields_count() {
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

    public @RawCType("ClassState") int init_state() {
        return unsafe.getByte(this.address + INIT_STATE_OFFSET) & 0xff;
    }

    public void set_init_state(int state) {
        unsafe.putByte(this.address + INIT_STATE_OFFSET, (byte) (state & 0xff));
    }
    public final static class ClassState{
        public static final int allocated = JVM.intConstant("InstanceKlass::allocated");
        public static final int loaded = JVM.intConstant("InstanceKlass::loaded");
        public static final int linked = JVM.intConstant("InstanceKlass::linked");
        public static final int being_initialized = JVM.intConstant("InstanceKlass::being_initialized");
        public static final int fully_initialized = JVM.intConstant("InstanceKlass::fully_initialized");
        public static final int initialization_error = JVM.intConstant("InstanceKlass::initialization_error");
    }

    public boolean is_loaded()                   { return init_state() >= loaded; }
    public boolean is_linked()                   { return init_state() >= linked; }
    public boolean is_initialized()              { return init_state() == fully_initialized; }
    public boolean is_not_initialized()          { return init_state() <  being_initialized; }
    public boolean is_being_initialized()        { return init_state() == being_initialized; }
    public boolean is_in_error_state()           { return init_state() == initialization_error; }

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

    public boolean isSharedBootClass() {
        return (this.getMiscFlags() & MiscFlags.IS_SHARED_BOOT_CLASS) != 0;
    }
    public boolean isSharedPlatformClass() {
        return (this.getMiscFlags() & MiscFlags.IS_SHARED_PLATFORM_CLASS) != 0;
    }
    public boolean isSharedAppClass() {
        return (this.getMiscFlags() & MiscFlags.IS_SHARED_APP_CLASS) != 0;
    }
    // The UNREGISTERED class loader type
    public boolean isSharedUnregisteredClass() {
        return (this.getMiscFlags() & sharedLoaderTypeBits()) == 0;
    }

    public boolean isShareable()  {
        if (JVM.includeCDS){
            ClassLoaderData loader_data = this.getClassLoaderData();
            if (!((loader_data.getClassLoader() == null ||
                    loader_data.getClassLoader()==ClassLoader.getSystemClassLoader() ||
                    loader_data.getClassLoader()==ClassLoader.getPlatformClassLoader()))) {
                return false;
            }

            if (this.isHidden()) {
                return false;
            }

            if (module().isPatched()) {
                return false;
            }

            return true;
        }else {
            return false;
        }
    }

    public ModuleEntry module() {
        if (this.isHidden() &&
                in_unnamed_package() &&
                this.getClassLoaderData().hasClassMirrorHolder()) {
            // For a non-strong hidden class defined to an unnamed package,
            // its (class held) CLD will not have an unnamed module created for it.
            // Two choices to find the correct ModuleEntry:
            // 1. If hidden class is within a nest, use nest host's module
            // 2. Find the unnamed module off from the class loader
            // For now option #2 is used since a nest host is not set until
            // after the instance class is created in jvm_lookup_define_class().
            if (this.getClassLoaderData().isBootClassLoaderData()) {
                return ClassLoaderData.nullClassLoaderData.getUnnamedModule();
            } else {
                throw new RuntimeException();
//                oop module = java_lang_ClassLoader::unnamedModule(class_loader_data()->class_loader());
//                assert(java_lang_Module::is_instance(module), "Not an instance of java.lang.Module");
//                return java_lang_Module::module_entry(module);
            }
        }

        // Class is in a named package
        if (!in_unnamed_package()) {
            return this.getPackage().getModule();
        }

        // Class is in an unnamed package, return its loader's unnamed module
        return this.getClassLoaderData().getUnnamedModule();
    }

    public void clearSharedClassLoaderType() {
        this.setMiscFlags(this.getMiscFlags()& ~sharedLoaderTypeBits());
    }

    public int sharedLoaderTypeBits() {
        return MiscFlags.IS_SHARED_BOOT_CLASS|MiscFlags.IS_SHARED_PLATFORM_CLASS|MiscFlags.IS_SHARED_APP_CLASS;
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


    public OopMapCache oop_map_cache(){
        return new OopMapCache(unsafe.getAddress(this.address+OOP_MAP_CACHE_OFFSET));
    }
    public void set_oop_map_cache(OopMapCache cache){
        unsafe.putAddress(this.address+OOP_MAP_CACHE_OFFSET,cache==null?0L:cache.address);
    }

    @Nullable
    public NMethod getOsrNMethodHead() {
        long addr = unsafe.getAddress(this.address + OSR_NMETHOD_HEAD_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!JVMObject.isEqual(this.headCache, addr)) {
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
            tmp = method.constMethod();
            if (tmp.getName().toString().equals(name) && tmp.getSignature().toString().equals(desc)) {
                return method;
            }
        }
        if (this.getDefaultMethods() != null) {
            for (Method method : this.getMethods()) {
                tmp = method.constMethod();
                if (tmp.getName().toString().equals(name) && tmp.getSignature().toString().equals(desc)) {
                    return method;
                }
            }
        }
        return null;
    }

    public Method methodWithIdNum(int idnum) {
        Method m = null;
        VMTypeArray<Method> methods = this.getMethods();
        if (idnum < methods.length()) {
            m = methods.get(idnum);
        }
        if (m == null || m.constMethod().getMethodIdnum() != idnum) {
//            for (int index = 0; index < methods()->length(); ++index) {
//                m = methods()->at(index);
//                if (m->method_idnum() == idnum) {
//                    return m;
//                }
//            }
            for (Method method : methods) {
                if (method.constMethod().getMethodIdnum() == idnum) {
                    return method;
                }
            }
            // None found, return null for the caller to handle.
            return null;
        }
        return m;
    }

    public VMTypeArray<Method> getMethods() {
        long addr = unsafe.getAddress(this.address + METHODS_OFFSET);
        if (!JVMObject.isEqual(this.methodsCache, addr)) {
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
        if (!JVMObject.isEqual(this.defaultMethodsCache, addr)) {
            this.defaultMethodsCache = new VMTypeArray<>(addr, Method.class, Method::getOrCreate);
        }
        return this.defaultMethodsCache;
    }

    public VMTypeArray<InstanceKlass> local_interfaces() {
        long addr = unsafe.getAddress(this.address + LOCAL_INTERFACES_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!JVMObject.isEqual(this.localInterfacesCache, addr)) {
            this.localInterfacesCache = new VMTypeArray<>(addr, InstanceKlass.class, InstanceKlass::getOrCreate);
        }
        return this.localInterfacesCache;
    }

    //All interfaces
    public VMTypeArray<InstanceKlass> getTransitiveInterfaces() {
        long addr = unsafe.getAddress(this.address + TRANSITIVE_INTERFACES_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!JVMObject.isEqual(this.transitiveInterfacesCache, addr)) {
            this.transitiveInterfacesCache = new VMTypeArray<>(addr, InstanceKlass.class, InstanceKlass::getOrCreate);
        }
        return this.transitiveInterfacesCache;
    }

    public U2Array getFields() {
        long addr = unsafe.getAddress(this.address + FIELDS_OFFSET);
        if (!JVMObject.isEqual(this.fieldsCache, addr)) {
            this.fieldsCache = new U2Array(addr);
        }
        return this.fieldsCache;
    }

    public void setFields(U2Array fields) {
        this.fieldsCache = null;
        unsafe.putAddress(this.address+FIELDS_OFFSET,fields.address);
    }

    public IntArray getMethodOrdering(){
        long addr = unsafe.getAddress(this.address + METHOD_ORDERING_OFFSET);
        if (!JVMObject.isEqual(this.methodOrderingCache, addr)) {
            this.methodOrderingCache = new IntArray(addr);
        }
        return this.methodOrderingCache;
    }

    public void setMethodOrdering(IntArray methodOrdering) {
        unsafe.putAddress(this.address+METHOD_ORDERING_OFFSET,methodOrdering.address);
    }

    public int     getFieldOffset      (int index) { return field(index).offset(); }
    public AccessFlags     getFieldAccessFlags(int index) { return field(index).getAccessFlags(); }
    public Symbol getFieldName        (int index) { return field(index).name(this.getConstantPool()); }
    public Symbol getFieldSignature   (int index) { return field(index).signature(this.getConstantPool()); }


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
        if (!JVM.includeJVMTI) {
            return null;
        }
        long addr = unsafe.getAddress(this.address + BREAKPOINTS_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!JVMObject.isEqual(this.breakpointInfoCache, addr)) {
            this.breakpointInfoCache = BreakpointInfo.of(addr);
        }
        return this.breakpointInfoCache;
    }

    public void setBreakpointInfo(@Nullable BreakpointInfo info) {
        if (JVM.includeJVMTI) {
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
        for (InstanceKlass klass1 : this.getTransitiveInterfaces()) {
            if (klass1.equals(klass)) {
                return true;
            }
        }
        return false;
    }

    public int getSizeHelper(){
        return Klass.LayoutHelper.toSizeHelper(this.getLayout());
    }

    public int nonstatic_oop_map_count(){
        return this.getNonstaticOopMapSize()/ OopMapBlock.size_in_words();
    }

    public long oopSize(OopDesc oop) {
        return (long) this.getSizeHelper() * oopSize;
    }

    public static int linear_search(VMTypeArray<Method> methods,
                          Symbol name,
                          Symbol signature) {
        final int len = methods.length();
        for (int index = 0; index < len; index++) {
            final Method m = methods.get(index);
            if (m.signature().equals(signature) && m.name().equals(name)){
                return index;
            }
        }
        return -1;
    }

    public static int linear_search(VMTypeArray<Method> methods, Symbol name) {
        int len = methods.length();
        int l = 0;
        int h = len - 1;
        while (l <= h) {
            Method m = methods.get(l);
            if (m.name().equals(name)){
                return l;
            }
            l++;
        }
        return -1;
    }
    public static int quick_search(VMTypeArray<Method> methods, Symbol name) {

        if (true/*_disable_method_binary_search*/) {
            //assert(DynamicDumpSharedSpaces, "must be");
            // At the final stage of dynamic dumping, the methods array may not be sorted
            // by ascending addresses of their names, so we can't use binary search anymore.
            // However, methods with the same name are still laid out consecutively inside the
            // methods array, so let's look for the first one that matches.
            return linear_search(methods, name);
        }
        throw new RuntimeException("How did you get here?");
//        int len = methods.length();
//        int l = 0;
//        int h = len - 1;
//
//        // methods are sorted by ascending addresses of their names, so do binary search
//        while (l <= h) {
//            int mid = (l + h) >> 1;
//            Method m = methods.get(mid);
//            //assert(m->is_method(), "must be method");
//            int res = m.name().fast_compare(name);
//            if (res == 0) {
//                return mid;
//            } else if (res < 0) {
//                l = mid + 1;
//            } else {
//                h = mid - 1;
//            }
//        }
//        return -1;
    }

    public int find_method_by_name(Symbol name, int[] end) {
        return find_method_by_name(this.getMethods(), name, end);
    }

    public static int find_method_by_name(VMTypeArray<Method> methods,
                                       final Symbol name,
                                           int[] end_ptr) {
        if (end_ptr==null){
            throw new IllegalArgumentException("just checking");
        }
        int start = quick_search(methods, name);
        int end = start + 1;
        if (start != -1) {
            while (start - 1 >= 0 && (methods.get(start - 1)).name() == name) --start;
            while (end < methods.length() && (methods.get(end)).name() == name) ++end;
            end_ptr[0] = end;
            return start;
        }
        return -1;
    }

    public Method find_method(final Symbol name,final Symbol signature) {
        return find_method_impl(name, signature,
                OverpassLookupMode.find,
                StaticLookupMode.find,
                PrivateLookupMode.find);
    }

    // Find looks up the name/signature in the local methods array
    // and filters on the overpass, static and private flags
    // This returns the first one found
    // note that the local methods array can have up to one overpass, one static
    // and one instance (private or not) with the same name/signature
    public Method find_local_method(Symbol name,
                             Symbol signature,
                                             @RawCType("OverpassLookupMode")int overpass_mode,
                                             @RawCType("StaticLookupMode")int static_mode,
                                             @RawCType("PrivateLookupMode")int private_mode){
        return InstanceKlass.find_method_impl(this.getMethods(),
                name,
                signature,
                overpass_mode,
                static_mode,
                private_mode);
    }

    // Find looks up the name/signature in the local methods array
    // and filters on the overpass, static and private flags
    // This returns the first one found
    // note that the local methods array can have up to one overpass, one static
    // and one instance (private or not) with the same name/signature
    public static Method find_local_method(VMTypeArray<Method> methods,
                                          Symbol name,
                                          Symbol signature,
                                             @RawCType("OverpassLookupMode")int overpass_mode,
                                             @RawCType("StaticLookupMode")int static_mode,
                                             @RawCType("PrivateLookupMode")int private_mode) {
        return InstanceKlass.find_method_impl(methods,
                name,
                signature,
                overpass_mode,
                static_mode,
                private_mode);
    }

    public static Method find_method(VMTypeArray<Method> methods,
                                    Symbol name,
                                    Symbol signature) {
        return InstanceKlass.find_method_impl(methods,
                name,
                signature,
                OverpassLookupMode.find,
                StaticLookupMode.find,
                PrivateLookupMode.find);
    }


    public Method find_method_impl(final Symbol name,
                                   final Symbol signature,
                                   @RawCType("OverpassLookupMode")int overpass_mode,
                                   @RawCType("StaticLookupMode")int static_mode,
                                   @RawCType("PrivateLookupMode")int private_mode){
        return InstanceKlass.find_method_impl(this.getMethods(),
                name,
                signature,
                overpass_mode,
                static_mode,
                private_mode);
    }

    public static Method find_method_impl(VMTypeArray<Method> methods,
                                        final Symbol name,
                                        final Symbol signature,
                                          @RawCType("OverpassLookupMode")int overpass_mode,
                                          @RawCType("StaticLookupMode")int static_mode,
                                          @RawCType("PrivateLookupMode")int private_mode) {
        int hit = find_method_index(methods, name, signature, overpass_mode, static_mode, private_mode);
        return hit >= 0 ? methods.get(hit): null;
    }

    // Used directly for default_methods to find the index into the
// default_vtable_indices, and indirectly by find_method
// find_method_index looks in the local methods array to return the index
// of the matching name/signature. If, overpass methods are being ignored,
// the search continues to find a potential non-overpass match.  This capability
// is important during method resolution to prefer a static method, for example,
// over an overpass method.
// There is the possibility in any _method's array to have the same name/signature
// for a static method, an overpass method and a local instance method
// To correctly catch a given method, the search criteria may need
// to explicitly skip the other two. For local instance methods, it
// is often necessary to skip private methods
    public static int find_method_index(VMTypeArray<Method> methods, final Symbol name, final Symbol signature, @RawCType("OverpassLookupMode")int overpass_mode, @RawCType("StaticLookupMode")int static_mode, @RawCType("PrivateLookupMode")int private_mode) {
        final boolean skipping_overpass = (overpass_mode == OverpassLookupMode.skip);
        final boolean skipping_static = (static_mode == StaticLookupMode.skip);
        final boolean skipping_private = (private_mode == PrivateLookupMode.skip);
        final int hit = quick_search(methods, name);
        if (hit != -1) {
            Method m = methods.get(hit);

            // Do linear search to find matching signature.  First, quick check
            // for common case, ignoring overpasses if requested.
            if (method_matches(m, signature, skipping_overpass, skipping_static, skipping_private)) {
                return hit;
            }

            // search downwards through overloaded methods
            int i;
            for (i = hit - 1; i >= 0; --i) {
                 m = methods.get(i);
                if (!m.name().equals(name)) {
                    break;
                }
                if (method_matches(m, signature, skipping_overpass, skipping_static, skipping_private)) {
                    return i;
                }
            }
            // search upwards
            for (i = hit + 1; i < methods.length(); ++i) {
                m = methods.get(i);
                if (!m.name().equals(name)){
                    break;
                }
                if (method_matches(m, signature, skipping_overpass, skipping_static, skipping_private)) {
                    return i;
                }
            }
            // not found
            if (JVM.ENABLE_EXTRA_CHECK){
                final int index = (skipping_overpass || skipping_static || skipping_private) ? -1 :
                        linear_search(methods, name, signature);
                if (!(-1 == index)){
                    throw new RuntimeException("binary search should have found entry "+index);
                }
            }
        }
        return -1;
    }

    // true if method matches signature and conforms to skipping_X conditions.
    public static boolean method_matches( Method m, Symbol signature,
                               boolean skipping_overpass,
                               boolean skipping_static,
                               boolean skipping_private) {
        return ((m.signature().equals(signature)) &&
                (!skipping_overpass || !m.is_overpass()) &&
                (!skipping_static || !m.is_static()) &&
                (!skipping_private || !m.is_private()));
    }

    @Override
    public String internal_name() {
        return this.external_name();
    }
    public void oop_print_value_on(OopDesc obj, PrintStream st) {
        st.print("a ");
        name().print_value_on(st);
        obj.print_address_on(st);
//        if (this.asClass() == String.class
//                && (obj.getObject()!=null)) {
//            int len = ((String)obj.getObject()).length();
//            int plen = (len < 24 ? len : 12);
//            //char* str = java_lang_String::as_utf8_string(obj, 0, plen);
//            st.printf(" = \"%s\"", ((String)obj.getObject()));
//            if (len > plen)
//                st.printf("...[%d]", len);
//        } else if (this.asClass() == Class.class) {
//            Klass k = Klass.asKlass(obj.getObject());
//            st.print(" = ");
//            if (k != null) {
//                k.print_value_on(st);
//            } else {
//                String tname = JVM.type2name(JavaClasses.Class.primitive_type(obj));
//                st.printf("%s", tname!=null ? tname : "type?");
//            }
//        } else if (this.asClass() == MethodType.class) {
//            st.print(" = ");
//            java_lang_invoke_MethodType::print_signature(obj, st);
//        } else if (java_lang_boxing_object::is_instance(obj)) {
//            st->print(" = ");
//            java_lang_boxing_object::print(obj, st);
//        } else if (this == vmClasses::LambdaForm_klass()) {
//            oop vmentry = java_lang_invoke_LambdaForm::vmentry(obj);
//            if (vmentry != NULL) {
//                st->print(" => ");
//                vmentry->print_value_on(st);
//            }
//        } else if (this == vmClasses::MemberName_klass()) {
//            Metadata* vmtarget = java_lang_invoke_MemberName::vmtarget(obj);
//            if (vmtarget != NULL) {
//                st->print(" = ");
//                vmtarget->print_value_on(st);
//            } else {
//                oop clazz = java_lang_invoke_MemberName::clazz(obj);
//                oop name  = java_lang_invoke_MemberName::name(obj);
//                if (clazz != NULL) {
//                    clazz->print_value_on(st);
//                } else {
//                    st->print("NULL");
//                }
//                st->print(".");
//                if (name != null) {
//                    name.print_value_on(st);
//                } else {
//                    st.print("NULL");
//                }
//            }
//        }
    }

    public void print_value_on(PrintStream st){
        if (JVM.getFlag("Verbose").getBool()  || JVM.getFlag("WizardMode").getBool()) {
            getAccessFlags().print_on(st);
        }
        name().print_value_on(st);
    }


    @Override
    public String toString() {
        return "Instance" + super.toString();
    }

    public FieldInfo field(int index){
        return FieldInfo.from_field_array(this.getFields(), index);
    }
    public boolean is_record(){
        return asClass().isRecord();
    }

    public @RawCType("AnnotationArray*")U1Array class_annotations() {
        return (annotations() != null) ? annotations().classAnnotations() : null;
    }
    public @RawCType("Array<AnnotationArray*>*")VMTypeArray<U1Array> fields_annotations() {
        return (annotations() != null) ? annotations().fieldsAnnotations() : null;
    }
    public @RawCType("AnnotationArray*")U1Array class_type_annotations() {
        return (annotations() != null) ? annotations().classTypeAnnotations() : null;
    }
    public @RawCType("Array<AnnotationArray*>*")VMTypeArray<U1Array> fields_type_annotations() {
        return (annotations() != null) ? annotations().fieldsTypeAnnotations() : null;
    }

    public Klass find_field(Symbol name, Symbol sig, fieldDescriptor fd) {
        // search order according to newest JVM spec (5.4.3.2, p.167).
        // 1) search for field in current klass
        if (find_local_field(name, sig, fd)) {
            return (this);
        }
        // 2) search for field recursively in direct superinterfaces
        {
            Klass intf = find_interface_field(name, sig, fd);
            if (intf != null) return intf;
        }
        // 3) apply field lookup recursively if superclass exists
        {
            Klass supr = getSuperKlass();
            if (supr != null)
                return (supr.asInstanceKlass()).find_field(name, sig, fd);
        }
        // 4) otherwise field lookup fails
        return null;
    }


    public Klass find_field(Symbol name, Symbol sig, boolean is_static, fieldDescriptor fd) {
        // search order according to newest JVM spec (5.4.3.2, p.167).
        // 1) search for field in current klass
        if (find_local_field(name, sig, fd)) {
            if (fd.is_static() == is_static)
                return (this);
        }
        // 2) search for field recursively in direct superinterfaces
        if (is_static) {
            Klass intf = find_interface_field(name, sig, fd);
            if (intf != null) return intf;
        }
        // 3) apply field lookup recursively if superclass exists
        {
            Klass supr = getSuperKlass();
            if (supr != null)
                return supr.asInstanceKlass().find_field(name, sig, is_static, fd);
        }
        // 4) otherwise field lookup fails
        return null;
    }

    public boolean find_local_field(Symbol name, Symbol sig, fieldDescriptor fd){
        for (JavaFieldStream fs=new JavaFieldStream (this); !fs.done(); fs.next()) {
            Symbol f_name = fs.name();
            Symbol f_sig  = fs.signature();
            if (f_name.equals(name)&& f_sig.equals(sig)) {
                fd.reinitialize((this), fs.index());
                return true;
            }
        }
        return false;
    }
    public Klass find_interface_field(Symbol name, Symbol sig, fieldDescriptor fd){
        final int n = local_interfaces().length();
        for (int i = 0; i < n; i++) {
            Klass intf1 = local_interfaces().get(i);
            if (JVM.ENABLE_EXTRA_CHECK&&!(intf1.isInterface())){
                throw new RuntimeException("just checking type");
            }
            // search for field in current interface
            if ((intf1.asInstanceKlass()).find_local_field(name, sig, fd)){
                if (!(fd.is_static())){
                    throw new RuntimeException("interface field must be static");
                }
                return intf1;
            }
            // search for field in direct superinterfaces
            Klass intf2 = (intf1.asInstanceKlass()).find_interface_field(name, sig, fd);
            if (intf2 != null) return intf2;
        }
        // otherwise field lookup fails
        return null;
    }

    // minor and major version numbers of class file
    public @RawCType("u2")int minor_version(){ return getConstantPool().minor_version(); }
    public void set_minor_version(@RawCType("u2")int minor_version) { getConstantPool().set_minor_version(minor_version); }
    public @RawCType("u2")int major_version(){ return getConstantPool().major_version(); }
    public void set_major_version(@RawCType("u2")int major_version) { getConstantPool().set_major_version(major_version); }

    public boolean is_reentrant_initialization(Thread thread){
        return (thread==null?0L:thread.address) == unsafe.getAddress(this.address+INIT_THREAD_OFFSET);
    }

    public boolean verify_itable_index(int i) {
        if (!JVM.ENABLE_EXTRA_CHECK){
            return true;
        }
        int method_count = KlassItable.method_count_for_interface(this);
        if (!(i >= 0 && i < method_count)){
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        return true;
    }

    // default method vtable_indices
    public IntArray default_vtable_indices(){
        long addr=unsafe.getAddress(this.address+DEFAULT_VTABLE_INDICES_OFFSET);
        if (addr==0L){
            return null;
        }
        return new IntArray(addr);
    }
    public void set_default_vtable_indices(IntArray v) {
        unsafe.putAddress(this.address+DEFAULT_VTABLE_INDICES_OFFSET,v==null?0L:v.address);
    }
    //public Array<int>* create_new_default_vtable_indices(int len, TRAPS);

    public int vtable_index_of_interface_method(Method intf_method) {
        if (!(is_linked())){
            throw new RuntimeException("required");
        }
        if (!(intf_method.method_holder().isInterface())){
            throw new RuntimeException("not an interface method");
        }
        //assert(is_subtype_of(intf_method.method_holder()), "interface not implemented");

        int vtable_index = Method.VtableIndexFlag.invalid_vtable_index;
        Symbol name = intf_method.name();
        Symbol signature = intf_method.signature();

        // First check in default method array
        if (!intf_method.is_abstract() && this.getDefaultMethods() != null) {
            int index = find_method_index(this.getDefaultMethods(),
                    name, signature,
                    Klass.OverpassLookupMode.find,
            Klass.StaticLookupMode.find,
                    Klass.PrivateLookupMode.find);
            if (index >= 0) {
                vtable_index = this.default_vtable_indices().get(index);
            }
        }
        if (vtable_index == Method.VtableIndexFlag.invalid_vtable_index) {
            // get vtable_index for miranda methods
            KlassVtable vt = vtable();
            vtable_index = vt.index_of_miranda(name, signature);
        }
        return vtable_index;
    }

    public void set_has_resolved_methods() {
        this.setMiscFlags(this.getMiscFlags()|MiscFlags.HAS_RESOLVED_METHODS);
    }


    public static final class MiscFlags {
        public static final int REWRITTEN = JVM.intConstant("InstanceKlass::_misc_rewritten");
        public static final int HAS_NONSTATIC_FIELDS = JVM.intConstant("InstanceKlass::_misc_has_nonstatic_fields");
        public static final int SHOULD_VERIFY_CLASS = JVM.intConstant("InstanceKlass::_misc_should_verify_class");
        public static final int IS_CONTENDED = JVM.intConstant("InstanceKlass::_misc_is_contended");
        public static final int HAS_NONSTATIC_CONCRETE_METHODS = JVM.intConstant("InstanceKlass::_misc_has_nonstatic_concrete_methods");
        public static final int DECLARES_NONSTATIC_CONCRETE_METHODS = JVM.intConstant("InstanceKlass::_misc_declares_nonstatic_concrete_methods");
        public static final int HAS_BEEN_REDEFINED = JVM.intConstant("InstanceKlass::_misc_has_been_redefined");
        public static final int SHARED_LOADING_FAILED=1 << 8;
        public static final int IS_SCRATCH_CLASS = JVM.intConstant("InstanceKlass::_misc_is_scratch_class");
        public static final int IS_SHARED_BOOT_CLASS = JVM.intConstant("InstanceKlass::_misc_is_shared_boot_class");
        public static final int IS_SHARED_PLATFORM_CLASS = JVM.intConstant("InstanceKlass::_misc_is_shared_platform_class");
        public static final int IS_SHARED_APP_CLASS = JVM.intConstant("InstanceKlass::_misc_is_shared_app_class");
        public static final int HAS_RESOLVED_METHODS=1 << 13;
        public static final int HAS_CONTENDED_ANNOTATIONS=1 << 14;
    }
}