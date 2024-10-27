package apphhzp.lib.hotspot.memory;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

import static apphhzp.lib.ClassHelper.unsafe;

public class Universe {
    public static final Type TYPE= JVM.type("Universe");
    public static final long COLLECTED_HEAP_ADDRESS=TYPE.global("_collectedHeap");
    private Universe() {}

    public long getCollectedHeap(){
        return unsafe.getAddress(COLLECTED_HEAP_ADDRESS);
    }
}
