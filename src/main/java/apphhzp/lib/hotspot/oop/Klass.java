package apphhzp.lib.hotspot.oop;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oop.method.Method;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static apphhzp.lib.ClassHelper.*;
import static apphhzp.lib.helfy.JVM.includeCDS;
import static apphhzp.lib.helfy.JVM.oopSize;

public class Klass extends Metadata {
    public static final long klassOffset = unsafe.getInt(JVM.type("java_lang_Class").global("_klass_offset"));
    public static final Type TYPE = JVM.type("Klass");
    public static final int SIZE = TYPE.size;
    public static final long LAYOUT_HELPER_OFFSET = TYPE.offset("_layout_helper");
    public static final long MODIFIER_FLAGS_OFFSET = TYPE.offset("_modifier_flags");
    public static final long NAME_OFFSET = TYPE.offset("_name");
    public static final long MIRROR_OFFSET = TYPE.offset("_java_mirror");
    public static final long SUPER_OFFSET = TYPE.offset("_super");
    public static final long NEXT_SIBLING_OFFSET = TYPE.offset("_next_sibling");
    public static final long NEXT_LINK_OFFSET = TYPE.offset("_next_link");
    public static final long CLASSLOADER_DATA_OFFSET = TYPE.offset("_class_loader_data");
    public static final long VTABLE_LEN_OFFSET = TYPE.offset("_vtable_len");
    public static final long ACC_FLAGS_OFFSET = TYPE.offset("_access_flags");
    public static final long PROTOTYPE_HEADER_OFFSET=TYPE.offset("_prototype_header");
    public static final long BIASED_LOCK_REVOCATION_COUNT_OFFSET=PROTOTYPE_HEADER_OFFSET+JVM.type("markWord").size;
    public static final long SHARED_CLASS_PATH_INDEX_OFFSET=BIASED_LOCK_REVOCATION_COUNT_OFFSET+4;
    public static final long SHARED_CLASS_FLAGS_OFFSET= includeCDS?SHARED_CLASS_PATH_INDEX_OFFSET+2:SHARED_CLASS_PATH_INDEX_OFFSET;
    public static final int _archived_lambda_proxy_is_available = 2, _has_value_based_class_annotation = 4, _verified_at_dump_time = 8;
    private static final Long2ObjectMap<Klass> CACHE = new Long2ObjectOpenHashMap<>();
    private Symbol nameCache;
    private Klass superKlassCache;
    private Klass nextSiblingCache;
    private Klass nextKlassCache;
    private ClassLoaderData CLDCache;
    private OopDesc mirrorCache;

    public static Klass asKlass(Class<?> target) {
        long addr = (oopSize == 8 ? unsafe.getLong(target, klassOffset) : unsafe.getInt(target, klassOffset) & 0xffffffffL);
        return getOrCreate(addr);
    }

    public static Klass getKlass(Object obj) {
        if (obj instanceof Class<?>)
            throw new IllegalArgumentException("getKlass(Object) couldn't accept Class object");
        long addr = JVM.usingCompressedClassPointers || !is64BitJVM ? OopDesc.decodeKlass(unsafe.getIntVolatile(obj, OopDesc.DESC_KLASS_OFFSET)) : unsafe.getLongVolatile(obj, OopDesc.DESC_KLASS_OFFSET);
        return getOrCreate(addr);
    }

    public static Klass getOrCreate(long addr) {
        if (addr == 0L) {
            throw new IllegalArgumentException("Klass pointer is NULL(0).");
        }
        Klass re=CACHE.get(addr);
        if (re!=null) {
            return re;
        }
        int layout = unsafe.getInt(addr + LAYOUT_HELPER_OFFSET);
        if (layout > 0) {
            re = new InstanceKlass(addr);
            CACHE.put(addr, re);
            return re;
        } else if (layout < 0) {//ArrayKlass
            //throw new IllegalStateException("Unsupported Klass:ArrayKlass");
        }
        re = new Klass(addr);
        CACHE.put(addr, re);
        return re;
    }

    public static List<Klass> getAllKlasses(){
        ArrayList<Klass> re=new ArrayList<>();
        for (ClassLoaderData cld:ClassLoaderData.getAllClassLoaderData()){
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

    public InstanceKlass asInstanceKlass(){
        if (this.isInstanceKlass()){
            return (InstanceKlass) this;
        }
        throw new IllegalStateException("Klass@0x"+Long.toHexString(this.address)+" is not an instanceKlass!");
    }

    public int getLayout(){
        return unsafe.getInt(this.address+LAYOUT_HELPER_OFFSET);
    }

    public void setLayout(int val){
        unsafe.putInt(this.address+LAYOUT_HELPER_OFFSET,val);
    }

    public int getModifierFlags(){
        return unsafe.getInt(this.address+MODIFIER_FLAGS_OFFSET);
    }

    public void putModifierFlags(int val){
        unsafe.putInt(this.address+MODIFIER_FLAGS_OFFSET,val);
    }

    public Symbol getName() {
        long addr = unsafe.getAddress(this.address + NAME_OFFSET);
        //Unlikely
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nameCache,addr)) {
            this.nameCache = Symbol.of(addr);
        }
        return this.nameCache;
    }

    public void setName(Symbol name){
        unsafe.putAddress(this.address+NAME_OFFSET,name.address);
    }

    @Nullable
    public Klass getSibling() {
        long addr = unsafe.getAddress(this.address + NEXT_SIBLING_OFFSET);
        if (addr==0L) {
            return null;
        }
        if (!isEqual(this.nextSiblingCache,addr)) {
            this.nextSiblingCache = getOrCreate(addr);
        }
        return this.nextSiblingCache;
    }

    public void setSibling(@Nullable Klass sibling) {
        unsafe.putAddress(this.address + NEXT_SIBLING_OFFSET,sibling==null?0L:sibling.address);
    }

    @Nullable
    public Klass getNextKlass() {
        long addr = unsafe.getAddress(this.address + NEXT_LINK_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.nextKlassCache,addr)) {
            this.nextKlassCache = getOrCreate(addr);
        }
        return this.nextKlassCache;
    }

    public void setNextKlass(@Nullable Klass next) {
        unsafe.putAddress(this.address + NEXT_LINK_OFFSET,next==null?0L:next.address);
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
        unsafe.putAddress(this.address + SUPER_OFFSET, superKlass==null?0L:superKlass.address);
    }

    public OopDesc getMirror() {
        long addr = OopDesc.fromOopHandle(this.address + MIRROR_OFFSET);
        if (!isEqual(this.mirrorCache, addr)) {
            this.mirrorCache = new OopDesc(addr);
        }
        return this.mirrorCache;
    }

    public Class<?> asClass(){
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

    public void updateReflectionData(){
        try {
            unsafe.putObjectVolatile(this.asClass(),unsafe.objectFieldOffset(Class.class.getDeclaredField("reflectionData")),null);
        }catch (Throwable t){
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

    public void setClassLoaderData(ClassLoaderData cld){
        this.CLDCache=null;
        unsafe.putAddress(this.address+CLASSLOADER_DATA_OFFSET,cld.address);
    }

    public boolean isAssignableFrom(Klass klass){
        if (klass.address==this.address){
            return true;
        }
        Klass fa=this.getSuperKlass();
        if (fa!=null){
            return fa.isAssignableFrom(klass);
        }
        return false;
    }

    public int getVTableLen() {
        return unsafe.getInt(this.address + VTABLE_LEN_OFFSET);
    }

    public Method getMethodAtVTable(int index){
        if (index<0||index>=this.getVTableLen()){
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return Method.getOrCreate(unsafe.getAddress(this.address+InstanceKlass.SIZE+ (long) index * oopSize));
    }

    public MarkWord getPrototypeHeader() {
        return new MarkWord(unsafe.getAddress(this.address+PROTOTYPE_HEADER_OFFSET));
    }

    public void setPrototypeHeader(long prototypeHeader) {
        unsafe.putAddress(this.address+PROTOTYPE_HEADER_OFFSET, prototypeHeader);
    }

    public int getSharedClassFlags(){
        return unsafe.getShort(this.address+SHARED_CLASS_FLAGS_OFFSET)&0xffff;
    }

    public void setSharedClassFlags(int flags){
        unsafe.putShort(this.address+SHARED_CLASS_FLAGS_OFFSET,(short) (flags&0xffff));
    }

    public void setHasValueBasedClassAnnotation(){
        if (includeCDS){
            this.setSharedClassFlags(this.getSharedClassFlags()|_has_value_based_class_annotation);
        }
    }

    public void clearHasValueBasedClassAnnotation(){
        if (includeCDS){
            this.setSharedClassFlags(this.getSharedClassFlags()&~_has_value_based_class_annotation);
        }
    }
    public boolean hasValueBasedClassAnnotation(){
        if (includeCDS){
            return (this.getSharedClassFlags()&_has_value_based_class_annotation)!=0;
        }
        return false;
    }

    public static int layout_helper_to_size_helper(int lh) {
        if (lh<=0){
            throw new IllegalArgumentException("must be instance");
        }
        // Note that the following expression discards _lh_instance_slow_path_bit.
        return lh >> JVM.LogBytesPerWord;
    }

    @Override
    public String toString() {
        return "Klass(" + this.getName() + ")@0x" + Long.toHexString(this.address);
    }
}
