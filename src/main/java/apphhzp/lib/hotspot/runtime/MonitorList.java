package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class MonitorList extends JVMObject implements Iterable<ObjectMonitor>{
    public static final Type TYPE= JVM.type("MonitorList");
    public static final Type SYNCHRONIZER_TYPE=JVM.type("ObjectSynchronizer");
    public static final int SIZE=TYPE.size;
    public static final long HEAD_OFFSET=TYPE.offset("_head");
    public static final long IN_USE_LIST_ADDRESS=SYNCHRONIZER_TYPE.global("_in_use_list");
    public static final MonitorList inUseList=new MonitorList(IN_USE_LIST_ADDRESS);
    private ObjectMonitor headCache;
    public MonitorList(long addr) {
        super(addr);
    }
    @Nullable
    public ObjectMonitor getHead(){
        long addr= unsafe.getAddress(this.address+HEAD_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.headCache,addr)){
            this.headCache=new ObjectMonitor(addr);
        }
        return this.headCache;
    }

    public void setHead(@Nullable ObjectMonitor monitor){
        unsafe.putAddress(this.address+HEAD_OFFSET,monitor==null?0L:monitor.address);
    }


    @Nonnull
    @Override
    public Iterator<ObjectMonitor> iterator() {
        return new Iterator<>() {
            private ObjectMonitor current=MonitorList.this.getHead();
            @Override
            public boolean hasNext() {
                return this.current!=null;
            }

            @Override
            public ObjectMonitor next() {
                if (this.current==null){
                    throw new NoSuchElementException();
                }
                ObjectMonitor re=this.current;
                this.current=re.getNext();
                return re;
            }
        };
    }
}
