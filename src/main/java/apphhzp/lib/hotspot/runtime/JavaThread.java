package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oop.Oop;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

import static apphhzp.lib.ClassHelper.unsafe;

public class JavaThread extends Thread {
    public static final Type TYPE = JVM.type("JavaThread");
    public static final int SIZE = TYPE.size;
    public static final long JAVA_THREAD_LIST = JVM.type("ThreadsSMRSupport").global("_java_thread_list");
    public static final long STACK_BASE_OFFSET = TYPE.offset("_stack_base");
    public static final long STACK_SIZE_OFFSET = TYPE.offset("_stack_size");
    public static final long THREAD_OBJ_OFFSET = TYPE.offset("_threadObj");
    public static final long VM_RESULT_OFFSET = TYPE.offset("_vm_result");
    public static final long VM_RESULT2_OFFSET = TYPE.offset("_vm_result_2");
    public static final long PENDING_MONITOR_OFFSET=TYPE.offset("_current_pending_monitor");
    public static final long WAITING_MONITOR_OFFSET=TYPE.offset("_current_waiting_monitor");
    public static final long STATE_OFFSET=TYPE.offset("_thread_state");
    private static final HashMap<Long, JavaThread> CACHE = new HashMap<>();
    private Oop vmResultCache;
    private Oop threadObjCache;

    public static JavaThread getOrCreate(long addr) {
        if (addr == 0L) {
            throw new IllegalArgumentException("Pointer is NULL(0).");
        }
        if (CACHE.containsKey(addr)) {
            return CACHE.get(addr);
        }
        JavaThread re;
        Type type=JVM.findDynamicTypeForAddress(addr,TYPE);
        if (type==CompilerThread.TYPE){
            re=new CompilerThread(addr);
        }else {
            re=new JavaThread(addr);
        }
        CACHE.put(addr, re);
        return re;
    }
    public static void clearCacheMap(){
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    protected JavaThread(long addr) {
        super(addr);
    }

    public long getStackBase(){
        return unsafe.getAddress(this.address+STACK_BASE_OFFSET);
    }

    public void setStackBase(long addr){
        unsafe.putAddress(this.address+STACK_BASE_OFFSET,addr);
    }

    public long getStackSize(){
        return JVM.size_tSize==8?unsafe.getLong(this.address+STACK_SIZE_OFFSET):unsafe.getInt(this.address+STACK_SIZE_OFFSET);
    }

    public void setStackSize(long size){
        if (JVM.size_tSize == 8) {
            unsafe.putLong(this.address + STACK_SIZE_OFFSET, size);
        } else {
            unsafe.putInt(this.address + STACK_SIZE_OFFSET, (int) size);
        }
    }

    @Nullable
    public Thread getThreadObj() {
        long addr = Oop.fromOopHandle(this.address + THREAD_OBJ_OFFSET);
        if (!isEqual(this.threadObjCache, addr)) {
            this.threadObjCache = new Oop(addr);
        }
        return this.threadObjCache.getObject();
    }

    public Oop getVMResult(){
        long addr=unsafe.getAddress(this.address+VM_RESULT_OFFSET);
        if (!isEqual(this.vmResultCache,addr)){
            this.vmResultCache=new Oop(addr);
        }
        return this.vmResultCache;
    }
    @Nullable
    public ObjectMonitor getPendingMonitor(){
        long addr=unsafe.getAddress(this.address+PENDING_MONITOR_OFFSET);
        if (addr==0L){
            return null;
        }
        return ObjectMonitor.getOrCreate(addr);
    }

    public void setPendingMonitor(ObjectMonitor monitor){
        unsafe.putAddress(this.address+PENDING_MONITOR_OFFSET,monitor.address);
    }

    @Nullable
    public ObjectMonitor getWaitingMonitor(){
        long addr=unsafe.getAddress(this.address+WAITING_MONITOR_OFFSET);
        if (addr==0L){
            return null;
        }
        return ObjectMonitor.getOrCreate(addr);
    }

    public void setWaitingMonitor(ObjectMonitor monitor){
        unsafe.putAddress(this.address+WAITING_MONITOR_OFFSET,monitor.address);
    }

    public JavaThreadState getState(){
        return JavaThreadState.of(unsafe.getInt(this.address+STATE_OFFSET));
    }

    public void setState(JavaThreadState state){
        unsafe.putInt(this.address+STATE_OFFSET,state.val);
    }
    private static ThreadsList list;
    public static ArrayList<JavaThread> getAllJavaThreads() {
        long addr = unsafe.getAddress(JAVA_THREAD_LIST);
        if (!isEqual(list, addr)) {
            list = new ThreadsList(addr);
        }
        return list.getAllThreads();
    }

    @Override
    public String toString() {
        return "JavaThread@0x"+Long.toHexString(this.address);
    }
}
