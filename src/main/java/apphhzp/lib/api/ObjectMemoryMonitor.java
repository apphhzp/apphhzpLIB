package apphhzp.lib.api;

public interface ObjectMemoryMonitor {
    default void onVMObjectAlloc(Thread thread, Object obj, Class<?> objClass, long size){}
    default void onObjectFree(long tag){}
    default void onSampledObjectAlloc(Thread thread, Object obj, Class<?> objClass, long size){}
}
