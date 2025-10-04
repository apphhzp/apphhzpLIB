package apphhzp.lib.hotspot.prims;

import apphhzp.lib.MethodHandleHelper;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.classfile.SystemDictionary;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.signature.ArgumentCount;
import apphhzp.lib.hotspot.runtime.signature.SignatureStream;
import apphhzp.lib.hotspot.util.ClassConstants;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import static apphhzp.lib.ClassHelperSpecial.*;

public class MethodHandles {
    public static final int  _suppress_defc = 1, _suppress_name = 2, _suppress_type = 4;
    public static final int  JVM_REF_MIN = ClassConstants.JVM_REF_getField, JVM_REF_MAX = ClassConstants.JVM_REF_invokeInterface ;
    private MethodHandles(){throw new UnsupportedOperationException("All Static");}
    public static boolean ref_kind_is_valid(int ref_kind) {
        return (ref_kind >= JVM_REF_MIN && ref_kind <= JVM_REF_MAX);
    }
    public static boolean ref_kind_is_field(int ref_kind) {
        if (!ref_kind_is_valid(ref_kind)){
            throw new IllegalArgumentException();
        }
        return (ref_kind <= ClassConstants.JVM_REF_putStatic);
    }
    public static boolean ref_kind_is_getter(int ref_kind) {
        if (!ref_kind_is_valid(ref_kind)){
            throw new IllegalArgumentException();
        }
        return (ref_kind <= ClassConstants.JVM_REF_getStatic);
    }
    public static boolean ref_kind_is_setter(int ref_kind) {
        return ref_kind_is_field(ref_kind) && !ref_kind_is_getter(ref_kind);
    }
    public static boolean ref_kind_is_method(int ref_kind) {
        return !ref_kind_is_field(ref_kind) && (ref_kind != ClassConstants.JVM_REF_newInvokeSpecial);
    }
    public static boolean ref_kind_has_receiver(int ref_kind) {
        if (!ref_kind_is_valid(ref_kind)){
            throw new IllegalArgumentException();
        }
        return (ref_kind & 1) != 0;
    }
    public static @RawCType("vmIntrinsics::ID")int signature_polymorphic_name_id(Symbol name) {
        @RawCType("vmSymbolID")int name_id = Symbol.find_sid(name);

        // The ID _invokeGeneric stands for all non-static signature-polymorphic methods, except built-ins.
        if (name_id == Symbol.find_sid(Symbol.getVMSymbol("invoke"))) {
            return VMIntrinsics.invokeGeneric;

        } // The only built-in non-static signature-polymorphic method is MethodHandle.invokeBasic:
        else if (name_id == Symbol.find_sid(Symbol.getVMSymbol("invokeBasic"))) {
            return VMIntrinsics.invokeBasic;

        }// There is one static signature-polymorphic method for each JVM invocation mode.
        else if (name_id == Symbol.find_sid(Symbol.getVMSymbol("linkToVirtual"))) {
            return VMIntrinsics.linkToVirtual;
        } else if (name_id == Symbol.find_sid(Symbol.getVMSymbol("linkToStatic"))) {
            return VMIntrinsics.linkToStatic;
        } else if (name_id == Symbol.find_sid(Symbol.getVMSymbol("linkToSpecial"))) {
            return VMIntrinsics.linkToSpecial;
        } else if (name_id == Symbol.find_sid(Symbol.getVMSymbol("linkToInterface"))) {
            return VMIntrinsics.linkToInterface;
        } else if (name_id == Symbol.find_sid(Symbol.getVMSymbol("linkToNative"))) {
            return VMIntrinsics.linkToNative;
        }

        // Cover the case of invokeExact and any future variants of invokeFoo.
        Klass mh_klass = Klass.asKlass(MethodHandle.class);
        if (is_method_handle_invoke_name(mh_klass, name)) {
            return VMIntrinsics.invokeGeneric;
        }

        // Cover the case of methods on VarHandle.
        Klass vh_klass = Klass.asKlass(VarHandle.class);
        if (is_method_handle_invoke_name(vh_klass, name)) {
            return VMIntrinsics.invokeGeneric;
        }

        // Note: The pseudo-intrinsic _compiledLambdaForm is never linked against.
        // Instead it is used to mark lambda forms bound to invokehandle or invokedynamic.
        return VMIntrinsics.none;
    }
    public static boolean is_signature_polymorphic_name(Symbol name) {
        return signature_polymorphic_name_id(name) != VMIntrinsics.none;
    }
    // JVM 2.9 Special Methods:
// A method is signature polymorphic if and only if all of the following conditions hold :
// * It is declared in the java.lang.invoke.MethodHandle/VarHandle classes.
// * It has a single formal parameter of type Object[].
// * It has a return type of Object for a polymorphic return type, otherwise a fixed return type.
// * It has the ACC_VARARGS and ACC_NATIVE flags set.
    public static boolean is_method_handle_invoke_name(Klass klass, Symbol name){
        if (klass == null)
            return false;
        // The following test will fail spuriously during bootstrap of MethodHandle itself:
        //    if (klass != vmClasses::MethodHandle_klass())
        // Test the name instead:
        if (!klass.name().toString().equals("java/lang/invoke/MethodHandle") &&
                !klass.name().toString().equals("java/lang/invoke/VarHandle")) {
            return false;
        }

        // Look up signature polymorphic method with polymorphic return type
        Symbol poly_sig = Symbol.getVMSymbol("([Ljava/lang/Object;)Ljava/lang/Object;");
        InstanceKlass iklass = (klass.asInstanceKlass());
        Method m = iklass.find_method(name, poly_sig);
        if (m != null) {
            int required = ClassConstants.JVM_ACC_NATIVE | ClassConstants.JVM_ACC_VARARGS;
            int flags = m.access_flags().flags;
            if ((flags & required) == required) {
                return true;
            }
        }

        // Look up signature polymorphic method with non-polymorphic (non Object) return type
        int[] me=new int[1];
        int ms = iklass.find_method_by_name(name, me);
        if (ms == -1) return false;
        for (; ms < me[0]; ms++) {
            m = iklass.getMethods().get(ms);
            int required = ClassConstants.JVM_ACC_NATIVE | ClassConstants.JVM_ACC_VARARGS;
            int flags = m.access_flags().flags;
            if ((flags & required) == required && new ArgumentCount(m.signature()).size() == 1) {
                return true;
            }
        }
        return false;
    }

    public static @RawCType("vmIntrinsics::ID")int signature_polymorphic_name_id(Klass klass, Symbol name) {
        if (klass != null &&
                (klass.name().toString().equals("java/lang/invoke/MethodHandle") ||
                klass.name().toString().equals("java/lang/invoke/VarHandle"))) {
            @RawCType("vmIntrinsics::ID")int iid = signature_polymorphic_name_id(name);
            if (iid != VMIntrinsics.none)
                return iid;
            if (is_method_handle_invoke_name(klass, name))
                return VMIntrinsics.invokeGeneric;
        }
        return VMIntrinsics.none;
    }
    public static boolean is_signature_polymorphic_name(Klass klass, Symbol name) {
        return signature_polymorphic_name_id(klass, name) != VMIntrinsics.none;
    }

    public static boolean is_signature_polymorphic_public_name(Klass klass, Symbol name) {
        if (is_signature_polymorphic_name(klass, name)) {
            InstanceKlass iklass = (klass.asInstanceKlass());
            int[] me=new int[1];
            int ms = iklass.find_method_by_name(name, me);
            if (ms==-1){
                throw new RuntimeException();
            }
            for (; ms < me[0]; ms++) {
                Method m = iklass.getMethods().get(ms);
                int required = ClassConstants.JVM_ACC_NATIVE | ClassConstants.JVM_ACC_VARARGS | ClassConstants.JVM_ACC_PUBLIC;
                int flags = m.access_flags().flags;
                if ((flags & required) == required && new ArgumentCount(m.signature()).size() == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int ref_kind_to_flags(int ref_kind) {
        if (!ref_kind_is_valid(ref_kind)){
            throw new IllegalArgumentException(Integer.toString(ref_kind));
        }
        int flags = (ref_kind << REFERENCE_KIND_SHIFT);
        if (ref_kind_is_field(ref_kind)) {
            flags |= IS_FIELD;
        } else if (ref_kind_is_method(ref_kind)) {
            flags |= IS_METHOD;
        } else if (ref_kind == ClassConstants.JVM_REF_newInvokeSpecial) {
            flags |= IS_CONSTRUCTOR;
        }
        return flags;
    }

    private static final MethodHandle resolve_MemberNameMethod;
    static {
        try {
            resolve_MemberNameMethod=lookup.findStatic(MethodHandleHelper.nativesClazz,"resolve",
                    MethodType.methodType(MethodHandleHelper.memberNameClass,MethodHandleHelper.memberNameClass, Class.class, int.class, boolean.class));
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static Object resolve_MemberName(Object self, Class<?> caller, int lookupMode,
                                          boolean speculativeResolve){
        if (!JavaClasses.MemberName.clazz.isInstance(self)){
            throw new IllegalArgumentException("wrong type");
        }
        try {
            return resolve_MemberNameMethod.invoke(self,caller,lookupMode,speculativeResolve);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    // import java_lang_invoke_MemberName.*
    private static final int
        IS_METHOD            = 0x00010000,
                IS_CONSTRUCTOR       =  0x00020000,
                IS_FIELD             = 0x00040000,
                IS_TYPE              = 0x00080000,
                CALLER_SENSITIVE     = 0x00100000,
                TRUSTED_FINAL        = 0x00200000,
                REFERENCE_KIND_SHIFT = 24,
                REFERENCE_KIND_MASK  = 0x0F000000 >> REFERENCE_KIND_SHIFT,
                SEARCH_SUPERCLASSES  = 0x00100000,
                SEARCH_INTERFACES    = 0x00200000,
                LM_UNCONDITIONAL     = 0x00000020,
                LM_MODULE            = 0x00000010,
                LM_TRUSTED           = -1,
                ALL_KINDS      = IS_METHOD | IS_CONSTRUCTOR | IS_FIELD | IS_TYPE;

    public static MethodType resolve_MemberName_type(Object mname, Klass caller) {
        if (!JavaClasses.MemberName.clazz.isInstance(mname)){
            throw new IllegalArgumentException("wrong type");
        }
        Object type=JavaClasses.MemberName.type(mname);
        if (type instanceof MethodType) {
            return (MethodType) type; // already resolved
        }
        Symbol signature = type==null?null:Symbol.newSymbol((String) type);
        if (signature == null) {
            return null;  // no such signature exists in the VM
        }
        Object resolved=null;
        int flags = JavaClasses.MemberName.flags(mname);
        switch (flags & ALL_KINDS) {
            case IS_METHOD:
            case IS_CONSTRUCTOR:
                resolved = SystemDictionary.find_method_handle_type(signature, caller);
                break;
            case IS_FIELD:
                resolved = SystemDictionary.find_field_handle_type(signature, caller);
                break;
            default:
                throw new InternalError("unrecognized MemberName format");
        }
        if (resolved==null) {
            throw new InternalError("bad MemberName type");
        }
        return (MethodType) resolved;
    }

    public static boolean is_signature_polymorphic(@RawCType("vmIntrinsics::ID") int iid){
        return iid >= VMIntrinsics.FIRST_MH_SIG_POLY && iid <= VMIntrinsics.LAST_MH_SIG_POLY;
    }

    public static boolean is_signature_polymorphic_method(Method m) {
        return is_signature_polymorphic(m.intrinsic_id());
    }

    public static boolean is_signature_polymorphic_intrinsic(@RawCType("vmIntrinsics::ID")int iid) {
        if (!is_signature_polymorphic(iid)){
            throw new IllegalArgumentException();
        }
        // Most sig-poly methods are intrinsics which do not require an
        // appeal to Java for adapter code.
        return iid != VMIntrinsics.invokeGeneric;
    }
    public static boolean is_signature_polymorphic_static(@RawCType("vmIntrinsics::ID")int iid) {
        if (!is_signature_polymorphic(iid)){
            throw new IllegalArgumentException();
        }
        return (iid >= VMIntrinsics.FIRST_MH_STATIC &&
                iid <= VMIntrinsics.LAST_MH_SIG_POLY);
    }

    public static boolean has_member_arg(@RawCType("vmIntrinsics::ID")int iid) {
        if (!is_signature_polymorphic(iid)){
            throw new IllegalArgumentException();
        }
        return (iid >= VMIntrinsics.linkToVirtual &&
                iid <= VMIntrinsics.linkToNative);
    }

    public static void print_as_basic_type_signature_on(PrintStream st,
                                                        Symbol sig) {
        boolean prev_type = false;
        boolean is_method = (sig.char_at(0) == '(');
        if (is_method) {
            st.print('(');
        }
        for (SignatureStream ss=new SignatureStream(sig, is_method); !ss.is_done(); ss.next()) {
            if (ss.at_return_type()) {
                st.print(')');
            } else if (prev_type) {
                st.print(',');
            }
            long cp =  ss.raw_bytes();
            if (ss.is_array()) {
                st.print('[');
                if (ss.array_prefix_length() == 1)
                    st.print((char) unsafe.getByte(cp+1));
                else
                    st.print('L');
            } else {
                st.print((char)unsafe.getByte(cp));
            }
        }
    }
    public static void print_as_basic_type_signature_on(StringBuilder st,
                                                        Symbol sig) {
        boolean prev_type = false;
        boolean is_method = (sig.char_at(0) == '(');
        if (is_method) {
            st.append('(');
        }
        for (SignatureStream ss=new SignatureStream(sig, is_method); !ss.is_done(); ss.next()) {
            if (ss.at_return_type()) {
                st.append(')');
            } else if (prev_type) {
                st.append(',');
            }
            long cp =  ss.raw_bytes();
            if (ss.is_array()) {
                st.append('[');
                if (ss.array_prefix_length() == 1)
                    st.append((char) unsafe.getByte(cp+1));
                else
                    st.append('L');
            } else {
                st.append((char)unsafe.getByte(cp));
            }
        }
    }
}
