package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import org.objectweb.asm.Opcodes;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class AccessFlags {
    public static final Type TYPE = JVM.type("AccessFlags");
    public static final int SIZE = TYPE.size;
    public static final int JVM_ACC_WRITTEN_FLAGS = JVM.intConstant("JVM_ACC_WRITTEN_FLAGS"),

    // Method* flags
    JVM_ACC_MONITOR_MATCH = JVM.intConstant("JVM_ACC_MONITOR_MATCH"),     // True if we know that monitorenter/monitorexit bytecodes match
            JVM_ACC_HAS_MONITOR_BYTECODES = JVM.intConstant("JVM_ACC_HAS_MONITOR_BYTECODES"),     // Method contains monitorenter/monitorexit bytecodes
            JVM_ACC_HAS_LOOPS = JVM.intConstant("JVM_ACC_HAS_LOOPS"),     // Method has loops
            JVM_ACC_LOOPS_FLAG_INIT = JVM.intConstant("JVM_ACC_LOOPS_FLAG_INIT"),// The loop flag has been initialized
            JVM_ACC_QUEUED = JVM.intConstant("JVM_ACC_QUEUED"),     // Queued for compilation
            JVM_ACC_NOT_C2_COMPILABLE = 0x02000000,
            JVM_ACC_NOT_C1_COMPILABLE = 0x04000000,
            JVM_ACC_NOT_C2_OSR_COMPILABLE = JVM.intConstant("JVM_ACC_NOT_C2_OSR_COMPILABLE"),
            JVM_ACC_HAS_LINE_NUMBER_TABLE = JVM.intConstant("JVM_ACC_HAS_LINE_NUMBER_TABLE"),
            JVM_ACC_HAS_CHECKED_EXCEPTIONS = JVM.intConstant("JVM_ACC_HAS_CHECKED_EXCEPTIONS"),
            JVM_ACC_HAS_JSRS = JVM.intConstant("JVM_ACC_HAS_JSRS"),
            JVM_ACC_IS_OLD = JVM.intConstant("JVM_ACC_IS_OLD"),     // RedefineClasses() has replaced this method
            JVM_ACC_IS_OBSOLETE = JVM.intConstant("JVM_ACC_IS_OBSOLETE"),     // RedefineClasses() has made method obsolete
            JVM_ACC_IS_PREFIXED_NATIVE = JVM.intConstant("JVM_ACC_IS_PREFIXED_NATIVE"),     // JVMTI has prefixed this native method
            JVM_ACC_ON_STACK = 0x00080000,     // RedefineClasses() was used on the stack
            JVM_ACC_IS_DELETED = 0x00008000,     // RedefineClasses() has deleted this method

    // Klass* flags
    JVM_ACC_HAS_MIRANDA_METHODS = JVM.intConstant("JVM_ACC_HAS_MIRANDA_METHODS"),     // True if this class has miranda methods in it's vtable
            JVM_ACC_HAS_VANILLA_CONSTRUCTOR = JVM.intConstant("JVM_ACC_HAS_VANILLA_CONSTRUCTOR"),     // True if klass has a vanilla default constructor
            JVM_ACC_HAS_FINALIZER = JVM.intConstant("JVM_ACC_HAS_FINALIZER"),     // True if klass has a non-empty finalize() method
            JVM_ACC_IS_CLONEABLE_FAST = JVM.intConstant("JVM_ACC_IS_CLONEABLE_FAST"),// True if klass implements the Cloneable interface and can be optimized in generated code
            JVM_ACC_HAS_FINAL_METHOD = 0x01000000,     // True if klass has final method
            JVM_ACC_IS_SHARED_CLASS = 0x02000000,     // True if klass is shared
            JVM_ACC_IS_HIDDEN_CLASS = JVM.includeJVMCI ? JVM.intConstant("JVM_ACC_IS_HIDDEN_CLASS") : 0x04000000,     // True if klass is hidden
            JVM_ACC_IS_VALUE_BASED_CLASS = 0x08000000,     // True if klass is marked as a ValueBased class
            JVM_ACC_IS_BEING_REDEFINED = 0x00100000,     // True if the klass is being redefined.

    // Klass* and Method* flags
    JVM_ACC_HAS_LOCAL_VARIABLE_TABLE = JVM.intConstant("JVM_ACC_HAS_LOCAL_VARIABLE_TABLE"),

    JVM_ACC_PROMOTED_FLAGS = JVM.intConstant("JVM_ACC_PROMOTED_FLAGS"),     // flags promoted from methods to the holding klass

    // field flags
    // Note: these flags must be defined in the low order 16 bits because
    // InstanceKlass only stores a ushort worth of information from the
    // AccessFlags value.
    // These bits must not conflict with any other field-related access flags
    // (e.g., ACC_ENUM).
    // Note that the class-related ACC_ANNOTATION bit conflicts with these flags.
    JVM_ACC_FIELD_ACCESS_WATCHED = JVM.intConstant("JVM_ACC_FIELD_ACCESS_WATCHED"), // field access is watched by JVMTI
            JVM_ACC_FIELD_MODIFICATION_WATCHED = JVM.intConstant("JVM_ACC_FIELD_MODIFICATION_WATCHED"), // field modification is watched by JVMTI
            JVM_ACC_FIELD_INTERNAL = JVM.intConstant("JVM_ACC_FIELD_INTERNAL"), // internal field, same as JVM_ACC_ABSTRACT
            JVM_ACC_FIELD_STABLE = JVM.intConstant("JVM_ACC_FIELD_STABLE"), // @Stable field, same as JVM_ACC_SYNCHRONIZED and JVM_ACC_SUPER
            JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE = JVM.includeJVMCI ? JVM.intConstant("JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE") : 0x00000100, // (static) final field updated outside (class) initializer, same as JVM_ACC_NATIVE
            JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE = JVM.intConstant("JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE"), // field has generic signature

    JVM_ACC_FIELD_INTERNAL_FLAGS = JVM_ACC_FIELD_ACCESS_WATCHED |
            JVM_ACC_FIELD_MODIFICATION_WATCHED |
            JVM_ACC_FIELD_INTERNAL |
            JVM_ACC_FIELD_STABLE |
            JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE,

    // flags accepted by set_field_flags()
    JVM_ACC_FIELD_FLAGS = (Opcodes.ACC_PUBLIC |
            Opcodes.ACC_PRIVATE |
            Opcodes.ACC_PROTECTED |
            Opcodes.ACC_STATIC |
            Opcodes.ACC_FINAL |
            Opcodes.ACC_VOLATILE |
            Opcodes.ACC_TRANSIENT |
            Opcodes.ACC_ENUM |
            Opcodes.ACC_SYNTHETIC) | JVM_ACC_FIELD_INTERNAL_FLAGS;
    private static final Map<Integer, AccessFlags> cache = new HashMap<>();
    public final String klassACCPrefix;
    public final String methodACCPrefix;
    public final String fieldACCPrefix;
    public final int flags;

    public static AccessFlags getOrCreate(int flags) {
        AccessFlags x;
        x = cache.get(flags);
        if (x != null) {
            return x;
        }
        x = new AccessFlags(flags);
        cache.put(flags, x);
        return x;
    }

    private AccessFlags(int flags) {
        this.flags = flags;
        StringJoiner klass = new StringJoiner(" "), method = new StringJoiner(" "), field = new StringJoiner(" ");
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
        if (this.isStatic()) {
            method.add("static");
            field.add("static");
        }
        if (this.isNative()) {
            method.add("native");
        }
        if (this.isFinal()) {
            klass.add("final");
            method.add("final");
            field.add("final");
        }
        if (this.isSynchronized()) {
            method.add("synchronized");
        }
        if (this.isAbstract() && !this.isInterface()) {
            klass.add("abstract");
            method.add("abstract");
        }
        if (this.isStrict()) {
            method.add("strictfp");
        }
        if (this.isTransient()) {
            field.add("transient");
        }
        if (this.isSynthetic()) {
            klass.add("(synthetic)");
            method.add("(synthetic)");
            field.add("(synthetic)");
        }
        if (this.isBridge()) {
            method.add("(bridge)");
        }
        if (this.isVarargs()) {
            method.add("(varargs)");
        }
        if (this.isInterface()) {
            if (this.isAnnotation()) {
                klass.add("@interface");
            } else {
                klass.add("interface");
            }
        } else if (this.isEnum()) {
            klass.add("enum");
            field.add("enum");
        } else {
            klass.add("class");
        }
        this.klassACCPrefix = klass.toString();
        this.methodACCPrefix = method.toString();
        this.fieldACCPrefix = field.toString();
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

    public boolean isSynchronized() {
        return (this.flags & Opcodes.ACC_SYNCHRONIZED) != 0;
    }

    public boolean isVolatile() {
        return (this.flags & Opcodes.ACC_VOLATILE) != 0;
    }

    public boolean isBridge() {
        return (this.flags & Opcodes.ACC_BRIDGE) != 0;
    }

    public boolean isVarargs() {
        return (this.flags & Opcodes.ACC_VARARGS) != 0;
    }

    public boolean isTransient() {
        return (this.flags & Opcodes.ACC_TRANSIENT) != 0;
    }

    public boolean isNative() {
        return (this.flags & Opcodes.ACC_NATIVE) != 0;
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

    public boolean isMonitorMatching() {
        return (this.flags & JVM_ACC_MONITOR_MATCH) != 0;
    }

    public boolean hasMonitorBytecodes() {
        return (this.flags & JVM_ACC_HAS_MONITOR_BYTECODES) != 0;
    }

    public boolean hasLoops() {
        return (this.flags & JVM_ACC_HAS_LOOPS) != 0;
    }

    public boolean loopsFlagInit() {
        return (this.flags & JVM_ACC_LOOPS_FLAG_INIT) != 0;
    }

    public boolean queuedForCompilation() {
        return (this.flags & JVM_ACC_QUEUED) != 0;
    }

    public boolean isNotC1Compilable() {
        return (this.flags & JVM_ACC_NOT_C1_COMPILABLE) != 0;
    }

    public boolean isNotC2Compilable() {
        return (this.flags & JVM_ACC_NOT_C2_COMPILABLE) != 0;
    }

    public boolean isNotC2OsrCompilable() {
        return (this.flags & JVM_ACC_NOT_C2_OSR_COMPILABLE) != 0;
    }

    public boolean hasLinenumberTable() {
        return (this.flags & JVM_ACC_HAS_LINE_NUMBER_TABLE) != 0;
    }

    public boolean hasCheckedExceptions() {
        return (this.flags & JVM_ACC_HAS_CHECKED_EXCEPTIONS) != 0;
    }

    public boolean hasJsrs() {
        return (this.flags & JVM_ACC_HAS_JSRS) != 0;
    }

    public boolean isOld() {
        return (this.flags & JVM_ACC_IS_OLD) != 0;
    }

    public boolean isObsolete() {
        return (this.flags & JVM_ACC_IS_OBSOLETE) != 0;
    }

    public boolean isDeleted() {
        return (this.flags & JVM_ACC_IS_DELETED) != 0;
    }

    public boolean isPrefixedNative() {
        return (this.flags & JVM_ACC_IS_PREFIXED_NATIVE) != 0;
    }

    // Klass* flags
    public boolean hasMirandaMethods() {
        return (this.flags & JVM_ACC_HAS_MIRANDA_METHODS) != 0;
    }

    public boolean hasVanillaConstructor() {
        return (this.flags & JVM_ACC_HAS_VANILLA_CONSTRUCTOR) != 0;
    }

    public boolean hasFinalizer() {
        return (this.flags & JVM_ACC_HAS_FINALIZER) != 0;
    }

    public boolean hasFinalMethod() {
        return (this.flags & JVM_ACC_HAS_FINAL_METHOD) != 0;
    }

    public boolean isCloneableFast() {
        return (this.flags & JVM_ACC_IS_CLONEABLE_FAST) != 0;
    }

    public boolean isSharedClass() {
        return (this.flags & JVM_ACC_IS_SHARED_CLASS) != 0;
    }

    public boolean isHiddenClass() {
        return (this.flags & JVM_ACC_IS_HIDDEN_CLASS) != 0;
    }

    public boolean isValueBasedClass() {
        return (this.flags & JVM_ACC_IS_VALUE_BASED_CLASS) != 0;
    }

    public boolean isSuper() {
        return (this.flags & Opcodes.ACC_SUPER) != 0;
    }

    public boolean hasLocalvariableTable() {
        return (this.flags & JVM_ACC_HAS_LOCAL_VARIABLE_TABLE) != 0;
    }

    public boolean isBeingRedefined() {
        return (this.flags & JVM_ACC_IS_BEING_REDEFINED) != 0;
    }

    // field flags
    public boolean isFieldAccessWatched() {
        return (this.flags & JVM_ACC_FIELD_ACCESS_WATCHED) != 0;
    }

    public boolean isFieldModificationWatched() {
        return (this.flags & JVM_ACC_FIELD_MODIFICATION_WATCHED) != 0;
    }

    public boolean hasFieldInitializedFinalUpdate() {
        return (this.flags & JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE) != 0;
    }

    public boolean onStack() {
        return (this.flags & JVM_ACC_ON_STACK) != 0;
    }

    public boolean isInternal() {
        return (this.flags & JVM_ACC_FIELD_INTERNAL) != 0;
    }

    public boolean isStable() {
        return (this.flags & JVM_ACC_FIELD_STABLE) != 0;
    }

    public boolean fieldHasGenericSignature() {
        return (this.flags & JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE) != 0;
    }

    public void print_on(PrintStream st){
        if (isPublic      ()) st.print("public "      );
        if (isPrivate     ()) st.print("private "     );
        if (isProtected   ()) st.print("protected "   );
        if (isStatic      ()) st.print("static "      );
        if (isFinal       ()) st.print("final "       );
        if (isSynchronized()) st.print("synchronized ");
        if (isVolatile    ()) st.print("volatile "    );
        if (isTransient   ()) st.print("transient "   );
        if (isNative      ()) st.print("native "      );
        if (isInterface   ()) st.print("interface "   );
        if (isAbstract    ()) st.print("abstract "    );
        if (isSynthetic   ()) st.print("synthetic "   );
        if (isOld         ()) st.print("{old} "       );
        if (isObsolete    ()) st.print("{obsolete} "  );
        if (onStack       ()) st.print("{on_stack} "  );
    }
}
