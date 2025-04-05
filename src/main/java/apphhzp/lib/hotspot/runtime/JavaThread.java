package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.oop.OopDesc;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

import static apphhzp.lib.ClassHelper.unsafe;

public class JavaThread extends Thread {
    public static final Type TYPE = JVM.type("JavaThread");
    public static final int SIZE = TYPE.size;
    //public static final long JAVA_THREAD_LIST = JVM.type("ThreadsSMRSupport").global("_java_thread_list");
    public static final long STACK_BASE_OFFSET = TYPE.offset("_stack_base");
    public static final long STACK_SIZE_OFFSET = TYPE.offset("_stack_size");
    public static final long THREAD_OBJ_OFFSET = TYPE.offset("_threadObj");
    public static final long ANCHOR_OFFSET = TYPE.offset("_anchor");
    public static final long JNI_ENVIRONMENT_OFFSET=JVM.includeJVMCI?TYPE.offset("_jni_environment"):ANCHOR_OFFSET+ 4L *JVM.oopSize;
    public static final long VM_RESULT_OFFSET = TYPE.offset("_vm_result");
    public static final long VM_RESULT2_OFFSET = TYPE.offset("_vm_result_2");
    public static final long PENDING_MONITOR_OFFSET=TYPE.offset("_current_pending_monitor");
    public static final long WAITING_MONITOR_OFFSET=TYPE.offset("_current_waiting_monitor");
    public static final long STATE_OFFSET=TYPE.offset("_thread_state");
    public static final long TERMINATED_OFFSET=TYPE.offset("_terminated");
    public static final long IN_DEOPT_HANDLER_OFFSET=JVM.computeOffset(4,TERMINATED_OFFSET+JVM.intSize);
    public static final long DOING_UNSAFE_ACCESS_OFFSET=JVM.includeJVMCI?TYPE.offset("_doing_unsafe_access"):JVM.computeOffset(1,IN_DEOPT_HANDLER_OFFSET+4);
    public static final long DO_NOT_UNLOCK_IF_SYNCHRONIZED_OFFSET=JVM.computeOffset(1,DOING_UNSAFE_ACCESS_OFFSET+1);
    public static final long JNI_ATTACH_STATE_OFFSET=JVM.computeOffset(JVM.intSize,DO_NOT_UNLOCK_IF_SYNCHRONIZED_OFFSET+1);
    public static final long PENDING_DEOPTIMIZATION_OFFSET=JVM.includeJVMCI?TYPE.offset("_pending_deoptimization"):-1;
    private static final HashMap<Long, JavaThread> CACHE = new HashMap<>();
    private OopDesc vmResultCache;
    private OopDesc threadObjCache;

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
    public java.lang.Thread getThreadObj() {
        long addr = OopDesc.fromOopHandle(this.address + THREAD_OBJ_OFFSET);
        if (!isEqual(this.threadObjCache, addr)) {
            this.threadObjCache = OopDesc.of(addr);
        }
        return this.threadObjCache.getObject();
    }

    public long getJNIEnv(){
        return this.address+JNI_ENVIRONMENT_OFFSET;
    }

    public OopDesc getVMResult(){
        long addr=unsafe.getAddress(this.address+VM_RESULT_OFFSET);
        if (!isEqual(this.vmResultCache,addr)){
            this.vmResultCache= OopDesc.of(addr);
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

    public int getJNIAttachState(){
        return unsafe.getInt(this.address+JNI_ATTACH_STATE_OFFSET);
    }

    public void setJNIAttachState(int state){
        unsafe.putInt(this.address+JNI_ATTACH_STATE_OFFSET,state);
    }

    public static ArrayList<JavaThread> getAllJavaThreads() {
        return ThreadsSMRSupport.javaThreadList().getAllThreads();
    }

    public static JavaThread first() {
        return ThreadsSMRSupport.javaThreadList().first();
    }

    @Override
    public String toString() {
        return "JavaThread@0x"+Long.toHexString(this.address);
    }
}
