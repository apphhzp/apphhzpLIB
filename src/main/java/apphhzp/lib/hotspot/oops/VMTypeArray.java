package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.LongFunction;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class VMTypeArray<T extends JVMObject> extends JVMObject implements Iterable<T>{
    public static final long DATA_OFFSET= JVM.oopSize;//内存对齐
    public final Class<T> type;
    public final LongFunction<T> constructor;

    public static <T extends JVMObject> VMTypeArray<T> create(int length,Class<T> type,LongFunction<T> constructor) {
        long addr=unsafe.allocateMemory(DATA_OFFSET+ (long) (length) *JVM.oopSize);
        unsafe.setMemory(addr,DATA_OFFSET+ (long) (length) *JVM.oopSize, (byte) 0);
        unsafe.putInt(addr,length);
        return new VMTypeArray<>(addr, type, constructor);
    }

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
        long addr=unsafe.getAddress(this.address+DATA_OFFSET+ (long) JVM.oopSize *index);
        if (addr==0L){
            return null;
        }
        try {
            return constructor.apply(addr);
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public long getAddress(int index) {
        checkBound(index);
        return unsafe.getAddress(this.address+DATA_OFFSET+ (long) JVM.oopSize *index);
    }


    public void set(int index,@Nullable T val) {
        setAddress(index,val==null?0L:val.address);
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
                long addr=unsafe.getAddress(this.address+DATA_OFFSET+(long) JVM.oopSize*i);
                re[i]= addr==0L?null:constructor.apply(addr);
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
                long addr=unsafe.getAddress(VMTypeArray.this.address+DATA_OFFSET+ (long) JVM.oopSize *this.index++);
                if (addr==0L){
                    return null;
                }
                try {
                    return constructor.apply(addr);
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
