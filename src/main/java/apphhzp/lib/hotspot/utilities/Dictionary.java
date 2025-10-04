package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.classfile.ProtectionDomainEntry;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.Symbol;

import java.util.function.LongFunction;

public class Dictionary extends Hashtable {
    public static final Type TYPE = JVM.type("Dictionary");
    public static final int SIZE = TYPE.size;

    public Dictionary(long addr) {
        super(addr);
    }

    public DictionaryEntry getEntry(int index, long hash, Symbol class_name) {
        for (DictionaryEntry entry = bucket(index); entry != null; entry = entry.getNext()) {
            if (entry.getHash() == hash && entry.equals(class_name)) {
                return entry;
            }
        }
        return null;
    }
    public DictionaryEntry getEntry(Symbol class_name) {
        long hash=this.computeHash(class_name);
        return this.getEntry(this.hashToIndex(hash),hash,class_name);
    }

    public InstanceKlass findClass(long hash,Symbol name) {
        int index = this.hashToIndex(hash);
        //assert (index == index_for(name), "incorrect index?");
        DictionaryEntry entry = this.getEntry(index, hash, name);
        return (entry != null) ? entry.getInstanceKlass() : null;
    }


    public boolean contains(InstanceKlass c) {
        long hash = computeHash(c.name());
        int index = hashToIndex(hash);
        for (DictionaryEntry entry = bucket(index); entry != null; entry = entry.getNext()) {
            if (entry.getInstanceKlass().equals(c)) {
                return true;
            }
        }
        return false;
    }

    public void freeEntry(DictionaryEntry entry){
        while (entry.getPDSet() != null) {
            ProtectionDomainEntry to_delete = entry.getPDSet();
            entry.setPDSet(to_delete.getNext());
            //delete to_delete;
        }
        super.freeEntry(entry);
    }

    public void addKlass(long hash, Symbol class_name,
                               InstanceKlass obj) {
        if (obj==null){
            throw new NullPointerException("adding NULL obj");
        }
        if (!class_name.equals(obj.name())){
            throw new IllegalArgumentException("sanity check on name");
        }
        DictionaryEntry entry = newEntry(hash, obj);
        int index =this.hashToIndex(hash);
        this.addEntry(index, entry);
        //check_if_needs_resize();
    }

    public DictionaryEntry newEntry(long hash, InstanceKlass klass) {
        DictionaryEntry entry = (DictionaryEntry) super.newEntry(hash, klass);
        entry.setPDSet(null);
        return entry;
    }

    @Override
    public LongFunction<DictionaryEntry> getHashtableEntryConstructor() {
        return DictionaryEntry::new;
    }

//    public void allEntriesDo(KlassClosure* closure) {
//        for (int index = 0; index < table_size(); index++) {
//            for (DictionaryEntry* probe = bucket(index);
//                 probe != NULL;
//                 probe = probe->next()) {
//                InstanceKlass* k = probe->instance_klass();
//                closure->do_klass(k);
//            }
//        }
//    }

    @Override
    public String toString() {
        return "Dictionary@0x" + Long.toHexString(this.address);
    }
}
