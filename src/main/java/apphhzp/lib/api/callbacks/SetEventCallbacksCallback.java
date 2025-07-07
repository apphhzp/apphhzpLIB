package apphhzp.lib.api.callbacks;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public interface SetEventCallbacksCallback extends Callback{
    final class EventCallbacks{
        public static final int VMInit=0,
                VMDeath=1,
                ThreadStart=2,
                ThreadEnd=3,
                ClassFileLoadHook=4,
                ClassLoad=5,
                ClassPrepare=6,
                VMStart=7,
                Exception=8,
                ExceptionCatch=9,
                SingleStep=10,
                FramePop=11,
                Breakpoint=12,
                FieldAccess=13,
                FieldModification=14,
                MethodEntry=15,
                MethodExit=16,
                NativeMethodBind=17,
                CompiledMethodLoad=18,
                CompiledMethodUnload=19,
                DynamicCodeGenerated=20,
                DataDumpRequest=21,
                MonitorWait=23,
                MonitorWaited=24,
                MonitorContendedEnter=25,
                MonitorContendedEntered=26,
                ResourceExhausted=30,
                GarbageCollectionStart=31,
                GarbageCollectionFinish=32,
                ObjectFree=33,
                VMObjectAlloc=34,
                SampledObjectAlloc=36;

        private final long address;
        public final int size;
        public EventCallbacks(long addr, int size){
            this.address = addr;
            this.size = size;
        }
        public long getCallbackFunc(int index){
            if (index<0||index* unsafe.addressSize()>size){
                throw new IndexOutOfBoundsException(index);
            }
            return unsafe.getAddress(address+ (long) index * unsafe.addressSize());
        }
        public void setCallbackFunc(int index, long new_func){
            if (index<0||index* unsafe.addressSize()>size){
                throw new IndexOutOfBoundsException(index);
            }
            unsafe.putAddress(this.address+(long) index * unsafe.addressSize(), new_func);
        }
    }
    /**Returns a non-negative value to cancel function execution and return the value.*/
    default int callback(EventCallbacks callbacks){
        return -1;
    }
}
