package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class VMClasses {
    public static final Type TYPE= JVM.type("vmClasses");
    public static final long java_lang_Object_address=TYPE.global("_klasses[static_cast<int>(vmClassID::Object_klass_knum)]");
    public static final long java_lang_String_address=TYPE.global("_klasses[static_cast<int>(vmClassID::String_klass_knum)]");
    public static final long java_lang_Class_address=TYPE.global("_klasses[static_cast<int>(vmClassID::Class_klass_knum)]");
    public static final long java_lang_ClassLoader_address=TYPE.global("_klasses[static_cast<int>(vmClassID::ClassLoader_klass_knum)]");
    public static final long java_lang_Thread_address=TYPE.global("_klasses[static_cast<int>(vmClassID::Thread_klass_knum)]");
    public static InstanceKlass objectKlass(){
        return InstanceKlass.getOrCreate(unsafe.getAddress(java_lang_Object_address));
    }

    public static InstanceKlass stringKlass(){
        return InstanceKlass.getOrCreate(unsafe.getAddress(java_lang_String_address));
    }

    public static InstanceKlass classKlass(){
        return InstanceKlass.getOrCreate(unsafe.getAddress(java_lang_Class_address));
    }

    public static InstanceKlass classLoaderKlass(){
        return InstanceKlass.getOrCreate(unsafe.getAddress(java_lang_ClassLoader_address));
    }

    public static InstanceKlass threadKlass(){
        return InstanceKlass.getOrCreate(unsafe.getAddress(java_lang_Thread_address));
    }
}
