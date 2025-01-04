package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.classfile.PackageEntry;
import apphhzp.lib.hotspot.classfile.PackageEntryTable;
import apphhzp.lib.hotspot.utilities.Dictionary;
import apphhzp.lib.hotspot.utilities.VMTypeGrowableArray;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static apphhzp.lib.ClassHelper.unsafe;

public class ClassLoaderData extends JVMObject {
    public static final Type TYPE = JVM.type("ClassLoaderData");
    public static final long CLASS_LOADER_OFFSET = TYPE.offset("_class_loader");
    public static final long HAS_CLASS_HOLDER_OFFSET = TYPE.offset("_has_class_mirror_holder");
    public static final long KLASSES_OFFSET = TYPE.offset("_klasses");
    public static final long PACKAGES_OFFSET=KLASSES_OFFSET+JVM.oopSize;
    public static final long MODULES_OFFSET=PACKAGES_OFFSET+JVM.oopSize;
    public static final long UNNAMED_MODULE_OFFSET=MODULES_OFFSET+JVM.oopSize;
    public static final long DICTIONARY_OFFSET=TYPE.offset("_dictionary");
    public static final long NEXT_OFFSET = TYPE.offset("_next");
    public static final long DEALLOCATE_LIST_OFFSET=NEXT_OFFSET-JVM.oopSize;

    public static final ClassLoaderData null_class_loader_data;
    private static final Map<Long, ClassLoaderData> CACHE;
    private ClassLoaderData nextCache;
    private PackageEntryTable packagesCache;
    private Dictionary dictionaryCache;
    private VMTypeGrowableArray<Metadata> deallocateListCache;
    static {
        CACHE=new HashMap<>();
        null_class_loader_data=Klass.asKlass(Object.class).getClassLoaderData();
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
            return null_class_loader_data;
        }
        return getOrCreate(unsafe.getLong(loader, JavaClasses.ClassLoader.loader_data_offset));
    }

    private ClassLoaderData(long address) {
        super(address);
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
        unsafe.putAddress(this.address+PACKAGES_OFFSET, packages == null ? 0L : packages.address);
    }

    public void packagesDo(PackagesVisitor function) {
        PackageEntryTable packages=this.getPackages();
        if (packages != null) {
            for (int i = 0; i < packages.getTableSize(); i++) {
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
        return new OopDesc(addr).getObject();
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
