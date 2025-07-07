package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.LongFunction;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class VMTypeGrowableArray<T extends JVMObject> extends JVMObject implements Iterable<T>{
    public static final Type BASE = JVM.type("GrowableArrayBase");
    public static final long LEN_OFFSET = BASE.offset("_len");
    public static final long MAX_OFFSET = BASE.offset("_max");
    public static final long DATA_OFFSET = JVM.type("GrowableArray<int>").offset("_data");//Math.max(BASE.size, JVM.oopSize);
    private final LongFunction<T> constructor;

    public VMTypeGrowableArray(long addr, LongFunction<T> constructor) {
        super(addr);
        this.constructor = constructor;
    }

    public int length() {
        return unsafe.getInt(this.address + LEN_OFFSET);
    }
    public void  setLength(int len) {
        unsafe.putInt(this.address + LEN_OFFSET, len);
    }

    public int maxLength() {
        return unsafe.getInt(this.address + MAX_OFFSET);
    }

    public void  setMaxLength(int len) {
        unsafe.putInt(this.address + MAX_OFFSET, len);
    }
    private long addressOf(int index) {
        return unsafe.getAddress(this.address + DATA_OFFSET) + (long) JVM.oopSize * index;
    }

    public T get(int index) {
        checkBound(index);
        return constructor.apply(unsafe.getAddress(this.addressOf(index)));
    }

    public long getAddress(int index) {
        checkBound(index);
        return unsafe.getAddress(this.addressOf(index));
    }

    public void set(T val, int index) {
        this.setAddress(val.address, index);
    }

    public void setAddress(long addr, int index) {
        checkBound(index);
        unsafe.putAddress(this.addressOf(index), addr);
    }

    public boolean contains(T val) {
        return this.contains(val.address);
    }

    public boolean contains(long addr) {
        return this.find(addr) != -1;
    }

    public int find(T val) {
        return this.find(val.address, 0);
    }

    public int find(T val, int st_index) {
        return this.find(val.address, st_index);
    }

    public int find(long addr) {
        return this.find(addr, 0);
    }

    public int find(long addr, int st_index) {
        for (int i = st_index, len = this.length(); i < len; i++) {
            if (unsafe.getAddress(this.addressOf(i)) == addr) {
                return i;
            }
        }
        return -1;
    }

    private void checkBound(int index) {
        if (index < 0 || index >= this.length()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    public int append(T elem) {
        if (this.length() == this.maxLength()) grow(this.length());
        int idx =this.length() ;
        this.setLength(idx+1);
        this.set(elem,idx);
        return idx;
    }

    public boolean append_if_missing(T elem) {
        // Returns TRUE if elem is added.
        boolean missed = !this.contains(elem);
        if (missed) append(elem);
        return missed;
    }

    public void clear(){
        unsafe.putAddress(this.address+DATA_OFFSET,0L);
        unsafe.putInt(this.address+LEN_OFFSET,0);
    }

    public void grow(int j) {
        throw new UnsupportedOperationException("Not supported.");
//        int old_max = this.maxLength();
//        // grow the array by increasing _max to the first power of two larger than the size we need
//        this.setMaxLength(next_power_of_2(j));
//        // j < _max
//        long newData = static_cast<Derived*>(this)->allocate();
//        int i = 0;
//        for (; i < this.length(); i++) ::new ((void*)&newData[i]) E(this->_data[i]);
//        for (     ; i <this.maxLength(); i++) ::new ((void*)&newData[i]) E();
//        for (i = 0; i < old_max; i++) this->_data[i].~E();
//        if (this->_data != NULL) {
//            static_cast<Derived*>(this)->deallocate(this->_data);
//        }
//        this->_data = newData;
    }

    public int round_up_power_of_2(int value) {
        if (value<=0){
            throw new IllegalArgumentException("Invalid value");
        }
        if ((value&(value-1))==0) {
            return value;
        }
        for (int i=0;i<32;i++){
            if ((1<<i)>=value){
                return (1<<i);
            }
        }
        throw new RuntimeException();
    }

    int next_power_of_2(int value)  {
        if (value == 2147483647){
            throw new IllegalArgumentException("Overflow");
        }
        return round_up_power_of_2(value + 1);
    }


    @Nonnull
    @Override
    public Iterator<T> iterator() {

        return new Iterator<>() {
            private final int size=VMTypeGrowableArray.this.length();
            private int index=0;
            @Override
            public boolean hasNext() {
                return index<size;
            }

            @Override
            public T next() {
                if (index>=size){
                    throw new NoSuchElementException();
                }
                return VMTypeGrowableArray.this.constructor.apply(unsafe.getAddress(VMTypeGrowableArray.this.addressOf(index++)));
            }
        };
    }
}
