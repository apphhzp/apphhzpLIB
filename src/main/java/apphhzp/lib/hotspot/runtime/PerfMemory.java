package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class PerfMemory {
    public static final Type TYPE= JVM.type("PerfMemory");
    public static final long START_ADDRESS=TYPE.global("_start");
    public static final long END_ADDRESS=TYPE.global("_end");
    public static final long TOP_ADDRESS=TYPE.global("_top");
    public static final long CAPACITY_ADDRESS=TYPE.global("_capacity");
    public static final long PROLOGUE_ADDRESS=TYPE.global("_prologue");
    public static final long INITIALIZED_ADDRESS=TYPE.global("_initialized");
    public static final int PERFDATA_MAJOR_VERSION=JVM.intConstant("PERFDATA_MAJOR_VERSION");
    public static final int PERFDATA_MINOR_VERSION=JVM.intConstant("PERFDATA_MINOR_VERSION");
    public static final int PERFDATA_BIG_ENDIAN=JVM.intConstant("PERFDATA_BIG_ENDIAN");
    public static final int PERFDATA_LITTLE_ENDIAN=JVM.intConstant("PERFDATA_LITTLE_ENDIAN");
    private static PerfDataPrologue cache;
    public static long start(){
        return unsafe.getAddress(START_ADDRESS);
    }

    public static long end(){
        return unsafe.getAddress(END_ADDRESS);
    }

    public static long used() { return  (top() - start()); }

    public static long top(){
        return unsafe.getAddress(TOP_ADDRESS);
    }

    public static long capacity(){
        return JVM.getSizeT(CAPACITY_ADDRESS);
    }

    public static PerfDataPrologue prologue(){
        long addr=unsafe.getAddress(PROLOGUE_ADDRESS);
        if (addr==0){
            return null;
        }
        if (!JVMObject.isEqual(cache,addr)){
            cache=new PerfDataPrologue(addr);
        }
        return cache;
    }

    private static int initialized(){
        return unsafe.getInt(INITIALIZED_ADDRESS);
    }

    public static boolean isInitialized(){
        return initialized()!=0;
    }


    public static boolean contains(long addr) {
        long start=start();
        return ((start != 0L) && (addr >= start) && (addr < end()));
    }

    public static void setAccessible(boolean value) {
        if (JVM.usePerfData) {
            prologue().setAccessible(value);
        }
    }

    public interface PerfDataEntryVisitor {
        // returns false to stop the iteration
        boolean visit(PerfDataEntry pde);
    }

    public static void iterate(PerfDataEntryVisitor visitor) {
        PerfDataPrologue header = prologue();
        int off = header.getEntryOffset();
        int num = header.getNumEntries();
        long addr = header.address;

        for (int i = 0; i < num; i++) {
            PerfDataEntry pde = new PerfDataEntry(addr+(off));
            off += pde.entryLength();
            if (!visitor.visit(pde)) return;
        }
    }
}
