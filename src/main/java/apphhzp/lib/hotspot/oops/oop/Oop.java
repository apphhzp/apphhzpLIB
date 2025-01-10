package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class Oop extends JVMObject {
    private OopDesc descCache;
    public Oop(long addr) {
        super(addr);
    }



    public OopDesc get(){
        if (this.address==0L){
            return OopDesc.NULL;
        }
        long addr= unsafe.getAddress(this.address);
        if (!isEqual(this.descCache,addr)){
            this.descCache=OopDesc.of(addr);
        }
        return descCache;
    }

    public <T> T getJavaObject(){
        return this.get().getObject();
    }

    public void set(OopDesc desc){
        if (this.address==0L){
            throw new NullPointerException();
        }
        this.descCache=null;
        unsafe.putAddress(this.address,desc.address);
    }

    @Override
    public String toString() {
        return "oop(oopDesc*)@0x"+Long.toHexString(this.address);
    }
}
