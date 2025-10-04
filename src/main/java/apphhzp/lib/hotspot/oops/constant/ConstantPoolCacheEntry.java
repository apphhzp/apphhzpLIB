package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.classfile.VMClasses;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.interpreter.CallInfo;
import apphhzp.lib.hotspot.oops.Metadata;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.runtime.VMVersion;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.TosState;

import static apphhzp.lib.ClassHelperSpecial.internalUnsafe;
import static apphhzp.lib.ClassHelperSpecial.unsafe;

// _flags     [tos|0|F=1|0|0|0|f|v|0 |0000|field_index] (for field entries)
// bit length [ 4 |1| 1 |1|1|1|1|1|1 |1     |-3-|----16-----]
// _flags     [tos|0|F=0|S|A|I|f|0|vf|indy_rf|000|00000|psize] (for method entries)
// bit length [ 4 |1| 1 |1|1|1|1|1|1 |-4--|--8--|--8--]
public class ConstantPoolCacheEntry extends JVMObject {
    public static final Type TYPE = JVM.type("ConstantPoolCacheEntry");
    public static final int SIZE = TYPE.size;
    public static final long INDICES_OFFSET = TYPE.offset("_indices");
    public static final long F1_OFFSET = TYPE.offset("_f1");
    public static final long F2_OFFSET = TYPE.offset("_f2");
    public static final long FLAGS_OFFSET = TYPE.offset("_flags");
    public static final int
            tos_state_bits = 4,
            tos_state_mask = JVM.right_n_bits(tos_state_bits),
            tos_state_shift = JVM.intConstant("ConstantPoolCacheEntry::tos_state_shift"),
            is_field_entry_shift = JVM.intConstant("ConstantPoolCacheEntry::is_field_entry_shift"),
            has_local_signature_shift = 25,
            has_appendix_shift = 24,
            is_forced_virtual_shift = JVM.intConstant("ConstantPoolCacheEntry::is_forced_virtual_shift"),
            is_final_shift = JVM.intConstant("ConstantPoolCacheEntry::is_final_shift"),
            is_volatile_shift = JVM.intConstant("ConstantPoolCacheEntry::is_volatile_shift"),
            is_vfinal_shift = JVM.intConstant("ConstantPoolCacheEntry::is_vfinal_shift"),
            indy_resolution_failed_shift = 19, // (indy_rf) did call site specifier resolution fail ?
    // low order bits give field index (for FieldInfo) or method parameter size:
    field_index_bits = 16,
            field_index_mask = JVM.right_n_bits(field_index_bits),
            parameter_size_bits = 8,  // subset of field_index_mask, range is 0..255
            parameter_size_mask = JVM.right_n_bits(parameter_size_bits),
            option_bits_mask = ~(((~0) << tos_state_shift) | (field_index_mask | parameter_size_mask));

    public static final int // specific bit definitions for the indices field:
            cp_index_bits = 2 * JVM.BitsPerByte,
            cp_index_mask = JVM.right_n_bits(cp_index_bits),
            bytecode_1_shift = cp_index_bits,
            bytecode_1_mask = JVM.right_n_bits(JVM.BitsPerByte), // == (u1)0xFF
            bytecode_2_shift = cp_index_bits + JVM.BitsPerByte,
            bytecode_2_mask = JVM.right_n_bits(JVM.BitsPerByte)  // == (u1)0xFF
                    ;
    public final ConstantPoolCache holder;

    public ConstantPoolCacheEntry(long addr, ConstantPoolCache cache) {
        super(addr);
        this.holder = cache;
    }


    public void set_bytecode_1(@RawCType("Bytecodes::Code")int code){
        // Need to flush pending stores here before bytecode is written.
        internalUnsafe.putAddressRelease(this.address+INDICES_OFFSET,
                unsafe.getAddress(this.address+INDICES_OFFSET) | ((code&0xff) << bytecode_1_shift));
    }
    public void set_bytecode_2(@RawCType("Bytecodes::Code")int code){
        // Need to flush pending stores here before bytecode is written.
        //noinspection IntegerMultiplicationImplicitCastToLong
        internalUnsafe.putAddressRelease(this.address+INDICES_OFFSET,
                unsafe.getAddress(this.address+INDICES_OFFSET) | ((code&0xff) << bytecode_2_shift));
    }
    public void set_f1(Metadata f1){
        unsafe.putAddress(this.address+F1_OFFSET,f1==null?0L:f1.address);
    }
    public void release_set_f1(Metadata f1){
        internalUnsafe.putAddressRelease(this.address+F1_OFFSET,f1==null?0L: f1.address);
    }
    public void set_f2(@RawCType("intx")long f2) {
        unsafe.putAddress(this.address+F2_OFFSET,f2);
    }
    public void set_f2_as_vfinal_method(Method f2){
        if (!is_vfinal()){
            throw new RuntimeException("flags must be set");
        }
        set_f2(f2==null?0L:f2.address);
    }
    public int make_flags(@RawCType("TosState")int state, int option_bits, int field_index_or_method_params){
        if (!(state < TosState.number_of_states)){
            throw new IllegalArgumentException("Invalid state in make_flags");
        }
        int f = (state << tos_state_shift) | option_bits | field_index_or_method_params;
        return (int) (unsafe.getAddress(this.address+FLAGS_OFFSET) | f);
    }
    public void set_flags(@RawCType("intx")long flags){
        unsafe.putAddress(this.address+FLAGS_OFFSET,flags);
    }
    public void set_field_flags(@RawCType("TosState")int field_type, int option_bits, int field_index) {
        if (!((field_index & field_index_mask) == field_index)){
            throw new IllegalStateException("field_index in range");
        }
        set_flags(make_flags(field_type, option_bits | (1 << is_field_entry_shift), field_index));
    }
    public void set_method_flags(@RawCType("TosState")int return_type, int option_bits, int method_params) {
        if (!((method_params & parameter_size_mask) == method_params)){
            throw new IllegalStateException("method_params in range");
        }
        set_flags(make_flags(return_type, option_bits, method_params));
    }


    public void set_field(// sets entry to resolved field state
    @RawCType("Bytecodes::Code")int get_code,                    // the bytecode used for reading the field
    @RawCType("Bytecodes::Code")int put_code,                    // the bytecode used for writing the field
    Klass          field_holder,                // the object/klass holding the field
    int             field_index,            // the original field index in the field holder
    int             field_offset,                // the field offset in words in the field holder
    @RawCType("TosState")int        field_type,                  // the (machine) field type
    boolean            is_final,                    // the field is final
    boolean            is_volatile                  // the field is volatile
    ){
        set_f1(field_holder);
        set_f2(field_offset);
        if (!((field_index & field_index_mask) == field_index)){
            throw new IllegalArgumentException("field index does not fit in low flag bits");
        }
        set_field_flags(field_type,
                ((is_volatile ? 1 : 0) << is_volatile_shift) |
                        ((is_final    ? 1 : 0) << is_final_shift),
                field_index);
        set_bytecode_1(get_code);
        set_bytecode_2(put_code);
        //NOT_PRODUCT(verify(tty));
    }


    public void set_direct_or_vtable_call(
            @RawCType("Bytecodes::Code")int invoke_code,                 // the bytecode used for invoking the method
            Method method,                  // the method/prototype if any (NULL, otherwise)
            int             vtable_index,                // the vtable index if any, else negative
            boolean            sender_is_interface){
        boolean is_vtable_call = (vtable_index >= 0);  // FIXME: split this method on this boolean
        if (method.interpreter_entry() == 0L){
            throw new RuntimeException("should have been set at this point");
        }
        if (method.is_obsolete()){
            throw new RuntimeException("attempt to write obsolete method to cpCache");
        }

        int byte_no = -1;
        boolean change_to_virtual = false;
        InstanceKlass holder = null;  // have to declare this outside the switch
        switch (invoke_code) {
            case Bytecodes.Code._invokeinterface:
                holder = method.method_holder();
                // check for private interface method invocations
                if (vtable_index == Method.VtableIndexFlag.nonvirtual_vtable_index && holder.isInterface() ) {
                    if (!(method.is_private())){
                        throw new RuntimeException("unexpected non-private method");
                    }
                    if (!(method.can_be_statically_bound())){
                        throw new RuntimeException("unexpected non-statically-bound method");
                    }
                    // set_f2_as_vfinal_method checks if is_vfinal flag is true.
                    set_method_flags(TosState.as_TosState(method.result_type()),
                            (                             1      << is_vfinal_shift) |
                                    ((method.is_final_method() ? 1 : 0) << is_final_shift),
                            method.size_of_parameters());
                    set_f2_as_vfinal_method(method);
                    byte_no = 2;
                    set_f1(holder); // interface klass*
                    break;
                }
                else {
                    // We get here from InterpreterRuntime::resolve_invoke when an invokeinterface
                    // instruction links to a non-interface method (in Object). This can happen when
                    // an interface redeclares an Object method (like CharSequence declaring toString())
                    // or when invokeinterface is used explicitly.
                    // In that case, the method has no itable index and must be invoked as a virtual.
                    // Set a flag to keep track of this corner case.

                    if (!(holder.isInterface() || holder.equals(VMClasses.objectKlass()))){
                        throw new RuntimeException("unexpected holder class");
                    }
                    if (!method.is_public()){
                        throw new RuntimeException("Calling non-public method in Object with invokeinterface");
                    }
                    change_to_virtual = true;

                    // ...and fall through as if we were handling invokevirtual:
                }
            case Bytecodes.Code._invokevirtual:
            {
                if (!is_vtable_call) {
                    if (!method.can_be_statically_bound()){
                        throw new RuntimeException();
                    }
                    // set_f2_as_vfinal_method checks if is_vfinal flag is true.
                    set_method_flags(TosState.as_TosState(method.result_type()),
                            (                             1      << is_vfinal_shift) |
                                    ((method.is_final_method() ? 1 : 0) << is_final_shift)  |
                                    ((change_to_virtual         ? 1 : 0) << is_forced_virtual_shift),
                            method.size_of_parameters());
                    set_f2_as_vfinal_method(method);
                } else {
                    if (method.can_be_statically_bound()){
                        throw new RuntimeException();
                    }
                    if (!(vtable_index >= 0)){
                        throw new RuntimeException("valid index");
                    }
                    if (method.is_final_method()){
                        throw new RuntimeException("sanity");
                    }
                    set_method_flags(TosState.as_TosState(method.result_type()),
                            ((change_to_virtual ? 1 : 0) << is_forced_virtual_shift),
                            method.size_of_parameters());
                    set_f2(vtable_index);
                }
                byte_no = 2;
                break;
            }

            case Bytecodes.Code._invokespecial:
            case Bytecodes.Code._invokestatic:
                if (is_vtable_call){
                    throw new RuntimeException();
                }
                // Note:  Read and preserve the value of the is_vfinal flag on any
                // invokevirtual bytecode shared with this constant pool cache entry.
                // It is cheap and safe to consult is_vfinal() at all times.
                // Once is_vfinal is set, it must stay that way, lest we get a dangling oop.
                set_method_flags(TosState.as_TosState(method.result_type()),
                        ((is_vfinal()               ? 1 : 0) << is_vfinal_shift) |
                                ((method.is_final_method() ? 1 : 0) << is_final_shift),
                        method.size_of_parameters());
                set_f1(method);
                byte_no = 1;
                break;
            default:
                throw new RuntimeException("ShouldNotReachHere()");
        }

        // Note:  byte_no also appears in TemplateTable::resolve.
        if (byte_no == 1) {
            boolean do_resolve = true;
            // Don't mark invokespecial to method as resolved if sender is an interface.  The receiver
            // has to be checked that it is a subclass of the current class every time this bytecode
            // is executed.
            if (invoke_code == Bytecodes.Code._invokespecial && sender_is_interface &&
                    !method.name().equals(Symbol.getVMSymbol("<init>"))) {
                do_resolve = false;
            }
            if (invoke_code == Bytecodes.Code._invokestatic) {
                if (!(method.method_holder().is_initialized() ||
                        method.method_holder().is_reentrant_initialization(JavaThread.current()))){
                    throw new RuntimeException("invalid class initialization state for invoke_static");
                }
                if (!VMVersion.supports_fast_class_init_checks() && method.needs_clinit_barrier()) {
                    // Don't mark invokestatic to method as resolved if the holder class has not yet completed
                    // initialization. An invokestatic must only proceed if the class is initialized, but if
                    // we resolve it before then that class initialization check is skipped.
                    //
                    // When fast class initialization checks are supported (VM_Version::supports_fast_class_init_checks() == true),
                    // template interpreter supports fast class initialization check for
                    // invokestatic which doesn't require call site re-resolution to
                    // enforce class initialization barrier.
                    do_resolve = false;
                }
            }
            if (do_resolve) {
                set_bytecode_1(invoke_code);
            }
        } else {
            if (change_to_virtual) {
                // NOTE: THIS IS A HACK - BE VERY CAREFUL!!!
                //
                // Workaround for the case where we encounter an invokeinterface, but we
                // should really have an _invokevirtual since the resolved method is a
                // virtual method in java.lang.Object. This is a corner case in the spec
                // but is presumably legal. javac does not generate this code.
                //
                // We do not set bytecode_1() to _invokeinterface, because that is the
                // bytecode # used by the interpreter to see if it is resolved.  In this
                // case, the method gets reresolved with caller for each interface call
                // because the actual selected method may not be public.
                //
                // We set bytecode_2() to _invokevirtual.
                // See also interpreterRuntime.cpp. (8/25/2000)
            } else {
                if (!(invoke_code == Bytecodes.Code._invokevirtual ||
                        (invoke_code == Bytecodes.Code._invokeinterface &&
                                ((method.is_private() ||
                                        (method.is_final() && method.method_holder().equals(VMClasses.objectKlass()))))))){
                    throw new RuntimeException("unexpected invocation mode");
                }
                if (invoke_code == Bytecodes.Code._invokeinterface &&
                        (method.is_private() || method.is_final())) {
                    // We set bytecode_1() to _invokeinterface, because that is the
                    // bytecode # used by the interpreter to see if it is resolved.
                    // We set bytecode_2() to _invokevirtual.
                    set_bytecode_1(invoke_code);
                }
            }
            // set up for invokevirtual, even if linking for invokeinterface also:
            set_bytecode_2(Bytecodes.Code._invokevirtual);
        }
        //NOT_PRODUCT(verify(tty));
    }


    public void set_direct_call(// sets entry to exact concrete method entry
@RawCType("Bytecodes::Code")int invoke_code,                 // the bytecode used for invoking the method
Method method,                  // the method to call
boolean sender_is_interface
    ){
        int index = Method.VtableIndexFlag.nonvirtual_vtable_index;
        // index < 0; FIXME: inline and customize set_direct_or_vtable_call
        set_direct_or_vtable_call(invoke_code, method, index, sender_is_interface);
    }

    public void set_vtable_call(                          // sets entry to vtable index
                                                   @RawCType("Bytecodes::Code")int invoke_code,                 // the bytecode used for invoking the method
    Method method,                  // resolved method which declares the vtable index
                                                   int             vtable_index                 // the vtable index
    ){
        // either the method is a miranda or its holder should accept the given index
        if (!(method.method_holder().isInterface() || method.method_holder().verify_vtable_index(vtable_index))){
            throw new RuntimeException();
        }
        // index >= 0; FIXME: inline and customize set_direct_or_vtable_call
        set_direct_or_vtable_call(invoke_code, method, vtable_index, false);
    }

    public void set_itable_call(
            @RawCType("Bytecodes::Code")int invoke_code,                 // the bytecode used; must be invokeinterface
            Klass referenced_klass,                     // the referenced klass in the InterfaceMethodref
            Method method,                  // the resolved interface method
            int index                             // index into itable for the method
    ){
        if (!(method.method_holder().verify_itable_index(index))){
            throw new RuntimeException();
        }
        if (!(invoke_code == Bytecodes.Code._invokeinterface)){
            throw new RuntimeException();
        }
        InstanceKlass interf = method.method_holder();
        if (!interf.isInterface()){
            throw new RuntimeException("must be an interface");
        }
        if (method.is_final_method()){
            throw new RuntimeException("interfaces do not have final methods; cannot link to one here");
        }
        set_f1(referenced_klass);
        set_f2(method.address);
        set_method_flags(TosState.as_TosState(method.result_type()),
                0,  // no option bits
                method.size_of_parameters());
        set_bytecode_1(Bytecodes.Code._invokeinterface);
    }

    public void set_method_handle(
    ConstantPool cpool,             // holding constant pool (required for locking)
     CallInfo call_info                    // Call link information
    ){
        set_method_handle_common(cpool, Bytecodes.Code._invokehandle, call_info);
    }

    public void set_dynamic_call(
            ConstantPool cpool,             // holding constant pool (required for locking)
            CallInfo call_info                    // Call link information
    ){
        set_method_handle_common(cpool, Bytecodes.Code._invokedynamic, call_info);
    }

    // Common code for invokedynamic and MH invocations.

    // The "appendix" is an optional call-site-specific parameter which is
    // pushed by the JVM at the end of the argument list.  This argument may
    // be a MethodType for the MH.invokes and a CallSite for an invokedynamic
    // instruction.  However, its exact type and use depends on the Java upcall,
    // which simply returns a compiled LambdaForm along with any reference
    // that LambdaForm needs to complete the call.  If the upcall returns a
    // null appendix, the argument is not passed at all.
    //
    // The appendix is *not* represented in the signature of the symbolic
    // reference for the call site, but (if present) it *is* represented in
    // the Method* bound to the site.  This means that static and dynamic
    // resolution logic needs to make slightly different assessments about the
    // number and types of arguments.
    public void set_method_handle_common(
            ConstantPool cpool,                    // holding constant pool (required for locking)
    @RawCType("Bytecodes::Code")int invoke_code,                 // _invokehandle or _invokedynamic
     CallInfo call_info                    // Call link information
    ){
// NOTE: This CPCE can be the subject of data races.
        // There are three words to update: flags, refs[f2], f1 (in that order).
        // Writers must store all other values before f1.
        // Readers must test f1 first for non-null before reading other fields.
        // Competing writers must acquire exclusive access via a lock.
        // A losing writer waits on the lock until the winner writes f1 and leaves
        // the lock, so that when the losing writer returns, he can use the linked
        // cache entry.

        Object[] resolved_references=(cpool.resolved_references());
        // Use the resolved_references() lock for this cpCache entry.
        // resolved_references are created for all classes with Invokedynamic, MethodHandle
        // or MethodType constant pool cache entries.
        if (resolved_references == null){
            throw new RuntimeException("a resolved_references array should have been created for this class");
        }
//        ObjectLocker ol(resolved_references, current);
        if (!is_f1_null()) {
            return;
        }

        if (indy_resolution_failed()) {
            // Before we got here, another thread got a LinkageError exception during
            // resolution.  Ignore our success and throw their exception.
            ConstantPoolCache cpCache = cpool.getCache();
            int index = -1;
            for (int i = 0; i < cpCache.getLength(); i++) {
                if (cpCache.entry_at(i).equals(this)) {
                    index = i;
                    break;
                }
            }
            if (!(index >= 0)){
                throw new RuntimeException("Didn't find cpCache entry!");
            }
            throw new RuntimeException("ConstantPool.throw_resolution_error");
//            int encoded_index = ResolutionErrorTable::encode_cpcache_index(
//                    ConstantPool.encode_invokedynamic_index(index));
//            JavaThread* THREAD = JavaThread::current(); // For exception macros.
//            ConstantPool.throw_resolution_error(cpool, encoded_index, THREAD);
//            return;
        }

        Method adapter            = call_info.resolved_method();
        Oop appendix      = call_info.resolved_appendix();
        boolean has_appendix    = appendix==null||appendix.get()==null;

        // Write the flags.
        // MHs and indy are always sig-poly and have a local signature.
        set_method_flags(TosState.as_TosState(adapter.result_type()),
                ((has_appendix    ? 1 : 0) << has_appendix_shift        ) |
                        (                   1      << has_local_signature_shift ) |
                        (                   1      << is_final_shift            ),
                adapter.size_of_parameters());

//        LogStream* log_stream = NULL;
//        LogStreamHandle(Debug, methodhandles, indy) lsh_indy;
//        if (lsh_indy.is_enabled()) {
//            ResourceMark rm;
//            log_stream = &lsh_indy;
//            log_stream->print_cr("set_method_handle bc=%d appendix=" PTR_FORMAT "%s method=" PTR_FORMAT " (local signature) ",
//                    invoke_code,
//                    p2i(appendix()),
//                    (has_appendix ? "" : " (unused)"),
//                    p2i(adapter));
//            adapter->print_on(log_stream);
//            if (has_appendix)  appendix()->print_on(log_stream);
//        }

        // Method handle invokes and invokedynamic sites use both cp cache words.
        // refs[f2], if not null, contains a value passed as a trailing argument to the adapter.
        // In the general case, this could be the call site's MethodType,
        // for use with java.lang.Invokers.checkExactType, or else a CallSite object.
        // f1 contains the adapter method which manages the actual call.
        // In the general case, this is a compiled LambdaForm.
        // (The Java code is free to optimize these calls by binding other
        // sorts of methods and appendices to call sites.)
        // JVM-level linking is via f1, as if for invokespecial, and signatures are erased.
        // The appendix argument (if any) is added to the signature, and is counted in the parameter_size bits.
        // Even with the appendix, the method will never take more than 255 parameter slots.
        //
        // This means that given a call site like (List)mh.invoke("foo"),
        // the f1 method has signature '(Ljl/Object;Ljl/invoke/MethodType;)Ljl/Object;',
        // not '(Ljava/lang/String;)Ljava/util/List;'.
        // The fact that String and List are involved is encoded in the MethodType in refs[f2].
        // This allows us to create fewer Methods, while keeping type safety.
        //

        // Store appendix, if any.
        if (has_appendix) {
            final int appendix_index = f2_as_index();
            if (!(appendix_index >= 0 && appendix_index < resolved_references.length)){
                throw new IndexOutOfBoundsException("oob");
            }
            if (!(resolved_references[(appendix_index)] == null)){
                throw new RuntimeException("init just once");
            }
            resolved_references[appendix_index]=appendix.getJavaObject();
        }

        release_set_f1(adapter);  // This must be the last one to set (see NOTE above)!

        // The interpreter assembly code does not check byte_2,
        // but it is used by is_resolved, method_if_resolved, etc.
        set_bytecode_1(invoke_code);
//        NOT_PRODUCT(verify(tty));

//        if (log_stream != NULL) {
//            this->print(log_stream, 0);
//        }

        if (!(has_appendix == this.has_appendix())){
            throw new RuntimeException("proper storage of appendix flag");
        }
        if (!(this.has_local_signature())){
            throw new RuntimeException("proper storage of signature flag");
        }
    }

    // Return TRUE if resolution failed and this thread got to record the failure
    // status.  Return FALSE if another thread succeeded or failed in resolving
    // the method and recorded the success or failure before this thread had a
    // chance to record its failure.
    public boolean save_and_throw_indy_exc(ConstantPool cpool, int cpool_index,
                                 int index, @RawCType("constantTag")int tag){
        throw new UnsupportedOperationException("");
//        assert(HAS_PENDING_EXCEPTION, "No exception got thrown!");
//        assert(PENDING_EXCEPTION->is_a(vmClasses::LinkageError_klass()),
//        "No LinkageError exception");
//
//        // Use the resolved_references() lock for this cpCache entry.
//        // resolved_references are created for all classes with Invokedynamic, MethodHandle
//        // or MethodType constant pool cache entries.
//        JavaThread* current = THREAD;
//        objArrayHandle resolved_references(current, cpool->resolved_references());
//        assert(resolved_references() != NULL,
//        "a resolved_references array should have been created for this class");
//        ObjectLocker ol(resolved_references, current);
//
//        // if f1 is not null or the indy_resolution_failed flag is set then another
//        // thread either succeeded in resolving the method or got a LinkageError
//        // exception, before this thread was able to record its failure.  So, clear
//        // this thread's exception and return false so caller can use the earlier
//        // thread's result.
//        if (!is_f1_null() || indy_resolution_failed()) {
//            CLEAR_PENDING_EXCEPTION;
//            return false;
//        }
//        ResourceMark rm(THREAD);
//        Symbol* error = PENDING_EXCEPTION->klass()->name();
//  const char* message = java_lang_Throwable::message_as_utf8(PENDING_EXCEPTION);
//
//        SystemDictionary::add_resolution_error(cpool, index, error, message);
//        set_indy_resolution_failed();
//        return true;
    }

    // invokedynamic and invokehandle call sites have an "appendix" item in the
    // resolved references array.
    public Method method_if_resolved(ConstantPool cpool){
        // Decode the action of set_method and set_interface_call
        @RawCType("Bytecodes::Code")int invoke_code = bytecode_1();
        if (invoke_code != 0) {
            Metadata f1 = f1_ord();
            if (f1 != null) {
                switch (invoke_code) {
                    case Bytecodes.Code._invokeinterface:
                        return f2_as_interface_method();
                    case Bytecodes.Code._invokestatic:
                    case Bytecodes.Code._invokespecial:
                        if (has_appendix()){
                            throw new RuntimeException();
                        }
                    case Bytecodes.Code._invokehandle:
                    case Bytecodes.Code._invokedynamic:
                        return Method.getOrCreate(f1.address);
                    default:
                        break;
                }
            }
        }
        invoke_code = bytecode_2();
        if (invoke_code != 0) {
            switch (invoke_code) {
                case Bytecodes.Code._invokevirtual:
                    if (is_vfinal()) {
                        // invokevirtual
                        Method m = f2_as_vfinal_method();
                        return m;
                    } else {
                        int holder_index = cpool.uncached_klass_ref_index_at(constant_pool_index());
                        if (cpool.tag_at(holder_index)==ConstantTag.Class){
                            Klass klass = cpool.resolved_klass_at(holder_index);
                            return klass.method_at_vtable(f2_as_index());
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return null;
    }
    public Object appendix_if_resolved(ConstantPool cpool){
        if (!has_appendix())
            return null;
        final int ref_index = f2_as_index();
        Object[] resolved_references = cpool.resolved_references();
        return resolved_references[(ref_index)];
    }

    public void set_parameter_size(int value){
//        // This routine is called only in corner cases where the CPCE is not yet initialized.
//        // See AbstractInterpreter::deopt_continue_after_entry.
//        assert(_flags == 0 || parameter_size() == 0 || parameter_size() == value,
//        "size must not change: parameter_size=%d, value=%d", parameter_size(), value);
        // Setting the parameter size by itself is only safe if the
        // current value of _flags is 0, otherwise another thread may have
        // updated it and we don't want to overwrite that value.  Don't
        // bother trying to update it once it's nonzero but always make
        // sure that the final parameter size agrees with what was passed.
        if (unsafe.getAddress(this.address+FLAGS_OFFSET) == 0) {
            @RawCType("intx")long newflags = (value & parameter_size_mask);
            unsafe.putAddress(this.address+FLAGS_OFFSET,newflags);
        }
        if (!(parameter_size() == value)){
            throw new RuntimeException("size must not change: parameter_size="+parameter_size()+", value="+value);
        }
    }

    // Which bytecode number (1 or 2) in the index field is valid for this bytecode?
    // Returns -1 if neither is valid.
    public static int bytecode_number(@RawCType("Bytecodes::Code")int code) {
        switch (code) {
            case Bytecodes.Code._getstatic       :    // fall through
            case Bytecodes.Code._getfield        :    // fall through
            case Bytecodes.Code._invokespecial   :    // fall through
            case Bytecodes.Code._invokestatic    :    // fall through
            case Bytecodes.Code._invokehandle    :    // fall through
            case Bytecodes.Code._invokedynamic   :    // fall through
            case Bytecodes.Code._invokeinterface : return 1;
            case Bytecodes.Code._putstatic       :    // fall through
            case Bytecodes.Code._putfield        :    // fall through
            case Bytecodes.Code._invokevirtual   : return 2;
            default                          : break;
        }
        return -1;
    }

    // Has this bytecode been resolved? Only valid for invokes and get/put field/static.
    public boolean is_resolved(@RawCType("Bytecodes::Code")int code){
        switch (bytecode_number(code)) {
            case 1:  return (bytecode_1() == code);
            case 2:  return (bytecode_2() == code);
        }
        return false;      // default: not resolved
    }


    // Accessors

    /* bit number |31                0|
     * bit length |-8--|-8--|---16----|
     * --------------------------------
     * _indices   [ b2 | b1 |  index  ]  index = constant_pool_index
     */
    @SuppressWarnings("GrazieInspection")
    public int indices() {
        return (int) unsafe.getAddress(this.address + INDICES_OFFSET);
    }

    public int indices_ord() {
        return (int) internalUnsafe.getAddressAcquire(this.address + INDICES_OFFSET);
    }

    public @RawCType("Bytecodes::Code") int bytecode_1() {
        return ((this.indices() >> bytecode_1_shift) & bytecode_1_mask);
    }

    public @RawCType("Bytecodes::Code") int bytecode_2() {
        return ((this.indices() >> bytecode_2_shift) & bytecode_2_mask);
    }

    public int constant_pool_index() {
        return (this.indices() & cp_index_mask);
    }

    public Metadata f1_ord() {
        return Metadata.getMetadata(internalUnsafe.getAddressAcquire(this.address + F1_OFFSET));
    }

    public Method f1_as_method() {
        Metadata f1 = f1_ord();
        if (!(f1 == null || f1 instanceof Method)) {
            throw new ClassCastException();
        }
        return (Method) f1;
    }

    public Klass f1_as_klass() {
        Metadata f1 = f1_ord();
        if (!(f1 == null || f1 instanceof Klass)) {
            throw new ClassCastException();
        }
        return (Klass) f1;
    }

    // Use the accessor f1() to acquire _f1's value. This is needed for
    // example in BytecodeInterpreter::run(), where is_f1_null() is
    // called to check if an invokedynamic call is resolved. This load
    // of _f1 must be ordered with the loads performed by
    // cache->main_entry_index().
    public boolean is_f1_null() {// classifies a CPC entry as unbound
        Metadata f1 = f1_ord();
        return f1 == null;
    }

    public int f2_as_index() {
        if (is_vfinal()) {
            throw new IllegalStateException();
        }
        return (int) unsafe.getAddress(this.address + F2_OFFSET);
    }

    public Method f2_as_vfinal_method() {
        if (!is_vfinal()) {
            throw new IllegalStateException();
        }
        return Method.getOrCreate(unsafe.getAddress(this.address + F2_OFFSET));
    }

    public Method f2_as_interface_method() {
        if (!(bytecode_1() == Bytecodes.Code._invokeinterface)) {
            throw new IllegalStateException();
        }
        return Method.getOrCreate(unsafe.getAddress(this.address + F2_OFFSET));
    }

    public @RawCType("intx") long flags_ord() {
        return internalUnsafe.getAddressAcquire(this.address + FLAGS_OFFSET);
    }

    public int field_index() {
        if (!is_field_entry()){
            throw new IllegalStateException();
        }
        return (int) (unsafe.getAddress(this.address + FLAGS_OFFSET) & field_index_mask);
    }

    public int parameter_size() {
        if (!is_method_entry()){
            throw new IllegalStateException();
        }
        return (int) (unsafe.getAddress(this.address + FLAGS_OFFSET) & parameter_size_mask);
    }

    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    public boolean is_volatile() {
        return (unsafe.getAddress(this.address + FLAGS_OFFSET) & (1 << is_volatile_shift)) != 0;
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    public boolean is_final() {
        return (unsafe.getAddress(this.address + FLAGS_OFFSET) & (1 << is_final_shift)) != 0;
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    public boolean is_forced_virtual() {
        return (unsafe.getAddress(this.address + FLAGS_OFFSET) & (1 << is_forced_virtual_shift)) != 0;
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    public boolean is_vfinal() {
        return (unsafe.getAddress(this.address + FLAGS_OFFSET) & (1 << is_vfinal_shift)) != 0;
    }

    public boolean indy_resolution_failed(){
        @RawCType("intx")long flags = flags_ord();
        return (flags & (1 << indy_resolution_failed_shift)) != 0;
    }

    public boolean has_appendix(){
        return (!is_f1_null()) && (unsafe.getAddress(this.address+FLAGS_OFFSET) & (1 << has_appendix_shift)) != 0;
    }

    public boolean has_local_signature(){
        return (!is_f1_null()) && (unsafe.getAddress(this.address+FLAGS_OFFSET) & (1 << has_local_signature_shift)) != 0;
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    public boolean is_method_entry() {
        return (unsafe.getAddress(this.address + FLAGS_OFFSET) & (1 << is_field_entry_shift)) == 0;
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    public boolean is_field_entry() {
        return (unsafe.getAddress(this.address + FLAGS_OFFSET) & (1 << is_field_entry_shift)) != 0;
    }

    public boolean is_long() {
        return flag_state() == TosState.ltos;
    }

    public boolean is_double() {
        return flag_state() == TosState.dtos;
    }

    public @RawCType("TosState") int flag_state() {
        //assert((uint)number_of_states <= (uint)tos_state_mask+1, "");
        return (int) ((unsafe.getAddress(this.address + FLAGS_OFFSET) >> tos_state_shift) & tos_state_mask);
    }

    public void set_indy_resolution_failed(){
        internalUnsafe.putAddressRelease(this.address+FLAGS_OFFSET,unsafe.getAddress(this.address + FLAGS_OFFSET)  | (1 << indy_resolution_failed_shift));
    }

    public void initialize_entry(int index) {
        if (!(0 < index && index < 0x10000)) {
            throw new RuntimeException("sanity check");
        }
        unsafe.putAddress(this.address + INDICES_OFFSET, index);
        unsafe.putAddress(this.address + F1_OFFSET, 0L);
        unsafe.putAddress(this.address + F2_OFFSET, 0L);
        unsafe.putAddress(this.address + FLAGS_OFFSET, 0L);
        if (!(this.constant_pool_index() == index)) {
            throw new RuntimeException();
        }
    }

    public void initialize_resolved_reference_index(int ref_index) {
        if (!(unsafe.getAddress(this.address + F2_OFFSET) == 0)) {
            // note: ref_index might be zero also
            throw new RuntimeException("set once");
        }
        unsafe.putAddress(this.address + F2_OFFSET, ref_index);
    }


    public static @RawCType("WordSize") int size() {
        return (int) (JVM.alignUp(ConstantPoolCacheEntry.SIZE, JVM.wordSize) / JVM.wordSize);
    }

    public static @RawCType("ByteSize")int size_in_bytes(){
        return ((ConstantPoolCacheEntry.SIZE));
    }

}
