package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.classfile.*;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.oops.oop.WeakHandle;
import apphhzp.lib.hotspot.utilities.Dictionary;
import apphhzp.lib.hotspot.utilities.VMTypeGrowableArray;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static apphhzp.lib.ClassHelper.unsafe;

public class ClassLoaderData extends JVMObject {
    public static final Type TYPE = JVM.type("ClassLoaderData");
    public static final long CLASS_LOADER_OFFSET = TYPE.offset("_class_loader");
    public static final long HOLDER_OFFSET=CLASS_LOADER_OFFSET-JVM.oopSize;
    public static final long HAS_CLASS_HOLDER_OFFSET = TYPE.offset("_has_class_mirror_holder");
    public static final long MODIFIED_OOPS_OFFSET=HAS_CLASS_HOLDER_OFFSET+1;
    public static final long KEEP_ALIVE_OFFSET=JVM.computeOffset(JVM.intSize,MODIFIED_OOPS_OFFSET+1);
    public static final long CLAIM_OFFSET=KEEP_ALIVE_OFFSET+JVM.intSize;
    public static final long KLASSES_OFFSET = TYPE.offset("_klasses");
    public static final long PACKAGES_OFFSET=KLASSES_OFFSET+JVM.oopSize;
    public static final long MODULES_OFFSET=PACKAGES_OFFSET+JVM.oopSize;
    public static final long UNNAMED_MODULE_OFFSET=MODULES_OFFSET+JVM.oopSize;
    public static final long DICTIONARY_OFFSET=TYPE.offset("_dictionary");
    public static final long NEXT_OFFSET = TYPE.offset("_next");
    public static final long DEALLOCATE_LIST_OFFSET=NEXT_OFFSET-JVM.oopSize;
    public static final long CLASS_LOADER_KLASS_OFFSET=NEXT_OFFSET+JVM.oopSize;
    public static final long NAME_OFFSET=CLASS_LOADER_KLASS_OFFSET+JVM.oopSize;
    public static final long NAME_AND_ID_OFFSET=NAME_OFFSET+JVM.oopSize;

    public static final ClassLoaderData nullClassLoaderData;

    public static final int claim_none         = 0,
            claim_finalizable  = 2,
            claim_strong       = 3,
            claim_other        = 4;

    private static final Map<Long, ClassLoaderData> CACHE;
    public final WeakHandle holder=new WeakHandle(this.address+HOLDER_OFFSET);
    private ClassLoaderData nextCache;
    private PackageEntryTable packagesCache;
    private ModuleEntryTable modulesCache;
    private Dictionary dictionaryCache;
    private VMTypeGrowableArray<Metadata> deallocateListCache;
    static {
        CACHE=new HashMap<>();
        nullClassLoaderData = Klass.asKlass(Object.class).getClassLoaderData();
    }
    public static ClassLoaderData getOrCreate(long addr) {
        if (addr == 0L) {
            throw new IllegalArgumentException("ClassLoaderData pointer is NULL(0L).");
        }
        if (CACHE.containsKey(addr)) {
            return CACHE.get(addr);
        }
        ClassLoaderData re = new ClassLoaderData(addr);
        CACHE.put(addr, re);
        return re;
    }

    public static ClassLoaderData as(ClassLoader loader){
        if (loader==null){
            return nullClassLoaderData;
        }
        return getOrCreate(JavaClasses.ClassLoader.getPointerFrom(loader));
    }

    private ClassLoaderData(long address) {
        super(address);
    }

    public Klass getHeadKlass(){
        return Klass.getOrCreate(unsafe.getAddress(this.address+KLASSES_OFFSET));
    }

    public List<Klass> getKlasses() {
        ArrayList<Klass> re = new ArrayList<>();
        long base = unsafe.getAddress(this.address + KLASSES_OFFSET);
        if (base != 0L) {
            Klass tmp = Klass.getOrCreate(base);
            do {
                re.add(tmp);
            } while ((tmp = tmp.getNextKlass()) != null);
        }
        return re;
    }

    public void setKlasses(Klass header) {
        unsafe.putAddress(this.address + KLASSES_OFFSET, header == null ? 0L : header.address);
    }

    public void klassesDo(KlassVisitor visitor) {
        // Lock-free access requires load_acquire
        for (Klass k = Klass.getOrCreate(unsafe.getAddress(this.address+KLASSES_OFFSET)); k != null; k = k.getNextKlass()) {
            visitor.visit(k);
        }
    }

    public interface KlassVisitor{
        void visit(Klass k);
    }

    @Nullable
    public PackageEntryTable getPackages(){
        long addr=unsafe.getAddress(this.address+PACKAGES_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.packagesCache,addr)){
            this.packagesCache=new PackageEntryTable(addr);
        }
        return this.packagesCache;
    }

    public void setPackages(@Nullable PackageEntryTable packages) {
        this.packagesCache=null;
        unsafe.putAddress(this.address+PACKAGES_OFFSET,packages==null?0L:packages.address);
    }

    public void packagesDo(PackagesVisitor function) {
        PackageEntryTable packages=this.getPackages();
        if (packages != null) {
            for (int i = 0,size=packages.getTableSize(); i < size; i++) {
                for (PackageEntry entry = packages.bucket(i);
                     entry != null;
                     entry = entry.getNext()) {
                    function.visit(entry);
                }
            }
        }
    }
    public interface PackagesVisitor{
        void visit(PackageEntry entry);
    }

    @Nullable
    public ModuleEntryTable getModules(){
        long addr=unsafe.getAddress(this.address+MODULES_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.modulesCache,addr)){
            this.modulesCache=new ModuleEntryTable(addr);
        }
        return this.modulesCache;
    }

    public void setModules(ModuleEntryTable modules) {
        this.modulesCache=null;
        unsafe.putAddress(this.address+MODULES_OFFSET,modules.address);
    }

    @Nullable
    public ModuleEntry getUnnamedModule(){
        long addr=unsafe.getAddress(this.address+UNNAMED_MODULE_OFFSET);
        if (addr==0L){
            return null;
        }
        return ModuleEntry.getOrCreate(addr);
    }

    public void setUnnamedModule(@Nullable ModuleEntry unnamedModule) {
        unsafe.putAddress(this.address+UNNAMED_MODULE_OFFSET,unnamedModule==null?0L:unnamedModule.address);
    }

    public void modulesDo(ModulesVisitor visitor){
        ModuleEntry unnamedModule = this.getUnnamedModule();
        if (unnamedModule!=null){
            visitor.visit(unnamedModule);
        }
        ModuleEntryTable modules = this.getModules();
        if (modules!=null){
            for (int i = 0,size=modules.getTableSize(); i < size; i++) {
                for (ModuleEntry entry = modules.bucket(i);
                     entry != null;
                     entry = entry.getNext()) {
                    visitor.visit(entry);
                }
            }
        }
    }

    public interface ModulesVisitor{
        void visit(ModuleEntry entry);
    }

    @Nullable
    public Dictionary getDictionary(){
        long addr=unsafe.getAddress(this.address+DICTIONARY_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.dictionaryCache,addr)){
            this.dictionaryCache=new Dictionary(addr);
        }
        return this.dictionaryCache;
    }

    public void setDictionary(@Nullable Dictionary dictionary) {
        unsafe.putAddress(this.address+DICTIONARY_OFFSET,dictionary==null?0L:dictionary.address);
    }

    @Nullable
    public ClassLoaderData getNextCLD() {
        long addr = unsafe.getAddress(this.address + NEXT_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.nextCache, addr)) {
            this.nextCache = getOrCreate(addr);
        }
        return this.nextCache;
    }

    public void setNextCLD(@Nullable ClassLoaderData next) {
        unsafe.putAddress(this.address + NEXT_OFFSET, next == null ? 0L : next.address);
    }

    @Nullable
    public VMTypeGrowableArray<Metadata> getDeallocateList() {
        long addr = unsafe.getAddress(this.address + DEALLOCATE_LIST_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.deallocateListCache, addr)) {
            this.deallocateListCache=new VMTypeGrowableArray<>(addr,Metadata::getMetadata);
        }
        return this.deallocateListCache;
    }

    public void setDeallocateList(@Nullable VMTypeGrowableArray<Metadata> deallocateList) {
        unsafe.putAddress(this.address+DEALLOCATE_LIST_OFFSET, deallocateList == null ? 0L : deallocateList.address);
    }

    @Nullable
    public ClassLoader getClassLoader() {
        long addr = OopDesc.fromOopHandle(this.address + CLASS_LOADER_OFFSET);
        return OopDesc.of(addr).getObject();
    }


    public Oop getClassLoaderOop() {
        return new Oop(unsafe.getAddress(this.address+CLASS_LOADER_OFFSET));
    }

    /* If true, CLD is dedicated to one class and that class determines the CLDs lifecycle.
       For example, a non-strong hidden class.
       Arrays of these classes are also assigned to these class loader datas.
    */
    public boolean hasClassMirrorHolder() {
        return unsafe.getByte(this.address + HAS_CLASS_HOLDER_OFFSET) != 0;
    }

    public void setHasClassMirrorHolder(boolean hasClassMirrorHolder) {
        unsafe.putByte(this.address+HAS_CLASS_HOLDER_OFFSET, (byte) (hasClassMirrorHolder?1:0));
    }

    public boolean hasModifiedOops() {
        return unsafe.getByte(this.address + MODIFIED_OOPS_OFFSET) != 0;
    }

    public void setHasModifiedOops(boolean hasModifiedOops) {
        unsafe.putByte(this.address+MODIFIED_OOPS_OFFSET, (byte) (hasModifiedOops?1:0));
    }

    public int getKeepAlive(){
        return unsafe.getInt(this.address+KEEP_ALIVE_OFFSET);
    }

    public void setKeepAlive(int keepAlive) {
        unsafe.putInt(this.address+KEEP_ALIVE_OFFSET, keepAlive);
    }

    // Non-strong hidden classes have their own ClassLoaderData that is marked to keep alive
    // while the class is being parsed, and if the class appears on the module fixup list.
    // Due to the uniqueness that no other class shares the hidden class' name or
    // ClassLoaderData, no other non-GC thread has knowledge of the hidden class while
    // it is being defined, therefore _keep_alive is not volatile or atomic.
    public void incKeepAlive() {
        if (this.hasClassMirrorHolder()) {
            int val=this.getKeepAlive();
            if (val<=0){
                throw new IllegalStateException("Invalid keep alive increment count");
            }
            this.setKeepAlive(val+1);
        }
    }

    public void decKeepAlive() {
        if (this.hasClassMirrorHolder()) {
            int val=this.getKeepAlive();
            if (val<=0){
                throw new IllegalStateException("Invalid keep alive increment count");
            }
            this.setKeepAlive(val-1);
        }
    }

    public boolean  keepAlive()        { return this.getKeepAlive() > 0; }

    public boolean isAlive() {
        return this.keepAlive() // null class loader and incomplete non-strong hidden class.
                || (holder.getOop() != null);
    }

    public int getClaim(){
        return unsafe.getInt(this.address+CLAIM_OFFSET);
    }

    public void setClaim(int claim) {
        unsafe.putInt(this.address+CLAIM_OFFSET, claim);
    }

    public void clearClaim(){
        this.setClaim(0);
    }
    public boolean claimed() { return this.getClaim() != 0; }
    public boolean claimed(int claim){ return (this.getClaim() & claim) == claim; }
    public void clearClaim(int claim) {
        for (;;) {
            int old_claim = this.getClaim();
            if ((old_claim & claim) == 0) {
                return;
            }
            int new_claim = old_claim & ~claim;
            //Atomic::cmpxchg(&_claim, old_claim, new_claim) == old_claim
            if (unsafe.compareAndSwapInt(null,this.address+CLAIM_OFFSET,old_claim,new_claim)) {
                return;
            }
        }
    }

    public boolean tryClaim(int claim) {
        for (;;) {
            int old_claim = this.getClaim();
            if ((old_claim & claim) == claim) {
                return false;
            }
            int new_claim = old_claim | claim;
            //Atomic::cmpxchg(&_claim, old_claim, new_claim) == old_claim
            if (unsafe.compareAndSwapInt(null,this.address+CLAIM_OFFSET,old_claim,new_claim)) {
                return true;
            }
        }
    }
//    public void add_to_deallocate_list(Metadata m) {
//        if (!m.isShared()) {
//            if (this.getDeallocateList() == null) {
//                throw new UnsupportedOperationException("Not supported yet");
//                //this.setDeallocateList(new (ResourceObj::C_HEAP, mtClass) VMTypeGrowableArray<Metadata>(100, mtClass));
//            }
//            this.getDeallocateList().append_if_missing(m);
//            ClassLoaderDataGraph::set_should_clean_deallocate_lists();
//        }
//    }

    public boolean isBootClassLoaderData(){
        return this.address == nullClassLoaderData.address || this.getClassLoader() ==null;
    }

    public boolean isSystemClassLoaderData(){
        return this.getClassLoader()==ClassLoader.getSystemClassLoader();
    }

    public boolean isPlatformClassLoaderData(){
        return this.getClassLoader()==ClassLoader.getPlatformClassLoader();
    }

    public boolean isBuiltinClassLoaderData(){
        return (isBootClassLoaderData() ||
                this.getClassLoader()==ClassLoader.getSystemClassLoader() ||
                this.getClassLoader()==ClassLoader.getPlatformClassLoader());
    }


    public boolean isPermanentClassLoaderData(){
        return this.isBuiltinClassLoaderData() && !this.hasClassMirrorHolder();
    }

    public void addClass(Klass k) {
        Klass old_value = this.getHeadKlass();
        k.setNextKlass(old_value);
        // Link the new item into the list, making sure the linked class is stable
        // since the list can be walked without a lock
        this.setKlasses(k);
        if (k.isArrayKlass()) {
            ClassLoaderDataGraph.incArrayClasses(1);
        } else {
            ClassLoaderDataGraph.incInstanceClasses(1);
        }
    }

    public boolean containsKlass(Klass klass) {
        // Lock-free access requires load_acquire
        for (Klass k =this.getHeadKlass(); k != null; k = k.getNextKlass()) {
            if (k.equals(klass)) return true;
        }
        return false;
    }

    public static List<ClassLoaderData> getAllClassLoaderData() {
        ArrayList<ClassLoaderData> re = new ArrayList<>();
        ClassLoaderData tmp = ClassLoaderDataGraph.getHead();
        do {
            re.add(tmp);
        } while ((tmp = tmp.getNextCLD()) != null);
        return re;
    }

    @Override
    public String toString() {
        return "ClassLoaderData@0x" + Long.toHexString(this.address);
    }
}
