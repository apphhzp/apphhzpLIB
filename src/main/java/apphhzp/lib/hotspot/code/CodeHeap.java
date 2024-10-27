package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.memory.VirtualSpace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelper.unsafe;
import static apphhzp.lib.hotspot.code.blob.CodeBlob.getCodeBlob;

public class CodeHeap extends JVMObject implements Iterable<CodeBlob>{
    public static final Type TYPE= JVM.type("CodeHeap");
    public static final int SIZE=TYPE.size;
    public static final long MEMORY_OFFSET=TYPE.offset("_memory");
    public static final long SEGMAP_OFFSET=TYPE.offset("_segmap");
    public static final long LOG2_SEGMENT_SIZE_OFFSET=TYPE.offset("_log2_segment_size");
    public final VirtualSpace memory;
    public final VirtualSpace segmap;
    public CodeHeap(long addr) {
        super(addr);
        this.memory=new VirtualSpace(this.address+MEMORY_OFFSET);
        this.segmap=new VirtualSpace(this.address+SEGMAP_OFFSET);
    }
    public int log2SegmentSize(){
        return unsafe.getInt(this.address+LOG2_SEGMENT_SIZE_OFFSET);
    }

    public long segmentFor(long p) {
        return p-this.begin() >> this.log2SegmentSize();
    }

    public long begin(){
        return this.memory.low();
    }

    public long end(){
        return this.memory.high();
    }

    public boolean contains(long addr){
        return this.begin()<=addr&&addr<this.end();
    }

    public long findStart(long addr){
        if (!this.contains(addr)){
            return 0L;
        }
        HeapBlock block=this.findBlock(addr);
        if (block==null||!block.header.isUsed()){
            return 0L;
        }
        return block.address+HeapBlock.SIZE;
    }

    @Nullable
    public HeapBlock findBlock(long addr) {
        long base = this.findBlockStart(addr);
        if (base == 0L){
            return null;
        }
        return new HeapBlock(base);
    }

    public long findBlockStart(long addr) {
        long i = this.segmentFor(addr);
        long b = this.segmap.low();
        if ((unsafe.getByte(b+i)&0xff)==0xFF) {
            return 0L;
        }
        while ((unsafe.getByte(b+i)&0xff)> 0) {
            i-=(unsafe.getByte(b+i)&0xff);
        }
        return this.begin()+(i<<this.log2SegmentSize());
    }

    public long nextBlock(HeapBlock block){
        if (block==null){
            return 0L;
        }
        return block.address+block.header.getLength()*(1L <<this.log2SegmentSize());
    }

    @Nonnull
    @Override
    public Iterator<CodeBlob> iterator() {
        return new BlobIterator(this);
    }

    private static final class BlobIterator implements Iterator<CodeBlob>{
        private final CodeHeap owner;
        private long addr;
        private CodeBlob last;
        private final long ed;

        public BlobIterator(CodeHeap heap){
            this.owner=heap;
            this.addr=heap.begin();
            this.ed=heap.end();
            this.last=this.getNext();
        }

        public CodeBlob getNext(){
            CodeBlob tmp=null;
            while (this.addr!=0L&&this.addr<this.ed&&tmp==null) {
                tmp=getCodeBlob(this.owner.findStart(this.addr));
                long next = this.owner.nextBlock(this.owner.findBlock(this.addr));
                if (next!=0L&& next<this.addr) {
                    throw new InternalError("Pointer moved backwards");
                }
                this.addr = next;
            }
            return tmp;
        }

        @Override
        public boolean hasNext() {
            return this.last!=null;
        }

        @Override
        public CodeBlob next() {
            if (!this.hasNext()){
                throw new NoSuchElementException();
            }
            CodeBlob re=this.last;
            this.last=this.getNext();
            return re;
        }
    }
}
