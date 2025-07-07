package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.utilities.VMTypeGrowableArray;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class CodeCache {
    public static final Type TYPE = JVM.type("CodeCache");
    public static final long LOW_BOUND_ADDRESS = TYPE.global("_low_bound");
    public static final long HIGH_BOUND_ADDRESS = TYPE.global("_high_bound");
    public static final long HEAPS_ADDRESS = TYPE.global("_heaps");
    public static final long COMPILED_HEAPS_ADDRESS=HEAPS_ADDRESS+JVM.oopSize;
    private static VMTypeGrowableArray<CodeHeap> heapsCache;
    private static VMTypeGrowableArray<CodeHeap> compiledHeapsCache;

    public static long getLowBound() {
        return unsafe.getAddress(LOW_BOUND_ADDRESS);
    }

    public static void setLowBound(long address) {
        unsafe.putAddress(LOW_BOUND_ADDRESS, address);
    }

    public static long getHighBound() {
        return unsafe.getAddress(HIGH_BOUND_ADDRESS);
    }

    public static void setHighBound(long address) {
        unsafe.putAddress(HIGH_BOUND_ADDRESS, address);
    }

    public static VMTypeGrowableArray<CodeHeap> getHeaps() {
        long addr = unsafe.getAddress(HEAPS_ADDRESS);
        if (!JVMObject.isEqual(heapsCache, addr)) {
            heapsCache = new VMTypeGrowableArray<>(addr, CodeHeap::new);
        }
        return heapsCache;
    }

    public static void setHeaps(VMTypeGrowableArray<CodeHeap> heaps) {
        unsafe.putAddress(HEAPS_ADDRESS, heaps.address);
    }

    public static VMTypeGrowableArray<CodeHeap> getCompiledHeaps() {
        long addr = unsafe.getAddress(COMPILED_HEAPS_ADDRESS);
        if (!JVMObject.isEqual(compiledHeapsCache, addr)) {
            compiledHeapsCache = new VMTypeGrowableArray<>(addr, CodeHeap::new);
        }
        return compiledHeapsCache;
    }

    public static void setCompiledHeaps(VMTypeGrowableArray<CodeHeap> heaps) {
        unsafe.putAddress(COMPILED_HEAPS_ADDRESS,heaps.address);
    }

    @Nullable
    public static CodeBlob findBlob(long addr){
        CodeBlob re=findBlobUnsafe(addr);
        if (re==null||re.isZombie()||re.isLockedByVM()){
            return null;
        }
        return re;
    }

    @Nullable
    public static CodeBlob findBlobUnsafe(long addr){
        CodeHeap heap=null;
        for (CodeHeap codeHeap:getHeaps()){
            if (codeHeap.contains(addr)){
                heap=codeHeap;
                break;
            }
        }
        if (heap==null){
            return null;
        }
        long start=heap.findStart(addr);
        if (start==0L){
            return null;
        }
        return CodeBlob.getCodeBlob(start);
    }

//    private static final Function find_nmethod_function=Function.getFunction(new Pointer(JVM.lookupSymbol("findnm")));
//    public static NMethod findNMethod(long addr){
//        return new NMethod(Pointer.nativeValue(find_nmethod_function.invokePointer(new Object[]{addr})));
//    }

    public static void markAllNMethodsForEvolDeoptimization() {
        if (JVM.includeJVMTI) {
            for (CodeHeap heap:getCompiledHeaps()) {
                for (CodeBlob blob : heap) {
                    if (blob instanceof CompiledMethod compiledMethod) {
                        if (!compiledMethod.getMethod().isMethodHandleIntrinsic()) {
                            System.err.println(compiledMethod.getMethod().getConstMethod().getName());
                            compiledMethod.markForDeoptimization(true);
//                            if (nm -> has_evol_metadata()) {
//                                add_to_old_table(nm);
//                            }
                        }
                    }
                }
            }
        }
    }

    /*
    https://github.com/openjdk/jdk17u/blob/master/src/hotspot/share/code/codeCache.hpp#L275
    static void mark_for_evol_deoptimization(InstanceKlass* dependee);
    static int  mark_dependents_for_evol_deoptimization();
    static void mark_all_nmethods_for_evol_deoptimization();
    //Flushes compiled methods dependent on redefined classes, that have already been marked for deoptimization.
    static void flush_evol_dependents();
    static void old_nmethods_do(MetadataClosure* f) NOT_JVMTI_RETURN;
    static void unregister_old_nmethod(CompiledMethod* c) NOT_JVMTI_RETURN;
    * */

    public static void markAllNMethodsForDeoptimization() {

        for (CodeHeap heap : getCompiledHeaps()) {
            for (CodeBlob blob : heap) {
                if (blob instanceof CompiledMethod compiledMethod) {
                    if (!compiledMethod.getMethod().getAccessFlags().isNative()) {
                        compiledMethod.markForDeoptimization(true);
                    }
                }
            }
        }
    }

    public static void makeMarkedNMethodsNotEntrant(){
        for (CodeHeap heap:getCompiledHeaps()) {//DeoptimizeMarkedClosure
            for (CodeBlob blob : heap) {
                if (blob instanceof CompiledMethod compiledMethod) {
                    if (!compiledMethod.isMarkedForDeoptimization()) {
                        compiledMethod.makeNotEntrant();
                    }
                }
            }
        }
    }
}
