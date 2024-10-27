package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.LongFunction;

import static apphhzp.lib.ClassHelper.unsafe;

public class VMTypeArray<T extends JVMObject> extends JVMObject implements Iterable<T>{
    public static final long DATA_OFFSET= JVM.oopSize;//内存对齐
    public final Class<T> type;
    public final LongFunction<T> constructor;

    public VMTypeArray(long addr,Class<T> type, LongFunction<T> constructor) {
        super(addr);
        try {
            this.type=type;
            this.constructor=constructor;
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }


    public T get(int index) {
        checkBound(index);
        try {
            return constructor.apply(unsafe.getAddress(this.address+DATA_OFFSET+ (long) JVM.oopSize *index));
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public long getAddress(int index) {
        checkBound(index);
        return unsafe.getAddress(this.address+DATA_OFFSET+ (long) JVM.oopSize *index);
    }


    public void set(int index, T val) {
        setAddress(index,val.address);
    }

    public void setAddress(int index,long addr) {
        checkBound(index);
        unsafe.putAddress(this.address+DATA_OFFSET+ (long) JVM.oopSize*index,addr);
    }


    @SuppressWarnings("unchecked")
    public T[] toArray(){
        try {
            T[] re= (T[]) Array.newInstance(this.type,this.length());
            for (int i=0,len=this.length();i<len;i++){
                re[i]= constructor.apply(unsafe.getAddress(this.address+DATA_OFFSET+(long) JVM.oopSize*i));
            }
            return re;
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public boolean contains(T val){
        return containsAddress(val.address);
    }

    public boolean containsAddress(long addr){
        for (int i=0,len=this.length();i<len;i++){
            if (unsafe.getAddress(this.address+DATA_OFFSET+ (long) i *JVM.oopSize)==addr){
                return true;
            }
        }
        return false;
    }

    public int find(T val){
        return find(val,0);
    }

    public int find(T val,int st_index){
        for (int i=st_index,len=this.length();i<len;i++){
            if (unsafe.getAddress(this.address+DATA_OFFSET+ (long) i *JVM.oopSize)==val.address){
                return i;
            }
        }
        return -1;
    }

    public VMTypeArray<T> copy(int expand){
        int len=this.length();
        long addr=unsafe.allocateMemory(DATA_OFFSET+ (long) (len + expand) *JVM.oopSize);
        unsafe.copyMemory(this.address,addr,DATA_OFFSET+Math.min((long) len *JVM.oopSize,(long)(len + expand) *JVM.oopSize));
        unsafe.putInt(addr,len+expand);
        return new VMTypeArray<>(addr,this.type,this.constructor);
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
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int index=0;
            private final int size=VMTypeArray.this.length();
            @Override
            public boolean hasNext() {
                return index<size;
            }

            @Override
            public T next() {
                if (index >= size)
                    throw new NoSuchElementException();
                try {
                    return constructor.apply(unsafe.getAddress(VMTypeArray.this.address+DATA_OFFSET+ (long) JVM.oopSize *this.index++));
                }catch (Throwable t){
                    throw new RuntimeException(t);
                }
            }
        };
    }

    @Override
    public String toString() {
        return "Array<"+this.type.getName()+"*>@0x"+Long.toHexString(this.address);
    }
}
