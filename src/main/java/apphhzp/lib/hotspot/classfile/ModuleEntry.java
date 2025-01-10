package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.utilities.HashtableEntry;
import apphhzp.lib.hotspot.utilities.VMTypeGrowableArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.security.ProtectionDomain;

import static apphhzp.lib.ClassHelper.unsafe;

public class ModuleEntry extends HashtableEntry {
    public static final long MODULE_OFFSET=HashtableEntry.LITERAL_OFFSET+ JVM.oopSize;
    public static final long SHARED_PD_OFFSET=MODULE_OFFSET+JVM.oopSize;
    public static final long LOADER_DATA_OFFSET=SHARED_PD_OFFSET+JVM.oopSize;
    public static final long READS_OFFSET=LOADER_DATA_OFFSET+JVM.oopSize;
    public static final long VERSION_OFFSET=READS_OFFSET+JVM.oopSize;
    public static final long LOCATION_OFFSET=VERSION_OFFSET+JVM.oopSize;
    public static final long SHARED_PATH_INDEX_OFFSET=LOCATION_OFFSET+JVM.oopSize;
    public static final long CAN_READ_ALL_UNNAMED_OFFSET=SHARED_PATH_INDEX_OFFSET+(JVM.includeCDS?JVM.intSize:0);
    public static final long HAS_DEFAULT_READ_EDGES_OFFSET=CAN_READ_ALL_UNNAMED_OFFSET+1;
    public static final long MUST_WALK_READS_OFFSET=HAS_DEFAULT_READ_EDGES_OFFSET+1;
    public static final long IS_OPEN_OFFSET=MUST_WALK_READS_OFFSET+1;
    public static final long IS_PATCHED_OFFSET=IS_OPEN_OFFSET+1;
    public static final long ARCHIVED_MODULE_INDEX_OFFSET=(IS_PATCHED_OFFSET+JVM.intSize)/JVM.intSize*JVM.intSize;
    private static final Long2ObjectMap<ModuleEntry> CACHE= new Long2ObjectOpenHashMap<>();
    private VMTypeGrowableArray<ModuleEntry> readsCache;
    public static ModuleEntry getOrCreate(long addr){
        if (CACHE.containsKey(addr)){
            return CACHE.get(addr);
        }
        ModuleEntry entry=new ModuleEntry(addr);
        CACHE.put(addr, entry);
        return entry;
    }

    private ModuleEntry(long addr) {
        super(addr);
    }

    public Symbol getName(){
        return Symbol.of(this.getLiteralPointer());
    }

    public void setName(Symbol symbol){
        this.setLiteralPointer(symbol.address);
    }

    @Nullable
    @Override
    public ModuleEntry getNext() {
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=ModuleEntry.getOrCreate(addr);
        }
        return (ModuleEntry) this.nextCache;
    }

    public Module getModule(){
        long addr= OopDesc.fromOopHandle(this.address+MODULE_OFFSET);
        return addr==0L?null:OopDesc.of(addr).getObject();
    }

    public Oop getModuleOop(){
        long addr=unsafe.getAddress(this.address+MODULE_OFFSET); // OopDesc.fromOopHandle(this.address+MODULE_OFFSET);
        return addr==0L?null:new Oop(addr);
    }

    public void setModule(Oop module){
        unsafe.putAddress(unsafe.getAddress(this.address+MODULE_OFFSET),module.address);
    }

    public ProtectionDomain getSharedPD(){
        long addr= OopDesc.fromOopHandle(this.address+SHARED_PD_OFFSET);
        return addr==0L?null:OopDesc.of(addr).getObject();
    }

    public Oop getSharedPDOop(){
        long addr=unsafe.getAddress(this.address+SHARED_PD_OFFSET);
        return addr==0L?null:new Oop(addr);
    }

    public void setSharedPD(Oop module){
        unsafe.putAddress(unsafe.getAddress(this.address+SHARED_PD_OFFSET),module.address);
    }

    public ClassLoaderData getLoaderData(){
        long addr=unsafe.getAddress(this.address+LOADER_DATA_OFFSET);
        if (addr==0L){
            return null;
        }
        return ClassLoaderData.getOrCreate(addr);
    }

    public void setLoaderData(ClassLoaderData data){
        unsafe.putAddress(this.address+LOADER_DATA_OFFSET,data==null?0L:data.address);
    }

    public VMTypeGrowableArray<ModuleEntry> getReads(){
        long addr=unsafe.getAddress(this.address+READS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.readsCache,addr)){
            this.readsCache=new VMTypeGrowableArray<>(addr,ModuleEntry::getOrCreate);
        }
        return this.readsCache;
    }

    public void setReads(VMTypeGrowableArray<ModuleEntry> reads){
        this.readsCache=null;
        unsafe.putAddress(this.address+READS_OFFSET,reads==null?0L:reads.address);
    }

    public int getArchivedModuleIndex(){
        return unsafe.getInt(this.address+ARCHIVED_MODULE_INDEX_OFFSET);
    }

    public void setArchivedModuleIndex(int index){
        unsafe.putInt(this.address+ARCHIVED_MODULE_INDEX_OFFSET, index);
    }
}
