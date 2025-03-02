package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.method.Method;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelper.*;
import static apphhzp.lib.helfy.JVM.includeCDS;
import static apphhzp.lib.helfy.JVM.oopSize;

public class Klass extends Metadata {
    public static final long klassOffset = unsafe.getInt(JVM.type("java_lang_Class").global("_klass_offset"));
    public static final Type TYPE = JVM.type("Klass");
    public static final int SIZE = TYPE.size;
    public static final long LAYOUT_HELPER_OFFSET = TYPE.offset("_layout_helper");
    public static final long ID_OFFSET=JVM.computeOffset(JVM.intSize,LAYOUT_HELPER_OFFSET+4);
    public static final long MODIFIER_FLAGS_OFFSET = TYPE.offset("_modifier_flags");
    public static final long SUPER_CHECK_OFFSET_OFFSET= TYPE.offset("_super_check_offset");
    public static final long NAME_OFFSET = TYPE.offset("_name");
    public static final long SECONDARY_SUPER_CACHE_OFFSET= TYPE.offset("_secondary_super_cache");
    public static final long SECONDARY_SUPERS_OFFSET= TYPE.offset("_secondary_supers");
    public static final long PRIMARY_SUPERS_OFFSET= TYPE.offset("_primary_supers[0]");
    public static final long MIRROR_OFFSET = TYPE.offset("_java_mirror");
    public static final long SUPER_OFFSET = TYPE.offset("_super");
    public static final long SUBKLASS_OFFSET= TYPE.offset("_subklass");
    public static final long NEXT_SIBLING_OFFSET = TYPE.offset("_next_sibling");
    public static final long NEXT_LINK_OFFSET = TYPE.offset("_next_link");
    public static final long CLASSLOADER_DATA_OFFSET = TYPE.offset("_class_loader_data");
    public static final long VTABLE_LEN_OFFSET = TYPE.offset("_vtable_len");
    public static final long ACC_FLAGS_OFFSET = TYPE.offset("_access_flags");
    public static final long LAST_BIASED_LOCK_BULK_REVOCATION_TIME_OFFSET=
            JVM.includeJFR?JVM.computeOffset(8,JVM.computeOffset(8,ACC_FLAGS_OFFSET+AccessFlags.SIZE)+8):JVM.computeOffset(8,ACC_FLAGS_OFFSET+AccessFlags.SIZE);
    public static final long PROTOTYPE_HEADER_OFFSET = TYPE.offset("_prototype_header");
    public static final long BIASED_LOCK_REVOCATION_COUNT_OFFSET = PROTOTYPE_HEADER_OFFSET + MarkWord.SIZE;
    public static final long SHARED_CLASS_PATH_INDEX_OFFSET = BIASED_LOCK_REVOCATION_COUNT_OFFSET + 4;
    public static final long SHARED_CLASS_FLAGS_OFFSET = includeCDS ? SHARED_CLASS_PATH_INDEX_OFFSET + 2 : SHARED_CLASS_PATH_INDEX_OFFSET;

    public static final int _archived_lambda_proxy_is_available = 2, _has_value_based_class_annotation = 4, _verified_at_dump_time = 8;
    public static final int _primary_super_limit=JVM.intConstant("Klass::_primary_super_limit");

    private static final Long2ObjectMap<Klass> CACHE = new Long2ObjectOpenHashMap<>();
    private Symbol nameCache;
    private Klass superKlassCache;
    private Klass nextSiblingCache;
    private Klass nextKlassCache;
    private ClassLoaderData CLDCache;
    private VMTypeArray<Klass> secondarySupersCache;
    private OopDesc mirrorCache;

    public static Klass asKlass(Class<?> target) {
        long addr = (oopSize == 8 ? unsafe.getLong(target, klassOffset) : unsafe.getInt(target, klassOffset) & 0xffffffffL);
        return getOrCreate(addr);
    }

    public static Klass getKlass(Object obj) {
//        if (obj instanceof Class<?>)
//            throw new IllegalArgumentException("getKlass(Object) couldn't accept Class object");
        long addr = JVM.usingCompressedClassPointers || !is64BitJVM ? OopDesc.decodeKlass(unsafe.getIntVolatile(obj, OopDesc.DESC_KLASS_OFFSET)) : unsafe.getLongVolatile(obj, OopDesc.DESC_KLASS_OFFSET);
        return getOrCreate(addr);
    }

    public static Klass getOrCreate(long addr) {
        if (addr == 0L) {
            throw new IllegalArgumentException("Klass pointer is NULL(0).");
        }
        Klass re = CACHE.get(addr);
        if (re != null) {
            return re;
        }
        int layout = unsafe.getInt(addr + LAYOUT_HELPER_OFFSET);
        int id=unsafe.getInt(addr+ID_OFFSET);
        if (id==KlassID.InstanceKlassID){
            re=new InstanceKlass(addr);
        }else if (id==KlassID.InstanceMirrorKlassID){
            re=new InstanceMirrorKlass(addr);
        }else if (id==KlassID.ObjArrayKlassID){
            re=new ObjArrayKlass(addr);
        }else if (id==KlassID.TypeArrayKlassID){
            re=new TypeArrayKlass(addr);
        }else if(id==KlassID.InstanceClassLoaderKlassID){
            re=new InstanceClassLoaderKlass(addr);
        }else if (id==KlassID.InstanceRefKlassID){
            re=new InstanceRefKlass(addr);
        } else {
            re = new Klass(addr);
        }
//        if (LayoutHelper.isInstance(layout)) {
//            if (id==KlassID.InstanceMirrorKlassID){
//                re=new InstanceMirrorKlass(addr);
//            }else {
//                re = new InstanceKlass(addr);
//            }
//            CACHE.put(addr, re);
//            return re;
//        }else if (LayoutHelper.isArray(layout)){
//            if (LayoutHelper.isTypeArray(layout)){
//                re = new TypeArrayKlass(addr);
//            }else{
//                re = new ObjArrayKlass(addr);
//            }
//            CACHE.put(addr, re);
//            return re;
//        }

        CACHE.put(addr, re);
        return re;
    }

    public static List<Klass> getAllKlasses() {
        ArrayList<Klass> re = new ArrayList<>();
        for (ClassLoaderData cld : ClassLoaderData.getAllClassLoaderData()) {
            re.addAll(cld.getKlasses());
        }
        return re;
    }

    protected Klass(long addr) {
        super(addr);
    }

    public boolean isInstanceKlass() {
        return false;
    }

    public InstanceKlass asInstanceKlass() {
        if (this.isInstanceKlass()) {
            return (InstanceKlass) this;
        }
        throw new IllegalStateException("Klass@0x" + Long.toHexString(this.address) + " is not an instanceKlass!");
    }

    public int getLayout() {
        return unsafe.getInt(this.address + LAYOUT_HELPER_OFFSET);
    }

    public void setLayout(int val) {
        unsafe.putInt(this.address + LAYOUT_HELPER_OFFSET, val);
    }

    public boolean isArrayKlass() {
        return  LayoutHelper.isArray(this.getLayout());
    }

    public boolean isObjArrayKlass() {
        return LayoutHelper.isObjArray(this.getLayout());
    }

    public boolean isTypeArrayKlass() {
        return LayoutHelper.isTypeArray(this.getLayout());
    }

    public int getModifierFlags() {
        return unsafe.getInt(this.address + MODIFIER_FLAGS_OFFSET);
    }

    public void putModifierFlags(int val) {
        unsafe.putInt(this.address + MODIFIER_FLAGS_OFFSET, val);
    }

    public Symbol getName() {
        long addr = unsafe.getAddress(this.address + NAME_OFFSET);
        //Unlikely
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.nameCache, addr)) {
            this.nameCache = Symbol.of(addr);
        }
        return this.nameCache;
    }

    public void setName(Symbol name) {
        unsafe.putAddress(this.address + NAME_OFFSET, name.address);
    }

    @Nullable
    public Klass getSibling() {
        long addr = unsafe.getAddress(this.address + NEXT_SIBLING_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.nextSiblingCache, addr)) {
            this.nextSiblingCache = getOrCreate(addr);
        }
        return this.nextSiblingCache;
    }

    public void setSibling(@Nullable Klass sibling) {
        unsafe.putAddress(this.address + NEXT_SIBLING_OFFSET, sibling == null ? 0L : sibling.address);
    }

    @Nullable
    public Klass getNextKlass() {
        long addr = unsafe.getAddress(this.address + NEXT_LINK_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.nextKlassCache, addr)) {
            this.nextKlassCache = getOrCreate(addr);
        }
        return this.nextKlassCache;
    }

    public void setNextKlass(@Nullable Klass next) {
        unsafe.putAddress(this.address + NEXT_LINK_OFFSET, next == null ? 0L : next.address);
    }

    @Nullable
    public Klass getSuperKlass() {
        long addr = unsafe.getAddress(this.address + SUPER_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.superKlassCache, addr)) {
            this.superKlassCache = getOrCreate(addr);
        }
        return this.superKlassCache;
    }

    public void setSuperKlass(@Nullable Klass superKlass) {
        unsafe.putAddress(this.address + SUPER_OFFSET, superKlass == null ? 0L : superKlass.address);
    }

    public VMTypeArray<Klass> getSecondarySupers(){
        long addr = unsafe.getAddress(this.address + SECONDARY_SUPERS_OFFSET);
        if (addr==0L){
            throw new RuntimeException();
        }
        if (!isEqual(this.secondarySupersCache, addr)) {
            this.secondarySupersCache=new VMTypeArray<>(addr,Klass.class,Klass::getOrCreate);
        }
        return this.secondarySupersCache;
    }

    public void setSecondarySupers(VMTypeArray<Klass> secondarySupers) {
        unsafe.putAddress(this.address+SECONDARY_SUPERS_OFFSET, secondarySupers.address);
    }

    public int superCheckOffset(){
        return unsafe.getInt(this.address+SUPER_CHECK_OFFSET_OFFSET);
    }

    public boolean canBePrimarySuper() {
        int secondary_offset = (int) SECONDARY_SUPER_CACHE_OFFSET;
        return superCheckOffset() != secondary_offset;
    }

    @Nullable
    public Klass primarySuperOfDepth(int i){
        if (i<0||i>=_primary_super_limit){
            throw new IndexOutOfBoundsException();
        }
        long addr=unsafe.getAddress(this.address+PRIMARY_SUPERS_OFFSET+ (long) i * oopSize);
        if (addr==0L){
            return null;
        }
        //assert(re == NULL || re->super_depth() == i, "correct display");
        //if (!(re==null||re.))
        return Klass.getOrCreate(addr);
    }

    public OopDesc getMirror() {
        long addr = OopDesc.fromOopHandle(this.address + MIRROR_OFFSET);
        if (!isEqual(this.mirrorCache, addr)) {
            this.mirrorCache = OopDesc.of(addr);
        }
        return this.mirrorCache;
    }

    public Class<?> asClass() {
        return this.getMirror().getObject();
    }

    public AccessFlags getAccessFlags() {
        return AccessFlags.getOrCreate(unsafe.getInt(this.address + ACC_FLAGS_OFFSET));
    }

    public void setAccessFlags(int flags) {
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }

    public void setAccessible() {
        int flags = unsafe.getInt(this.address + ACC_FLAGS_OFFSET);
        flags |= Opcodes.ACC_PUBLIC;
        flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }

    public void updateReflectionData() {
        try {
            unsafe.putObjectVolatile(this.asClass(), unsafe.objectFieldOffset(Class.class.getDeclaredField("reflectionData")), null);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public ClassLoaderData getClassLoaderData() {
        long addr = unsafe.getAddress(this.address + CLASSLOADER_DATA_OFFSET);
        if (this.CLDCache == null || this.CLDCache.address != addr) {
            this.CLDCache = ClassLoaderData.getOrCreate(addr);
        }
        return this.CLDCache;
    }

    public void setClassLoaderData(ClassLoaderData cld) {
        this.CLDCache = null;
        unsafe.putAddress(this.address + CLASSLOADER_DATA_OFFSET, cld.address);
    }

    public boolean isAssignableFrom(Klass klass) {
        if (klass.address == this.address) {
            return true;
        }
        Klass fa = this.getSuperKlass();
        if (fa != null) {
            return fa.isAssignableFrom(klass);
        }
        return false;
    }

    public int getVTableLength() {
        return unsafe.getInt(this.address + VTABLE_LEN_OFFSET);
    }

    public long startOfVTable(){
        return this.address+InstanceKlass.SIZE;
    }

    public VTableEntry getVTableEntry(int index) {
        int limit=this.getVTableLength()/(VTableEntry.SIZE/ oopSize);
        if (index < 0 || index >= limit){
            throw new ArrayIndexOutOfBoundsException(String.format("index %d out of bounds %d", index, limit));
        }
        return new VTableEntry((this.startOfVTable() + (long) index * VTableEntry.SIZE));
    }

    public Method getMethodAtVTable(int index) {
        return this.getVTableEntry(index).method();
    }

    public Iterable<VTableEntry> getVTableEntries() {
        return new Iterable<>() {
            @Override
            public Iterator<VTableEntry> iterator() {
                return  new Iterator<>(){
                    private final long begin = Klass.this.startOfVTable();
                    private final int length = Klass.this.getVTableLength();
                    private int now=0;
                    @Override
                    public boolean hasNext() {
                        return now*(VTableEntry.SIZE/ oopSize)<length;
                    }

                    @Override
                    public VTableEntry next() {
                        if(!hasNext()){
                            throw new NoSuchElementException();
                        }
                        return new VTableEntry((this.begin + (long)(now++) * VTableEntry.SIZE));
                    }
                };
            }
        };
    }

    public MarkWord getPrototypeHeader() {
        return new MarkWord(unsafe.getAddress(this.address + PROTOTYPE_HEADER_OFFSET));
    }

    public void setPrototypeHeader(long prototypeHeader) {
        unsafe.putAddress(this.address + PROTOTYPE_HEADER_OFFSET, prototypeHeader);
    }

    public int getSharedClassFlags() {
        return unsafe.getShort(this.address + SHARED_CLASS_FLAGS_OFFSET) & 0xffff;
    }

    public void setSharedClassFlags(int flags) {
        unsafe.putShort(this.address + SHARED_CLASS_FLAGS_OFFSET, (short) (flags & 0xffff));
    }

    public void setHasValueBasedClassAnnotation() {
        if (includeCDS) {
            this.setSharedClassFlags(this.getSharedClassFlags() | _has_value_based_class_annotation);
        }
    }

    public void clearHasValueBasedClassAnnotation() {
        if (includeCDS) {
            this.setSharedClassFlags(this.getSharedClassFlags() & ~_has_value_based_class_annotation);
        }
    }

    public boolean hasValueBasedClassAnnotation() {
        if (includeCDS) {
            return (this.getSharedClassFlags() & _has_value_based_class_annotation) != 0;
        }
        return false;
    }

    public long oopSize(OopDesc oop){
        return 0L;
    }


    public boolean isPublic()                { return this.getAccessFlags().isPublic(); }
    public boolean isFinal()                 { return this.getAccessFlags().isFinal(); }
    public boolean isInterface()             { return this.getAccessFlags().isInterface(); }
    public boolean isAbstract()              { return this.getAccessFlags().isAbstract(); }
    public boolean isSuper()                 { return this.getAccessFlags().isSuper(); }
    public boolean isSynthetic()             { return this.getAccessFlags().isSynthetic(); }
    //public void set_is_synthetic()               { this.getAccessFlags().set_is_synthetic(); }
    public boolean hasFinalizer()             { return this.getAccessFlags().hasFinalizer(); }
    public boolean hasFinalMethod()         { return this.getAccessFlags().hasFinalMethod(); }
    //public void set_has_finalizer()              { this.getAccessFlags().setHas_finalizer(); }
    //public void set_has_final_method()           { this.getAccessFlags().set_has_final_method(); }
    public boolean hasVanillaConstructor()  { return this.getAccessFlags().hasVanillaConstructor(); }
    //public void set_has_vanilla_constructor()    { this.getAccessFlags().set_has_vanilla_constructor(); }
    public boolean hasMirandaMethods ()     { return this.getAccessFlags().hasMirandaMethods(); }
    //public void set_has_miranda_methods()        { this.getAccessFlags().set_has_miranda_methods(); }
    public boolean isShared()                { return this.getAccessFlags().isSharedClass(); } // shadows MetaspaceObj::is_shared)()
    //public void set_is_shared()                  { this.getAccessFlags().set_is_shared_class(); }
    public boolean isHidden()                { return this.getAccessFlags().isHiddenClass(); }
    //public void set_is_hidden()                  { this.getAccessFlags().set_is_hidden_class(); }
    public boolean isValueBased()                 { return this.getAccessFlags().isValueBasedClass(); }
    //public void set_is_value_based()             { this.getAccessFlags().set_is_value_based_class(); }

    @Override
    public String toString() {
        return "Klass(" + this.getName() + ")@0x" + Long.toHexString(this.address);
    }

    public static class LayoutHelper {
        public static int _lh_neutral_value = JVM.intConstant("Klass::_lh_neutral_value");  // neutral non-array non-instance value
        public static int _lh_instance_slow_path_bit = JVM.intConstant("Klass::_lh_instance_slow_path_bit");
        public static int _lh_log2_element_size_shift = JVM.intConstant("Klass::_lh_log2_element_size_shift");
        public static int _lh_log2_element_size_mask = JVM.intConstant("Klass::_lh_log2_element_size_mask");
        public static int _lh_element_type_shift = JVM.intConstant("Klass::_lh_element_type_shift");
        public static int _lh_element_type_mask = JVM.intConstant("Klass::_lh_element_type_mask");  // shifted mask
        public static int _lh_header_size_shift = JVM.intConstant("Klass::_lh_header_size_shift");
        public static int _lh_header_size_mask = JVM.intConstant("Klass::_lh_header_size_mask");  // shifted mask
//        public static int _lh_array_tag_bits = 2;
        public static int _lh_array_tag_shift = JVM.intConstant("Klass::_lh_array_tag_shift");
        public static int _lh_array_tag_obj_value = JVM.intConstant("Klass::_lh_array_tag_obj_value");   // 0x80000000 >> 30

        public static int _lh_array_tag_type_value = JVM.intConstant("Klass::_lh_array_tag_type_value"); // ~0x00,  // 0xC0000000 >> 30

        public static boolean isArray(int lh) {
            return lh < _lh_neutral_value;
        }

        public static boolean isInstance(int lh) {
            return lh > _lh_neutral_value;
        }

        public static boolean isTypeArray(int lh) {
            // _lh_array_tag_type_value == (lh >> _lh_array_tag_shift);
            return (lh&0xffffffffL) >= (((_lh_array_tag_type_value&0xffffffffL) << _lh_array_tag_shift)&0xffffffffL);
        }

        public static boolean isObjArray(int lh) {
            // _lh_array_tag_obj_value == (lh >> _lh_array_tag_shift);
            return lh < (_lh_array_tag_type_value << _lh_array_tag_shift);
        }

        public static int toSizeHelper(int lh) {
            if (lh <= 0) {
                throw new IllegalArgumentException("must be instance");
            }
            return lh >> JVM.LogBytesPerWord;
        }

        public static boolean needsSlowPath(int lh) {
            if (lh <= 0) {
                throw new IllegalArgumentException("must be instance");
            }
            return (lh & _lh_instance_slow_path_bit) != 0;
        }

        public static int log2ElementSize(int lh) {
            if (lh>=0){
                throw new IllegalArgumentException("must be array");
            }
            int l2esz = (lh >> _lh_log2_element_size_shift) & _lh_log2_element_size_mask;
            if (l2esz>JVM.LogBytesPerLong) {
                throw new RuntimeException("sanity. l2esz: 0x"+Long.toHexString(l2esz)+" for lh: 0x"+Long.toHexString(lh));
            }
            return l2esz;
        }

        public static int headerSize(int lh) {
            if (lh>=0){
                throw new IllegalArgumentException("must be array");
            }
            int hsize = (lh >> _lh_header_size_shift) & _lh_header_size_mask;
            if (!(hsize > 0 && hsize < (int)OopDesc.SIZE*3)){
                throw new RuntimeException("sanity");
            }
            return hsize;
        }
    }
}
