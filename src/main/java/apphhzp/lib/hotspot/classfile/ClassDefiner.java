package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oop.*;

import java.security.ProtectionDomain;

public final class ClassDefiner {
    public static Class<?> jvm_define_class_common(char[] name,
                                                   ClassLoader loader, byte[] buf,
                                                   int len, ProtectionDomain pd, char[] source) {
        if (source == null){
            source = "__JVM_DefineClass__".toCharArray();
        }

//        if (UsePerfData) {
//            ClassLoader::perf_app_classfile_bytes_read()->inc(len);
//        }

        // Class resolution will get the class name from the .class stream if the name is null.
        Symbol class_name = name == null ? null : SystemDictionary.class_name_symbol(name);
        ClassFileStream st=new ClassFileStream(buf, len, source, ClassFileStream.verify);
        OopDesc class_loader =new OopDesc(loader);
        OopDesc protection_domain=new OopDesc(pd);
        ClassLoadInfo cl_info=new ClassLoadInfo(protection_domain);
        Klass k =SystemDictionary.resolve_from_stream(st, class_name, class_loader, cl_info);
        return k.getMirror().getObject();
    }




}
