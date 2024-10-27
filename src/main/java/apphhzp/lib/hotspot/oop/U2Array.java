package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelper.unsafe;

public class U2Array extends JVMObject implements Iterable<Short>{
    public static final Type TYPE= JVM.type("Array<u2>");
    public static final long DATA_OFFSET=TYPE.offset("_data");
    public U2Array(long addr) {
        super(addr);
    }

    public short get(int index) {
        checkBound(index);
        return unsafe.getShort(this.address+DATA_OFFSET+ 2L *index);
    }


    public void set(int index, short val) {
        checkBound(index);
        unsafe.putShort(this.address+DATA_OFFSET+ 2L*index,val);
    }

    public short[] toShortArray(){
        short[] re=new short[this.length()];
        for (int i=0,len=this.length();i<len;i++){
            re[i]=unsafe.getShort(this.address+DATA_OFFSET+i*2L);
        }
        return re;
    }

    public boolean contains(short val){
        return find(val,0)!=-1;
    }

    public int find(short val){
        return find(val,0);
    }

    public int find(short val,int st_index){
        for (int i=st_index,len=this.length();i<len;i++){
            if (unsafe.getShort(this.address+DATA_OFFSET+i*2L) ==val){
                return i;
            }
        }
        return -1;
    }

    public U2Array copy(int expand){
        int len=this.length();
        long addr=unsafe.allocateMemory(DATA_OFFSET+(len+expand)*2L);
        unsafe.copyMemory(this.address,addr,DATA_OFFSET+Math.min(len*2L,(len+expand)*2L));
        unsafe.putInt(addr,len+expand);
        return new U2Array(addr);
    }

    private void checkBound(int index){
        if (index<0||index>=this.length()){
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    public int length(){
        return unsafe.getInt(this.address);
    }

    @Nonnull
    @Override
    public Iterator<Short> iterator() {
        return new Iterator<>() {
            private int index=0;
            private final int size=U2Array.this.length();
            @Override
            public boolean hasNext() {
                return index<size;
            }

            @Override
            public Short next() {
                if (index >= size)
                    throw new NoSuchElementException();
                return unsafe.getShort(U2Array.this.address+DATA_OFFSET+(index++)*2L);
            }
        };
    }

    @Override
    public String toString() {
        return "Array<u2>@0x"+Long.toHexString(this.address);
    }
}
