package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static apphhzp.lib.ClassHelper.unsafe;

public class VMTypeGrowableArray<T extends JVMObject> extends JVMObject implements Iterable<T>{
    public static final Type BASE = JVM.type("GrowableArrayBase");
    public static final long LEN_OFFSET = BASE.offset("_len");
    public static final long MAX_OFFSET = BASE.offset("_max");
    public static final long DATA_OFFSET = Math.max(BASE.size, JVM.oopSize);
    private final Function<Long, T> constructor;

    public VMTypeGrowableArray(long addr, Function<Long, T> constructor) {
        super(addr);
        this.constructor = constructor;
    }

    public int length() {
        return unsafe.getInt(this.address + LEN_OFFSET);
    }

    public int maxLength() {
        return unsafe.getInt(this.address + MAX_OFFSET);
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

    public void clear(){
        unsafe.putAddress(this.address+DATA_OFFSET,0L);
        unsafe.putInt(this.address+LEN_OFFSET,0);
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
