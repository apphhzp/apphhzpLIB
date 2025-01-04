package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oop.Oop;
import apphhzp.lib.hotspot.oop.OopDesc;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class ProtectionDomainEntry extends JVMObject {
    public static final int SIZE= JVM.oopSize*2;
    public static final long PD_CACHE_OFFSET=0L;
    public static final long NEXT_OFFSET=PD_CACHE_OFFSET+JVM.oopSize;
    private ProtectionDomainCacheEntry pdCacheCache;
    private ProtectionDomainEntry nextCache;
    public ProtectionDomainEntry(long addr) {
        super(addr);
    }

    public static ProtectionDomainEntry create(@Nullable ProtectionDomainCacheEntry pd_cache,@Nullable ProtectionDomainEntry next){
        long addr=unsafe.allocateMemory(SIZE);
        unsafe.putAddress(addr+PD_CACHE_OFFSET,pd_cache==null?0L:pd_cache.address);
        unsafe.putAddress(addr+NEXT_OFFSET,next==null?0L:next.address);
        return new ProtectionDomainEntry(addr);
    }
    @Nullable
    public ProtectionDomainEntry getNext(){
        long addr= unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if(!isEqual(this.nextCache,addr)){
            this.nextCache=new ProtectionDomainEntry(addr);
        }
        return this.nextCache;
    }

    public void setNext(@Nullable ProtectionDomainEntry entry){
        this.nextCache=null;
        unsafe.putAddress(this.address+NEXT_OFFSET,entry==null?0L:entry.address);
    }


    public ProtectionDomainCacheEntry getPDCache(){
        long addr=unsafe.getAddress(this.address+PD_CACHE_OFFSET);
        if(!isEqual(this.pdCacheCache,addr)){
            this.pdCacheCache=new ProtectionDomainCacheEntry(addr);
        }
        return this.pdCacheCache;
    }

    public void setPDCache(ProtectionDomainCacheEntry entry){
        this.pdCacheCache=null;
        unsafe.putAddress(this.address+PD_CACHE_OFFSET,entry==null?0L:entry.address);
    }

    public Oop getPD(){
        return this.getPDCache().getPD();
    }

    @Override
    public String toString() {
        return "ProtectionDomainEntry@0x"+Long.toHexString(this.address);
    }
}
