package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class WeakHandle extends JVMObject {
    private Oop oopCache;
    public WeakHandle(long addr) {
        super(addr);

    }

    @Nullable
    public Oop getOop(){
        long addr=unsafe.getAddress(this.address);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.oopCache,addr)){
            this.oopCache =new Oop(addr);
        }
        return this.oopCache;
    }

    @Nullable
    public OopDesc getOopDesc(){
        Oop oop=this.getOop();
        if (oop==null){
            return null;
        }
        return oop.get();
    }

    public boolean isEmpty(){
        return this.getOop()==null;
    }

    public void setOop(@Nullable Oop oop){
        this.oopCache=null;
        unsafe.putAddress(this.address,oop==null?0L:oop.address);
    }

    @Override
    public String toString() {
        return "WeakHandle@0x"+Long.toHexString(this.address);
    }
}
