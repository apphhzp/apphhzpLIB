package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.classfile.ProtectionDomainCacheEntry;
import apphhzp.lib.hotspot.classfile.ProtectionDomainEntry;
import apphhzp.lib.hotspot.oop.*;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class DictionaryEntry extends HashtableEntry{
    public static final Type TYPE= JVM.type("DictionaryEntry");
    public static final int SIZE=TYPE.size;
    public static final long PD_SET_OFFSET=HashtableEntry.LITERAL_OFFSET+JVM.oopSize;
    private ProtectionDomainEntry pdSetCache;
    public DictionaryEntry(long addr) {
        super(addr);
    }

    public InstanceKlass getInstanceKlass(){
        return (InstanceKlass) Metadata.getMetadata(this.getLiteralPointer());
    }

    public boolean equals(Symbol className) {
        return this.getInstanceKlass().getName().equals(className);
    }

    @Nullable
    public DictionaryEntry getNext(){
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=new DictionaryEntry(addr);
        }else if (!(this.nextCache instanceof DictionaryEntry)){
            this.nextCache=new DictionaryEntry(addr);
        }
        return (DictionaryEntry) this.nextCache;
    }

    @Nullable
    public ProtectionDomainEntry getPDSet(){
        long addr=unsafe.getAddress(this.address+PD_SET_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.pdSetCache,addr)){
            this.pdSetCache=new ProtectionDomainEntry(addr);
        }
        return this.pdSetCache;
    }

    public void setPDSet(ProtectionDomainEntry pdSet){
        this.pdSetCache=null;
        unsafe.putAddress(this.address+PD_SET_OFFSET,pdSet==null?0L:pdSet.address);
    }

    public boolean containsPD(OopDesc pd){
        if (pd.equals(new OopDesc(((Class<?>)(this.getInstanceKlass().getMirror().getObject())).getProtectionDomain()))){
            return true;
        }
        for (ProtectionDomainEntry current =this.getPDSet();
             current != null;
             current = current.getNext()) {
            if (current.getPD().get().equals(pd)){
                return true;
            }
        }
        return false;
    }

    public void addPD(OopDesc protection_domain, ProtectionDomainCacheEntry entry) {
        if (!containsPD(protection_domain)) {
            ProtectionDomainEntry new_head = ProtectionDomainEntry.create(entry,this.getPDSet());
            this.setPDSet(new_head);
        }
    }

    @Override
    public String toString() {
        return "DictionaryEntry@0x"+Long.toHexString(this.address);
    }
}
