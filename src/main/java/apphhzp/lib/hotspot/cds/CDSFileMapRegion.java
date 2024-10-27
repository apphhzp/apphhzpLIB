package apphhzp.lib.hotspot.cds;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class CDSFileMapRegion extends JVMObject {
    public static final Type TYPE= JVM.type("CDSFileMapRegion");
    public static final int SIZE=TYPE.size;
    public static final long USED_OFFSET=TYPE.offset("_used");
    public static final long MAPPED_BASE_OFFSET=TYPE.offset("_mapped_base");
    public CDSFileMapRegion(long addr) {
        super(addr);
    }

    public long getUsed(){
        return unsafe.getAddress(this.address+USED_OFFSET);
    }

    public long getMappedBase(){
        return unsafe.getAddress(this.address+MAPPED_BASE_OFFSET);
    }
}
