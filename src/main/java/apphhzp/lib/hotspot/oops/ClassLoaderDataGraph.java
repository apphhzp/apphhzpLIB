package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.oops.oop.OopDesc;

import java.util.ConcurrentModificationException;

import static apphhzp.lib.ClassHelper.unsafe;

public final class ClassLoaderDataGraph {
    public static final Type TYPE= JVM.type("ClassLoaderDataGraph");
    public static final long HEAD_ADDRESS=TYPE.global("_head");
    public static final long UNLOADING_ADDRESS=HEAD_ADDRESS+JVM.oopSize;
    public static final long SHOULD_CLEAN_DEALLOCATE_LISTS_ADDRESS=UNLOADING_ADDRESS+JVM.oopSize;
    public static final long SAFEPOINT_CLEANUP_NEEDED_ADDRESS=SHOULD_CLEAN_DEALLOCATE_LISTS_ADDRESS+1;
    public static final long METASPACE_OOM_ADDRESS=SAFEPOINT_CLEANUP_NEEDED_ADDRESS+1;
    public static final long NUM_INSTANCE_CLASSES_ADDRESS=JVM.computeOffset(JVM.size_tSize,METASPACE_OOM_ADDRESS+1-HEAD_ADDRESS)+HEAD_ADDRESS;
    public static final long NUM_ARRAY_CLASSES_ADDRESS=NUM_INSTANCE_CLASSES_ADDRESS+JVM.size_tSize;
    private static ClassLoaderData headCache;
    private ClassLoaderDataGraph(){}
    public static ClassLoaderData getHead(){
        long addr= unsafe.getAddress(HEAD_ADDRESS);
        if (!JVMObject.isEqual(headCache,addr)){
            headCache=ClassLoaderData.getOrCreate(addr);
        }
        return headCache;
    }

    public static void setHead(ClassLoaderData head){
        unsafe.putAddress(HEAD_ADDRESS,head.address);
    }

    public static long getInstanceClassesNumber(){
        return JVM.getSizeT(NUM_INSTANCE_CLASSES_ADDRESS);
    }

    public static void setInstanceClassesNumber(long num){
        JVM.putSizeT(NUM_INSTANCE_CLASSES_ADDRESS, num);
    }

    public static long getArrayClassesNumber(){
        return JVM.getSizeT(NUM_ARRAY_CLASSES_ADDRESS);
    }

    public static void setArrayClassesNumber(long num){
        JVM.putSizeT(NUM_ARRAY_CLASSES_ADDRESS, num);
    }

    public static void incInstanceClasses(long count) {
        if (JVM.size_tSize==4){
            unsafe.getAndAddInt(null,NUM_INSTANCE_CLASSES_ADDRESS,(int)(count&0xffffffffL));
        }else {
            unsafe.getAndAddLong(null,NUM_INSTANCE_CLASSES_ADDRESS, count);
        }
    }

    public static void decInstanceClasses(long count) {
        long old_count;
        if (JVM.size_tSize==4){
            old_count=unsafe.getAndAddInt(null,NUM_INSTANCE_CLASSES_ADDRESS,(int)-(count&0xffffffffL));
        }else {
            old_count=unsafe.getAndAddLong(null,NUM_INSTANCE_CLASSES_ADDRESS,-count);
        }
        if (old_count<count){
            throw new ConcurrentModificationException("Sanity");
        }
    }

    public static void incArrayClasses(long count) {
        if (JVM.size_tSize==4){
            unsafe.getAndAddInt(null,NUM_ARRAY_CLASSES_ADDRESS,(int)(count&0xffffffffL));
        }else {
            unsafe.getAndAddLong(null,NUM_ARRAY_CLASSES_ADDRESS, count);
        }
    }

    public static void decArrayClasses(long count) {
        long old_count;
        if (JVM.size_tSize==4){
            old_count=unsafe.getAndAddInt(null,NUM_ARRAY_CLASSES_ADDRESS,(int)-(count&0xffffffffL));
        }else {
            old_count=unsafe.getAndAddLong(null,NUM_ARRAY_CLASSES_ADDRESS,-count);
        }
        if (old_count<count){
            throw new ConcurrentModificationException("Sanity");
        }
    }

    public static ClassLoaderData add_to_graph(OopDesc loader, boolean has_class_mirror_holder) {
        //throw new UnsupportedOperationException("Not supported yet.");
        ClassLoaderData cld;
        // First check if another thread beat us to creating the CLD and installing
        // it into the loader while we were waiting for the lock.
        if (!has_class_mirror_holder && loader.address!=0L) {
            cld = JavaClasses.ClassLoader.loader_data_acquire(loader);
            if (cld != null) {
                return cld;
            }
        }
        throw new UnsupportedOperationException("Not supported yet.");
//        cld = new ClassLoaderData(loader, has_class_mirror_holder);
//        // First install the new CLD to the Graph.
//        cld->set_next(_head);
//        cld.setNextCLD();
//        Atomic::release_store(&_head, cld);
//        if (!has_class_mirror_holder) {
//            java_lang_ClassLoader::release_set_loader_data(loader, cld);
//        }
//        return cld;
    }

    public static ClassLoaderData find_or_create(OopDesc loader) {
        if (loader.address == 0){
            throw new IllegalArgumentException("Loader is null");
        }
        ClassLoaderData loader_data = JavaClasses.ClassLoader.loader_data_acquire(loader);
        if (loader_data!=null) {
            return loader_data;
        }
        return add(loader, false);
    }

    public static ClassLoaderData add(OopDesc loader, boolean has_class_mirror_holder) {
        return add_to_graph(loader, has_class_mirror_holder);
    }


}
