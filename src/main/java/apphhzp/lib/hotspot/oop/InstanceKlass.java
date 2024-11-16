package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.compiler.CompLevel;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.method.ConstMethod;
import apphhzp.lib.hotspot.oop.method.Method;
import apphhzp.lib.hotspot.runtime.Thread;

import javax.annotation.Nullable;

import java.util.Objects;

import static apphhzp.lib.ClassHelper.unsafe;

public class InstanceKlass extends Klass {
    public final static Type TYPE = JVM.type("InstanceKlass");
    public final static int SIZE = TYPE.size;
    public final static long CONSTANTS_OFFSET = TYPE.offset("_constants");
    public final static long FIELDS_COUNT_OFFSET = TYPE.offset("_java_fields_count");
    public final static long MISC_FLAGS_OFFSET=TYPE.offset("_misc_flags");
    public final static long INIT_THREAD_OFFSET=TYPE.offset("_init_thread");
    public final static long OSR_NMETHOD_HEAD_OFFSET=TYPE.offset("_osr_nmethods_head");
    public final static long METHODS_OFFSET = TYPE.offset("_methods");
    public final static long DEFAULT_METHODS_OFFSET = TYPE.offset("_default_methods");
    public final static long LOCAL_INTERFACES_OFFSET = TYPE.offset("_local_interfaces");
    public final static long TRANSITIVE_INTERFACES_OFFSET = TYPE.offset("_transitive_interfaces");
    public final static long FIELDS_OFFSET = TYPE.offset("_fields");
    public final static long BREAKPOINTS_OFFSET=JVM.isJVMTISupported?TYPE.offset("_breakpoints"):-1;
    private ConstantPool constantPoolCache;
    private NMethod headCache;
    private VMTypeArray<Method> methodsCache;
    private VMTypeArray<Method> defaultMethodsCache;
    private VMTypeArray<InstanceKlass> localInterfacesCache;
    private VMTypeArray<InstanceKlass> transitiveInterfacesCache;
    private U2Array fieldsCache;
    private BreakpointInfo breakpointInfoCache;

    public static InstanceKlass getOrCreate(long addr){
        Klass klass=Klass.getOrCreate(addr);
        if (klass.isInstanceKlass()){
            return (InstanceKlass) klass;
        }
        throw new IllegalArgumentException("Need a InstanceKlass pointer!");
    }

    protected InstanceKlass(long addr) {
        super(addr);
    }

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
        VMTypeArray<Method> methods=this.getMethods();
        for (Method method:methods){
            method.getConstMethod().setConstantPool(pool);
        }
        methods=this.getDefaultMethods();
        if (methods!=null){
            for (Method method:methods){
                method.getConstMethod().setConstantPool(pool);
            }
        }
    }

    public int getFieldsCount(){
        return unsafe.getShort(this.address+FIELDS_COUNT_OFFSET)&0xffff;
    }

    public void setFieldsCount(int cnt){
        unsafe.putShort(this.address+FIELDS_COUNT_OFFSET, (short) (cnt&0xffff));
    }

    public int getMiscFlags(){
        return unsafe.getShort(this.address+MISC_FLAGS_OFFSET)&0xffff;
    }

    public void setMiscFlags(int flags){
        unsafe.putShort(this.address+MISC_FLAGS_OFFSET, (short) (flags&0xffff));
    }

    @Nullable
    public Thread getInitThread(){
        long addr=unsafe.getAddress(this.address+INIT_THREAD_OFFSET);
        if (addr==0L){
            return null;
        }
        return Thread.getOrCreate(addr);
    }
    @Nullable
    public NMethod getOsrNMethodHead(){
        long addr=unsafe.getAddress(this.address+OSR_NMETHOD_HEAD_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.headCache,addr)){
            this.headCache=new NMethod(addr);
        }
        return this.headCache;
    }

    public void setOsrNMethodHead(@Nullable NMethod nMethod){
        unsafe.putAddress(this.address+OSR_NMETHOD_HEAD_OFFSET,nMethod==null?0L:nMethod.address);
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
        if (!n.isOsrMethod()){
            throw new IllegalArgumentException("wrong kind of nmethod");
        }
        NMethod last = null;
        NMethod cur  = this.getOsrNMethodHead();
        int max_level = CompLevel.NONE.id;
        Method m = n.getMethod();
        boolean found = false;
        while(cur != null && !cur.equals(n)) {
            if (Objects.equals(m,cur.getMethod())){
                max_level = Math.max(max_level, cur.getCompLevel().id);
            }
            last = cur;
            cur = cur.getNext();
        }
        NMethod next = null;
        if (Objects.equals(cur,n)) {
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
            if (Objects.equals(m,cur.getMethod())){
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
        while (osr!=null) {
            if (isEqual(osr.getMethod(),m.address)) {
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
            this.methodsCache = new VMTypeArray<>(addr, Method.class,Method::getOrCreate);
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
            this.defaultMethodsCache = new VMTypeArray<>(addr, Method.class,Method::getOrCreate);
        }
        return this.defaultMethodsCache;
    }

    public VMTypeArray<InstanceKlass> getLocalInterfaces(){
        long addr=unsafe.getAddress(this.address+LOCAL_INTERFACES_OFFSET);
        if (!isEqual(this.localInterfacesCache,addr)){
            this.localInterfacesCache=new VMTypeArray<>(addr, InstanceKlass.class,InstanceKlass::getOrCreate);
        }
        return this.localInterfacesCache;
    }

    public VMTypeArray<InstanceKlass> getAllInterfaces(){
        long addr=unsafe.getAddress(this.address+TRANSITIVE_INTERFACES_OFFSET);
        if (!isEqual(this.transitiveInterfacesCache,addr)){
            this.transitiveInterfacesCache=new VMTypeArray<>(addr, InstanceKlass.class,InstanceKlass::getOrCreate);
        }
        return this.transitiveInterfacesCache;
    }

    public U2Array getFields(){
        long addr=unsafe.getAddress(this.address+FIELDS_OFFSET);
        if (!isEqual(this.fieldsCache,addr)){
            this.fieldsCache=new U2Array(addr);
        }
        return this.fieldsCache;
    }

    public FieldInfo[] getFieldInfos(){
        int len=getFieldsCount();
        FieldInfo[] re=new FieldInfo[len];
        long base=unsafe.getAddress(this.address+FIELDS_OFFSET)+U2Array.DATA_OFFSET;
        for (int i=0;i<len;i++){
            re[i]=new FieldInfo(base+i*2L*6L);
        }
        return re;
    }

    @Override
    public void setAccessible() {
        super.setAccessible();
        for (Method method : this.getMethods()) {
            method.setAccessible();
        }
        if (this.getDefaultMethods()!=null) {
            for (Method method : this.getDefaultMethods()) {
                method.setAccessible();
            }
        }
        for (FieldInfo info:this.getFieldInfos()){
            info.setAccessible();
        }
        this.updateReflectionData();
    }
    @Nullable
    public BreakpointInfo getBreakpointInfo(){
        if (!JVM.isJVMTISupported){
            return null;
        }
        long addr=unsafe.getAddress(this.address+BREAKPOINTS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.breakpointInfoCache,addr)){
            this.breakpointInfoCache=BreakpointInfo.of(addr);
        }
        return this.breakpointInfoCache;
    }

    public void setBreakpointInfo(@Nullable BreakpointInfo info){
        if (JVM.isJVMTISupported){
            unsafe.putAddress(this.address+BREAKPOINTS_OFFSET,info==null?0L:info.address);
        }
    }

    @Override
    public boolean isAssignableFrom(Klass klass) {
        if (super.isAssignableFrom(klass)){
            return true;
        }
        for (InstanceKlass klass1: this.getLocalInterfaces()){
            if (klass1.isAssignableFrom(klass)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Instance" + super.toString();
    }

    public static final class MiscFlags{
        public static final int REWRITTEN=JVM.intConstant("InstanceKlass::_misc_rewritten");
        public static final int HAS_NONSTATIC_FIELDS=JVM.intConstant("InstanceKlass::_misc_has_nonstatic_fields");
        public static final int SHOULD_VERIFY_CLASS=JVM.intConstant("InstanceKlass::_misc_should_verify_class");
        public static final int IS_CONTENDED=JVM.intConstant("InstanceKlass::_misc_is_contended");
        public static final int HAS_NONSTATIC_CONCRETE_METHODS=JVM.intConstant("InstanceKlass::_misc_has_nonstatic_concrete_methods");
        public static final int DECLARES_NONSTATIC_CONCRETE_METHODS=JVM.intConstant("InstanceKlass::_misc_declares_nonstatic_concrete_methods");
        public static final int HAS_BEEN_REDEFINED=JVM.intConstant("InstanceKlass::_misc_has_been_redefined");
        public static final int IS_SCRATCH_CLASS=JVM.intConstant("InstanceKlass::_misc_is_scratch_class");
        public static final int IS_SHARED_BOOT_CLASS=JVM.intConstant("InstanceKlass::_misc_is_shared_boot_class");
        public static final int IS_SHARED_PLATFORM_CLASS=JVM.intConstant("InstanceKlass::_misc_is_shared_platform_class");
        public static final int IS_SHARED_APP_CLASS=JVM.intConstant("InstanceKlass::_misc_is_shared_app_class");
    }
}
