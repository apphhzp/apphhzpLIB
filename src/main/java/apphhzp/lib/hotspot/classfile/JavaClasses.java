package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.stream.AllFieldStream;
import apphhzp.lib.hotspot.stream.InternalFieldStream;

import javax.annotation.Nullable;
import java.lang.invoke.VarHandle;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class JavaClasses {
    public static class ClassLoader{
        public static final long loader_data_offset;
        static {
            int ld_offset =-1;
            for (AllFieldStream stream = new AllFieldStream(VMClasses.classLoaderKlass()); !stream.done(); stream.next()){
                if (stream.field().isInternal()){
                    if(stream.getName().toString().equals("loader_data")){
                        ld_offset =stream.getOffset();
                    }
                }
            }
            if (ld_offset ==-1){
                throw new RuntimeException("Could not get loader_data_offset");
            }
            loader_data_offset= ld_offset;
        }

        public static long getPointerFrom(java.lang.ClassLoader loader){
            return JVM.oopSize==8?unsafe.getLong(loader, loader_data_offset): unsafe.getInt(loader, loader_data_offset);
        }

        public static ClassLoaderData loader_data_acquire(OopDesc loader) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            return ClassLoaderData.getOrCreate(unsafe.getLong(loader.getObject(),loader_data_offset));//HeapAccess<MO_ACQUIRE>::load_at(loader, _loader_data_offset);
        }

        public static ClassLoaderData loader_data_raw(OopDesc loader) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            return ClassLoaderData.getOrCreate(unsafe.getLong(loader.getObject(),loader_data_offset));//RawAccess<>::load_at(loader, _loader_data_offset);
        }

        public static void release_set_loader_data(OopDesc loader, ClassLoaderData new_data) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            unsafe.putLong(loader.getObject(),loader_data_offset, new_data.address);
            //HeapAccess<MO_RELEASE>::store_at(loader, _loader_data_offset, new_data);
        }
    }

    public static class String{

    }

    public static class Class{
        public static final long oop_size_offset;
        static {
            int ld_offset =-1;
            for (AllFieldStream stream = new AllFieldStream(VMClasses.classLoaderKlass()); !stream.done(); stream.next()){
                if (stream.field().isInternal()){
                    if(stream.getName().toString().equals("oop_size")){
                        ld_offset =stream.getOffset();
                    }
                }
            }
            if (ld_offset ==-1){
                throw new RuntimeException("Could not get oop_size_offset");
            }
            oop_size_offset= ld_offset;
        }

        public static int getOopSizeRaw(java.lang.Class<?> oop){
            return unsafe.getInt(oop,oop_size_offset);
        }
    }

    public static class Thread{
        public static final VarHandle eetopGetter;
        static {
            try {
                eetopGetter= ClassHelperSpecial.lookup.findVarHandle(java.lang.Thread.class,"eetop",long.class);
            } catch (Throwable throwable){
                throw new RuntimeException(throwable);
            }
        }

        public static JavaThread thread(java.lang.Thread thread){
            Long val= (Long) eetopGetter.get(thread);
            if (val==0L){
                return null;
            }
            return JavaThread.getOrCreate(val );
        }

        public static void setThread(java.lang.Thread thread,@Nullable JavaThread newThread){
            eetopGetter.set(thread,newThread==null?0L:newThread.address);
        }
    }

    public static class StackFrameInfo{
        public static final java.lang.Class<?> clazz;
        public static final long version_offset;
        static {
            try {
                clazz=java.lang.Class.forName("java.lang.StackFrameInfo");
                long offset=-1;
                for (InternalFieldStream fieldStream=new InternalFieldStream(Klass.asKlass(clazz).asInstanceKlass()); !fieldStream.done(); fieldStream.next()){
                    if (fieldStream.getName().toString().equals("version")){
                        offset=fieldStream.getOffset();
                    }
                }
                if (offset==-1){
                    throw new RuntimeException("Could not get _version_offset!");
                }
                version_offset= offset;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public static short getVersion(Object object){
            if (!object.getClass().isAssignableFrom(clazz)){
                throw new IllegalArgumentException();
            }
            return unsafe.getShort(object,version_offset);
        }

        public static void setVersion(Object object,short version){
            if (!object.getClass().isAssignableFrom(clazz)){
                throw new IllegalArgumentException();
            }
            unsafe.putShort(object,version_offset,version);
        }
    }
}
