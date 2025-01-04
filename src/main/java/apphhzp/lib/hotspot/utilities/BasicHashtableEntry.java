package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class BasicHashtableEntry extends JVMObject {
    public static final Type TYPE= JVM.type("BasicHashtableEntry<mtInternal>");
    public static final int SIZE=TYPE.size;
    public static final long HASH_OFFSET=TYPE.offset("_hash");
    public static final long NEXT_OFFSET=TYPE.offset("_next");
    protected BasicHashtableEntry nextCache;
    public BasicHashtableEntry(long addr) {
        super(addr);
    }

    public long getHash(){
        return unsafe.getInt(this.address+HASH_OFFSET)&0xFFFFFFFFL;
    }

    public void  setHash(long hash){
        unsafe.putInt(this.address+HASH_OFFSET,(int)(hash&0xFFFFFFFFL));
    }
    @Nullable
    public BasicHashtableEntry getNext(){
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=new BasicHashtableEntry(addr);
        }
        return this.nextCache;
    }

    public void setNext(@Nullable BasicHashtableEntry next){
        this.nextCache=null;
        unsafe.putAddress(this.address+NEXT_OFFSET,next==null?0L:next.address);
    }

    @Override
    public String toString() {
        return "BasicHashtableEntry@0x"+Long.toHexString(this.address);
    }
}
