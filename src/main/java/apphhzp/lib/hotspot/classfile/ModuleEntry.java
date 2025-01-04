package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oop.OopDesc;
import apphhzp.lib.hotspot.oop.Symbol;
import apphhzp.lib.hotspot.utilities.HashtableEntry;

import static apphhzp.lib.ClassHelper.unsafe;

public class ModuleEntry extends HashtableEntry {
    public static final long MODULE_OFFSET=HashtableEntry.LITERAL_OFFSET+ JVM.oopSize;
    public static final long SHARED_PD_OFFSET=MODULE_OFFSET+JVM.oopSize;
    public static final long LOADER_DATA_OFFSET=SHARED_PD_OFFSET+JVM.oopSize;
    public static final long READS_OFFSET=LOADER_DATA_OFFSET+JVM.oopSize;
    public static final long VERSION_OFFSET=READS_OFFSET+JVM.oopSize;
    public static final long LOCATION_OFFSET=VERSION_OFFSET+JVM.oopSize;
    public static final long SHARED_PATH_INDEX_OFFSET=LOCATION_OFFSET+JVM.oopSize;
    public static final long CAN_READ_ALL_UNNAMED_OFFSET=SHARED_PATH_INDEX_OFFSET+(JVM.includeCDS?JVM.intSize:0);
    public static final long HAS_DEFAULT_READ_EDGES_OFFSET=CAN_READ_ALL_UNNAMED_OFFSET+1;
    public static final long MUST_WALK_READS_OFFSET=HAS_DEFAULT_READ_EDGES_OFFSET+1;
    public static final long IS_OPEN_OFFSET=MUST_WALK_READS_OFFSET+1;
    public static final long IS_PATCHED_OFFSET=IS_OPEN_OFFSET+1;
    public static final long ARCHIVED_MODULE_INDEX_OFFSET=(IS_PATCHED_OFFSET+JVM.intSize)/JVM.intSize*JVM.intSize;
    public ModuleEntry(long addr) {
        super(addr);
    }

    public Symbol getName(){
        return Symbol.of(this.getLiteralPointer());
    }

    public void setName(Symbol symbol){
        this.setLiteralPointer(symbol.address);
    }

    public Module getModule(){
        long addr= OopDesc.fromOopHandle(this.address+MODULE_OFFSET);
        return addr==0L?null:new OopDesc(addr).getObject();
    }

    public OopDesc getModuleOop(){
        long addr= OopDesc.fromOopHandle(this.address+MODULE_OFFSET);
        return addr==0L?null:new OopDesc(addr);
    }
    public void setModule(Module module){
        unsafe.putAddress(unsafe.getAddress(this.address+MODULE_OFFSET),new OopDesc(module).address);
    }

    public int getArchivedModuleIndex(){
        return unsafe.getInt(this.address+ARCHIVED_MODULE_INDEX_OFFSET);
    }

    public void setArchivedModuleIndex(int index){
        unsafe.putInt(this.address+ARCHIVED_MODULE_INDEX_OFFSET, index);
    }
}
