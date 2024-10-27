package apphhzp.lib.hotspot.runtime;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import apphhzp.lib.helfy.JVM;

public enum JavaThreadState {
    UNINITIALIZED("_thread_uninitialized"),
    NEW("_thread_new"),
    NEW_TRANS("_thread_new_trans"),
    IN_NATIVE("_thread_in_native"),
    IN_NATIVE_TRANS("_thread_in_native_trans"),
    IN_VM("_thread_in_vm"),
    IN_VM_TRANS("_thread_in_vm_trans"),
    IN_JAVA("_thread_in_Java"),
    IN_JAVA_TRANS("_thread_in_Java_trans"),
    BLOCKED("_thread_blocked"),
    BLOCKED_TRANS("_thread_blocked_trans");

    public final int val;
    public final String desc;
    private static final Int2ObjectMap<JavaThreadState> map=new Int2ObjectOpenHashMap<>();

    JavaThreadState(String s) {
        val = JVM.intConstant(s);
        desc = s;
    }

    public static JavaThreadState of(int v){
        JavaThreadState re=map.get(v);
        if (re==null){
            throw new IllegalArgumentException("Wrong JavaThreadState code:"+v);
        }
        return re;
    }

    static {
        map.put(UNINITIALIZED.val, UNINITIALIZED);
        map.put(NEW.val, NEW);
        map.put(NEW_TRANS.val, NEW_TRANS);
        map.put(IN_NATIVE.val, IN_NATIVE);
        map.put(IN_NATIVE_TRANS.val, IN_NATIVE_TRANS);
        map.put(IN_VM.val, IN_VM);
        map.put(IN_VM_TRANS.val, IN_VM_TRANS);
        map.put(IN_JAVA.val, IN_JAVA);
        map.put(IN_JAVA_TRANS.val, IN_JAVA_TRANS);
        map.put(BLOCKED.val, BLOCKED);
        map.put(BLOCKED_TRANS.val, BLOCKED_TRANS);
    }
}
