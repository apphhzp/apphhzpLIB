package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.utilities.Hashtable;
import apphhzp.lib.hotspot.utilities.HashtableEntry;

import javax.annotation.Nullable;
import java.util.function.LongFunction;

public class ModuleEntryTable extends Hashtable {

    public static final ModuleEntry javabase_module= Klass.asKlass(Object.class).asInstanceKlass().module();
    public ModuleEntryTable(long addr) {
        super(addr);
    }

    @Override
    public LongFunction<? extends HashtableEntry> getHashtableEntryConstructor() {
        return ModuleEntry::getOrCreate;
    }

    @Override
    public long computeHash(@Nullable Symbol name) {
        return name==null?0L:name.identityHash();
    }

    @Nullable
    public ModuleEntry lookupOnly(Symbol name){
        if (name==null){
            throw new NullPointerException("name cannot be NULL");
        }
        int index = this.indexFor(name);
        for(ModuleEntry m = bucket(index); m != null; m = m.getNext()) {
            if (m.getName().address==name.address) {
                return m;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ModuleEntryTable@0x"+Long.toHexString(this.address);
    }
}
