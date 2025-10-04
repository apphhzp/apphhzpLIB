package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.classfile.SystemDictionary;
import apphhzp.lib.hotspot.interpreter.BootstrapInfo;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.signature.Signature;
import apphhzp.lib.hotspot.util.Atomic;
import apphhzp.lib.hotspot.util.ConstantPoolHelper;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;
import it.unimi.dsi.fastutil.ints.IntList;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static apphhzp.lib.ClassHelperSpecial.*;
import static apphhzp.lib.helfy.JVM.*;
import static apphhzp.lib.hotspot.oops.constant.ConstantTag.*;

public class ConstantPool extends Metadata {
    public static final Type TYPE = JVM.type("ConstantPool");
    public static final int SIZE = TYPE.size;
    public static final long TAGS_OFFSET = TYPE.offset("_tags");
    public static final long CACHE_OFFSET = TYPE.offset("_cache");
    public static final long HOLDER_OFFSET = TYPE.offset("_pool_holder");
    public static final long OPERANDS_OFFSET = TYPE.offset("_operands");
    public static final long KLASSES_OFFSET = TYPE.offset("_resolved_klasses");
    public static final long MAJOR_VER_OFFSET = TYPE.offset("_major_version");
    public static final long MINOR_VER_OFFSET = TYPE.offset("_minor_version");
    public static final long GENERIC_SIGNATURE_OFFSET = TYPE.offset("_generic_signature_index");
    public static final long SOURCE_FILE_NAME_OFFSET = TYPE.offset("_source_file_name_index");
    public static final long FLAGS_OFFSET = JVM.includeJVMCI ? TYPE.offset("_flags") : SOURCE_FILE_NAME_OFFSET + 2;
    public static final long LENGTH_OFFSET = TYPE.offset("_length");
    public static final int CPCACHE_INDEX_TAG= intConstant("ConstantPool::CPCACHE_INDEX_TAG");
    private static final HashMap<Long, ConstantPool> CACHE = new HashMap<>();

    private U1Array tagsCache;
    private ConstantPoolCache cacheCache;
    private U2Array operandsCache;
    private VMTypeArray<Klass> resolvedKlassesCache;

    public static ConstantPool getOrCreate(long addr) {
        if (addr == 0L) {
            throw new IllegalArgumentException("The pointer is NULL(0L)!");
        }
        ConstantPool re = CACHE.get(addr);
        if (re != null) {
            return re;
        }
        CACHE.put(addr, re = new ConstantPool(addr));

        return re;
    }

    public static ConstantPool allocate(int length) {
        U1Array tags = new U1Array(length);
        int size = size(length);
        long addr = unsafe.allocateMemory((long) size * JVM.wordSize);
        unsafe.setMemory(addr, (long) size * JVM.wordSize, (byte) 0);
        ConstantPool re = getOrCreate(addr);
        re.setTags(tags);
        re.setLength(length);
        return re;
    }

    /**
     * In words
     */
    public static int header_size() {
        return (int) (JVM.alignUp((int) SIZE, JVM.wordSize) / JVM.wordSize);
    }

    /**
     * In words
     */
    public static int size(int length) {
        return (int) JVM.align_metadata_size(header_size() + length);
    }

    public static void clearCacheMap() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
    public static int decode_cpcache_index(int raw_index) {
        return decode_cpcache_index(raw_index,false);
    }

    public static int decode_cpcache_index(int raw_index, boolean invokedynamic_ok) {
        if (invokedynamic_ok && is_invokedynamic_index(raw_index))
            return decode_invokedynamic_index(raw_index);
        else
            return raw_index - CPCACHE_INDEX_TAG;
    }

    private ConstantPool(long addr) {
        super(addr);
    }

    public long constantAddress(int which) {
        checkBound(which);
        return this.address + SIZE + (long) which * oopSize;
    }

    public <T extends Constant> T getConstant(int which) {
        checkBound(which);
        byte tag = this.getTags().get(which);
        //noinspection unchecked
        return (T) switch (tag) {
            case Utf8 -> new Utf8Constant(this, which);
            case Unicode -> throw new IllegalStateException("Unused tag:Unicode");
            case Integer -> new IntegerConstant(this, which);
            case Float -> new FloatConstant(this, which);
            case Long -> new LongConstant(this, which);
            case Double -> new DoubleConstant(this, which);
            case Class, UnresolvedClass, UnresolvedClassInError -> new ClassConstant(this, which);
            case NameAndType -> new NameAndTypeConstant(this, which);
            case String -> new StringConstant(this, which);
            case Fieldref -> new FieldRefConstant(this, which);
            case Methodref, InterfaceMethodref -> new MethodRefConstant(this, which);
            case MethodHandle, MethodHandleInError -> new MethodHandleConstant(this, which);
            case MethodType, MethodTypeInError -> new MethodTypeConstant(this, which);
            case InvokeDynamic, Dynamic -> new InvokeDynamicConstant(this, which);
            case Invalid -> throw new IllegalStateException("Invalid tag(CONSTANT_Double or CONSTANT_Long?)");
            default -> throw new IllegalStateException("Unsupported tag: " + tag);
        };
    }

    public <T extends Constant> T findConstant(Predicate<T> predicate, byte type) {
        return this.findConstant(predicate, type, 1);
    }

    public <T extends Constant> T findConstant(Predicate<T> predicate, byte type, int st_index) {
        checkTag(type);
        checkBound(st_index);
        U1Array tags = this.getTags();
        for (int i = st_index, len = tags.length(); i < len; i++) {
            byte tag = tags.get(i);
            if (tag == type) {
                T tmp = this.getConstant(i);
                if (predicate.test(tmp)) {
                    return tmp;
                }
            }
            if (tag == Long || tag == Double) {
                i++;
            }
        }
        return null;
    }

    public <T extends Constant> T findConstant(byte type) {
        return this.findConstant(type, 1);
    }

    public <T extends Constant> T findConstant(byte type, int st_index) {
        checkTag(type);
        checkBound(st_index);
        U1Array tags = this.getTags();
        for (int i = st_index, len = tags.length(); i < len; i++) {
            byte tag = tags.get(i);
            if (tag == type) {
                return this.getConstant(i);
            }
            if (tag == Long || tag == Double) {
                i++;
            }
        }
        return null;
    }

    public Symbol findSymbol(String s) {
        U1Array array = this.getTags();
        byte[] tags = array.toByteArray();
        for (int i = 1, len = tags.length; i < len; i++) {
            byte tag = tags[i];
            if (tag == Utf8) {
                Symbol tmp = Symbol.of(unsafe.getAddress(this.address + SIZE + (long) i * oopSize));
                if (s.equals(tmp.toString())) {
                    return tmp;
                }
            }
        }
        return null;
    }

    public U1Array getTags() {
        long addr = unsafe.getAddress(this.address + TAGS_OFFSET);
        if (!isEqual(this.tagsCache, addr)) {
            this.tagsCache = new U1Array(addr);
        }
        return this.tagsCache;
    }

    public void setTags(U1Array array) {
        unsafe.putAddress(this.address + TAGS_OFFSET, array.address);
    }

    @Nullable
    public ConstantPoolCache getCache() {
        long addr = unsafe.getAddress(this.address + CACHE_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.cacheCache, addr)) {
            this.cacheCache = new ConstantPoolCache(addr);
        }
        return this.cacheCache;
    }

    public void set_cache(ConstantPoolCache cache) {
        this.cacheCache=null;
        unsafe.putAddress(this.address+CACHE_OFFSET,cache==null?0L: cache.address);
    }

    public InstanceKlass pool_holder() {
        long addr=unsafe.getAddress(this.address + HOLDER_OFFSET);
        if (addr==0L){
            return null;
        }
        return (InstanceKlass) Klass.getOrCreate(addr);
    }

    public void setHolder(InstanceKlass klass) {
        unsafe.putAddress(this.address + HOLDER_OFFSET, klass.address);
    }

    public U2Array getOperands() {
        long addr = unsafe.getAddress(this.address + OPERANDS_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.operandsCache, addr)) {
            this.operandsCache = new U2Array(addr);
        }
        return this.operandsCache;
    }

    public void setOperands(U2Array array) {
        unsafe.putAddress(this.address + OPERANDS_OFFSET, array.address);
    }

    public Klass getResolvedKlass(int index) {
        return Klass.getOrCreate(this.resolved_klasses().getAddress(index));
    }

    public VMTypeArray<Klass> resolved_klasses() {
        long addr = unsafe.getAddress(address + KLASSES_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.resolvedKlassesCache, addr)) {
            this.resolvedKlassesCache = new VMTypeArray<>(addr, Klass.class, Klass::getOrCreate);
        }
        return this.resolvedKlassesCache;
    }

    public void set_resolved_klasses(VMTypeArray<Klass> val) {
        unsafe.putAddress(address + KLASSES_OFFSET,val==null?0L:val.address);
    }

    public int major_version() {
        return unsafe.getShort(this.address + MAJOR_VER_OFFSET) & 0xffff;
    }

    public void set_major_version(int version) {
        unsafe.putShort(this.address + MAJOR_VER_OFFSET, (short) (version & 0xffff));
    }

    public int minor_version() {
        return unsafe.getShort(this.address + MINOR_VER_OFFSET) & 0xffff;
    }

    public void set_minor_version(int version) {
        unsafe.putShort(this.address + MINOR_VER_OFFSET, (short) (version & 0xffff));
    }

    public int generic_signature_index() {
        return unsafe.getShort(this.address + GENERIC_SIGNATURE_OFFSET) & 0xffff;
    }

    public void set_generic_signature_index(int index) {
        unsafe.putShort(this.address + GENERIC_SIGNATURE_OFFSET, (short) (index & 0xffff));
    }

    public int source_file_name_index() {
        return unsafe.getShort(this.address + SOURCE_FILE_NAME_OFFSET) & 0xffff;
    }

    public void set_source_file_name_index(int index) {
        unsafe.putShort(this.address + SOURCE_FILE_NAME_OFFSET, (short) (index & 0xffff));
    }

    public int length() {
        return unsafe.getInt(address + LENGTH_OFFSET);
    }

    public void setLength(int length) {
        unsafe.putInt(address + LENGTH_OFFSET, length);
    }


    //JVM func start...

    public static class CPSlot {

        private @RawCType("intptr_t") long _ptr;
        //TagBits
        private static final int _pseudo_bit = 1;

        public CPSlot(@RawCType("intptr_t") long ptr) {
            this._ptr = ptr;
        }

        public CPSlot(Symbol ptr) {
            this(ptr, 0);
        }

        public CPSlot(Symbol ptr, int tag_bits) {
            this._ptr = (ptr.address | tag_bits);
        }

        public @RawCType("intptr_t") long value() {
            return _ptr;
        }

        public Symbol get_symbol() {
            return Symbol.of(_ptr & ~_pseudo_bit);
        }
    }


    /**
     * This represents a JVM_CONSTANT_Class, JVM_CONSTANT_UnresolvedClass, or JVM_CONSTANT_UnresolvedClassInError slot in the constant pool.
     */
    public static class CPKlassSlot {
        // cp->symbol_at(_name_index) gives the name of the class.
        private final int _name_index;

        // cp->_resolved_klasses->at(_resolved_klass_index) gives the Klass* for the class.
        private final int _resolved_klass_index;

        public static final int _temp_resolved_klass_index = 0xffff;

        public CPKlassSlot(int n, int rk) {
            _name_index = n;
            _resolved_klass_index = rk;
        }

        public int name_index() {
            return _name_index;
        }

        int resolved_klass_index() {
            if (_resolved_klass_index == _temp_resolved_klass_index) {
                throw new IllegalStateException("constant pool merging was incomplete");
            }
            return _resolved_klass_index;
        }
    }

    ;

    public @RawCType("intptr_t*") long base() {
        return this.address + SIZE;
    }

    public int tag_at(int which) {
        return this.getTags().get(which)&0xff;
    }

    public void tag_at_put(int which, int t) {
        this.getTags().set(which, (byte) (t&0xff));
    }

    public CPSlot slot_at(int which) {
        checkBound(which);
        int tag = tag_at(which);
        if (!(tag != UnresolvedClass && tag != UnresolvedClassInError)) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        // Uses volatile because the klass slot changes without a lock.
        long adr = unsafe.getAddress(at_addr(which));
        if (!(adr != 0 || which == 0)) {
            throw new RuntimeException("cp entry for klass should not be zero");
        }
        return new CPSlot(adr);
    }

    public void slot_at_put(int which, CPSlot s) {
        checkBound(which);
        if (s.value() == 0) {
            throw new IllegalArgumentException("Caught something");
        }
        unsafe.putAddress(this.base() + (long) which * oopSize, s.value());
    }

    public @RawCType("intptr_t*") long at_addr(int which) {
        checkBound(which);
        return base() + (long) which * oopSize;
    }

    public CPKlassSlot klass_slot_at(int which) {
        int tag = tag_at(which);
        if (tag != UnresolvedClass && tag != Class) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        int value = unsafe.getInt(at_addr(which));
        int name_index = extract_high_short_from_int(value);
        int resolved_klass_index = extract_low_short_from_int(value);
        return new CPKlassSlot(name_index, resolved_klass_index);
    }

    public Symbol klass_name_at(int which) {
        return symbol_at(klass_slot_at(which).name_index());
    }

    public int klass_name_index_at(int which) {
        return klass_slot_at(which).name_index();
    }

    public Klass resolved_klass_at(int which) {  // Used by Compiler
        if (tag_at(which) != Class) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        // Must do an acquire here in case another thread resolved the klass
        // behind our back, lest we later load stale values thru the oop.
        CPKlassSlot kslot = klass_slot_at(which);
        if (tag_at(kslot.name_index()) != Utf8) {
            throw new RuntimeException("sanity");
        }
        //@RawCType("Klass**") long adr = ;
        return this.resolved_klasses().get(kslot.resolved_klass_index());
    }

    public Symbol klass_at_noresolve(int which) {
        return klass_name_at(which);
    }

    public void temp_unresolved_klass_at_put(int which, int name_index) {
        // Used only during constant pool merging for class redefinition. The resolved klass index
        // will be initialized later by a call to initialize_unresolved_klasses().
        unresolved_klass_at_put(which, name_index, CPKlassSlot._temp_resolved_klass_index);
    }

    public void unresolved_klass_at_put(int which, int name_index, int resolved_klass_index) {
        tag_at_put(which, UnresolvedClass);
        if ((name_index & 0xffff0000) != 0) {
            throw new IllegalArgumentException("must be");
        }
        if ((resolved_klass_index & 0xffff0000) != 0) {
            throw new IllegalArgumentException("must be");
        }
        unsafe.putInt(this.base() + (long) which * oopSize, JVM.build_int_from_shorts(resolved_klass_index, name_index));
    }

    public void method_handle_index_at_put(int which, int ref_kind, int ref_index) {
        tag_at_put(which, MethodHandle);
        unsafe.putInt(this.base() + (long) oopSize * which, (ref_index << 16) | ref_kind);
    }

    public void method_type_index_at_put(int which, int ref_index) {
        tag_at_put(which, MethodType);
        unsafe.putInt(this.base() + (long) which * oopSize, ref_index);
    }

    public void dynamic_constant_at_put(int which, int bsms_attribute_index, int name_and_type_index) {
        tag_at_put(which, Dynamic);
        unsafe.putInt(this.base() + (long) which * oopSize, (name_and_type_index << 16) | bsms_attribute_index);
    }

    public void invoke_dynamic_at_put(int which, int bsms_attribute_index, int name_and_type_index) {
        tag_at_put(which, InvokeDynamic);
        unsafe.putInt(this.base() + (long) which * oopSize, (name_and_type_index << 16) | bsms_attribute_index);
    }

    public void unresolved_string_at_put(int which, Symbol s) {
        tag_at_put(which, String);
        slot_at_put(which, new CPSlot(s));
    }

    public void int_at_put(int which, int i) {
        this.tag_at_put(which, Integer);
        unsafe.putInt(this.address + SIZE + (long) which * oopSize, i);
    }

    public void float_at_put(int which, float f) {
        this.tag_at_put(which, Float);
        unsafe.putFloat(this.address + SIZE + (long) which * oopSize, f);
    }

    public void long_at_put(int which, long l) {
        this.tag_at_put(which, Long);
        unsafe.putLong(this.address + SIZE + (long) which * oopSize, l);
    }

    public void double_at_put(int which, double d) {
        this.tag_at_put(which, Double);
        unsafe.putDouble(this.address + SIZE + (long) which * oopSize, d);
    }

    public void symbol_at_put(int which, Symbol symbol) {
        if (symbol.getRefCount() == 0) {
            throw new IllegalArgumentException("should have nonzero refcount");
        }
        this.tag_at_put(which, Utf8);
        unsafe.putAddress(this.address + SIZE + (long) oopSize * which, symbol.address);
    }

    public void string_at_put(int obj_index, Object str) {
        ConstantPoolCache cache = this.getCache();
        assert cache != null;
        checkBound(cache.objectToCpcIndex(obj_index));
        cache.getResolvedReferences()[obj_index] = str;
    }

    public void string_at_put(int which, int obj_index, String str) {
        this.getCache().getResolvedReferences()[obj_index] = str;
    }

    public void string_index_at_put(int which, int string_index) {
        checkBound(which);
        this.getTags().set(which, StringIndex);
        unsafe.putInt(this.address + SIZE + (long) oopSize * which, string_index);
    }

    public void field_at_put(int which, int class_index, int name_and_type_index) {
        tag_at_put(which, Fieldref);
        unsafe.putInt(this.base() + (long) which * oopSize, (name_and_type_index << 16) | class_index);
    }

    public void method_at_put(int which, int class_index, int name_and_type_index) {
        tag_at_put(which, Methodref);
        unsafe.putInt(this.base() + (long) which * oopSize, (name_and_type_index << 16) | class_index);
    }

    public void interface_method_at_put(int which, int class_index, int name_and_type_index) {
        tag_at_put(which, InterfaceMethodref);
        unsafe.putInt(this.base() + (long) which * oopSize, (name_and_type_index << 16) | class_index);// Not so nice
    }

    public void name_and_type_at_put(int which, int name_index, int signature_index) {
        tag_at_put(which, NameAndType);
        unsafe.putInt(this.base() + (long) which * oopSize, (signature_index << 16) | name_index);// Not so nice
    }

    public Klass klass_at(int which) {
        return klass_at_impl(this, which);
    }


    public static Klass klass_at_impl(ConstantPool this_cp, int which) {
        {
            int oldTag=this_cp.tag_at(which);
            boolean[] flg=new boolean[]{false};
            Klass klass= ConstantPoolHelper.getClassAt(this_cp,which,flg);
            if (!flg[0]){
                return klass;
            }
            this_cp.tag_at_put(which,oldTag);
        }
        // A resolved constantPool entry will contain a Klass*, otherwise a Symbol*.
        // It is not safe to rely on the tag bit's here, since we don't have a lock, and
        // the entry and tag is not updated atomicly.
        CPKlassSlot kslot = this_cp.klass_slot_at(which);
        int resolved_klass_index = kslot.resolved_klass_index();
        int name_index = kslot.name_index();
        if (this_cp.tag_at(name_index) != Utf8) {
            throw new IllegalArgumentException("sanity");
        }

        // The tag must be JVM_CONSTANT_Class in order to read the correct value from
        // the unresolved_klasses() array.
        if (this_cp.tag_at(which) == Class) {
            Klass klass = this_cp.resolved_klasses().get(resolved_klass_index);
            if (klass != null) {
                return klass;
            }
        }

        // This tag doesn't change back to unresolved class unless at a safepoint.
        if (this_cp.tag_at(which) == UnresolvedClassInError) {
            // The original attempt to resolve this constant pool entry failed so find the
            // class of the original error and throw another error of the same class
            // (JVMS 5.4.3).
            // If there is a detail message, pass that detail message to the error.
            // The JVMS does not strictly require us to duplicate the same detail message,
            // or any internal exception fields such as cause or stacktrace.  But since the
            // detail message is often a class name or other literal string, we will repeat it
            // if we can find it in the symbol table.
            throw new RuntimeException(java.lang.Integer.toString(which));
//            throw_resolution_error(this_cp, which, CHECK_NULL);
//            ShouldNotReachHere();
        }

        Class<?> mirror_handle;
        Symbol name = this_cp.symbol_at(name_index);
        ClassLoader loader=this_cp.pool_holder().getClassLoaderData().getClassLoader();
        ProtectionDomain protection_domain= (this_cp.pool_holder().asClass().getProtectionDomain());

        Klass k;
        {
            // Turn off the single stepping while doing class resolution
            try {
                k = Klass.asKlass(java.lang.Class.forName(name.toString().replace('/','.'),true,loader));
            }catch (Throwable t){
                // Failed to resolve class. We must record the errors so that subsequent attempts
                // to resolve this constant pool entry fail with the same error (JVMS 5.4.3).
//                if (HAS_PENDING_EXCEPTION) {
//                    save_and_throw_exception(this_cp, which, constantTag(JVM_CONSTANT_UnresolvedClass), CHECK_NULL);
//                }
                // If CHECK_NULL above doesn't return the exception, that means that
                // some other thread has beaten us and has resolved the class.
                // To preserve old behavior, we return the resolved class.
                Klass klass = this_cp.resolved_klasses().get(resolved_klass_index);
                if (klass!=null){
                    return klass;
                }
                throwOriginalException(t);
                throw new RuntimeException(t);
            }
        } //  JvmtiHideSingleStepping jhss(javaThread);

//        if (!HAS_PENDING_EXCEPTION) {
//            // preserve the resolved klass from unloading
//            mirror_handle = Handle(THREAD, k->java_mirror());
//            // Do access check for klasses
//            verify_constant_pool_resolve(this_cp, k, THREAD);
//        }



        // logging for class+resolve.
//        if (log_is_enabled(Debug, class, resolve)){
//            trace_class_resolution(this_cp, k);
//        }

//        Klass** adr = this_cp->resolved_klasses()->adr_at(resolved_klass_index);
//        Atomic::release_store(adr, k);
        this_cp.resolved_klasses().set(resolved_klass_index,k);
        // The interpreter assumes when the tag is stored, the klass is resolved
        // and the Klass* stored in _resolved_klasses is non-NULL, so we need
        // hardware store ordering here.
        // We also need to CAS to not overwrite an error from a racing thread.

        int old_tag = this_cp.tag_at(which);
        this_cp.tag_at_put(which,Class);
//        Atomic::cmpxchg((jbyte*)this_cp->tag_addr_at(),
//                (jbyte)JVM_CONSTANT_UnresolvedClass,
//                (jbyte)JVM_CONSTANT_Class);

        // We need to recheck exceptions from racing thread and return the same.
        if (old_tag == UnresolvedClassInError) {
            // Remove klass.
            this_cp.resolved_klasses().set(resolved_klass_index, null);
            //throw_resolution_error(this_cp, which, CHECK_NULL);
            throw new RuntimeException(java.lang.Integer.toString(which));
        }

        return k;
    }

    /** Does not update ConstantPool* - to avoid any exception throwing.
     *  Used by compiler and exception handling.
     *  Also used to avoid classloads for instanceof operations.
     *  Returns NULL if the class has not been loaded or if the verification of constant pool failed*/
    public static Klass klass_at_if_loaded(ConstantPool this_cp, int which) {
        {
            boolean[] flg=new boolean[]{false};
            Klass re=ConstantPoolHelper.getClassAtIfLoaded(this_cp,which,flg);
            if (!flg[0]){
                return re;
            }
        }
        CPKlassSlot kslot = this_cp.klass_slot_at(which);
        int resolved_klass_index = kslot.resolved_klass_index();
        int name_index = kslot.name_index();
        if (this_cp.tag_at(name_index)!=Utf8){
            throw new RuntimeException("sanity");
        }
        if (this_cp.tag_at(which)==Class) {
            Klass k = this_cp.resolved_klasses().get(resolved_klass_index);
            if (k==null){
                throw new RuntimeException("should be resolved");
            }
            return k;
        } else if (this_cp.tag_at(which)==UnresolvedClassInError) {
            return null;
        } else {
            Symbol name = this_cp.symbol_at(name_index);
            ClassLoader loader = this_cp.pool_holder().getClassLoaderData().getClassLoader();
            java.lang.Class<?> cls=ClassHelperSpecial.findLoadedClass(loader,name.toString().replace('/','.'));
            Klass k = cls==null?null:Klass.asKlass(cls);
            return k;
        }
    }


    public int int_at(int which) {
        if (tag_at(which) != Integer) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getInt(this.base() + (long) which * oopSize);
    }

    public long long_at(int which) {
        if (tag_at(which) != Long) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getLong(this.base() + (long) which * oopSize);
    }

    public float float_at(int which) {
        if (tag_at(which) != Float) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getFloat(this.base() + (long) which * oopSize);
    }

    public double double_at(int which) {
        if (tag_at(which) != Double) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getDouble(this.base() + (long) which * oopSize);
    }

    public Symbol symbol_at(int which) {
        if (tag_at(which) != Utf8) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return Symbol.of(unsafe.getAddress(this.base() + (long) which * oopSize));
    }

    public String string_at(int which, int obj_index) {
        return string_at_impl(this, which, obj_index);
    }

    public static String string_at_impl(ConstantPool this_cp, int which, int obj_index) {
        // If the string has already been interned, this entry will be non-null
        String str = (java.lang.String) this_cp.getCache().getResolvedReferences()[obj_index];
        if (str != null) {
            return str;
        }
        Symbol sym = this_cp.unresolved_string_at(which);
        str = sym.toString();
        this_cp.string_at_put(which, obj_index, str);
        return str;
    }

    public String string_at(int which) {
        int obj_index = this.getCache().cpcToObjectIndex(which);
        return string_at(which, obj_index);
    }

    public String uncached_string_at(int which) {
        Symbol sym = unresolved_string_at(which);
        return sym.toString();
    }

    public String resolved_string_at(int which) {
        if (tag_at(which) != String) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        ConstantPoolCache cache = this.getCache();
        // Must do an acquire here in case another thread resolved the klass
        // behind our back, lest we later load stale values thru the oop.
        // we might want a volatile_obj_at in ObjArrayKlass.
        int obj_index = cache.cpcToObjectIndex(which);
        return (java.lang.String) cache.getResolvedReferences()[obj_index];
    }

    public Symbol unresolved_string_at(int which) {
        if (tag_at(which) != String) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return slot_at(which).get_symbol();
    }

    public @RawCType("char*") byte[] string_at_noresolve(int which) {
        return unresolved_string_at(which).toString().getBytes(StandardCharsets.UTF_8);
    }

    public static void resolve_string_constants_impl(ConstantPool this_cp) {
        for (int index = 1, length = this_cp.length(); index < length; index++) { // Index 0 is unused
            if (this_cp.tag_at(index) == String) {
                this_cp.string_at(index);
            }
        }
    }


    public int name_and_type_at(int which) {
        if (tag_at(which) != NameAndType) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getInt(this.base() + (long) which * oopSize);
    }

    public int method_handle_ref_kind_at(int which) {
        int tag = tag_at(which);
        if (tag != MethodHandle && tag != MethodHandleInError) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return extract_low_short_from_int(unsafe.getInt(this.base() + (long) which * oopSize));  // mask out unwanted ref_index bits
    }

    public int method_handle_index_at(int which) {
        int tag = tag_at(which);
        if (tag != MethodHandle && tag != MethodHandleInError) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return extract_high_short_from_int(unsafe.getInt(this.base() + (long) which * oopSize));  // shift out unwanted ref_kind bits
    }

    public int method_type_index_at(int which) {
        int tag = tag_at(which);
        if (tag != MethodType && tag != MethodTypeInError) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getInt(this.base() + (long) which * oopSize);
    }

    public Symbol name_ref_at(int which)                { return impl_name_ref_at(which, false); }
    public Symbol signature_ref_at(int which)           { return impl_signature_ref_at(which, false); }

    // Derived queries:
    public Symbol method_handle_name_ref_at(int which) {
        int member = method_handle_index_at(which);
        return impl_name_ref_at(member, true);
    }

    public Symbol impl_name_ref_at(int which, boolean uncached) {
        int name_index = name_ref_index_at(impl_name_and_type_ref_index_at(which, uncached));
        return symbol_at(name_index);
    }

    public int impl_name_and_type_ref_index_at(int which, boolean uncached) {
        int i = which;
        if (!uncached && this.getCache() != null) {
            if (is_invokedynamic_index(which)) {
                // Invokedynamic index is index into the constant pool cache
                int pool_index = invokedynamic_bootstrap_ref_index_at(which);
                pool_index = bootstrap_name_and_type_ref_index_at(pool_index);
                if (tag_at(pool_index) != NameAndType) {
                    throw new RuntimeException();
                }
                return pool_index;
            }
            // change byte-ordering and go via cache
            i = remap_instruction_operand_from_cache(which);
        } else {
            if (ConstantTag.has_bootstrap(tag_at(which))) {
                int pool_index = bootstrap_name_and_type_ref_index_at(which);
                if (tag_at(pool_index) != NameAndType) {
                    throw new RuntimeException();
                }
                return pool_index;
            }
        }
        {
            int tag = tag_at(i);
            if (!(tag == Methodref || tag == InterfaceMethodref || tag == Fieldref)) {
                throw new RuntimeException("Corrupted constant pool");
            }
            if (ConstantTag.has_bootstrap(tag_at(i))) {
                throw new RuntimeException("Must be handled above");
            }
        }
        int ref_index = unsafe.getInt(this.base() + (long) i * oopSize);
        return extract_high_short_from_int(ref_index);
    }

    public static boolean is_invokedynamic_index(int i) {
        return (i < 0);
    }

    public static int decode_invokedynamic_index(int i) {
        if (!is_invokedynamic_index(i)) {
            throw new IllegalArgumentException();
        }
        return ~i;
    }

    public static int encode_invokedynamic_index(int i) {
        if (is_invokedynamic_index(i)) {
            throw new IllegalArgumentException();
        }
        return ~i;
    }

    public int invokedynamic_cp_cache_index(int indy_index) {
        if (!is_invokedynamic_index(indy_index)) {
            throw new IllegalArgumentException("should be a invokedynamic index");
        }
        return decode_invokedynamic_index(indy_index);
    }

    public ConstantPoolCacheEntry invokedynamic_cp_cache_entry_at(int indy_index) {
        // decode index that invokedynamic points to.
        int cp_cache_index = invokedynamic_cp_cache_index(indy_index);
        return this.getCache().entry_at(cp_cache_index);
    }

    // Given the per-instruction index of an indy instruction, report the
    // main constant pool entry for its bootstrap specifier.
    // From there, uncached_name/signature_ref_at will get the name/type.
    public int invokedynamic_bootstrap_ref_index_at(int indy_index) {
        return invokedynamic_cp_cache_entry_at(indy_index).constant_pool_index();
    }

    public int name_ref_index_at(int which_nt) {
        int ref_index = name_and_type_at(which_nt);
        return extract_low_short_from_int(ref_index);
    }


    public int signature_ref_index_at(int which_nt) {
        int ref_index = name_and_type_at(which_nt);
        return extract_high_short_from_int(ref_index);
    }


    public Symbol method_handle_signature_ref_at(int which) {
        int member = method_handle_index_at(which);
        return impl_signature_ref_at(member, true);
    }

    public Symbol impl_signature_ref_at(int which, boolean uncached) {
        int signature_index = signature_ref_index_at(impl_name_and_type_ref_index_at(which, uncached));
        return symbol_at(signature_index);
    }

    public int method_handle_klass_index_at(int which) {
        int member = method_handle_index_at(which);
        return impl_klass_ref_index_at(member, true);
    }

    public Symbol method_type_signature_at(int which) {
        int sym = method_type_index_at(which);
        return symbol_at(sym);
    }

    public int bootstrap_name_and_type_ref_index_at(int which) {
        if (!ConstantTag.has_bootstrap(tag_at(which))) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return extract_high_short_from_int(unsafe.getInt(this.base() + (long) which * oopSize));
    }

    public int bootstrap_methods_attribute_index(int which) {
        if (!ConstantTag.has_bootstrap(tag_at(which))) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return extract_low_short_from_int(unsafe.getInt(this.base() + (long) which * oopSize));
    }

    public int bootstrap_operand_base(int which) {
        int bsms_attribute_index = bootstrap_methods_attribute_index(which);
        return operand_offset_at(this.getOperands(), bsms_attribute_index);
    }

    // The first part of the operands array consists of an index into the second part.
    // Extract a 32-bit index value from the first part.
    public static int operand_offset_at(U2Array operands, int bsms_attribute_index) {
        int n = (bsms_attribute_index * 2);
        if (!(n >= 0 && n + 2 <= operands.length())) {
            throw new IllegalArgumentException("oob");
        }
//        // The first 32-bit index points to the beginning of the second part
//        // of the operands array.  Make sure this index is in the first part.
//        DEBUG_ONLY(int second_part = build_int_from_shorts(operands.get(0),
//                operands.get(1)));
//        assert(second_part == 0 || n+2 <= second_part, "oob (2)");
        int offset = build_int_from_shorts(operands.get(n),
                operands.get(n + 1));
//        // The offset itself must point into the second part of the array.
//        assert(offset == 0 || offset >= second_part && offset <= operands->length(), "oob (3)");
        return offset;
    }

    public static void operand_offset_at_put(U2Array operands, int bsms_attribute_index, int offset) {
        int n = bsms_attribute_index * 2;
        if (!(n >= 0 && n + 2 <= operands.length())) {
            throw new IllegalArgumentException("oob");
        }
        operands.set(n, (short) extract_low_short_from_int(offset));
        operands.set(n + 1, (short) extract_high_short_from_int(offset));
    }

    public static int operand_array_length(U2Array operands) {
        if (operands == null || operands.length() == 0) {
            return 0;
        }
        int second_part = operand_offset_at(operands, 0);
        return (second_part / 2);
    }

    // Layout of InvokeDynamic and Dynamic bootstrap method specifier
    // data in second part of operands array.  This encodes one record in
    // the BootstrapMethods attribute.  The whole specifier also includes
    // the name and type information from the main constant pool entry.
    public static final int _indy_bsm_offset = 0,  // CONSTANT_MethodHandle bsm
            _indy_argc_offset = 1,  // u2 argc
            _indy_argv_offset = 2   // u2 argv[argc]
                    ;

    public Klass klass_ref_at(int which) {
        return klass_at(klass_ref_index_at(which));
    }

    public Symbol klass_ref_at_noresolve(int which) {
        int ref_index = klass_ref_index_at(which);
        return klass_at_noresolve(ref_index);
    }

    public Symbol uncached_klass_ref_at_noresolve(int which) {
        int ref_index = uncached_klass_ref_index_at(which);
        return klass_at_noresolve(ref_index);
    }

    public int klass_ref_index_at(int which) {
        return impl_klass_ref_index_at(which, false);
    }

    public int impl_klass_ref_index_at(int which, boolean uncached) {
        if (this.getTags().get(which) == InvokeDynamic) {
            throw new IllegalArgumentException("an invokedynamic instruction does not have a klass");
        }
        int i = which;
        if (!uncached && this.getCache() != null) {
            // change byte-ordering and go via cache
            i = remap_instruction_operand_from_cache(which);
        }
        {
            int tag = tag_at(i);
            if (!(tag == Methodref || tag == Fieldref || tag == InterfaceMethodref)) {
                throw new RuntimeException("Corrupted constant pool");
            }
        }
        int ref_index = unsafe.getInt(this.address + SIZE + (long) i * oopSize);
        return ref_index & 0xFFFF;
    }


    public void set_resolved_references(OopDesc s){
        getCache().setResolvedReferences(s);
    }

    @Nullable
    public U2Array reference_map(){
        ConstantPoolCache cache=this.getCache();
        return cache==null?null:cache.getReferenceMap();
    }

    public void set_reference_map(U2Array o){
        getCache().set_reference_map(o);
    }

    public int object_to_cp_index(int index)         { return this.getCache().getReferenceMap().get(index); }
    public int cp_to_object_index(int cp_index) {
        // this is harder don't do this so much.
        int i = reference_map().find((short) (cp_index&0xffff));
        // We might not find the index for jsr292 call.
        return (i < 0) ? -1 : i;
    }

    public Object[] resolved_references() {
        return this.getCache().getResolvedReferences();
    }

    public @RawCType("BasicType")int basic_type_for_constant_at(int which) {
        int tag = tag_at(which);
        if (tag==ConstantTag.Dynamic ||
                tag==ConstantTag.DynamicInError) {
            // have to look at the signature for this one
            Symbol constant_type = uncached_signature_ref_at(which);
            return Signature.basic_type(constant_type);
        }
        return ConstantTag.basic_type(tag);
    }


    public int remap_instruction_operand_from_cache(int operand) {
        return this.getCache().entry_at(operand).constant_pool_index();
    }

    public int uncached_klass_ref_index_at(int which) {
        return impl_klass_ref_index_at(which, true);
    }

    public int name_and_type_ref_index_at(int which) {
        return impl_name_and_type_ref_index_at(which, false);
    }

    public Symbol uncached_name_ref_at(int which){
        return impl_name_ref_at(which, true);
    }
    public Symbol uncached_signature_ref_at(int which) {
        return impl_signature_ref_at(which, true);
    }
    public int uncached_name_and_type_ref_index_at(int which){
        return impl_name_and_type_ref_index_at(which, true);
    }
    // Used while constructing constant pool (only by ClassFileParser)
    public int klass_index_at(int which) {
        if (tag_at(which)!=ClassIndex){
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getInt(this.base()+ (long) which * oopSize);
    }

    public int string_index_at(int which) {
        if (tag_at(which)!=StringIndex){
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getInt(this.base()+ (long) which * oopSize);
    }

    public void klass_at_put(int class_index, Klass k) {
        if (k==null){
            throw new IllegalArgumentException("must be valid klass");
        }
        CPKlassSlot kslot = klass_slot_at(class_index);
        int resolved_klass_index = kslot.resolved_klass_index();
//        Klass** adr = resolved_klasses()->adr_at(resolved_klass_index);
//        Atomic::release_store(adr, k);
        this.resolved_klasses().set(resolved_klass_index,k);
        // The interpreter assumes when the tag is stored, the klass is resolved
        // and the Klass* non-NULL, so we need hardware store ordering here.
        //      ^
        //      |
        //Not implemented
        tag_at_put(class_index, Class);
    }

    public void copy_bootstrap_arguments_at(int index,
                                     int start_arg, int end_arg,
                                     Object[] info, int pos,
                                     boolean must_resolve, @RawCType("Handle")Object if_not_available) {
        copy_bootstrap_arguments_at_impl(this, index, start_arg, end_arg,
                info, pos, must_resolve, if_not_available);
    }

    public static void copy_bootstrap_arguments_at_impl(ConstantPool this_cp, int index,
                                                        int start_arg, int end_arg,
                                                        Object[] info, int pos,
                                                        boolean must_resolve, @RawCType("Handle")Object if_not_available) {
        int argc;
        int limit = pos + end_arg - start_arg;
        // checks: index in range [0..this_cp->length),
        // tag at index, start..end in range [0..argc],
        // info array non-null, pos..limit in [0..info.length]
        if ((0 >= index    || index >= this_cp.length())  ||
                !( this_cp.tag_at(index)==InvokeDynamic    ||
                        this_cp.tag_at(index)==Dynamic) ||
                (0 > start_arg || start_arg > end_arg) ||
                (end_arg > (argc = this_cp.bootstrap_argument_count_at(index))) ||
                (0 > pos       || pos > limit)         ||
                (info==null || limit > info.length)) {
            // An index or something else went wrong; throw an error.
            // Since this is an internal API, we don't expect this,
            // so we don't bother to craft a nice message.
            throw new LinkageError("bad BSM argument access");
        }
        // now we can loop safely
        int info_i = pos;
        for (int i = start_arg; i < end_arg; i++) {
            int arg_index = this_cp.bootstrap_argument_index_at(index, i);
            Object arg_oop;
            if (must_resolve) {
                arg_oop = this_cp.resolve_possibly_cached_constant_at(arg_index);
            } else {
                boolean[] found_it = new boolean[]{false};
                arg_oop = this_cp.find_cached_constant_at(arg_index, found_it);
                if (!found_it[0])  arg_oop = if_not_available;
            }
            info[info_i++]=arg_oop;
        }
    }

    public Object resolve_constant_at(int index) {
        return resolve_constant_at_impl(this, index, _no_index_sentinel, null);
    }

    public Object resolve_cached_constant_at(int cache_index) {
        return resolve_constant_at_impl(this, _no_index_sentinel, cache_index, null);
    }

    public Object resolve_possibly_cached_constant_at(int pool_index) {
        return resolve_constant_at_impl(this, pool_index, _possible_index_sentinel, null);
    }

    public Object find_cached_constant_at(int pool_index,boolean[] found_it) {
        return resolve_constant_at_impl(this, pool_index, _possible_index_sentinel, found_it);
    }

    private static final int _no_index_sentinel = -1, _possible_index_sentinel = -2;
    // Called to resolve constants in the constant pool and return an oop.
// Some constant pool entries cache their resolved oop. This is also
// called to create oops from constants to use in arguments for invokedynamic
    public static Object resolve_constant_at_impl(ConstantPool this_cp,
                                               int index, int cache_index,
                                               @RawCType("bool*")boolean[] status_return) {
        Object result_oop = null;
        //Handle throw_exception;

        if (cache_index == _possible_index_sentinel) {
            // It is possible that this constant is one which is cached in the objects.
            // We'll do a linear search.  This should be OK because this usage is rare.
            // FIXME: If bootstrap specifiers stress this code, consider putting in
            // a reverse index.  Binary search over a short array should do it.
            if (index<=0){
                throw new RuntimeException("valid index");
            }
            cache_index = this_cp.cp_to_object_index(index);
        }
        if (!(cache_index == _no_index_sentinel || cache_index >= 0)){
            throw new RuntimeException();
        }
        if (!(index == _no_index_sentinel || index >= 0)){
            throw new RuntimeException();
        }

        if (cache_index >= 0) {
            result_oop = this_cp.resolved_references()[(cache_index)];
            if (result_oop != null) {
//                if (result_oop == Universe::the_null_sentinel()) {
//                    DEBUG_ONLY(int temp_index = (index >= 0 ? index : this_cp->object_to_cp_index(cache_index)));
//                    assert(this_cp->tag_at(temp_index).is_dynamic_constant(), "only condy uses the null sentinel");
//                    result_oop = null;
//                }
                if (status_return != null)  status_return[0] = true;
                return result_oop;
                // That was easy...
            }
            index = this_cp.object_to_cp_index(cache_index);
        }

        int tag = this_cp.tag_at(index);

        if (status_return != null) {
            // don't trigger resolution if the constant might need it
            switch (tag&0xff) {
                case Class:
                {
                    CPKlassSlot kslot = this_cp.klass_slot_at(index);
                    int resolved_klass_index = kslot.resolved_klass_index();
                    if (this_cp.resolved_klasses().get(resolved_klass_index) == null) {
                        status_return[0] = false;
                        return null;
                    }
                    // the klass is waiting in the CP; go get it
                    break;
                }
                case String:
                case Integer:
                case Float:
                case Long:
                case Double:
                    // these guys trigger OOM at worst
                    break;
                default:
                    status_return[0] = false;
                    return null;
            }
            // from now on there is either success or an OOME
            status_return[0] = true;
        }

        switch (tag&0xff) {

            case UnresolvedClass:
            case Class: {
                if (!(cache_index == _no_index_sentinel)){
                    throw new RuntimeException("should not have been set");
                }
                Klass resolved = klass_at_impl(this_cp, index);
                // ldc wants the java mirror.
                result_oop = resolved.asClass();
                break;
            }

            case Dynamic:
            {

                // Resolve the Dynamically-Computed constant to invoke the BSM in order to obtain the resulting oop.
                BootstrapInfo bootstrap_specifier=new BootstrapInfo(this_cp, index);

                // The initial step in resolving an unresolved symbolic reference to a
                // dynamically-computed constant is to resolve the symbolic reference to a
                // method handle which will be the bootstrap method for the dynamically-computed
                // constant. If resolution of the java.lang.invoke.MethodHandle for the bootstrap
                // method fails, then a MethodHandleInError is stored at the corresponding
                // bootstrap method's CP index for the CONSTANT_MethodHandle_info. No need to
                // set a DynamicConstantInError here since any subsequent use of this
                // bootstrap method will encounter the resolution of MethodHandleInError.
                // Both the first, (resolution of the BSM and its static arguments), and the second tasks,
                // (invocation of the BSM), of JVMS Section 5.4.3.6 occur within invoke_bootstrap_method()
                // for the bootstrap_specifier created above.
                SystemDictionary.invoke_bootstrap_method(bootstrap_specifier);
//                Exceptions::wrap_dynamic_exception(/* is_indy */ false, THREAD);
//                if (HAS_PENDING_EXCEPTION) {
//                    // Resolution failure of the dynamically-computed constant, save_and_throw_exception
//                    // will check for a LinkageError and store a DynamicConstantInError.
//                    save_and_throw_exception(this_cp, index, tag, CHECK_NULL);
//                }
                result_oop = bootstrap_specifier.resolved_value();
                @RawCType("BasicType")int type = Signature.basic_type(bootstrap_specifier.signature());
                if (!BasicType.is_reference_type(type)) {
                    // Make sure the primitive value is properly boxed.
                    // This is a JDK responsibility.
                    String fail = null;
                    if (result_oop == null) {
                        fail = "null result instead of box";
                    } else if (!BasicType.is_java_primitive(type)) {
                        // FIXME: support value types via unboxing
                        fail = "can only handle references and primitives";
                    } else if (!JavaClasses.BoxingObject.is_instance(result_oop, type)) {
                        fail = "primitive is not properly boxed";
                    }
                    if (fail != null) {
                        // Since this exception is not a LinkageError, throw exception
                        // but do not save a DynamicInError resolution result.
                        // See section 5.4.3 of the VM spec.
                        throw new InternalError(fail);
                    }
                }

//                LogTarget(Debug, methodhandles, condy) lt_condy;
//                if (lt_condy.is_enabled()) {
//                    LogStream ls(lt_condy);
//                    bootstrap_specifier.print_msg_on(&ls, "resolve_constant_at_impl");
//                }
                break;
            }

            case String:
                if (cache_index == _no_index_sentinel){
                    throw new RuntimeException("should have been set");
                }
                result_oop = string_at_impl(this_cp, index, cache_index);
                break;

            case MethodHandle:
            {
                int ref_kind                 = this_cp.method_handle_ref_kind_at(index);
                int callee_index             = this_cp.method_handle_klass_index_at(index);
                Symbol  name =      this_cp.method_handle_name_ref_at(index);
                Symbol  signature = this_cp.method_handle_signature_ref_at(index);
                @RawCType("constantTag")int m_tag  = this_cp.tag_at(this_cp.method_handle_index_at(index));
                {
//                    ResourceMark rm(THREAD);
//                    log_debug(class, resolve)("resolve JVM_CONSTANT_MethodHandle:%d [%d/%d/%d] %s.%s",
//                        ref_kind, index, this_cp->method_handle_index_at(index),
//                        callee_index, name->as_C_string(), signature->as_C_string());
                }

                Klass callee = klass_at_impl(this_cp, callee_index);
//                if (HAS_PENDING_EXCEPTION) {
//                    save_and_throw_exception(this_cp, index, tag, CHECK_NULL);
//                }

                // Check constant pool method consistency
                if ((callee.getAccessFlags().isInterface() && m_tag==Methodref) ||
                        (!callee.getAccessFlags().isInterface() && m_tag==InterfaceMethodref)) {
                    throw new IncompatibleClassChangeError();
//                    ResourceMark rm(THREAD);
//                    stringStream ss;
//                    ss.print("Inconsistent constant pool data in classfile for class %s. "
//                            "Method '", callee->name()->as_C_string());
//                    signature->print_as_signature_external_return_type(&ss);
//                    ss.print(" %s(", name->as_C_string());
//                    signature->print_as_signature_external_parameters(&ss);
//                    ss.print(")' at index %d is %s and should be %s",
//                            index,
//                            callee->is_interface() ? "CONSTANT_MethodRef" : "CONSTANT_InterfaceMethodRef",
//                            callee->is_interface() ? "CONSTANT_InterfaceMethodRef" : "CONSTANT_MethodRef");
//                    Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_IncompatibleClassChangeError(), "%s", ss.as_string());
//                    save_and_throw_exception(this_cp, index, tag, CHECK_NULL);
                }

                Klass klass = this_cp.pool_holder();
                result_oop = SystemDictionary.link_method_handle_constant(klass, ref_kind,
                        callee, name, signature);
                break;
            }

            case MethodType:
            {
                Symbol  signature = this_cp.method_type_signature_at(index);
//                {
//                    ResourceMark rm(THREAD);
//                    log_debug(class, resolve)("resolve JVM_CONSTANT_MethodType [%d/%d] %s",
//                        index, this_cp->method_type_index_at(index),
//                        signature->as_C_string());
//                }
                Klass klass = this_cp.pool_holder();
                result_oop = SystemDictionary.find_method_handle_type(signature, klass);
//                if (HAS_PENDING_EXCEPTION) {
//                    save_and_throw_exception(this_cp, index, tag, CHECK_NULL);
//                }
                break;
            }

            case Integer:
                if (cache_index != _no_index_sentinel){
                    throw new RuntimeException("should not have been set");
                }
                result_oop = this_cp.int_at(index);
                break;

            case Float:
                if (cache_index != _no_index_sentinel){
                    throw new RuntimeException("should not have been set");
                }
                result_oop = this_cp.float_at(index);
                break;

            case Long:
                if (cache_index != _no_index_sentinel){
                    throw new RuntimeException("should not have been set");
                }
                result_oop = this_cp.long_at(index);
                break;

            case Double:
                if (cache_index != _no_index_sentinel){
                    throw new RuntimeException("should not have been set");
                }
                result_oop = this_cp.double_at(index);
                break;

            case UnresolvedClassInError:
            case DynamicInError:
            case MethodHandleInError:
            case MethodTypeInError:
                throw new RuntimeException("resolution_error "+this_cp+" index:"+index);

            default:
                throw new RuntimeException(java.lang.String.format("unexpected constant tag at CP 0x"+ java.lang.Long.toHexString(this_cp.address) +"[%d/%d] = %d", index, cache_index, tag&0xff));
        }

        if (cache_index >= 0) {
            // Benign race condition:  resolved_references may already be filled in.
            // The important thing here is that all threads pick up the same result.
            // It doesn't matter which racing thread wins, as long as only one
            // result is used by all threads, and all future queries.
            Object old_result =  Atomic.atomic_compare_exchange_oop(this_cp.resolved_references(),cache_index, (result_oop),null);
            if (old_result == null) {
                return result_oop;  // was installed
            } else {
                // Return the winning thread's result.  This can be different than
                // the result here for MethodHandles.
                return old_result;
            }
        } else {
            if (result_oop == null){
                throw new RuntimeException();
            }
            return result_oop;
        }
    }


    public int bootstrap_method_ref_index_at(int which) {
        if (!ConstantTag.has_bootstrap(tag_at(which))){
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        int op_base = bootstrap_operand_base(which);
        return this.getOperands().get(op_base + _indy_bsm_offset)&0xffff;
    }
    public int bootstrap_argument_count_at(int which) {
        if (!ConstantTag.has_bootstrap(tag_at(which))){
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        int op_base = bootstrap_operand_base(which);
        int argc = this.getOperands().get(op_base + _indy_argc_offset);
        if (ENABLE_EXTRA_CHECK){
            int end_offset = op_base + _indy_argv_offset + argc;
            int next_offset = bootstrap_operand_limit(which);
            if (end_offset != next_offset){
                throw new RuntimeException("matched ending");
            }
        }
        return argc;
    }
    public static int operand_limit_at(U2Array operands, int bsms_attribute_index) {
        if (!ENABLE_EXTRA_CHECK){
            throw new RuntimeException();
        }
        int nextidx = bsms_attribute_index + 1;
        if (nextidx == operand_array_length(operands))
            return operands.length();
        else
            return operand_offset_at(operands, nextidx);
    }
    public int bootstrap_operand_limit(int which) {
        if (!ENABLE_EXTRA_CHECK){
            throw new RuntimeException();
        }
        int bsms_attribute_index = bootstrap_methods_attribute_index(which);
        return operand_limit_at(this.getOperands(), bsms_attribute_index);
    }
    public int bootstrap_argument_index_at(int which, int j) {
        int op_base = bootstrap_operand_base(which);
        if (ENABLE_EXTRA_CHECK){
            int argc = this.getOperands().get(op_base + _indy_argc_offset);
            if (!((j&0xffffffffL)<(argc&0xffffffffL))){
                throw new RuntimeException("oob");
            }
        }
        return this.getOperands().get(op_base + _indy_argv_offset + j);
    }
    //---


    public ConstantPool copy(int expand) {
        if (expand < 0) {
            throw new IllegalArgumentException("Extension length less than 0:" + expand);
        }
        int newLen = this.length() + expand;
        long addr = unsafe.allocateMemory(SIZE + (long) oopSize * (newLen + 1));
        unsafe.copyMemory(this.address, addr, SIZE + (long) oopSize * (this.length() + 1));
        ConstantPool re = getOrCreate(addr);
        re.setLength(newLen);
        re.setTags(this.getTags().copy(expand));
        return re;
    }



    private void checkBound(int val) {
        if (JVM.ENABLE_EXTRA_CHECK && (val < 1 || val >= this.getTags().length())) {
            throw new NoSuchElementException("ConstantPool index out of range: " + val);
        }
    }

    /**
     * In words
     */
    public int size() {
        return size(this.length());
    }

    // For temporary use while constructing constant pool
    public void klass_index_at_put(int which, int name_index) {
        tag_at_put(which, ClassIndex);
        unsafe.putInt(this.base()+ (long) which * oopSize,name_index);
    }

    public void copy_cp_to(int start_i, int end_i, ConstantPool to_cp, int to_i) {
        copy_cp_to_impl(this, start_i, end_i, to_cp, to_i);
    }
    public static final class Flags {
        public static final int
        _has_preresolution    = 1,       // Flags
                _on_stack             = 2,
                _is_shared            = 4,
                _has_dynamic_constant = 8;
    };

    public boolean has_dynamic_constant(){
        return (unsafe.getShort(this.address+FLAGS_OFFSET)&0xffff & Flags._has_dynamic_constant) != 0;
    }
    public void set_has_dynamic_constant(){
        unsafe.putShort(this.address+FLAGS_OFFSET, (short) ((unsafe.getShort(this.address+FLAGS_OFFSET)&0xffff)|Flags._has_dynamic_constant));
    }

    public void copy_fields(ConstantPool orig) {
        // Preserve dynamic constant information from the original pool
        if (orig.has_dynamic_constant()) {
            set_has_dynamic_constant();
        }
        set_major_version(orig.major_version());
        set_minor_version(orig.minor_version());
        set_source_file_name_index(orig.source_file_name_index());
        set_generic_signature_index(orig.generic_signature_index());
    }


    // Copy this constant pool's entries at start_i to end_i (inclusive)
    // to the constant pool to_cp's entries starting at to_i. A total of
    // (end_i - start_i) + 1 entries are copied.
    public static void copy_cp_to_impl(ConstantPool from_cp, int start_i, int end_i, ConstantPool to_cp, int to_i){
        int dest_i = to_i;  // leave original alone for debug purposes

        for (int src_i = start_i; src_i <= end_i; /* see loop bottom */ ) {
            copy_entry_to(from_cp, src_i, to_cp, dest_i);
            switch (from_cp.tag_at(src_i)) {
                case Double:
                case Long:
                    // double and long take two constant pool entries
                    src_i += 2;
                    dest_i += 2;
                    break;

                default:
                    // all others take one constant pool entry
                    src_i++;
                    dest_i++;
                    break;
            }
        }
        copy_operands(from_cp, to_cp);
    }

    // Copy this constant pool's entry at from_i to the constant pool
    // to_cp's entry at to_i.
    public static void copy_entry_to(ConstantPool from_cp, int from_i,
                              ConstantPool to_cp, int to_i) {

        int tag = from_cp.tag_at(from_i);
        switch (tag) {
            case ClassIndex:
            {
                int ki = from_cp.klass_index_at(from_i);
                to_cp.klass_index_at_put(to_i, ki);
            } break;

            case Double:
            {
                double d = from_cp.double_at(from_i);
                to_cp.double_at_put(to_i, d);
                // double takes two constant pool entries so init second entry's tag
                to_cp.tag_at_put(to_i + 1, Invalid);
            } break;

            case Fieldref:
            {
                int class_index = from_cp.uncached_klass_ref_index_at(from_i);
                int name_and_type_index = from_cp.uncached_name_and_type_ref_index_at(from_i);
                to_cp.field_at_put(to_i, class_index, name_and_type_index);
            } break;

            case Float:
            {
                float f = from_cp.float_at(from_i);
                to_cp.float_at_put(to_i, f);
            } break;

            case Integer:
            {
                int i = from_cp.int_at(from_i);
                to_cp.int_at_put(to_i, i);
            } break;

            case InterfaceMethodref:
            {
                int class_index = from_cp.uncached_klass_ref_index_at(from_i);
                int name_and_type_index = from_cp.uncached_name_and_type_ref_index_at(from_i);
                to_cp.interface_method_at_put(to_i, class_index, name_and_type_index);
            } break;

            case Long:
            {
                long l = from_cp.long_at(from_i);
                to_cp.long_at_put(to_i, l);
                // long takes two constant pool entries so init second entry's tag
                to_cp.tag_at_put(to_i + 1, Invalid);
            } break;

            case Methodref:
            {
                int class_index = from_cp.uncached_klass_ref_index_at(from_i);
                int name_and_type_index = from_cp.uncached_name_and_type_ref_index_at(from_i);
                to_cp.method_at_put(to_i, class_index, name_and_type_index);
            } break;

            case NameAndType:
            {
                int name_ref_index = from_cp.name_ref_index_at(from_i);
                int signature_ref_index = from_cp.signature_ref_index_at(from_i);
                to_cp.name_and_type_at_put(to_i, name_ref_index, signature_ref_index);
            } break;

            case StringIndex:
            {
                int si = from_cp.string_index_at(from_i);
                to_cp.string_index_at_put(to_i, si);
            } break;

            case Class:
            case UnresolvedClass:
            case UnresolvedClassInError:
            {
                // Revert to JVM_CONSTANT_ClassIndex
                int name_index = from_cp.klass_slot_at(from_i).name_index();
                if (!(from_cp.tag_at(name_index)==Utf8)){
                    throw new RuntimeException("sanity");
                }
                to_cp.klass_index_at_put(to_i, name_index);
            } break;

            case String:
            {
                Symbol s = from_cp.unresolved_string_at(from_i);
                to_cp.unresolved_string_at_put(to_i, s);
            } break;

            case Utf8:
            {
                Symbol s = from_cp.symbol_at(from_i);
                // Need to increase refcount, the old one will be thrown away and deferenced
                s.incrementRefCount();
                to_cp.symbol_at_put(to_i, s);
            } break;

            case MethodType:
            case MethodTypeInError:
            {
                int k = from_cp.method_type_index_at(from_i);
                to_cp.method_type_index_at_put(to_i, k);
            } break;

            case MethodHandle:
            case MethodHandleInError:
            {
                int k1 = from_cp.method_handle_ref_kind_at(from_i);
                int k2 = from_cp.method_handle_index_at(from_i);
                to_cp.method_handle_index_at_put(to_i, k1, k2);
            } break;

            case Dynamic:
            case DynamicInError:
            {
                int k1 = from_cp.bootstrap_methods_attribute_index(from_i);
                int k2 = from_cp.bootstrap_name_and_type_ref_index_at(from_i);
                k1 += operand_array_length(to_cp.getOperands());  // to_cp might already have operands
                to_cp.dynamic_constant_at_put(to_i, k1, k2);
            } break;

            case InvokeDynamic:
            {
                int k1 = from_cp.bootstrap_methods_attribute_index(from_i);
                int k2 = from_cp.bootstrap_name_and_type_ref_index_at(from_i);
                k1 += operand_array_length(to_cp.getOperands());  // to_cp might already have operands
                to_cp.invoke_dynamic_at_put(to_i, k1, k2);
            } break;

            // Invalid is used as the tag for the second constant pool entry
            // occupied by JVM_CONSTANT_Double or JVM_CONSTANT_Long. It should
            // not be seen by itself.
            case Invalid: // fall through
            default: {
                throw new RuntimeException("ShouldNotReachHere()");
            }
        }
    } // end copy_entry_to()

    public static void copy_operands(ConstantPool from_cp,
                                     ConstantPool to_cp) {

        int from_oplen = operand_array_length(from_cp.getOperands());
        int old_oplen  = operand_array_length(to_cp.getOperands());
        if (from_oplen != 0) {
            // append my operands to the target's operands array
            if (old_oplen == 0) {
                // Can't just reuse from_cp's operand list because of deallocation issues
                int len = from_cp.getOperands().length();
                U2Array new_ops = from_cp.getOperands().copy(0);
                to_cp.setOperands(new_ops);
            } else {
                int old_len  = to_cp.getOperands().length();
                int from_len = from_cp.getOperands().length();
                int old_off  = old_oplen * 2;
                int from_off = from_oplen * 2;
                // Use the metaspace for the destination constant pool
                U2Array new_operands = U2Array.create(old_len + from_len);
                int fillp = 0, len = 0;
                // first part of dest
                unsafe.copyMemory(to_cp.getOperands().adr_at(0),new_operands.adr_at(fillp),(len = old_off) *2L);
                fillp += len;
                // first part of src
                unsafe.copyMemory(from_cp.getOperands().adr_at(0),
                        new_operands.adr_at(fillp),
                        (len = from_off) *2L);
                fillp += len;
                // second part of dest
                unsafe.copyMemory(to_cp.getOperands().adr_at(old_off),
                        new_operands.adr_at(fillp),
                        (len = old_len - old_off) * 2L);
                fillp += len;
                // second part of src
                unsafe.copyMemory(from_cp.getOperands().adr_at(from_off),
                        new_operands.adr_at(fillp),
                        (len = from_len - from_off) * 2L);
                fillp += len;
                if (!(fillp == new_operands.length())){
                    throw new RuntimeException();
                }

                // Adjust indexes in the first part of the copied operands array.
                for (int j = 0; j < from_oplen; j++) {
                    int offset = operand_offset_at(new_operands, old_oplen + j);
                    if (!(offset == operand_offset_at(from_cp.getOperands(), j))){
                        throw new RuntimeException("correct copy");
                    }
                    offset += old_len;  // every new tuple is preceded by old_len extra u2's
                    operand_offset_at_put(new_operands, old_oplen + j, offset);
                }

                // replace target operands array with combined array
                to_cp.setOperands(new_operands);
            }
        }
    } // end copy_operands()

    public void initialize_unresolved_klasses(ClassLoaderData loader_data) {
        int len = this.length();
        int num_klasses = 0;
        for (int i = 1; i <len; i++) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (tag_at(i)) {
                case ClassIndex:
                {
                    final int class_index = klass_index_at(i);
                    unresolved_klass_at_put(i, class_index, num_klasses++);
                }
                break;
//#ifndef PRODUCT
//                case Class:
//                case UnresolvedClass:
//                case UnresolvedClassInError:
//                    // All of these should have been reverted back to ClassIndex before calling
//                    // this function.
//                    ShouldNotReachHere();
//#endif
            }
        }
        allocate_resolved_klasses(loader_data, num_klasses);
    }
    public void allocate_resolved_klasses(ClassLoaderData loader_data, int num_klasses) {
        // A ConstantPool can't possibly have 0xffff valid class entries,
        // because entry #0 must be CONSTANT_Invalid, and each class entry must refer to a UTF8
        // entry for the class's name. So at most we will have 0xfffe class entries.
        // This allows us to use 0xffff (ConstantPool::_temp_resolved_klass_index) to indicate
        // UnresolvedKlass entries that are temporarily created during class redefinition.
        if (!(num_klasses < CPKlassSlot._temp_resolved_klass_index)){
            throw new RuntimeException("sanity");
        }
        if (this.resolved_klasses()!=null){
            throw new RuntimeException("sanity");
        }
        VMTypeArray<Klass> rk = VMTypeArray.create(num_klasses,Klass.class,Klass::getOrCreate);
        set_resolved_klasses(rk);
    }
    // Create object cache in the constant pool
    public void initialize_resolved_references(@RawCType("intStack&") IntList reference_map,
                                        int constant_pool_map_length){
        // Initialized the resolved object cache.
        int map_length = reference_map.size();
        if (map_length > 0) {
            // Only need mapping back to constant pool entries.  The map isn't used for
            // invokedynamic resolved_reference entries.  For invokedynamic entries,
            // the constant pool cache index has the mapping back to both the constant
            // pool and to the resolved reference index.
            if (constant_pool_map_length > 0) {
                U2Array om = U2Array.create(constant_pool_map_length);
                for (int i = 0; i < constant_pool_map_length; i++) {
                    int x = reference_map.getInt(i);
                    if (!(x == (x&0xffff))){
                        throw new RuntimeException("klass index is too big");
                    }
                    om.set(i, (short) (x&0xffff));
                }
                this.set_reference_map(om);
            }

            // Create Java array for holding resolved strings, methodHandles,
            // methodTypes, invokedynamic and invokehandle appendix objects, etc.
            Object[] stom = new Object[map_length];
            this.set_resolved_references(OopDesc.of(stom));
        }
    }

    @Override
    public String toString() {
        return "ConstantPool@0x" + java.lang.Long.toHexString(address);
    }
}
