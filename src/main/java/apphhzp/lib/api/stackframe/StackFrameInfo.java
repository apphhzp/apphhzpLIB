package apphhzp.lib.api.stackframe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;

public class StackFrameInfo implements StackWalker.StackFrame {
    private static final Class<?> JDK_CLASS;
    private static final VarHandle retainClassRefVar;
    private static final VarHandle memberNameVar;
    private static final VarHandle bciVar;
    private static final VarHandle steVar;

    private static final VarHandle clazzVar;
    private static final MethodHandle nameMethod;
    private static final MethodHandle methodTypeMethod;
    private static final MethodHandle methodDescriptorMethod;
    private static final MethodHandle ofMethod;
    private static final MethodHandle isNativeMethod;
    static {
        try {
            JDK_CLASS =Class.forName("java.lang.StackFrameInfo");
            retainClassRefVar=lookup.findVarHandle(JDK_CLASS,"retainClassRef",boolean.class);
            memberNameVar=lookup.findVarHandle(JDK_CLASS,"memberName",Object.class);
            bciVar=lookup.findVarHandle(JDK_CLASS,"bci",int.class);
            steVar=lookup.findVarHandle(JDK_CLASS,"ste",StackTraceElement.class);
            Class<?> klass=Class.forName("java.lang.invoke.MemberName");
            clazzVar=lookup.findVarHandle(klass,"clazz",Class.class);
            nameMethod=lookup.findVirtual(klass,"getName",MethodType.methodType(String.class));
            methodTypeMethod=lookup.findVirtual(klass,"getMethodType", MethodType.methodType(MethodType.class));
            methodDescriptorMethod=lookup.findVirtual(klass,"getMethodDescriptor", MethodType.methodType(String.class));
            isNativeMethod=lookup.findVirtual(klass,"isNative",MethodType.methodType(boolean.class));
            ofMethod=lookup.findStatic(StackTraceElement.class,"of", MethodType.methodType(StackTraceElement.class, JDK_CLASS));
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    private final Object oldIns;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final boolean retainClassRef;
    private final Object memberName;
    private final int bci;
    private volatile StackTraceElement ste;


    public StackFrameInfo(Object info) {
        if (!isInfo(info)){
            throw new IllegalArgumentException("Illegal class: " + info.getClass());
        }
        oldIns=info;
        retainClassRef= (boolean) retainClassRefVar.get(info);
        memberName=  memberNameVar.get(info);
        bci= (int) bciVar.get(info);
        ste= (StackTraceElement) steVar.get(info);
    }

    public static boolean isInfo(Object obj){
        return JDK_CLASS.isInstance(obj);
    }

    // package-private called by StackStreamFactory to skip
    // the capability check
    Class<?> declaringClass() {
        return (Class<?>) clazzVar.get(memberName);
    }

    // ----- implementation of StackFrame methods

    @Override
    public String getClassName() {
        return declaringClass().getName();
    }

    @Override
    public Class<?> getDeclaringClass() {
        ensureRetainClassRefEnabled();
        return declaringClass();
    }

    public static Class<?> getDeclaringClass(Object memberName){
        try {
            return (Class<?>) clazzVar.get(memberName);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    @Override
    public String getMethodName() {
        try {
            return (String) nameMethod.invoke(memberName);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public MethodType getMethodType() {
        ensureRetainClassRefEnabled();
        try {
            return (MethodType) methodTypeMethod.invoke(memberName);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescriptor() {
        try {
            return (String) methodDescriptorMethod.invoke(memberName);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getByteCodeIndex() {
        // bci not available for native methods
        if (isNativeMethod())
            return -1;
        return bci;
    }

    @Override
    public String getFileName() {
        return toStackTraceElement().getFileName();
    }

    @Override
    public int getLineNumber() {
        // line number not available for native methods
        if (isNativeMethod())
            return -2;
        return toStackTraceElement().getLineNumber();
    }


    @Override
    public boolean isNativeMethod() {
        try {
            return (boolean) isNativeMethod.invoke(memberName);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return toStackTraceElement().toString();
    }

    @Override
    public StackTraceElement toStackTraceElement() {
        StackTraceElement s = ste;
        if (s == null) {
            synchronized (this) {
                s = ste;
                if (s == null) {
                    try {
                        ste = s = (StackTraceElement) ofMethod.invoke(oldIns);
                    } catch (Throwable e) {
                        throwOriginalException(e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return s;
    }

    private void ensureRetainClassRefEnabled() {
        // Bypass it!
//        if (!retainClassRef) {
//            throw new UnsupportedOperationException("No access to RETAIN_CLASS_REFERENCE");
//        }
    }
}
