package apphhzp.lib.hotspot.cds;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oop.InstanceKlass;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.method.Method;

import static apphhzp.lib.ClassHelper.unsafe;

public class FileMapHeader extends JVMObject {
    public static final Type TYPE= JVM.type("FileMapHeader");
    public static final int SIZE=TYPE.size;
    public static final long SPACE_OFFSET=TYPE.offset("_space[0]");
    public static final long CLONED_VTABLES_OFFSET_OFFSET= TYPE.offset("_cloned_vtables_offset");
    public static final long MAPPED_BASE_ADDRESS_OFFSET=TYPE.offset("_mapped_base_address");
    public final long rwRegionBaseAddress;
    public final long rwRegionEndAddress;
    private final CDSFileMapRegion[] cdsFileMapRegions;
    public final long vtablesIndex;
    private final Long2ObjectMap<Type> vtbl2TypeMap;
//    public static final int NUM_CDS_REGIONS=unsafe.getInt(JVM.getSymbol("NUM_CDS_REGIONS"));
    public FileMapHeader(long addr) {
        super(addr);
        this.cdsFileMapRegions=new CDSFileMapRegion[7];
        for (int i=0;i<7;i++){
            this.cdsFileMapRegions[i]=new CDSFileMapRegion(this.address+SPACE_OFFSET+ (long) i *JVM.oopSize);
        }
        CDSFileMapRegion rwSpace=this.cdsFileMapRegions[0];
        this.rwRegionBaseAddress=rwSpace.getMappedBase();
        this.rwRegionEndAddress=rwRegionBaseAddress+rwSpace.getUsed();
        this.vtablesIndex=this.getMappedBaseAddress()+this.getClonedVtablesOffset();
        this.vtbl2TypeMap=new Long2ObjectOpenHashMap<>();
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex)+JVM.oopSize, ConstantPool.TYPE);
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex+JVM.oopSize)+JVM.oopSize, InstanceKlass.TYPE);
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex+JVM.oopSize*2L)+JVM.oopSize, JVM.type("InstanceClassLoaderKlass"));
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex+JVM.oopSize*3L)+JVM.oopSize, JVM.type("InstanceMirrorKlass"));
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex+JVM.oopSize*4L)+JVM.oopSize, JVM.type("InstanceRefKlass"));
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex+JVM.oopSize*5L)+JVM.oopSize, Method.TYPE);
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex+JVM.oopSize*6L)+JVM.oopSize, JVM.type("ObjArrayKlass"));
        vtbl2TypeMap.put(unsafe.getAddress(this.vtablesIndex+JVM.oopSize*7L)+JVM.oopSize, JVM.type("TypeArrayKlass"));
    }

    public CDSFileMapRegion getSpace(int index){
        /*
        #define NUM_CDS_REGIONS 7 // this must be the same as MetaspaceShared::n_regions
        */
        if (index<0||index>=7){
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return this.cdsFileMapRegions[index];
    }

    public long getClonedVtablesOffset(){
        return unsafe.getAddress(this.address+CLONED_VTABLES_OFFSET_OFFSET);
    }

    public long getMappedBaseAddress(){
        return unsafe.getAddress(this.address+MAPPED_BASE_ADDRESS_OFFSET);
    }

    public boolean inCopiedVtableSpace(long vptr) {
        if (vptr == 0L) {
            return false;
        }
        return vptr > (rwRegionBaseAddress) && vptr <= (rwRegionEndAddress);
    }

    public Type getTypeForVptrAddress(long addr) {
        return this.vtbl2TypeMap.get(addr);
    }
}
