package apphhzp.lib.hotspot.memory;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.gc.CollectedHeap;
import apphhzp.lib.hotspot.gc.g1.G1CollectedHeap;
import apphhzp.lib.hotspot.utilities.BasicType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class Universe {
    public static final Type TYPE= JVM.type("Universe");
    public static final long COLLECTED_HEAP_ADDRESS=TYPE.global("_collectedHeap");
    private static CollectedHeap collectedHeapCache;
    private Universe() {}

    public static CollectedHeap getCollectedHeap(){
        long addr=unsafe.getAddress(COLLECTED_HEAP_ADDRESS);
        if (!JVMObject.isEqual(collectedHeapCache,addr)){
            Type type=JVM.findDynamicTypeForAddress(addr,CollectedHeap.TYPE);
            if (type==CollectedHeap.TYPE){
                collectedHeapCache=new CollectedHeap(addr);
            }else if (type== G1CollectedHeap.TYPE){
                collectedHeapCache=new G1CollectedHeap(addr);
            }else{

            }
        }
        return collectedHeapCache;
    }

    public static boolean elementTypeShouldBeAligned(int type) {
        return type == BasicType.T_DOUBLE || type == BasicType.T_LONG;
    }

    public static boolean fieldTypeShouldBeAligned(int type) {
        return type == BasicType.T_DOUBLE || type == BasicType.T_LONG;
    }
}
