package apphhzp.lib.hotspot.memory;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class MemRegion extends JVMObject {
    public static final Type TYPE= JVM.type("MemRegion");
    public static final int SIZE=TYPE.size;
    public static final long START_OFFSET= TYPE.offset("_start");
    public static final long WORD_SIZE_OFFSET= TYPE.offset("_word_size");
    private final boolean fakePointer;
    private long start;
    private long wordSize;
    private long byteSize;
    public static MemRegion createFake(long start,long wordSize){
        return new MemRegion(start,wordSize);
    }

    public static MemRegion createLimitFake(long start,long limit){
        return new MemRegion(start,limit,0);
    }

    public MemRegion(long addr) {
        super(addr);
        fakePointer=false;
    }
    private MemRegion(long start, long wordSize) {
        super(0);
        this.fakePointer=true;
        this.start=start;
        this.wordSize=wordSize;
        this.byteSize=wordSize*JVM.oopSize;
    }

    private MemRegion(long start, long limit,int __) {
        super(0);
        this.fakePointer=true;
        this.start=start;
        this.byteSize=limit-start;
        this.wordSize=this.byteSize/JVM.oopSize;
    }

    private MemRegion() {
        super(0);
        this.fakePointer=true;
    }

    public long start(){
        if (fakePointer){
            return start;
        }
        return unsafe.getAddress(this.address+START_OFFSET);
    }

    public long wordSize(){
        if (fakePointer){
            return wordSize;
        }
        return JVM.getSizeT(this.address+WORD_SIZE_OFFSET);
    }

    public long end() {return this.start() + this.wordSize()*JVM.oopSize;}
    public void setEnd(long end){
        if (fakePointer){
            byteSize = end-this.start();
            wordSize=byteSize/JVM.oopSize;
        }else {

        }
    }
    public long last(){ return this.start() + (this.wordSize() - 1)*JVM.oopSize;}

    public long byteSize(){ return this.wordSize() * JVM.oopSize; }
    public boolean isEmpty(){ return this.wordSize()==0;}

    public boolean contains(MemRegion mr2) {
        return this.start() <= mr2.start() && this.end() >= mr2.end();
    }
    public boolean contains(long addr){
        return addr >= this.start() && addr < end();
    }

    public MemRegion union(MemRegion mr2) {
        MemRegion res = new MemRegion();
        res.start = Math.min(mr2.start(), this.start());
        res.setEnd(Math.max(this.end(), mr2.end()));
        return res;
    }

    public MemRegion intersection(MemRegion mr2) {
        MemRegion res = new MemRegion();
        res.start = Math.max(mr2.start(), this.start());
        long resEnd;
        long end = end();
        long mr2End = mr2.end();
        resEnd = Math.min(end, mr2End);
        if (resEnd<res.start()){
            res.wordSize=0;
            res.byteSize=0;
            res.start=0;
        } else {
            res.setEnd(resEnd);
        }
        return res;
    }

    @Override
    public String toString() {
        if (fakePointer){
            return "MemRegion@("+this.start+","+this.wordSize+")";
        }
        return "MemRegion@0x"+Long.toHexString(this.address);
    }
}
