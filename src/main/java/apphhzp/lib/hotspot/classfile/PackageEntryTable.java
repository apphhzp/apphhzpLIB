package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oop.ClassLoaderData;
import apphhzp.lib.hotspot.oop.Symbol;
import apphhzp.lib.hotspot.utilities.Hashtable;
import apphhzp.lib.hotspot.utilities.HashtableEntry;

import javax.annotation.Nullable;
import java.util.function.LongFunction;

public class PackageEntryTable extends Hashtable {
    public PackageEntryTable(long addr) {
        super(addr);
    }

    @Override
    public LongFunction<? extends HashtableEntry> getHashtableEntryConstructor() {
        return PackageEntry::new;
    }

    public void addEntry(int index, PackageEntry new_entry) {
        super.addEntry(index,new_entry);
    }

    @Nullable
    public PackageEntry lookupOnly(Symbol name) {
        int index =this.indexFor(name);
        for (PackageEntry p = bucket(index); p != null; p = p.getNext()) {
            if (p.getName().address==name.address) {
                return p;
            }
        }
        return null;
    }

    public PackageEntry new_entry(long hash, Symbol name, ModuleEntry module) {
        PackageEntry entry = (PackageEntry)super.newEntry(hash, name);
        // Initialize fields specific to a PackageEntry
        entry.init();
        entry.getName().incrementRefCount();
        entry.setModule(module);
        return entry;
    }
}
