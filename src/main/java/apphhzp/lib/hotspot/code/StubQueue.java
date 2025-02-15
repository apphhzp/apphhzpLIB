package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.LongFunction;

import static apphhzp.lib.ClassHelper.unsafe;

public class StubQueue<T extends Stub> extends JVMObject implements Iterable<T> {
    public static final Type TYPE = JVM.type("StubQueue");
    public static final int SIZE = TYPE.size;
    public static final long STUB_BUFFER_OFFSET = TYPE.offset("_stub_buffer");
    public static final long BUFFER_LIMIT_OFFSET = TYPE.offset("_buffer_limit");
    public static final long BUFFER_SIZE_OFFSET = BUFFER_LIMIT_OFFSET - JVM.intSize;
    public static final long QUEUE_BEGIN_OFFSET = TYPE.offset("_queue_begin");
    public static final long QUEUE_END_OFFSET = TYPE.offset("_queue_end");
    public static final long NUMBER_OF_STUBS_OFFSET = TYPE.offset("_number_of_stubs");
    private final LongFunction<T> constructor;

    public StubQueue(long addr, LongFunction<T> constructor) {
        super(addr);
        this.constructor = constructor;
    }

    public long codeStart() {
        return unsafe.getAddress(this.address + STUB_BUFFER_OFFSET);
    }

    public int getBufferSize() {
        return unsafe.getInt(this.address + BUFFER_SIZE_OFFSET);
    }

    public void setBufferSize(int size) {
        unsafe.putInt(this.address + BUFFER_SIZE_OFFSET, size);
    }

    public int getBufferLimit() {
        return unsafe.getInt(this.address + BUFFER_LIMIT_OFFSET);
    }

    public void setBufferLimit(int limit) {
        unsafe.putInt(this.address + BUFFER_LIMIT_OFFSET, limit);
    }

    public int getQueueBegin() {
        return unsafe.getInt(this.address + QUEUE_BEGIN_OFFSET);
    }

    public void setQueueBegin(int val){
        unsafe.putInt(this.address + QUEUE_BEGIN_OFFSET, val);
    }

    public int getQueueEnd() {
        return unsafe.getInt(this.address + QUEUE_END_OFFSET);
    }

    public void setQueueEnd(int val){
        unsafe.putInt(this.address + QUEUE_END_OFFSET, val);
    }

    public int getNumberOfStubs() {
        return unsafe.getInt(this.address + NUMBER_OF_STUBS_OFFSET);
    }

    public void setNumberOfStubs(int val){
        unsafe.putInt(this.address + NUMBER_OF_STUBS_OFFSET, val);
    }

    public long codeEnd() {
        return this.getQueueBegin() + this.getBufferLimit();
    }

    public boolean isEmpty() {
        return this.getQueueBegin() == this.getQueueEnd();
    }

    public int totalSpace() {
        return this.getBufferSize() - 1;
    }

    public int availableSpace() {
        int d = this.getQueueBegin() - this.getQueueEnd() - 1;
        return d < 0 ? d + this.getBufferSize() : d;
    }

    public int usedSpace() {
        return totalSpace() - availableSpace();
    }

    public boolean contains(long pc) {
        return this.codeStart() <= pc && pc < this.codeEnd();
    }

    private void checkIndex(int i) {
        if (!(0 <= i && i < this.getBufferLimit() && i % JVM.codeEntryAlignment == 0)){
            throw new IndexOutOfBoundsException("illegal index");
        }
    }

    public boolean isContiguous() {
        return this.getQueueBegin() <= this.getQueueEnd();
    }

    public int indexOf(T s) {
        int i = (int) (s.address - this.codeStart());
        checkIndex(i);
        return i;
    }

    public T stubAt(int i) {
        checkIndex(i);
        return this.constructor.apply(this.codeStart() + i);
    }

    public T currentStub() {
        return stubAt(this.getQueueEnd());
    }

    public T first(){ return this.getNumberOfStubs() > 0 ? stubAt(this.getQueueBegin()) : null; }
    public T next(T s){
        int i = indexOf(s) + (s.getSize());
        // Only wrap around in the non-contiguous case (see stubss.cpp)
        if (i == this.getBufferLimit() && this.getQueueEnd() < this.getBufferLimit()){
            i = 0;
        }
        return (i == this.getQueueEnd()) ? null : stubAt(i);
    }

    public void removeFirst() {
        if (this.getNumberOfStubs()== 0) return;
        T s = first();
        s.c_finalize();
        this.setQueueBegin(this.getQueueBegin()+s.getSize());
        if (this.getQueueBegin()>this.getBufferLimit()){
            throw new RuntimeException("sanity check");
        }
        if (this.getQueueBegin() == this.getQueueEnd()) {
            // buffer empty
            // => reset queue indices
            this.setQueueBegin(0);
            this.setQueueEnd(0);
            this.setBufferLimit(this.getBufferSize());
        } else if (this.getQueueBegin() == this.getBufferLimit()) {
            // buffer limit reached
            // => reset buffer limit & wrap around
            this.setBufferLimit(this.getBufferSize());
            this.setQueueBegin(0);
        }
        this.setNumberOfStubs(this.getNumberOfStubs() - 1);
    }

    public void removeFirst(int n) {
        int i = Math.min(n, this.getNumberOfStubs());
        while (i-- > 0) removeFirst();
    }


    public void removeAll(){
        removeFirst(this.getNumberOfStubs());
        if (JVM.ASSERTS_ENABLED&&this.getNumberOfStubs()!=0){
            throw new RuntimeException("sanity check");
        }
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private T current=StubQueue.this.first();
            @Override
            public boolean hasNext() {
                return current!=null;
            }

            @Override
            public T next(){
                T re=current;
                if (re==null){
                    throw new NoSuchElementException();
                }
                current=StubQueue.this.next(current);
                return re;
            }
        };
    }
}
