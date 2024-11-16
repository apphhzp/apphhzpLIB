package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class AccessFlags {
    public static final int JVM_ACC_FIELD_STABLE= JVM.intConstant("JVM_ACC_FIELD_STABLE");
    public static final int JVM_ACC_FIELD_INTERNAL=JVM.intConstant("JVM_ACC_FIELD_INTERNAL");
    private static final Map<Integer,AccessFlags> cache=new HashMap<>();
    public final String klassACCPrefix;
    public final String methodACCPrefix;
    public final String fieldACCPrefix;
    public final int flags;

    public static AccessFlags getOrCreate(int flags){
        AccessFlags x;
        x=cache.get(flags);
        if (x!=null){
            return x;
        }
        x=new AccessFlags(flags);
        cache.put(flags,x);
        return x;
    }

    private AccessFlags(int flags) {
        this.flags = flags;
        StringJoiner klass = new StringJoiner(" "),method=new StringJoiner(" "),field=new StringJoiner(" ");
        if (this.isPublic()) {
            klass.add("public");
            method.add("public");
            field.add("public");
        }
        if (this.isProtected()) {
            klass.add("protected");
            method.add("protected");
            field.add("protected");
        }
        if (this.isPrivate()) {
            klass.add("private");
            method.add("private");
            field.add("private");
        }
        if (this.isStatic()){
            method.add("static");
            field.add("static");
        }
        if (this.isNative()){
            method.add("native");
        }
        if (this.isFinal()) {
            klass.add("final");
            method.add("final");
            field.add("final");
        }
        if (this.isSynchronized()){
            method.add("synchronized");
        }
        if (this.isAbstract()&&!this.isInterface()) {
            klass.add("abstract");
            method.add("abstract");
        }
        if (this.isStrict()){
            method.add("strictfp");
        }
        if (this.isTransient()){
            field.add("transient");
        }
        if (this.isSynthetic()){
            klass.add("(synthetic)");
            method.add("(synthetic)");
            field.add("(synthetic)");
        }
        if (this.isBridge()){
            method.add("(bridge)");
        }
        if (this.isVarargs()){
            method.add("(varargs)");
        }
        if (this.isInterface()) {
            if (this.isAnnotation()){
                klass.add("@interface");
            }else {
                klass.add("interface");
            }
        }else if (this.isEnum()) {
            klass.add("enum");
            field.add("enum");
        }else {
            klass.add("class");
        }
        this.klassACCPrefix = klass.toString();
        this.methodACCPrefix=method.toString();
        this.fieldACCPrefix=field.toString();
    }
    public boolean isPublic() {
        return (this.flags & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isPrivate() {
        return (this.flags & Opcodes.ACC_PRIVATE) != 0;
    }

    public boolean isProtected() {
        return (this.flags & Opcodes.ACC_PROTECTED) != 0;
    }

    public boolean isStatic() {
        return (this.flags & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isSynthetic() {
        return (this.flags & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public boolean isFinal() {
        return (this.flags & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isSynchronized(){
        return (this.flags&Opcodes.ACC_SYNCHRONIZED)!=0;
    }

    public boolean isVolatile(){
        return (this.flags&Opcodes.ACC_VOLATILE)!=0;
    }

    public boolean isBridge(){
        return (this.flags&Opcodes.ACC_BRIDGE)!=0;
    }
    public boolean isVarargs(){
        return (this.flags&Opcodes.ACC_VARARGS)!=0;
    }
    public boolean isTransient(){
        return (this.flags&Opcodes.ACC_TRANSIENT)!=0;
    }
    public boolean isNative(){
        return (this.flags&Opcodes.ACC_NATIVE)!=0;
    }
    public boolean isInterface() {
        return (this.flags & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return (this.flags & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isAnnotation() {
        return (this.flags & Opcodes.ACC_ANNOTATION) != 0;
    }

    public boolean isEnum() {
        return (this.flags & Opcodes.ACC_ENUM) != 0;
    }

    public boolean isModule() {
        return (this.flags & Opcodes.ACC_MODULE) != 0;
    }

    public boolean isStrict() {
        return (this.flags & Opcodes.ACC_STRICT) != 0;
    }

    public boolean fieldHasGenericSignature(){
        return false;
    }
}
