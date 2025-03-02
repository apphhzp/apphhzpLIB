package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static apphhzp.lib.ClassHelper.unsafe;

public class ThreadsList extends JVMObject {
    public static final Type TYPE = JVM.type("ThreadsList");
    public static final int SIZE=TYPE.size;
    public static final long MAGIC_OFFSET=0;
    public static final long LENGTH_OFFSET= TYPE.offset("_length");
    public static final long NEXT_LIST_OFFSET=JVM.computeOffset(JVM.oopSize,LENGTH_OFFSET+JVM.intSize);
    public static final long THREADS_OFFSET= TYPE.offset("_threads");


    public static final int THREADS_LIST_MAGIC= (int)(('T' << 24) | ('L' << 16) | ('S' << 8) | 'T');
    static {
        if (JVM.computeOffset(JVM.intSize, MAGIC_OFFSET + JVM.intSize) != LENGTH_OFFSET)
            throw new AssertionError();
        if (JVM.computeOffset(JVM.oopSize, NEXT_LIST_OFFSET + JVM.oopSize) != THREADS_OFFSET)
            throw new AssertionError();
    }
    private ThreadsList nextListCache;

    public ThreadsList(long addr) {
        super(addr);
    }

    public int length(){
        return unsafe.getInt(this.address+LENGTH_OFFSET);
    }

    public long getThreadAddressAt(int index){
        checkBound(index);
        return unsafe.getAddress(this.address+THREADS_OFFSET+ (long) JVM.oopSize *index);
    }

    @Nullable
    public ThreadsList getNextList(){
        long addr=unsafe.getAddress(this.address+NEXT_LIST_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextListCache,addr)){
            this.nextListCache=new ThreadsList(addr);
        }
        return this.nextListCache;
    }

    public ArrayList<JavaThread> getAllThreads(){
        int len=this.length();
        ArrayList<JavaThread> re=new ArrayList<>();
        long base=unsafe.getAddress(this.address+THREADS_OFFSET);
        for (int i=0;i<len;i++){
            long addr=unsafe.getAddress(base+ (long) JVM.oopSize *i);
            if (addr!=0L){
                re.add(JavaThread.getOrCreate(addr));
            }
        }
        return re;
    }

    public JavaThread first(){
        int len=this.length();
        long base=unsafe.getAddress(this.address+THREADS_OFFSET);
        for (int i=0;i<len;i++){
            long addr=unsafe.getAddress(base+ (long) JVM.oopSize *i);
            if (addr!=0L){
                return (JavaThread.getOrCreate(addr));
            }
        }
        return null;
    }

    private void checkBound(int i){
        if (i<0||i>=this.length()){
            throw new ArrayIndexOutOfBoundsException(i);
        }
    }

    @Override
    public String toString() {
        return "ThreadsList@0x"+Long.toHexString(this.address);
    }
}
