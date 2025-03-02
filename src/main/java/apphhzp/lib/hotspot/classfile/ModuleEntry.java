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
import static apphhzp.lib.helfy.JVM.oopSize;

public class ModuleEntry extends HashtableEntry {
    public static final long MODULE_OFFSET = JVM.computeOffset(oopSize, HashtableEntry.LITERAL_OFFSET + oopSize);
    public static final long SHARED_PD_OFFSET = JVM.computeOffset(oopSize, MODULE_OFFSET + oopSize);
    public static final long LOADER_DATA_OFFSET = JVM.computeOffset(oopSize, SHARED_PD_OFFSET + oopSize);
    public static final long READS_OFFSET = JVM.computeOffset(oopSize, LOADER_DATA_OFFSET + oopSize);
    public static final long VERSION_OFFSET = JVM.computeOffset(oopSize, READS_OFFSET + oopSize);
    public static final long LOCATION_OFFSET = JVM.computeOffset(oopSize, VERSION_OFFSET + oopSize);
    public static final long SHARED_PATH_INDEX_OFFSET = JVM.computeOffset(JVM.intSize, LOCATION_OFFSET + oopSize);
    public static final long CAN_READ_ALL_UNNAMED_OFFSET = JVM.includeCDS ? JVM.computeOffset(1, SHARED_PATH_INDEX_OFFSET + JVM.intSize) : SHARED_PATH_INDEX_OFFSET;
    public static final long HAS_DEFAULT_READ_EDGES_OFFSET = JVM.computeOffset(1, CAN_READ_ALL_UNNAMED_OFFSET + 1);
    public static final long MUST_WALK_READS_OFFSET = JVM.computeOffset(1, HAS_DEFAULT_READ_EDGES_OFFSET + 1);
    public static final long IS_OPEN_OFFSET = JVM.computeOffset(1, MUST_WALK_READS_OFFSET + 1);
    public static final long IS_PATCHED_OFFSET = JVM.computeOffset(1, IS_OPEN_OFFSET + 1);
    public static final long ARCHIVED_MODULE_INDEX_OFFSET = JVM.computeOffset(JVM.intSize, IS_PATCHED_OFFSET + 1);//(IS_PATCHED_OFFSET+JVM.intSize)/JVM.intSize*JVM.intSize;
    private static final Long2ObjectMap<ModuleEntry> CACHE = new Long2ObjectOpenHashMap<>();
    private VMTypeGrowableArray<ModuleEntry> readsCache;

    public static ModuleEntry getOrCreate(long addr) {
        if (CACHE.containsKey(addr)) {
            return CACHE.get(addr);
        }
        ModuleEntry entry = new ModuleEntry(addr);
        CACHE.put(addr, entry);
        return entry;
    }

    private ModuleEntry(long addr) {
        super(addr);
    }

    @Nullable
    public Symbol getName() {
        long addr = this.getLiteralPointer();
        return addr == 0L ? null : Symbol.of(this.getLiteralPointer());
    }

    public void setName(@Nullable Symbol symbol) {
        this.setLiteralPointer(symbol == null ? 0L : symbol.address);
    }

    @Nullable
    @Override
    public ModuleEntry getNext() {
        long addr = unsafe.getAddress(this.address + NEXT_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.nextCache, addr)) {
            this.nextCache = ModuleEntry.getOrCreate(addr);
        }
        return (ModuleEntry) this.nextCache;
    }

    public Module getModule() {
        long addr = OopDesc.fromOopHandle(this.address + MODULE_OFFSET);
        return addr == 0L ? null : OopDesc.of(addr).getObject();
    }

    public Oop getModuleOop() {
        long addr = unsafe.getAddress(this.address + MODULE_OFFSET); // OopDesc.fromOopHandle(this.address+MODULE_OFFSET);
        return addr == 0L ? null : new Oop(addr);
    }

    public void setModule(Oop module) {
        unsafe.putAddress(unsafe.getAddress(this.address + MODULE_OFFSET), module.address);
    }

    public ProtectionDomain getSharedPD() {
        long addr = OopDesc.fromOopHandle(this.address + SHARED_PD_OFFSET);
        return addr == 0L ? null : OopDesc.of(addr).getObject();
    }

    public Oop getSharedPDOop() {
        long addr = unsafe.getAddress(this.address + SHARED_PD_OFFSET);
        return addr == 0L ? null : new Oop(addr);
    }

    public void setSharedPD(Oop module) {
        unsafe.putAddress(unsafe.getAddress(this.address + SHARED_PD_OFFSET), module.address);
    }

    public ClassLoaderData getLoaderData() {
        long addr = unsafe.getAddress(this.address + LOADER_DATA_OFFSET);
        if (addr == 0L) {
            return null;
        }
        return ClassLoaderData.getOrCreate(addr);
    }

    public void setLoaderData(ClassLoaderData data) {
        unsafe.putAddress(this.address + LOADER_DATA_OFFSET, data == null ? 0L : data.address);
    }

    public VMTypeGrowableArray<ModuleEntry> getReads() {
        long addr = unsafe.getAddress(this.address + READS_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.readsCache, addr)) {
            this.readsCache = new VMTypeGrowableArray<>(addr, ModuleEntry::getOrCreate);
        }
        return this.readsCache;
    }

    public void setReads(VMTypeGrowableArray<ModuleEntry> reads) {
        unsafe.putAddress(this.address + READS_OFFSET, reads == null ? 0L : reads.address);
    }

    public boolean hasReadsList(){
        return ((unsafe.getAddress(this.address + READS_OFFSET) != 0L) && this.getReads().length()!=0);
    }

    public Symbol getVersion() {
        long addr = unsafe.getAddress(this.address + VERSION_OFFSET);
        return addr == 0L ? null : Symbol.of(addr);
    }

    public void setVersion(@Nullable Symbol version) {
        Symbol _version = this.getVersion();
        if (_version != null) {
            // _version symbol's refcounts are managed by ModuleEntry,
            // must decrement the old one before updating.
            _version.decrementRefCount();
        }
        unsafe.putAddress(this.address + VERSION_OFFSET, version == null ? 0L : version.address);
        if (version != null) {
            version.incrementRefCount();
        }
    }

    public Symbol getLocation() {
        long addr = unsafe.getAddress(this.address + LOCATION_OFFSET);
        return addr == 0L ? null : Symbol.of(addr);
    }

//    public void setLocation(@Nullable Symbol location){
//
//        if (location != null) {
//            // _location symbol's refcounts are managed by ModuleEntry,
//            // must decrement the old one before updating.
//            this.getLocation().decrementRefCount();
//        }
//
//        unsafe.putAddress(this.address+LOCATION_OFFSET,location==null?0L:location.address);
//        if (location != null) {
//            location.incrementRefCount();
//            if (JVM.includeCDS){
//                if (JVM.usingSharedSpaces) {
//                    _shared_path_index = FileMapInfo::get_module_shared_path_index(location);
//                }
//            }
//        }
//        //
//    }

    public int getArchivedModuleIndex() {
        if (JVM.includeCDSJavaHeap) {
            return unsafe.getInt(this.address + ARCHIVED_MODULE_INDEX_OFFSET);
        }
        throw new UnsupportedOperationException();
    }

    public void setArchivedModuleIndex(int index) {
        if (JVM.includeCDSJavaHeap) {
            unsafe.putInt(this.address + ARCHIVED_MODULE_INDEX_OFFSET, index);
        }
        throw new UnsupportedOperationException();
    }

    public boolean isNamed() {
        return (this.getLiteralPointer() != 0L);
    }

    public boolean canReadAllUnnamed() {
        boolean _can_read_all_unnamed=unsafe.getByte(this.address+CAN_READ_ALL_UNNAMED_OFFSET)!=0;
        if (!(this.isNamed()|| _can_read_all_unnamed)){
            throw new IllegalStateException("unnamed modules can always read all unnamed modules");
        }
        return _can_read_all_unnamed;
    }

    // Modules can only go from strict to loose.
    public void setCanReadAllUnnamed() {
        unsafe.putByte(this.address+CAN_READ_ALL_UNNAMED_OFFSET, (byte) 1);
    }

    public boolean isOpen() {
        return unsafe.getByte(this.address + IS_OPEN_OFFSET) != 0;
    }

    public boolean isPatched() {
        return unsafe.getByte(this.address + IS_PATCHED_OFFSET) != 0;
    }

    public void setIsPatched() {
        unsafe.putByte(this.address+IS_PATCHED_OFFSET, (byte) 1);
        if (JVM.includeCDS){
            unsafe.putInt(this.address+SHARED_PATH_INDEX_OFFSET,-1); // Mark all shared classes in this module invisible.
        }
    }

    public boolean hasDefaultReadEdges() {
        return unsafe.getByte(this.address + HAS_DEFAULT_READ_EDGES_OFFSET) != 0;
    }

    // Sets true and returns the previous value.
    public boolean setHasDefaultReadEdges() {
        boolean prev = this.hasDefaultReadEdges();
        unsafe.putByte(this.address + HAS_DEFAULT_READ_EDGES_OFFSET, (byte) 1);
        return prev;
    }

    public boolean canRead(ModuleEntry m){
        if (m==null){
            throw new NullPointerException("No module to lookup in this module's reads list");
        }
        // Unnamed modules read everyone and all modules
        // read java.base.  If either of these conditions
        // hold, readability has been established.
        if (!this.isNamed() ||
                (m.equals(ModuleEntryTable.javabase_module))) {
            return true;
        }

        // This is a guard against possible race between agent threads that redefine
        // or retransform classes in this module. Only one of them is adding the
        // default read edges to the unnamed modules of the boot and app class loaders
        // with an upcall to jdk.internal.module.Modules.transformedByAgent.
        // At the same time, another thread can instrument the module classes by
        // injecting dependencies that require the default read edges for resolution.
        if (this.hasDefaultReadEdges() && !m.isNamed()) {
            ClassLoaderData cld = m.getLoaderData();
            if (cld.hasClassMirrorHolder()){
                throw new RuntimeException("module's cld should have a ClassLoader holder not a Class holder");
            }
            if (cld.equals(ClassLoaderData.nullClassLoaderData)|| cld.isSystemClassLoaderData()) {
                return true; // default read edge
            }
        }
        if (!hasReadsList()) {
            return false;
        } else {
            return this.getReads().contains(m);
        }
    }

    public void module_reads_do(ModulesVisitor f) {
        if (f==null){
            throw new NullPointerException("invariant");
        }
        if (this.hasReadsList()) {
            VMTypeGrowableArray<ModuleEntry> reads = this.getReads();
            for (ModuleEntry m:reads){
                f.visit(m);
            }
        }
    }

    public interface ModulesVisitor{
        void visit(ModuleEntry entry);
    }

    @Override
    public String toString() {
        return "ModuleEntry@0x" + Long.toHexString(this.address);
    }
}
