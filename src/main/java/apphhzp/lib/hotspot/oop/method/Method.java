package apphhzp.lib.hotspot.oop.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.compiler.CompLevel;
import apphhzp.lib.hotspot.oop.AccessFlags;
import apphhzp.lib.hotspot.oop.InstanceKlass;
import apphhzp.lib.hotspot.oop.MethodCounters;
import apphhzp.lib.hotspot.oop.MethodData;
import apphhzp.lib.hotspot.prims.VMIntrinsics;
import apphhzp.lib.hotspot.runtime.AdapterHandlerEntry;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelper.unsafe;

public class Method extends JVMObject {
    public static final Type TYPE = JVM.type("Method");
    public static final int SIZE = TYPE.size;
    public static final long CONSTMETHOD_OFFSET = TYPE.offset("_constMethod");
    public static final long METHOD_DATA_OFFSET=TYPE.offset("_method_data");
    public static final long METHODCOUNTERS_OFFSET = TYPE.offset("_method_counters");
    public static final long ADAPTER_OFFSET = METHODCOUNTERS_OFFSET+JVM.oopSize;
    public static final long ACC_FLAGS_OFFSET = TYPE.offset("_access_flags");
    public static final long INTRINSIC_ID_OFFSET=TYPE.offset("_intrinsic_id");
    public static final long FLAGS_OFFSET=TYPE.offset("_flags");
    public static final long I2I_ENTRY_OFFSET=TYPE.offset("_i2i_entry");
    public static final long FROM_COMPILED_ENTRY_OFFSET=TYPE.offset("_from_compiled_entry");
    public static final long CODE_OFFSET = TYPE.offset("_code");
    public static final long FROM_INTERPRETED_OFFSET=TYPE.offset("_from_interpreted_entry");
    private static final HashMap<Long,Method> CACHE=new HashMap<>();
    //[FLAG]
    public static final int CALLER_SENSITIVE=JVM.intConstant("Method::_caller_sensitive");
    public static final int FORCE_INLINE=JVM.intConstant("Method::_force_inline");
    public static final int DONT_INLINE=JVM.intConstant("Method::_dont_inline");
    public static final int HIDDEN=JVM.intConstant("Method::_hidden");
    //END
    private ConstMethod constMethodCache;
    private CompiledMethod codeCache;
    private MethodCounters countersCache;
    private AdapterHandlerEntry adapterCache;
    private MethodData dataCache;

    public static Method getOrCreate(long addr){
        if (addr==0L){
            throw new IllegalArgumentException("The pointer is NULL(0L)!");
        }
        Method re=CACHE.get(addr);
        if (re!=null){
            return re;
        }
        CACHE.put(addr,re=new Method(addr));
        return re;
    }
    public static void clearCacheMap(){
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
    public MethodData getMethodData(){
        long addr=unsafe.getAddress(this.address+METHOD_DATA_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.dataCache,addr)){
            this.dataCache=new MethodData(addr);
        }
        return this.dataCache;
    }

    @Nullable
    public MethodCounters getCounters() {
        long addr = unsafe.getAddress(this.address + METHODCOUNTERS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.countersCache, addr)) {
            this.countersCache = new MethodCounters(addr);
        }
        return this.countersCache;
    }

    public void setCounters(@Nullable MethodCounters counters) {
        unsafe.putAddress(this.address + METHODCOUNTERS_OFFSET, counters==null?0L:counters.address);
    }

    @Nullable
    public AdapterHandlerEntry getAdapter() {
        long addr = unsafe.getAddress(this.address + ADAPTER_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.adapterCache, addr)) {
            this.adapterCache = new AdapterHandlerEntry(addr);
        }
        return this.adapterCache;
    }

    public void setAdapter(@Nullable AdapterHandlerEntry adapter) {
        unsafe.putAddress(this.address + ADAPTER_OFFSET, adapter==null?0L:adapter.address);
    }

    public AccessFlags getAccessFlags() {
        return AccessFlags.getOrCreate(unsafe.getInt(this.address + ACC_FLAGS_OFFSET));
    }

    public void setAccessFlags(int flags) {
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }

    public void setAccessible() {
        int flags = unsafe.getInt(this.address + ACC_FLAGS_OFFSET);
        flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        flags |= Opcodes.ACC_PUBLIC;
        unsafe.putInt(this.address + ACC_FLAGS_OFFSET, flags);
    }
    public int getIntrinsicId(){
        return unsafe.getShort(this.address+INTRINSIC_ID_OFFSET)&0xffff;
    }

    public void setIntrinsicId(int id){
        unsafe.putShort(this.address+INTRINSIC_ID_OFFSET,(short) (id&0xffff));
    }

    public boolean isMethodHandleIntrinsic(){
        int id=this.getIntrinsicId();
        return VMIntrinsics.isSignaturePolymorphic(id)&&VMIntrinsics.isSignaturePolymorphicIntrinsic(id);
    }

    public int getFlags(){
        return unsafe.getShort(this.address+FLAGS_OFFSET)&0xffff;
    }

    public void setFlags(int flags){
        unsafe.putShort(this.address+FLAGS_OFFSET,(short) (flags&0xffff));
    }

    public long getI2IEntry(){
        return unsafe.getAddress(this.address+I2I_ENTRY_OFFSET);
    }

    public void setI2IEntry(long addr){
        unsafe.putAddress(this.address+I2I_ENTRY_OFFSET,addr);
    }

    public long getFromCompiledEntry(){
        return unsafe.getAddress(this.address+FROM_COMPILED_ENTRY_OFFSET);
    }

    public void setFromCompiledEntry(long addr) {
        unsafe.putAddress(this.address + FROM_COMPILED_ENTRY_OFFSET, addr);
    }

    @Nullable
    public CompiledMethod getCode() {
        long addr = unsafe.getAddress(this.address + CODE_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.codeCache, addr)) {
            Type type=JVM.findDynamicTypeForAddress(addr,CompiledMethod.TYPE);
            if (type==CompiledMethod.TYPE){
                this.codeCache = new CompiledMethod(addr);
            }else if (type== NMethod.TYPE){
                this.codeCache=new NMethod(addr);
            }else{
                throw new NoSuchElementException("Unknown type:"+(type==null?"null":type.name));
            }
        }
        return this.codeCache;
    }

    public void setCode(@Nullable CompiledMethod method) {
        unsafe.putAddress(this.address + CODE_OFFSET, method == null ? 0L : method.address);
    }

    public void unlinkCode(CompiledMethod compare) {
        if (unsafe.getAddress(this.address+CODE_OFFSET) == compare.address ||
                this.getFromCompiledEntry()==compare.getVerifiedEntryPoint()) {
            this.clearCode();
        }
    }

    public void unlinkCode() {
        this.clearCode();
    }

    public void clearCode() {
        AdapterHandlerEntry adapter=this.getAdapter();
        if (adapter==null) {
            this.setFromCompiledEntry(0L);
        }else{
            this.setFromCompiledEntry(adapter.getC2IEntry());
        }
        this.setFromInterpretedEntry(this.getI2IEntry());
        this.setCode(null);
    }

    public long getFromInterpretedEntry(){
        return unsafe.getAddress(this.address+FROM_INTERPRETED_OFFSET);
    }

    public void setFromInterpretedEntry(long addr) {
        unsafe.putAddress(this.address + FROM_INTERPRETED_OFFSET, addr);
    }
    public InstanceKlass getHolder(){
        return this.getConstMethod().getConstantPool().getHolder();
    }

    public void setHighestOsrCompLevel(CompLevel level) {
        MethodCounters mcs = this.getCounters();
        if (mcs != null) {
            mcs.setHighestOsrCompLevel(level);
        }
    }

    public static boolean caller_sensitive(int flags){
        return (flags&CALLER_SENSITIVE)!=0;
    }

    public static boolean force_inline(int flags){
        return (flags&FORCE_INLINE)!=0;
    }

    public static boolean dont_inline(int flags){
        return (flags&DONT_INLINE)!=0;
    }

    public static boolean is_hidden(int flags){
        return (flags&HIDDEN)!=0;
    }


}
