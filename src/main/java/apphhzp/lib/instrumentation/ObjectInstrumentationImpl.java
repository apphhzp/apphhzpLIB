package apphhzp.lib.instrumentation;

import apphhzp.lib.api.ObjectInstrumentation;
import apphhzp.lib.api.ObjectMemoryMonitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class ObjectInstrumentationImpl implements ObjectInstrumentation {
    private static final Logger LOGGER= LogManager.getLogger(ObjectInstrumentationImpl.class);
    private final long nativeAgent;
    private ObjectMemoryMonitor[] transformers;
    public ObjectInstrumentationImpl(long nativeAgent){
        this.nativeAgent=nativeAgent;
        this.transformers=new ObjectMemoryMonitor[0];
    }

    @Override
    public void handleVMObjectAllocEvent(Thread thread, Object obj, Class<?> objClass, long size) {
        for (ObjectMemoryMonitor transformer:this.transformers){
            try{
                transformer.onVMObjectAlloc(thread,obj,objClass,size);
            }catch (Throwable t){
                LOGGER.throwing(t);
            }
        }
    }

    @Override
    public void handleObjectFreeEvent(long tag) {
        for (ObjectMemoryMonitor transformer:this.transformers){
            try{
                transformer.onObjectFree(tag);
            }catch (Throwable t){
                LOGGER.throwing(t);
            }
        }
    }

    @Override
    public void handleSampledObjectAllocEvent(Thread thread, Object obj, Class<?> objClass, long size) {
        for (ObjectMemoryMonitor transformer:this.transformers){
            try{
                transformer.onSampledObjectAlloc(thread,obj,objClass,size);
            }catch (Throwable t){
                LOGGER.throwing(t);
            }
        }
    }

    @Override
    public boolean canHookVMObjectAllocEvents() {
        return this.canHookVMObjectAllocEvents0(nativeAgent);
    }

    @Override
    public boolean canHookObjectFreeEvents() {
        return this.canHookObjectFreeEvents0(this.nativeAgent);
    }

    @Override
    public boolean canHookSampledObjectAllocEvents() {
        return this.canHookSampledObjectAllocEvents0(this.nativeAgent);
    }

    @Override
    public void addMonitor(ObjectMemoryMonitor monitor) {
        if (monitor == null) {
            throw new NullPointerException("null passed as 'monitor' in addMonitor");
        }
        if (this.hasHookSupport()){
            if (this.transformers.length==0) {
                setHasMonitors(this.nativeAgent, true);
            }
            ObjectMemoryMonitor[] oldList = this.transformers;
            ObjectMemoryMonitor[] newList = new ObjectMemoryMonitor[oldList.length + 1];
            System.arraycopy(oldList,
                    0,
                    newList,
                    0,
                    oldList.length);
            newList[oldList.length] = monitor;
            this.transformers = newList;
        }else {
            throw new UnsupportedOperationException("Adding object monitors is not supported in this environment.");
        }
    }

    @Override
    public boolean removeMonitor(ObjectMemoryMonitor monitor) {
        if (monitor == null) {
            throw new NullPointerException("null passed as 'monitor' in removeMonitor");
        }
        boolean found = false;
        ObjectMemoryMonitor[] oldList = this.transformers;
        int oldLength = oldList.length;
        int newLength = oldLength - 1;
        int matchingIndex = 0;
        for (int x = oldLength - 1; x >= 0; x--) {
            if (oldList[x] == monitor) {
                found = true;
                matchingIndex = x;
                break;
            }
        }
        if (found) {
            ObjectMemoryMonitor[] newList = new ObjectMemoryMonitor[newLength];
            if (matchingIndex > 0) {
                System.arraycopy(oldList,
                        0,
                        newList,
                        0,
                        matchingIndex);
            }
            if (matchingIndex < (newLength)) {
                System.arraycopy(oldList,
                        matchingIndex + 1,
                        newList,
                        matchingIndex,
                        (newLength) - matchingIndex);
            }
            this.transformers = newList;
        }
        if (this.transformers.length==0){
            this.setHasMonitors(this.nativeAgent,false);
        }
        return found;
    }

    @Override
    public void setHeapSamplingInterval(int samplingInterval) {
        if (!this.canHookSampledObjectAllocEvents()){
            throw new UnsupportedOperationException("The environment does not possess the capability.");
        }
        if (samplingInterval<0){
            throw new UnsupportedOperationException("samplingInterval is less than zero.");
        }
        this.setHeapSamplingInterval0(this.nativeAgent,samplingInterval);
    }

    private native void setHeapSamplingInterval0(long pointer,int val);

    private native boolean canHookVMObjectAllocEvents0(long pointer);

    private native boolean canHookObjectFreeEvents0(long pointer);

    private native boolean canHookSampledObjectAllocEvents0(long pointer);
    private native boolean setHasMonitors(long pointer,boolean has);
}
