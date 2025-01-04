package apphhzp.lib.hotspot.oop;

import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class WeakHandle extends JVMObject {
    private Oop oopCache;
    public WeakHandle(long addr) {
        super(addr);

    }

    public Oop getOop(){
        long addr=unsafe.getAddress(this.address);
        if (!isEqual(this.oopCache,addr)){
            this.oopCache =new Oop(addr);
        }
        return this.oopCache;
    }

    public OopDesc getOopDesc(){
        return this.getOop().get();
    }

    public boolean isEmpty(){
        return this.getOop()==null;
    }

    public void setOop(@Nullable Oop oopDesc){
        this.oopCache=null;
        unsafe.putAddress(this.address,oopDesc==null?0L:oopDesc.address);
    }

    @Override
    public String toString() {
        return "WeakHandle@0x"+Long.toHexString(this.address);
    }
}
