package apphhzp.lib.hotspot.util;

import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.klass.Klass;

import java.lang.invoke.MethodType;

import static apphhzp.lib.ClassHelperSpecial.JLA_INSTANCE;
import static apphhzp.lib.ClassHelperSpecial.lookup;

public final class ConstantPoolHelper {
    public static final Class<?> cpClass;
    private static final java.lang.invoke.MethodHandle getConstantPoolMethod;
    private static final java.lang.invoke.MethodHandle getClassAtMethod;
    private static final java.lang.invoke.MethodHandle getClassAtIfLoadedMethod;
    static {
        try {
            Class<?> JLAClass= java.lang.Class.forName("jdk.internal.access.JavaLangAccess");
            cpClass= java.lang.Class.forName("jdk.internal.reflect.ConstantPool");
            getConstantPoolMethod=lookup.findVirtual(JLAClass,"getConstantPool", java.lang.invoke.MethodType.methodType(cpClass, java.lang.Class.class));
            getClassAtMethod=lookup.findVirtual(cpClass,"getClassAt", java.lang.invoke.MethodType.methodType(java.lang.Class.class, int.class));
            getClassAtIfLoadedMethod=lookup.findVirtual(cpClass,"getClassAtIfLoaded", MethodType.methodType(Class.class, int.class));
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }
    private ConstantPoolHelper() {throw new UnsupportedOperationException("All static");}
    public static Klass getClassAt(final ConstantPool this_cp,final int which,boolean[] failed) {
        try {
            Object cp=getConstantPoolMethod.invoke(JLA_INSTANCE,this_cp.pool_holder().asClass());
            Class<?> cls= (Class<?>) getClassAtMethod.invoke(cp,which);
            return cls==null?null:Klass.asKlass(cls);
        }catch (Throwable i){
            failed[0]=true;
            return null;
        }
    }

    public static Klass getClassAtIfLoaded(final ConstantPool this_cp,final int which,boolean[] failed) {
        try {
            Object cp=getConstantPoolMethod.invoke(JLA_INSTANCE,this_cp.pool_holder().asClass());
            Class<?> cls=(java.lang.Class<?>) getClassAtIfLoadedMethod.invoke(cp,which);
            return cls==null?null:Klass.asKlass(cls);
        }catch (Throwable i){
            failed[0]=true;
            return null;
        }
    }
}
