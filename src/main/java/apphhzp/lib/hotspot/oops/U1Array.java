package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;


/**
 * typedef U1Array AnnotationArray;
 * */
public class U1Array extends JVMObject implements Iterable<Byte>{
    public static final Type TYPE= JVM.type("Array<u1>");
    public static final long DATA_OFFSET=TYPE.offset("_data");
    public U1Array(long addr) {
        super(addr);
    }
    public U1Array(byte[] arr) {
        super(unsafe.allocateMemory(arr.length+TYPE.size+1));
        unsafe.putInt(this.address,arr.length);
        for (int i=0;i<arr.length;i++){
            unsafe.putByte(this.address+DATA_OFFSET+i,arr[i]);
        }
    }

    public byte get(int index) {
        checkBound(index);
        return unsafe.getByte(this.address+DATA_OFFSET+index);
    }


    public void set(int index, byte val) {
        checkBound(index);
        unsafe.putByte(this.address+DATA_OFFSET+index,val);
    }

    public byte[] toByteArray(){
        byte[] re=new byte[this.length()];
        for (int i=0,len=this.length();i<len;i++){
            re[i]=unsafe.getByte(this.address+DATA_OFFSET+i);
        }
        return re;
    }

    public boolean contains(byte val){
        return find(val,0)!=-1;
    }

    public int find(byte val){
        return find(val,0);
    }

    public int find(byte val,int st_index){
        for (int i=st_index,len=this.length();i<len;i++){
            if (unsafe.getByte(this.address+DATA_OFFSET+i) ==val){
                return i;
            }
        }
        return -1;
    }

    public U1Array copy(int expand){
        int len=this.length();
        if (len+expand<0){
            throw new IllegalArgumentException();
        }
        long addr=unsafe.allocateMemory(DATA_OFFSET+len+expand);
        unsafe.copyMemory(this.address,addr,DATA_OFFSET+Math.min(len,len+expand));
        unsafe.putInt(addr,len+expand);
        return new U1Array(addr);
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
    public Iterator<Byte> iterator() {
        return new Iterator<>() {
            private int index=0;
            private final int size= U1Array.this.length();
            @Override
            public boolean hasNext() {
                return index<size;
            }

            @Override
            public Byte next() {
                if (index >= size)
                    throw new NoSuchElementException();
                return unsafe.getByte(U1Array.this.address+DATA_OFFSET+ index++);
            }
        };
    }

    @Override
    public String toString() {
        return "Array<u1>@0x"+Long.toHexString(this.address);
    }
}
