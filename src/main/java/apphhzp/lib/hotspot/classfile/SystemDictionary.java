package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.MethodHandleHelper;
import apphhzp.lib.hotspot.interpreter.BootstrapInfo;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.ClassLoaderDataGraph;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.klass.ObjArrayKlass;
import apphhzp.lib.hotspot.oops.klass.TypeArrayKlass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.prims.MethodHandles;
import apphhzp.lib.hotspot.prims.VMIntrinsics;
import apphhzp.lib.hotspot.runtime.signature.ArgumentCount;
import apphhzp.lib.hotspot.runtime.signature.ResolvingSignatureStream;
import apphhzp.lib.hotspot.runtime.signature.SignatureStream;
import apphhzp.lib.hotspot.util.ClassConstants;
import apphhzp.lib.hotspot.util.RawCType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.security.ProtectionDomain;
import java.util.Arrays;

public final class SystemDictionary {

    public static Symbol class_name_symbol(char[] name) {
        if (name == null) {
            throw new NullPointerException("No class name given");
        }
        if (name.length > Symbol.MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("Class name exceeds maximum length of %d: %s",Symbol.MAX_LENGTH, Arrays.toString(name)));
        }

        // Make a new symbol for the class name.
        return Symbol.newSymbol(Arrays.toString(name));
    }
    public static InstanceKlass resolve_from_stream(ClassFileStream st,
                                                    Symbol class_name,
                                                    OopDesc class_loader, ClassLoadInfo cl_info) {
        if (cl_info.is_hidden()) {
            throw new UnsupportedOperationException();
            //return resolve_hidden_class_from_stream(st, class_name, class_loader, cl_info);
        } else {
            return resolve_class_from_stream(st, class_name, class_loader, cl_info);
        }
    }

    @SuppressWarnings("ConstantValue")
    public static InstanceKlass resolve_class_from_stream(ClassFileStream st, Symbol class_name, OopDesc class_loader, ClassLoadInfo cl_info) {
        ClassLoaderData loader_data = register_loader(class_loader);

        // Classloaders that support parallelism, e.g. bootstrap classloader,
        // do not acquire lock here

        // Parse the stream and create a klass.
        // Note that we do this even though this klass might
        // already be present in the SystemDictionary, otherwise we would not
        // throw potential ClassFormatErrors.
        InstanceKlass k = null;

//#if INCLUDE_CDS
//        if (!DumpSharedSpaces) {
//            k = SystemDictionaryShared::lookup_from_stream(class_name,
//                    class_loader,
//                    cl_info.protection_domain(),
//                    st,
//                    CHECK_NULL);
//        }
//#endif

//        if (k == null) {
//            k = KlassFactory::create_from_stream(st, class_name, loader_data, cl_info);
//        }
//        if (k==null){
//            throw new RuntimeException("no klass created");
//        }
//        Symbol h_name = k.getName();
//        if (!(class_name == null || class_name.equals(h_name))){
//            throw new RuntimeException("name mismatch");
//        }
//
//        if (((ClassLoader)class_loader.getObject()).isRegisteredAsParallelCapable()) {
//            k = find_or_define_instance_class(h_name, class_loader, k);
//        } else {
//            define_instance_class(k, class_loader);
////            if (HAS_PENDING_EXCEPTION) {
////                if (k==null){
////                    throw new RuntimeException("Must have an instance klass here!");
////                }
////                loader_data.add_to_deallocate_list(k);
////                return null;
////            }
//        }
        return k;
    }

    public void define_instance_class(InstanceKlass k, OopDesc class_loader) {

        ClassLoaderData loader_data = k.getClassLoaderData();
        //assert( loader_data->class_loader() == class_loader(), "they must be the same");
        if (!ClassLoaderData.as(class_loader.getObject()).equals(loader_data)){
            throw new IllegalArgumentException("they must be the same");
        }
        // Bootstrap and other parallel classloaders don't acquire a lock,
        // they use placeholder token.
        // If a parallelCapable class loader calls define_instance_class instead of
        // find_or_define_instance_class to get here, we have a timing
        // hole with systemDictionary updates and check_constraints
//        if (!is_parallelCapable(class_loader)) {
//            assert(ObjectSynchronizer::current_thread_holds_lock(THREAD,
//                    get_loader_lock_or_null(class_loader)),
//            "define called without lock");
//        }

        // Check class-loading constraints. Throw exception if violation is detected.
        // Grabs and releases SystemDictionary_lock
        // The check_constraints/find_class call and update_dictionary sequence
        // must be "atomic" for a specific class/classloader pair so we never
        // define two different instanceKlasses for that class/classloader pair.
        // Existing classloaders will call define_instance_class with the
        // classloader lock held
        // Parallel classloaders will call find_or_define_instance_class
        // which will require a token to perform the define class
//        Symbol  name_h = k.getName();
//        Dictionary* dictionary = loader_data->dictionary();
//        unsigned int name_hash = dictionary->compute_hash(name_h);
//        check_constraints(name_hash, k, class_loader, true,);
//
//        // Register class just loaded with class loader (placed in ArrayList)
//        // Note we do this before updating the dictionary, as this can
//        // fail with an OutOfMemoryError (if it does, we will *not* put this
//        // class in the dictionary and will not update the class hierarchy).
//        // JVMTI FollowReferences needs to find the classes this way.
//        if (k.getClassLoaderData() != null) {
//            methodHandle m(THREAD, Universe::loader_addClass_method());
//            JavaValue result(T_VOID);
//            JavaCallArguments args(class_loader);
//            args.push_oop(Handle(THREAD, k->java_mirror()));
//            JavaCalls::call(&result, m, &args, CHECK);
//        }
//
//        // Add the new class. We need recompile lock during update of CHA.
//        {
//            // Add to class hierarchy, and do possible deoptimizations.
//            add_to_hierarchy(k);
//            update_dictionary(name_hash, k, class_loader);
//        }
//        k->eager_initialize(THREAD);
    }

    public static ClassLoaderData register_loader(OopDesc class_loader) {
        return register_loader(class_loader,false);
    }

    public static ClassLoaderData register_loader(OopDesc class_loader, boolean create_mirror_cld) {
        if (create_mirror_cld) {
            throw new UnsupportedOperationException();
            //return ClassLoaderDataGraph::add(class_loader, true);
        } else {
            return (class_loader.address == 0L) ? ClassLoaderData.nullClassLoaderData :
            ClassLoaderDataGraph.find_or_create(class_loader);
        }
    }

    public static Method unpack_method_and_appendix(Object mname,
                                                    Klass accessing_klass,
                                                    Object[] appendix_box,
                                                    @RawCType("Handle*")Object[] appendix_result) {
        if (mname!=null) {
            Method m = JavaClasses.MemberName.vmtarget(mname);
            if (m != null) {
                Object appendix = appendix_box[(0)];
//                LogTarget(Info, methodhandles) lt;
//                if (lt.develop_is_enabled()) {
//                    ResourceMark rm(THREAD);
//                    LogStream ls(lt);
//                    ls.print("Linked method=" INTPTR_FORMAT ": ", p2i(m));
//                    m->print_on(&ls);
//                    if (appendix != NULL) { ls.print("appendix = "); appendix->print_on(&ls); }
//                    ls.cr();
//                }

                appendix_result[0] = appendix;
                // the target is stored in the cpCache and if a reference to this
                // MemberName is dropped we need a way to make sure the
                // class_loader containing this method is kept alive.
//                methodHandle mh(THREAD, m); // record_dependency can safepoint.
//                ClassLoaderData* this_key = accessing_klass->class_loader_data();
//                this_key->record_dependency(m->method_holder());
                return m;
            }
        }
        throw new LinkageError("bad value from MethodHandleNatives");
    }

    public static void invoke_bootstrap_method(BootstrapInfo bootstrap_specifier) {
        // Resolve the bootstrap specifier, its name, type, and static arguments
        bootstrap_specifier.resolve_bsm();

        // This should not happen.  JDK code should take care of that.
        if (bootstrap_specifier.caller() == null || bootstrap_specifier.type_arg()==null) {
            throw new InternalError("Invalid bootstrap method invocation with no caller or type argument");
        }

        boolean is_indy = bootstrap_specifier.is_method_call();
        Object[] appendix_box=null;
        if (is_indy) {
            // Some method calls may require an appendix argument.  Arrange to receive it.
            appendix_box = new Object[1];// oopFactory::new_objArray_handle(vmClasses::Object_klass(), 1, CHECK);
            //assert(appendix_box->obj_at(0) == NULL, "");
            if (appendix_box[0]!=null){
                throw new RuntimeException();
            }
        }

        // call condy: java.lang.invoke.MethodHandleNatives::linkDynamicConstant(caller, condy_index, bsm, type, info)
        //       indy: java.lang.invoke.MethodHandleNatives::linkCallSite(caller, indy_index, bsm, name, mtype, info, &appendix)
//        JavaCallArguments args;
//        args.push_oop(Handle(THREAD, bootstrap_specifier.caller_mirror()));
//        args.push_int(bootstrap_specifier.bss_index());
//        args.push_oop(bootstrap_specifier.bsm());
//        args.push_oop(bootstrap_specifier.name_arg());
//        args.push_oop(bootstrap_specifier.type_arg());
//        args.push_oop(bootstrap_specifier.arg_values());
//        if (is_indy) {
//            args.push_oop(appendix_box);
//        }
//        JavaValue result(T_OBJECT);
//        JavaCalls::call_static(&result,
//                vmClasses::MethodHandleNatives_klass(),
//                is_indy ? vmSymbols::linkCallSite_name() : vmSymbols::linkDynamicConstant_name(),
//                is_indy ? vmSymbols::linkCallSite_signature() : vmSymbols::linkDynamicConstant_signature(),
//                         &args, CHECK);
//
//        Handle value(THREAD, result.get_oop());
        Object value;
        if (is_indy){
            value= MethodHandleHelper.linkCallSite(bootstrap_specifier.caller_mirror(),
                    bootstrap_specifier.bss_index(),
                    bootstrap_specifier.bsm(),
                    bootstrap_specifier.name_arg(),
                    bootstrap_specifier.type_arg(),
                    bootstrap_specifier.arg_values(),
                    appendix_box);
        }else {
            value=MethodHandleHelper.linkDynamicConstant(bootstrap_specifier.caller_mirror(),
                    bootstrap_specifier.bss_index(),
                    bootstrap_specifier.bsm(),
                    bootstrap_specifier.name_arg(),
                    bootstrap_specifier.type_arg(),
                    bootstrap_specifier.arg_values());
        }
        if (is_indy) {
            Object[] appendix=new Object[1];
            Method method = unpack_method_and_appendix(value,
                    bootstrap_specifier.caller(),
                    appendix_box,
                    appendix);
            bootstrap_specifier.set_resolved_method(method, appendix[0]);
        } else {
            bootstrap_specifier.set_resolved_value(value);
        }

        // sanity check
        if (!(bootstrap_specifier.is_resolved() ||
                (bootstrap_specifier.is_method_call() && bootstrap_specifier.resolved_method()!=null))){
            throw new RuntimeException("bootstrap method call failed");
        }
    }

    static boolean is_always_visible_class(Class<?> mirror) {
        Klass klass = Klass.asKlass(mirror);
        if (klass instanceof ObjArrayKlass objArrayKlass) {
            klass = objArrayKlass.bottom_klass(); // check element type
        }
        if (klass instanceof TypeArrayKlass) {
            return true; // primitive array
        }
        if (!klass.isInstanceKlass()){
            throw new RuntimeException(klass.name().toString());
        }
        return klass.getAccessFlags().isPublic() &&
                (klass.asClass().getPackageName().equals(Object.class.getPackageName()) ||       // java.lang
        klass.asClass().getPackageName().equals(MethodHandle.class.getPackageName()));  // java.lang.invoke
    }

    /** Ask Java code to find or construct a java.lang.invoke.MethodType for the given signature,
     *  as interpreted relative to the given class loader. Because of class loader constraints,
     *  all method handle usage must be consistent with this loader.*/
    public static MethodType find_method_handle_type(Symbol signature,
                                                     Klass accessing_klass) {
        Object empty;
        int null_iid = VMIntrinsics.none;  // distinct from all method handle invoker intrinsics
        //We cannot use cache :(
//        @RawCType("unsigned int")int hash  = invoke_method_table()->compute_hash(signature, null_iid);
//        int          index = invoke_method_table()->hash_to_index(hash);
//        SymbolPropertyEntry* spe = invoke_method_table()->find_entry(index, hash, signature, null_iid);
//        if (spe != NULL && spe->method_type() != NULL) {
//            assert(java_lang_invoke_MethodType::is_instance(spe->method_type()), "");
//            return Handle(THREAD, spe->method_type());
//        } else if (!THREAD->can_call_java()) {
//            warning("SystemDictionary::find_method_handle_type called from compiler thread");  // FIXME
//            return Handle();  // do not attempt from within compiler, unless it was cached
//        }

        ClassLoader class_loader=null;
        ProtectionDomain protection_domain=null;
        if (accessing_klass != null) {
            class_loader      =  accessing_klass.getClassLoaderData().getClassLoader();
            protection_domain =  accessing_klass.asClass().getProtectionDomain();
        }
        boolean can_be_cached = true;
        int npts = new ArgumentCount(signature).size();
        Class<?>[] pts =new Class<?>[npts];//  oopFactory::new_objArray_handle(vmClasses::Class_klass(), , CHECK_(empty));
        int arg = 0;
        Class<?> rt=null; // the return type from the signature
        //ResourceMark rm(THREAD);
        for (SignatureStream ss=new SignatureStream(signature); !ss.is_done(); ss.next()) {
            Class<?> mirror = null;
            if (can_be_cached) {
                // Use neutral class loader to lookup candidate classes to be placed in the cache.
                mirror = ss.as_java_mirror(null, null, SignatureStream.ReturnNull);
                if (mirror == null || (ss.is_reference() && !is_always_visible_class(mirror))) {
                    // Fall back to accessing_klass context.
                    can_be_cached = false;
                }
            }
            if (!can_be_cached) {
                // Resolve, throwing a real error if it doesn't work.
                mirror = ss.as_java_mirror(class_loader, protection_domain,
                        SignatureStream.NCDFError);
            }
            if (mirror == null){
                throw new RuntimeException(ss.as_symbol().toString());
            }
            if (ss.at_return_type())
                rt = mirror;
            else
                pts[arg++]=mirror;

            // Check accessibility.
            if (!(mirror.isPrimitive()) && accessing_klass != null) {
                Klass sel_klass = Klass.asKlass(mirror);
                mirror = null;  // safety
                // Emulate ConstantPool::verify_constant_pool_resolve.
                //LinkResolver::check_klass_accessibility(accessing_klass, sel_klass, CHECK_(empty));
            }
        }
        if (arg != npts){
            throw new RuntimeException();
        }

        // call java.lang.invoke.MethodHandleNatives::findMethodHandleType(Class rt, Class[] pts) -> MethodType

        //We cannot use cache :(
//        if (can_be_cached) {
//            // We can cache this MethodType inside the JVM.
//            spe = invoke_method_table()->find_entry(index, hash, signature, null_iid);
//            if (spe == null)
//                spe = invoke_method_table()->add_entry(index, hash, signature, null_iid);
//            if (spe->method_type() == NULL) {
//                spe->set_method_type(method_type());
//            }
//        }

        // report back to the caller with the MethodType
        return MethodHandleHelper.findMethodHandleType(rt,pts);
    }
    // Find or construct the Java mirror (java.lang.Class instance) for
// the given field type signature, as interpreted relative to the
// given class loader.  Handles primitives, void, references, arrays,
// and all other reflectable types, except method types.
// N.B.  Code in reflection should use this entry point.
    public static Class<?> find_java_mirror_for_type(Symbol signature,
                                                       Klass accessing_klass,
                                                       ClassLoader class_loader,
                                                       ProtectionDomain protection_domain,
                                                       @RawCType("SignatureStream::FailureMode")int failure_mode) {
        if (!(accessing_klass == null || (class_loader==null && protection_domain==null))){
            throw new IllegalArgumentException("one or the other, or perhaps neither");
        }
        // What we have here must be a valid field descriptor,
        // and all valid field descriptors are supported.
        // Produce the same java.lang.Class that reflection reports.
        if (accessing_klass != null) {
            class_loader      = accessing_klass.getClassLoaderData().getClassLoader();
            protection_domain = accessing_klass.asClass().getProtectionDomain();
        }
        ResolvingSignatureStream ss=new ResolvingSignatureStream(signature, class_loader, protection_domain, false);
        Class<?> mirror_oop = ss.as_java_mirror(failure_mode);
        if (mirror_oop == null) {
            return null;  // report failure this way
        }
        Class<?> mirror=(mirror_oop);

        if (accessing_klass != null) {
            // Check accessibility, emulating ConstantPool::verify_constant_pool_resolve.
            Klass sel_klass = Klass.asKlass(mirror);
            //LinkResolver::check_klass_accessibility(accessing_klass, sel_klass, CHECK_NH);
        }
        return mirror;
    }

    public static Class<?>    find_java_mirror_for_type(Symbol signature,
                                               Klass accessing_klass,
                                               @RawCType("SignatureStream::FailureMode")int failure_mode) {
        // callee will fill in CL/PD from AK, if they are needed
        return find_java_mirror_for_type(signature, accessing_klass, null,null, failure_mode);
    }

    // Ask Java code to find or construct a method handle constant.
    public static MethodHandle link_method_handle_constant(Klass caller,
                                                         int ref_kind, //e.g., JVM_REF_invokeVirtual
                                                         Klass callee,
                                                         Symbol name,
                                                         Symbol signature) {
        if (caller == null) {
            throw new InternalError("bad MH constant");
        }
        String name_str      = name.toString();
        String signature_str = signature.toString();

        // Put symbolic info from the MH constant into freshly created MemberName and resolve it.
        Object mname = ClassHelperSpecial.allocateInstance(JavaClasses.MemberName.clazz);
        JavaClasses.MemberName.set_clazz(mname, callee.asClass());
        JavaClasses.MemberName.set_name (mname, name_str);
        JavaClasses.MemberName.set_type (mname, signature_str);
        JavaClasses.MemberName.set_flags(mname, MethodHandles.ref_kind_to_flags(ref_kind));

        if (ref_kind == ClassConstants.JVM_REF_invokeVirtual &&
                MethodHandles.is_signature_polymorphic_public_name(callee, name)) {
            // Skip resolution for public signature polymorphic methods such as
            // j.l.i.MethodHandle.invoke()/invokeExact() and those on VarHandle
            // They require appendix argument which MemberName resolution doesn't handle.
            // There's special logic on JDK side to handle them
            // (see MethodHandles.linkMethodHandleConstant() and MethodHandles.findVirtualForMH()).
        } else {
            mname=MethodHandles.resolve_MemberName(mname, caller.asClass(), 0, false /*speculative_resolve*/);
        }

        // After method/field resolution succeeded, it's safe to resolve MH signature as well.
        Object type = MethodHandles.resolve_MemberName_type(mname, caller);

        // call java.lang.invoke.MethodHandleNatives::linkMethodHandleConstant(Class caller, int refKind, Class callee, String name, Object type) -> MethodHandle
//        JavaCallArguments args;
//        args.push_oop(Handle(THREAD, caller->java_mirror()));  // the referring class
//        args.push_int(ref_kind);
//        args.push_oop(Handle(THREAD, callee->java_mirror()));  // the target class
//        args.push_oop(name_str);
//        args.push_oop(type);
//        JavaValue result(T_OBJECT);
//        JavaCalls::call_static(&result,
//                vmClasses::MethodHandleNatives_klass(),
//                vmSymbols::linkMethodHandleConstant_name(),
//                vmSymbols::linkMethodHandleConstant_signature(),
//                         &args, CHECK_(empty));
        return MethodHandleHelper.linkMethodHandleConstant(caller.asClass(),ref_kind,callee.asClass(),name_str,type);
    }

    public static Class<?> find_field_handle_type(Symbol signature,
                                                    Klass accessing_klass) {
        SignatureStream ss=new SignatureStream(signature, /*is_method=*/ false);
        if (!ss.is_done()) {
            ClassLoader class_loader=null;
            ProtectionDomain protection_domain=null;
            if (accessing_klass != null) {
                class_loader      = accessing_klass.getClassLoaderData().getClassLoader();
                protection_domain = accessing_klass.asClass().getProtectionDomain();
            }
            Class<?> mirror = ss.as_java_mirror(class_loader, protection_domain, SignatureStream.NCDFError);
            ss.next();
            if (ss.is_done()) {
                return mirror;
            }
        }
        return null;
    }

    public static boolean is_nonpublic_Object_method(Method m){
        if (m==null){
            throw new RuntimeException("Unexpected NULL Method*");
        }
        return !m.is_public() && m.method_holder().equals(VMClasses.objectKlass());
    }
}
