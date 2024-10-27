package apphhzp.lib.api;

public interface ObjectInstrumentation{
    void handleVMObjectAllocEvent(Thread thread,Object obj,Class<?> objClass,long size);
    void handleObjectFreeEvent(long tag);
    void handleSampledObjectAllocEvent(Thread thread,Object obj,Class<?> objClass,long size);
    default boolean hasHookSupport(){
        return this.canHookObjectFreeEvents()||this.canHookVMObjectAllocEvents()||this.canHookSampledObjectAllocEvents();
    }
    boolean canHookVMObjectAllocEvents();
    boolean canHookObjectFreeEvents();
    boolean canHookSampledObjectAllocEvents();
    void addMonitor(ObjectMemoryMonitor transformer);
    boolean removeMonitor(ObjectMemoryMonitor transformer);
    void setHeapSamplingInterval(int samplingInterval);
}
