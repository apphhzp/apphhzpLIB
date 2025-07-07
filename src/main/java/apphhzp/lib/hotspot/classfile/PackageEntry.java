package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.utilities.HashtableEntry;
import apphhzp.lib.hotspot.utilities.VMTypeGrowableArray;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class PackageEntry extends HashtableEntry {
    public static final int
            PKG_EXP_UNQUALIFIED = 0x0001,
            PKG_EXP_ALLUNNAMED  = 0x0002,
            PKG_EXP_UNQUALIFIED_OR_ALL_UNAMED =(PKG_EXP_UNQUALIFIED | PKG_EXP_ALLUNNAMED);
    public static final long MODULE_OFFSET= HashtableEntry.LITERAL_OFFSET+ JVM.oopSize;
    public static final long EXPORT_FLAGS_OFFSET=MODULE_OFFSET+JVM.oopSize;
    public static final long CLASSPATH_INDEX_OFFSET=EXPORT_FLAGS_OFFSET+JVM.intSize;
    public static final long MUST_WALK_EXPORTS_OFFSET=CLASSPATH_INDEX_OFFSET+2;
    public static final long QUALIFIED_EXPORTS_OFFSET=JVM.computeOffset(JVM.oopSize,MUST_WALK_EXPORTS_OFFSET+1);//EXPORT_FLAGS_OFFSET+JVM.oopSize>MUST_WALK_EXPORTS_OFFSET?EXPORT_FLAGS_OFFSET+JVM.oopSize:EXPORT_FLAGS_OFFSET+JVM.oopSize*2L;
    public static final long TRACE_ID_OFFSET=JVM.includeJFR?JVM.computeOffset(8,QUALIFIED_EXPORTS_OFFSET+JVM.oopSize):QUALIFIED_EXPORTS_OFFSET;
    public static final long DEFINED_BY_CDS_IN_CLASS_PATH_OFFSET=TRACE_ID_OFFSET+(JVM.includeJFR?JVM.oopSize:8);
    public static final long SIZE=JVM.computeOffset(8,DEFINED_BY_CDS_IN_CLASS_PATH_OFFSET+JVM.intSize);
    private ModuleEntry moduleCache;
    private VMTypeGrowableArray<ModuleEntry> qualifiedExportsCache;

    public static PackageEntry create(){
        long addr=unsafe.allocateMemory(SIZE);
        unsafe.setMemory(addr,SIZE, (byte) 0);
        return new PackageEntry(addr);
    }
    public PackageEntry(long addr) {
        super(addr);
    }

    public Symbol getName(){
        return Symbol.of(this.getLiteralPointer());
    }

    public void setName(Symbol symbol){
        this.setLiteralPointer(symbol.address);
    }

    @Nullable
    public PackageEntry getNext(){
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=new PackageEntry(addr);
        }
        return (PackageEntry) this.nextCache;
    }


    public ModuleEntry getModule(){
        long addr=unsafe.getAddress(this.address+MODULE_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.moduleCache,addr)){
            this.moduleCache=ModuleEntry.getOrCreate(addr);
        }
        return this.moduleCache;
    }

    public void setModule(ModuleEntry module){
        this.moduleCache=null;
        unsafe.putAddress(this.address+MODULE_OFFSET,module==null?0L:module.address);
    }

    public int getExportFlags(){
        return unsafe.getInt(this.address+EXPORT_FLAGS_OFFSET);
    }

    public void setExportFlags(int flags){
        unsafe.putInt(this.address+EXPORT_FLAGS_OFFSET,flags);
    }

    public int getClasspathIndex(){
        return unsafe.getShort(this.address+CLASSPATH_INDEX_OFFSET)&0xffff;
    }

    public void setClasspathIndex(int index){
        unsafe.putShort(this.address+CLASSPATH_INDEX_OFFSET,(short)(index&0xffff));
    }

    public boolean isMustWalkExports(){
        return unsafe.getByte(this.address+MUST_WALK_EXPORTS_OFFSET)!=0;
    }

    public void setMustWalkExports(boolean flag){
        unsafe.putByte(this.address+MUST_WALK_EXPORTS_OFFSET,(byte)(flag?1:0));
    }

    @Nullable
    public VMTypeGrowableArray<ModuleEntry> getQualifiedExports(){
        long addr=unsafe.getAddress(this.address+QUALIFIED_EXPORTS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.qualifiedExportsCache,addr)){
            this.qualifiedExportsCache=new VMTypeGrowableArray<>(addr,ModuleEntry::getOrCreate);
        }
        return this.qualifiedExportsCache;
    }

    public void setQualifiedExports(@Nullable VMTypeGrowableArray<ModuleEntry> qualifiedExports){
        this.qualifiedExportsCache=null;
        unsafe.putAddress(this.address+QUALIFIED_EXPORTS_OFFSET, qualifiedExports==null?0L:qualifiedExports.address);
    }

    public static int maxIndexForDefinedInClassPath() {
        return JVM.intSize * JVM.BitsPerByte;
    }

    public boolean isDefinedByCdsInClassPath(int idx){
        if (idx>=maxIndexForDefinedInClassPath()){
            throw new IllegalArgumentException("sanity");
        }
        return (unsafe.getInt(this.address+DEFINED_BY_CDS_IN_CLASS_PATH_OFFSET)& (1 << idx))!=0;
    }
    public void setDefinedByCdsInClassPath(int idx) {
        if (idx>=maxIndexForDefinedInClassPath()){
            throw new IllegalArgumentException("sanity");
        }
        int old_val;
        int new_val;
        do {
            old_val = unsafe.getInt(this.address+DEFINED_BY_CDS_IN_CLASS_PATH_OFFSET);
            new_val = old_val | (1 << idx);
        } while (!unsafe.compareAndSwapInt(null,this.address+DEFINED_BY_CDS_IN_CLASS_PATH_OFFSET,old_val,new_val));
    }

    public void init(){
        unsafe.setMemory(this.address,SIZE,(byte)0);
        this.setClasspathIndex(-1);
    }

    @Override
    public String toString() {
        return "PackageEntry@0x"+Long.toHexString(this.address);
    }
}
