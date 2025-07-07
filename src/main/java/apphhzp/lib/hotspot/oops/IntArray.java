package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class IntArray extends JVMObject implements Iterable<Integer> {
    public static final Type TYPE= JVM.type("Array<int>");
    public static final int SIZE=TYPE.size;
    public static final long LENGTH_OFFSET=TYPE.offset("_length");
    public static final long DATA_OFFSET=TYPE.offset("_data");
    public IntArray(long addr) {
        super(addr);
    }

    public int length(){
        return unsafe.getInt(this.address+LENGTH_OFFSET);
    }

    private void checkBound(int index){
        if (index<0||index>=this.length()){
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    public void set(int index, int value) {
        checkBound(index);
        unsafe.putInt(this.address+DATA_OFFSET+ (long) JVM.intSize *index, value);
    }

    public int get(int index) {
        checkBound(index);
        return unsafe.getInt(this.address+DATA_OFFSET + (long) JVM.intSize *index);
    }

    public int[] toIntArray(){
        int[] array=new int[length()];
        for(int i=0;i<array.length;i++){
            array[i]=unsafe.getInt(this.address+DATA_OFFSET + (long) JVM.intSize *i);
        }
        return array;
    }

    public boolean contains(int val){
        return find(val,0)!=-1;
    }

    public int find(int val){
        return find(val,0);
    }

    public int find(int val,int st_index){
        for (int i=st_index,len=this.length();i<len;i++){
            if (unsafe.getInt(this.address+DATA_OFFSET+ (long) i *JVM.intSize) ==val){
                return i;
            }
        }
        return -1;
    }

    public IntArray copy(int expand){
        int len=this.length();
        long addr=unsafe.allocateMemory(DATA_OFFSET+ (long) (len + expand) *JVM.intSize);
        unsafe.copyMemory(this.address,addr,DATA_OFFSET+Math.min(len*JVM.intSize,(len+expand)*JVM.intSize));
        unsafe.putInt(addr,len+expand);
        return new IntArray(addr);
    }

    @Override
    public String toString() {
        return "Array<int>@0x"+Long.toHexString(this.address);
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int index=0;
            private final int size=IntArray.this.length();
            @Override
            public boolean hasNext() {
                return index<size;
            }

            @Override
            public Integer next() {
                if (index >= size)
                    throw new NoSuchElementException();
                return unsafe.getInt(IntArray.this.address+DATA_OFFSET+ (long) (index++) *JVM.intSize);
            }
        };
    }
}
