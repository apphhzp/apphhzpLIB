package apphhzp.lib.hotspot.memory;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class MetaspaceObj {
    public static final Type TYPE= JVM.type("MetaspaceObj");
    public static final long shared_metaspace_base_address=TYPE.global("_shared_metaspace_base");
    public static final long shared_metaspace_top_address=TYPE.global("_shared_metaspace_top");
    public static boolean isShared(long addr) {
        return unsafe.getAddress(shared_metaspace_base_address)<= addr && addr< unsafe.getAddress(shared_metaspace_top_address);
    }
}
