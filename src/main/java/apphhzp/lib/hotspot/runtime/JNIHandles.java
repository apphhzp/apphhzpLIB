package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.gc.OopStorage;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class JNIHandles {
    public static final Type TYPE= JVM.type("JNIHandles");
    public static final long GLOBAL_HANDLES_ADDRESS= TYPE.global("_global_handles");
    public static final long WEAK_GLOBAL_HANDLES_ADDRESS=TYPE.global("_weak_global_handles");
    public static OopStorage global_handles(){
        return new OopStorage(unsafe.getAddress(GLOBAL_HANDLES_ADDRESS));
    }
    public static OopStorage weak_global_handles(){
        return new OopStorage(unsafe.getAddress(WEAK_GLOBAL_HANDLES_ADDRESS));
    }
}
