package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static apphhzp.lib.ClassHelper.unsafe;

public class StubQueue<T extends Stub> extends JVMObject implements Iterable<T>{
    public static final Type TYPE= JVM.type("StubQueue");
    public static final int SIZE= TYPE.size;
    public static final long STUB_BUFFER_OFFSET=TYPE.offset("_stub_buffer");
    public static final long BUFFER_LIMIT_OFFSET=TYPE.offset("_buffer_limit");
    public static final long QUEUE_BEGIN_OFFSET=TYPE.offset("_queue_begin");
    public static final long QUEUE_END_OFFSET=TYPE.offset("_queue_end");
    public static final long NUMBER_OF_STUBS_OFFSET=TYPE.offset("_number_of_stubs");
    private final Function<Long,T> constructor;
    public StubQueue(long addr, Function<Long,T> constructor) {
        super(addr);
        this.constructor=constructor;
    }

    public long bufferBase(){
        return unsafe.getAddress(this.address+STUB_BUFFER_OFFSET);
    }

    public int getBufferLimit() {
        return unsafe.getInt(this.address+BUFFER_LIMIT_OFFSET);
    }

    public int getQueueBegin() {
        return unsafe.getInt(this.address+QUEUE_BEGIN_OFFSET);
    }

    public int getQueueEnd() {
        return unsafe.getInt(this.address+QUEUE_END_OFFSET);
    }

    public int getNumberOfStubs() {
        return unsafe.getInt(this.address+NUMBER_OF_STUBS_OFFSET);
    }

    public T getFirst(){
        return this.getNumberOfStubs()>0?this.getStub(this.getQueueBegin()):null;
    }

    public T getStub(long offset){
        checkAddress(offset);
        return this.constructor.apply(this.bufferBase()+offset);
    }

    @Nullable
    public T getNextOf(T val){
        long offset=val.address-this.bufferBase()+val.getSize();
        if (offset==this.getBufferLimit()){
            offset=0;
        }
        return offset==this.getQueueEnd()?null:this.getStub(offset);
    }



    private void checkBound(int index){
        if (index<0||index>=this.getNumberOfStubs()){
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    private void checkAddress(long offset){
        if (offset<0||offset>=this.getBufferLimit()||offset%JVM.oopSize!=0){
            throw new ArrayIndexOutOfBoundsException("Offset:"+offset);
        }
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private final long endAddress=StubQueue.this.getQueueEnd();
            private final long baseAddress=StubQueue.this.bufferBase();
            private long offset=StubQueue.this.getQueueBegin();
            @Override
            public boolean hasNext() {
                return offset<endAddress;
            }

            @Override
            public T next() {
                if (offset>=endAddress){
                    throw new NoSuchElementException();
                }
                T re= StubQueue.this.constructor.apply(baseAddress+offset);
                offset+=re.getSize();
                return re;
            }
        };
    }
}
