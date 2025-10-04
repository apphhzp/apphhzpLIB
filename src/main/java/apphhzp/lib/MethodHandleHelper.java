package apphhzp.lib;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;

public final class MethodHandleHelper {
    public static final Class<?> nativesClazz;
    public static final Class<?> memberNameClass;
    private static final MethodHandle findMethodHandleTypeMethod;
    private static final MethodHandle linkCallSiteMethod;
    private static final MethodHandle linkDynamicConstantMethod;
    private static final MethodHandle linkMethodHandleConstantMethod;
    static {
        try {
            memberNameClass=Class.forName("java.lang.invoke.MemberName");
            Class<?> klass=nativesClazz=Class.forName("java.lang.invoke.MethodHandleNatives");
            findMethodHandleTypeMethod=lookup.findStatic(klass,"findMethodHandleType", MethodType.methodType(MethodType.class, Class.class, Class[].class));
            linkCallSiteMethod=lookup.findStatic(klass,"linkCallSite", MethodType.methodType(memberNameClass, Object.class, int.class, Object.class, Object.class, Object.class, Object.class, Object[].class));
            linkDynamicConstantMethod=lookup.findStatic(klass,"linkDynamicConstant", MethodType.methodType(Object.class, Object.class, int.class, Object.class, Object.class, Object.class, Object.class));
            linkMethodHandleConstantMethod=lookup.findStatic(klass,"linkMethodHandleConstant", MethodType.methodType(MethodHandle.class, Class.class, int.class, Class.class, String.class, Object.class));
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
    private MethodHandleHelper(){throw new UnsupportedOperationException();}
    /**
     * The JVM wants a pointer to a MethodType.  Oblige it by finding or creating one.
     */
    public static MethodType findMethodHandleType(Class<?> rtype, Class<?>[] ptypes) {
        try {
            return (MethodType) findMethodHandleTypeMethod.invoke(rtype,ptypes);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
    /**
     * The JVM is linking an invokedynamic instruction.  Create a reified call site for it.
     */
    public static /*MemberName*/ Object linkCallSite(Object callerObj,
                                          int indexInCP,
                                          Object bootstrapMethodObj,
                                          Object nameObj, Object typeObj,
                                          Object staticArguments,
                                          Object[] appendixResult){
        try {
            return  linkCallSiteMethod.invoke(callerObj,indexInCP,bootstrapMethodObj,nameObj,typeObj,staticArguments,appendixResult);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    // this implements the upcall from the JVM, MethodHandleNatives.linkDynamicConstant:
    public static Object linkDynamicConstant(Object callerObj,
                                      int indexInCP,
                                      Object bootstrapMethodObj,
                                      Object nameObj, Object typeObj,
                                      Object staticArguments) {
        try {
            return linkDynamicConstantMethod.invoke(callerObj,indexInCP,bootstrapMethodObj,nameObj,typeObj,staticArguments);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    /**
     * The JVM is resolving a CONSTANT_MethodHandle CP entry.  And it wants our help.
     * It will make an up-call to this method.  (Do not change the name or signature.)
     * The type argument is a Class for field requests and a MethodType for non-fields.
     * <p>
     * Recent versions of the JVM may also pass a resolved MemberName for the type.
     * In that case, the name is ignored and may be null.
     */
    public static MethodHandle linkMethodHandleConstant(Class<?> callerClass, int refKind,
                                                 Class<?> defc, String name, Object type) {
        try {
            return (MethodHandle) linkMethodHandleConstantMethod.invoke(callerClass,refKind,defc,name,type);
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
}
