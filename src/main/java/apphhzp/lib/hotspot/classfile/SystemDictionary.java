package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oop.*;

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
            return (class_loader.address == 0L) ? ClassLoaderData.null_class_loader_data :
            ClassLoaderDataGraph.find_or_create(class_loader);
        }
    }
}
