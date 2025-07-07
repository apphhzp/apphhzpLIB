package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.util.RawCType;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
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

    public InstanceKlass getHolder() {
        return (InstanceKlass) Klass.getOrCreate(unsafe.getAddress(this.address + HOLDER_OFFSET));
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
        return Klass.getOrCreate(this.getResolvedKlasses().getAddress(index));
    }

    public VMTypeArray<Klass> getResolvedKlasses() {
        long addr = unsafe.getAddress(address + KLASSES_OFFSET);
        if (!isEqual(this.resolvedKlassesCache, addr)) {
            this.resolvedKlassesCache = new VMTypeArray<>(addr, Klass.class, Klass::getOrCreate);
        }
        return this.resolvedKlassesCache;
    }

    public void setResolvedKlasses(VMTypeArray<Klass> val) {
        unsafe.putAddress(address + KLASSES_OFFSET, val.address);
    }

    public int getMajorVer() {
        return unsafe.getShort(this.address + MAJOR_VER_OFFSET) & 0xffff;
    }

    public void setMajorVer(int version) {
        unsafe.putShort(this.address + MAJOR_VER_OFFSET, (short) (version & 0xffff));
    }

    public int getMinorVer() {
        return unsafe.getShort(this.address + MINOR_VER_OFFSET) & 0xffff;
    }

    public void setMinorVer(int version) {
        unsafe.putShort(this.address + MINOR_VER_OFFSET, (short) (version & 0xffff));
    }

    public int getGenericSigIndex() {
        return unsafe.getShort(this.address + GENERIC_SIGNATURE_OFFSET) & 0xffff;
    }

    public void setGenericSigIndex(int index) {
        unsafe.putShort(this.address + GENERIC_SIGNATURE_OFFSET, (short) (index & 0xffff));
    }

    public int getSourceFileNameIndex() {
        return unsafe.getShort(this.address + SOURCE_FILE_NAME_OFFSET) & 0xffff;
    }

    public void setSourceFileNameIndex(int index) {
        unsafe.putShort(this.address + SOURCE_FILE_NAME_OFFSET, (short) (index & 0xffff));
    }

    public int getLength() {
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

    public byte tag_at(int which) {
        return this.getTags().get(which);
    }

    public void tag_at_put(int which, byte t) {
        this.getTags().set(which, t);
    }

    public CPSlot slot_at(int which) {
        checkBound(which);
        byte tag = tag_at(which);
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
        byte tag = tag_at(which);
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
        return this.getResolvedKlasses().get(kslot.resolved_klass_index());
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

    public Klass klass_at_impl(ConstantPool this_cp, int which) {
        JavaThread javaThread = JavaClasses.Thread.thread(Thread.currentThread());

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
            Klass klass = this_cp.getResolvedKlasses().get(resolved_klass_index);
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
            throw new RuntimeException();
//            throw_resolution_error(this_cp, which, CHECK_NULL);
//            ShouldNotReachHere();
        }
        throw new UnsupportedOperationException("Could not resolve klass in Java");
//        Oop mirror_handle;
//        Symbol name = this_cp.symbol_at(name_index);
//        ClassLoader loader=this_cp.getHolder().getClassLoaderData().getClassLoader();
//        ProtectionDomain protection_domain= (this_cp.getHolder().asClass().getProtectionDomain());
//
//        Klass* k;
//        {
//            // Turn off the single stepping while doing class resolution
//            JvmtiHideSingleStepping jhss(javaThread);
//            k = SystemDictionary::resolve_or_fail(name, loader, protection_domain, true, THREAD);
//        } //  JvmtiHideSingleStepping jhss(javaThread);
//
//        if (!HAS_PENDING_EXCEPTION) {
//            // preserve the resolved klass from unloading
//            mirror_handle = Handle(THREAD, k->java_mirror());
//            // Do access check for klasses
//            verify_constant_pool_resolve(this_cp, k, THREAD);
//        }
//
//        // Failed to resolve class. We must record the errors so that subsequent attempts
//        // to resolve this constant pool entry fail with the same error (JVMS 5.4.3).
//        if (HAS_PENDING_EXCEPTION) {
//            save_and_throw_exception(this_cp, which, constantTag(JVM_CONSTANT_UnresolvedClass), CHECK_NULL);
//            // If CHECK_NULL above doesn't return the exception, that means that
//            // some other thread has beaten us and has resolved the class.
//            // To preserve old behavior, we return the resolved class.
//            Klass* klass = this_cp->resolved_klasses()->at(resolved_klass_index);
//            assert(klass != NULL, "must be resolved if exception was cleared");
//            return klass;
//        }
//
//        // logging for class+resolve.
//        if (log_is_enabled(Debug, class, resolve)){
//            trace_class_resolution(this_cp, k);
//        }
//
//        Klass** adr = this_cp->resolved_klasses()->adr_at(resolved_klass_index);
//        Atomic::release_store(adr, k);
//        // The interpreter assumes when the tag is stored, the klass is resolved
//        // and the Klass* stored in _resolved_klasses is non-NULL, so we need
//        // hardware store ordering here.
//        // We also need to CAS to not overwrite an error from a racing thread.
//
//        jbyte old_tag = Atomic::cmpxchg((jbyte*)this_cp->tag_addr_at(which),
//                (jbyte)JVM_CONSTANT_UnresolvedClass,
//                (jbyte)JVM_CONSTANT_Class);
//
//        // We need to recheck exceptions from racing thread and return the same.
//        if (old_tag == JVM_CONSTANT_UnresolvedClassInError) {
//            // Remove klass.
//            this_cp->resolved_klasses()->at_put(resolved_klass_index, NULL);
//            throw_resolution_error(this_cp, which, CHECK_NULL);
//        }
//
//        return k;
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
        for (int index = 1, length = this_cp.getLength(); index < length; index++) { // Index 0 is unused
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
        byte tag = tag_at(which);
        if (tag != MethodHandle && tag != MethodHandleInError) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return extract_low_short_from_int(unsafe.getInt(this.base() + (long) which * oopSize));  // mask out unwanted ref_index bits
    }

    public int method_handle_index_at(int which) {
        byte tag = tag_at(which);
        if (tag != MethodHandle && tag != MethodHandleInError) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return extract_high_short_from_int(unsafe.getInt(this.base() + (long) which * oopSize));  // shift out unwanted ref_kind bits
    }

    public int method_type_index_at(int which) {
        byte tag = tag_at(which);
        if (tag != MethodType && tag != MethodTypeInError) {
            throw new IllegalArgumentException("Corrupted constant pool");
        }
        return unsafe.getInt(this.base() + (long) which * oopSize);
    }

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
            byte tag = tag_at(i);
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
        return this.getCache().getEntry(cp_cache_index);
    }

    // Given the per-instruction index of an indy instruction, report the
    // main constant pool entry for its bootstrap specifier.
    // From there, uncached_name/signature_ref_at will get the name/type.
    public int invokedynamic_bootstrap_ref_index_at(int indy_index) {
        return invokedynamic_cp_cache_entry_at(indy_index).getConstantPoolIndex();
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
            byte tag = tag_at(i);
            if (!(tag == Methodref || tag == Fieldref || tag == InterfaceMethodref)) {
                throw new RuntimeException("Corrupted constant pool");
            }
        }
        int ref_index = unsafe.getInt(this.address + SIZE + (long) i * oopSize);
        return ref_index & 0xFFFF;
    }


    public int remap_instruction_operand_from_cache(int operand) {
        return this.getCache().getEntry(operand).getConstantPoolIndex();
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
        this.getResolvedKlasses().set(resolved_klass_index,k);
        // The interpreter assumes when the tag is stored, the klass is resolved
        // and the Klass* non-NULL, so we need hardware store ordering here.
        //      ^
        //      |
        //Not implemented
        tag_at_put(class_index, Class);
    }
    //---


    public ConstantPool copy(int expand) {
        if (expand < 0) {
            throw new IllegalArgumentException("Extension length less than 0:" + expand);
        }
        int newLen = this.getLength() + expand;
        long addr = unsafe.allocateMemory(SIZE + (long) oopSize * (newLen + 1));
        unsafe.copyMemory(this.address, addr, SIZE + (long) oopSize * (this.getLength() + 1));
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
        return size(this.getLength());
    }

    @Override
    public String toString() {
        return "ConstantPool@0x" + java.lang.Long.toHexString(address);
    }
}
