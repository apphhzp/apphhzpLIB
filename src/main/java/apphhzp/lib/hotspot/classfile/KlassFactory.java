package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.prims.JvmtiCachedClassFileData;

public class KlassFactory {
    public InstanceKlass create_from_stream(ClassFileStream stream,
                                            Symbol name,
                                            ClassLoaderData loader_data,ClassLoadInfo cl_info){
        if (stream==null||loader_data==null){
            throw new IllegalArgumentException("invariant");
        }
        JvmtiCachedClassFileData cached_class_file = null;
//
//        // increment counter
//        THREAD->statistical_info().incr_define_class_count();
//
//        ClassFileParser parser(stream,
//                name,
//                loader_data,
//                         &cl_info,
//                ClassFileParser::BROADCAST);
//
//        ClassInstanceInfo cl_inst_info = cl_info.class_hidden_info_ptr();
//        InstanceKlass result = parser.create_instance_klass(false,cl_inst_info);
//        if (result==null){
//            throw new RuntimeException("result cannot be null with no pending exception");
//        }
//        if (cached_class_file != null) {
//            // JVMTI: we have an InstanceKlass now, tell it about the cached bytes
//            result->set_cached_class_file(cached_class_file);
//        }

        return null;//result;
    }
}
