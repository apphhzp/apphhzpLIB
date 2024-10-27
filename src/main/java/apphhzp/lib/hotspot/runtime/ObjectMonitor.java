package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oop.Oop;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

import static apphhzp.lib.ClassHelper.unsafe;

public class ObjectMonitor extends JVMObject {
    public static final Type TYPE= JVM.type("ObjectMonitor");
    public static final int SIZE=TYPE.size;
    public static final long IN_USE_LIST_ADDRESS=JVM.type("ObjectSynchronizer").global("_in_use_list");
    public static final long HEADER_OFFSET=TYPE.offset("_header");
    public static final long OBJECT_OFFSET=TYPE.offset("_object");
    public static final long NEXT_OFFSET=TYPE.offset("_next_om");
    public static final long WAITERS_OFFSET=TYPE.offset("_waiters");
    private static final HashMap<Long,ObjectMonitor> CACHE=new HashMap<>();
    private Oop objCache;
    private ObjectMonitor nextCache;
    public static ObjectMonitor getOrCreate(long addr){
        if (addr==0L){
            throw new RuntimeException("Pointer is NULL(0)!");
        }
        if (CACHE.containsKey(addr)){
            return CACHE.get(addr);
        }
        ObjectMonitor re=new ObjectMonitor(addr);
        CACHE.put(addr,re);
        return re;
    }

    public static void clearCacheMap(){
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public ObjectMonitor(long addr) {
        super(addr);
    }

    public Object getObject(){
        long addr=Oop.fromOopHandle(this.address+OBJECT_OFFSET);
        if (!isEqual(this.objCache,addr)){
            this.objCache=new Oop(addr);
        }
        return this.objCache.getObject();
    }
    @Nullable
    public ObjectMonitor getNext(){
        long addr= unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L) {
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=getOrCreate(addr);
        }
        return this.nextCache;
    }

    public int getWaiters(){
        return unsafe.getInt(this.address+WAITERS_OFFSET);
    }

    public void setWaiters(int waiters){
        unsafe.putInt(this.address+WAITERS_OFFSET,waiters);
    }

    public void setNext(ObjectMonitor monitor){
        unsafe.putAddress(this.address+NEXT_OFFSET,monitor.address);
    }

    public static ArrayList<ObjectMonitor> getAllObjMonitors(){
        ArrayList<ObjectMonitor> list=new ArrayList<>();
        ObjectMonitor tmp=getOrCreate(unsafe.getAddress(IN_USE_LIST_ADDRESS));
        while (tmp!=null){
            list.add(tmp);
            tmp=tmp.getNext();
        }
        return list;
    }
}
