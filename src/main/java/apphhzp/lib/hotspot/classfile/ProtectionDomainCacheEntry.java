package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.oops.oop.WeakHandle;
import apphhzp.lib.hotspot.utilities.HashtableEntry;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class ProtectionDomainCacheEntry extends HashtableEntry {
    public final WeakHandle literal;
    public ProtectionDomainCacheEntry(long addr) {
        super(addr);
        literal=new WeakHandle(addr+HashtableEntry.LITERAL_OFFSET);
    }
    public static ProtectionDomainCacheEntry create(long hash,@Nullable ProtectionDomainCacheEntry next,Oop handle) {
        long addr=unsafe.allocateMemory(HashtableEntry.SIZE);
        ProtectionDomainCacheEntry re=new ProtectionDomainCacheEntry(addr);
        re.setHash(hash);
        re.setPD(handle);
        re.setNext(next);
        return re;
    }

    @Override
    public long getLiteralPointer(){
        throw new UnsupportedOperationException("_literal is not a pointer");
    }

    @Override
    public void setLiteralPointer(long literal){
        throw new UnsupportedOperationException("_literal is not a pointer");
    }

    public Oop getPD() {
        return this.literal.getOop();
    }

    public void setPD(Oop pd){
        this.literal.setOop(pd);
        //this.setLiteralPointer(pd.address);
    }

    @Nullable
    @Override
    public ProtectionDomainCacheEntry getNext() {
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=new ProtectionDomainCacheEntry(addr);
        }
        return (ProtectionDomainCacheEntry) this.nextCache;
    }

    @Override
    public String toString() {
        return "ProtectionDomainCacheEntry@0x"+Long.toHexString(this.address);
    }
}
