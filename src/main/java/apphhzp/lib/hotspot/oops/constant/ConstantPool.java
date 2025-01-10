package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static apphhzp.lib.ClassHelper.unsafe;
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
    public static final long FLAGS_OFFSET=JVM.includeJVMCI?TYPE.offset("_flags"):SOURCE_FILE_NAME_OFFSET+2;
    public static final long LENGTH_OFFSET = TYPE.offset("_length");
    private static final HashMap<Long,ConstantPool> CACHE=new HashMap<>();
    private U1Array tagsCache;
    private ConstantPoolCache cacheCache;
    private U2Array operandsCache;
    private VMTypeArray<Klass> resolvedKlassesCache;
    public static ConstantPool getOrCreate(long addr){
        if (addr==0L){
            throw new IllegalArgumentException("The pointer is NULL(0L)!");
        }
        ConstantPool re=CACHE.get(addr);
        if (re!=null){
            return re;
        }
        CACHE.put(addr,re=new ConstantPool(addr));
        return re;
    }
    public static void clearCacheMap(){
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private ConstantPool(long addr) {
        super(addr);
    }

    public long constantAddress(int which) {
        checkBound(which);
        return this.address + SIZE + (long) which * JVM.oopSize;
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
                Symbol tmp = Symbol.of(unsafe.getAddress(this.address + SIZE + (long) i * JVM.oopSize));
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
            this.resolvedKlassesCache = new VMTypeArray<>(addr, Klass.class,Klass::getOrCreate);
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

    public void int_at_put(int which, int i) {
        checkBound(which);
        this.getTags().set(which, Integer);
        unsafe.putInt(this.address + SIZE + (long) which * JVM.oopSize, i);
    }

    public void float_at_put(int which, float f) {
        checkBound(which);
        this.getTags().set(which, Float);
        unsafe.putFloat(this.address + SIZE + (long) which * JVM.oopSize, f);
    }

    public void long_at_put(int which, long l) {
        checkBound(which);
        this.getTags().set(which, Long);
        unsafe.putLong(this.address + SIZE + (long) which * JVM.oopSize, l);
    }

    public void double_at_put(int which, double d) {
        checkBound(which);
        this.getTags().set(which, Double);
        unsafe.putDouble(this.address + SIZE + (long) which * JVM.oopSize, d);
    }

    public void utf8_at_put(int which, Symbol symbol) {//symbol_at_put
        checkBound(which);
        this.getTags().set(which, Utf8);
        unsafe.putAddress(this.address + SIZE + (long) JVM.oopSize * which, symbol.address);
    }

    public void string_at_put(int obj_index, Object str) {
        ConstantPoolCache cache = this.getCache();
        checkBound(cache.objectToCpcIndex(obj_index));
        cache.getResolvedReferences()[obj_index] = str;
    }

    public void string_index_at_put(int which, int string_index) {
        checkBound(which);
        this.getTags().set(which, StringIndex);
        unsafe.putInt(this.address + SIZE + (long) JVM.oopSize * which, string_index);
    }

    public void methodRef_at_put(int which, int klass_index, int nameAndType_index) {
        checkBound(which);
        this.getTags().set(which, Methodref);
        unsafe.putInt(this.address + SIZE + (long) which * JVM.oopSize, (nameAndType_index << 16) | klass_index);
    }

    public void fieldRef_at_put(int which, int klass_index, int nameAndType_index) {
        checkBound(which);
        this.getTags().set(which, Fieldref);
        unsafe.putInt(this.address + SIZE + (long) which * JVM.oopSize, (nameAndType_index << 16) | klass_index);
    }

    public void nameAndType_at_put(int which, int name_index, int type_index) {
        checkBound(which);
        this.getTags().set(which, NameAndType);
        unsafe.putInt(this.address + SIZE + (long) which * JVM.oopSize, (type_index << 16) | name_index);
    }

    public void klass_at_put(int which, int resolved_klass_index, int name_index) {
        checkBound(which);
        this.getTags().set(which, Class);
        unsafe.putInt(this.address + SIZE + (long) which * JVM.oopSize, (name_index << 16) | resolved_klass_index);
    }

    public void klass_index_at_put(int which, int name_index) {
        checkBound(which);
        this.getTags().set(which, ClassIndex);
        unsafe.putInt(this.address + SIZE + (long) which * JVM.oopSize, name_index);
    }

    public ConstantPool copy(int expand) {
        if (expand < 0) {
            throw new IllegalArgumentException("Extension length less than 0:" + expand);
        }
        int newLen = this.getLength() + expand;
        long addr = unsafe.allocateMemory(SIZE + (long) JVM.oopSize * (newLen + 1));
        unsafe.copyMemory(this.address, addr, SIZE + (long) JVM.oopSize * (this.getLength() + 1));
        ConstantPool re = getOrCreate(addr);
        re.setLength(newLen);
        re.setTags(this.getTags().copy(expand));
        return re;
    }

    private void checkBound(int val) {
        if (JVM.ASSERTS_ENABLED && (val < 1 || val >= this.getTags().length())) {
            throw new NoSuchElementException("ConstantPool index out of range: "+val);
        }
    }

    @Override
    public String toString() {
        return "ConstantPool@0x" + java.lang.Long.toHexString(address);
    }
}
