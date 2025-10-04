package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.internalUnsafe;
import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class Oop extends JVMObject {
    public final boolean isFake;
    @Nullable
    private OopDesc descCache;
    public Oop(long addr) {
        super(addr);
        isFake=false;
    }

    public Oop(OopDesc addr) {
        super(addr==null?0L:addr.address);
        isFake = true;
        descCache=addr;
    }

    public OopDesc get(){
        if (isFake){
            return descCache;
        }
        if (this.address==0L){
            return null;
        }
        long addr= unsafe.getAddress(this.address);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.descCache,addr)){
            this.descCache=OopDesc.of(addr);
        }
        return descCache;
    }

    public long getAddress(){
        if (isFake){
            return descCache==null?0L:descCache.address;
        }
        return unsafe.getAddress(this.address);
    }

    public <T> T getJavaObject(){
        if (isFake){
            return descCache==null?null:descCache.getObject();
        }
        return internalUnsafe.getUncompressedObject(this.address);
    }

    public void set(OopDesc desc){
        if (isFake){
            descCache=desc;
            return;
        }
        if (this.address==0L){
            throw new NullPointerException();
        }
        this.descCache=null;
        unsafe.putAddress(this.address,desc==null?0L:desc.address);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==this){
            return true;
        }
        if (obj instanceof Oop oop){
            return getAddress()==oop.getAddress();
        }
        return false;
    }

    @Override
    public String toString() {
        if (isFake){
            return "oop(fake)@0x"+Long.toHexString(descCache==null?0L:descCache.address);
        }
        return "oop(oopDesc*)@0x"+Long.toHexString(this.address);
    }
}
