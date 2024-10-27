package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.util.ArrayList;

import static apphhzp.lib.ClassHelper.unsafe;

public class ThreadsList extends JVMObject {
    public static final Type TYPE = JVM.type("ThreadsList");
    public static final long LENGTH_OFFSET= TYPE.offset("_length");
    public static final long THREADS_OFFSET= TYPE.offset("_threads");
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

    private void checkBound(int i){
        if (i<0||i>=this.length()){
            throw new ArrayIndexOutOfBoundsException(i);
        }
    }
}
