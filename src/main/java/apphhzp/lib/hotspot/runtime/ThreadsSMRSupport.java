package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class ThreadsSMRSupport {
    public static final Type TYPE= JVM.type("ThreadsSMRSupport");
    public static final long java_thread_list_address=TYPE.global("_java_thread_list");

    private static ThreadsList javaThreadListCache;
    public static ThreadsList javaThreadList() {
        long addr= unsafe.getAddress(java_thread_list_address);
        if (!JVMObject.isEqual(javaThreadListCache,addr)){
            javaThreadListCache= new ThreadsList(addr);
        }
        return javaThreadListCache;
    }
}
