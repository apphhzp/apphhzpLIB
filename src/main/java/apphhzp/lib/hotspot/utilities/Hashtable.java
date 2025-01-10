package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.Symbol;

import java.util.function.LongFunction;

public class Hashtable extends BasicHashtable{
    public static final Type TYPE= JVM.type("IntptrHashtable");
    public static final int SIZE=TYPE.size;
    public Hashtable(long addr) {
        super(addr);
    }

    @Override
    public LongFunction<? extends HashtableEntry> getHashtableEntryConstructor() {
        return HashtableEntry::new;
    }

    public long computeHash(Symbol name) {
        return name.identityHash();
    }

    public int hashToIndex(long fullHash) {
        return (int) (fullHash % this.getTableSize());
    }

    public int indexFor(Symbol name){
        return this.hashToIndex(this.computeHash(name));
    }

    public static long hashSymbol(byte[] buf) {
        long h = 0;
        int s = 0;
        int len = buf.length;
        while (len-- > 0) {
            h = 31*h + (0xFFL & buf[s]);
            s++;
        }
        return h & 0xFFFFFFFFL;
    }

    public HashtableEntry newEntry(long hashValue, JVMObject obj) {
        return HashtableEntry.create(hashValue, obj,null,this.entrySize(),this.getHashtableEntryConstructor());
    }

    @Override
    public String toString() {
        return "Hashtable@0x"+Long.toHexString(this.address);
    }
}
