package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.hotspot.oop.ClassLoaderData;
import apphhzp.lib.hotspot.oop.OopDesc;
import apphhzp.lib.hotspot.stream.AllFieldStream;

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
        public static ClassLoaderData loader_data_acquire(OopDesc loader) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            return ClassLoaderData.getOrCreate(ClassHelper.unsafe.getLong(loader.getObject(),loader_data_offset));//HeapAccess<MO_ACQUIRE>::load_at(loader, _loader_data_offset);
        }

        public static ClassLoaderData loader_data_raw(OopDesc loader) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            return ClassLoaderData.getOrCreate(ClassHelper.unsafe.getLong(loader.getObject(),loader_data_offset));//RawAccess<>::load_at(loader, _loader_data_offset);
        }

        public static void release_set_loader_data(OopDesc loader, ClassLoaderData new_data) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            ClassHelper.unsafe.putLong(loader.getObject(),loader_data_offset, new_data.address);
            //HeapAccess<MO_RELEASE>::store_at(loader, _loader_data_offset, new_data);
        }
    }

    public static class String{

    }
}
