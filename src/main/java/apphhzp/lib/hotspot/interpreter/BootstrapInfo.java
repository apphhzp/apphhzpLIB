package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.classfile.SystemDictionary;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantPoolCacheEntry;
import apphhzp.lib.hotspot.oops.constant.ConstantTag;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.signature.SignatureStream;

import java.lang.invoke.MethodType;

import static apphhzp.lib.hotspot.oops.constant.ConstantTag.*;

/**BootstrapInfo provides condensed information from the constant pool necessary to invoke a bootstrap method.*/
public class BootstrapInfo {
    private ConstantPool _pool;     // constant pool containing the bootstrap specifier
    private final int _bss_index;       // index of bootstrap specifier in CP (condy or indy)
    private final int _indy_index;      // internal index of indy call site, or -1 if a condy call
    private final int _argc;            // number of static arguments
    private Symbol _name;            // extracted from JVM_CONSTANT_NameAndType
    private Symbol _signature;

    // pre-bootstrap resolution state:
    private Object _bsm;             // resolved bootstrap method
    private Object _name_arg;        // resolved String
    private Object _type_arg;        // resolved Class or MethodType
    private Object _arg_values;      // array of static arguments; null implies either
    // uresolved or zero static arguments are specified

    // post-bootstrap resolution state:
    private boolean _is_resolved;       // set true when any of the next fields are set
    private Object _resolved_value;    // bind this as condy constant
    private Method _resolved_method;  // bind this as indy behavior
    private Object _resolved_appendix; // extra opaque static argument for _resolved_method

    public BootstrapInfo(ConstantPool pool, int bss_index) {
        this(pool, bss_index, -1);
    }

    public BootstrapInfo(ConstantPool pool, int bss_index, int indy_index) {
        //: _pool(pool),
        //    _bss_index(bss_index),
        //    _indy_index(indy_index),
        //    // derived and eagerly cached:
        //    _argc(      pool->bootstrap_argument_count_at(bss_index) ),
        //    _name(      pool->uncached_name_ref_at(bss_index) ),
        //    _signature( pool->uncached_signature_ref_at(bss_index) )
        this._pool = pool;
        this._bss_index = bss_index;
        this._indy_index = indy_index;
        this._argc = pool.bootstrap_argument_count_at(bss_index);
        this._name = pool.uncached_name_ref_at(bss_index);
        this._signature = pool.uncached_signature_ref_at(bss_index);
        _is_resolved = false;
        if (!ConstantTag.has_bootstrap(pool.tag_at(bss_index))) {
            throw new IllegalArgumentException();
        }
        if (!(indy_index == -1 || pool.invokedynamic_bootstrap_ref_index_at(indy_index) == bss_index)) {
            throw new IllegalArgumentException("invalid bootstrap specifier index");
        }
    }

    public ConstantPool pool() {
        return _pool;
    }

    public int bss_index() {
        return _bss_index;
    }

    public int indy_index() {
        return _indy_index;
    }

    public int argc() {
        return _argc;
    }

    public boolean is_method_call() {
        return (_indy_index != -1);
    }

    public Symbol name() {
        return _name;
    }

    public Symbol signature() {
        return _signature;
    }

    // accessors to lazy state
    public Object bsm() {
        return _bsm;
    }

    public Object name_arg() {
        return _name_arg;
    }

    public Object type_arg() {
        return _type_arg;
    }

    public Object arg_values() {
        return _arg_values;
    }

    public boolean is_resolved() {
        return _is_resolved;
    }

    public Object resolved_value() {
        if (is_method_call()){
            throw new RuntimeException();
        }
        return _resolved_value;
    }

    public Method resolved_method() {
        if (!is_method_call()){
            throw new RuntimeException();
        }
        return _resolved_method;
    }

    public Object resolved_appendix() {
        if (!is_method_call()){
            throw new RuntimeException();
        }
        return _resolved_appendix;
    }

    // derived accessors
    public InstanceKlass caller() {
        return _pool.pool_holder();
    }

    public Class<?> caller_mirror() {
        return caller().asClass();
    }

    public int decode_indy_index() {
        return ConstantPool.decode_invokedynamic_index(_indy_index);
    }

    public int bsms_attr_index() {
        return _pool.bootstrap_methods_attribute_index(_bss_index);
    }

    public int bsm_index() {
        return _pool.bootstrap_method_ref_index_at(_bss_index);
    }

    //int argc() is eagerly cached in _argc
    public int arg_index(int i) {
        return _pool.bootstrap_argument_index_at(_bss_index, i);
    }

    // CP cache entry for call site (indy only)
    public ConstantPoolCacheEntry invokedynamic_cp_cache_entry() {
        if (!is_method_call()) {
            throw new RuntimeException();
        }
        return _pool.invokedynamic_cp_cache_entry_at(_indy_index);
    }

    // If there is evidence this call site was already linked, set the
    // existing linkage data into result, or throw previous exception.
    // Return true if either action is taken, else false.
//    public boolean resolve_previously_linked_invokedynamic(CallInfo& result, TRAPS){
//        assert(_indy_index != -1, "");
//        ConstantPoolCacheEntry* cpce = invokedynamic_cp_cache_entry();
//        if (!cpce->is_f1_null()) {
//            methodHandle method(     THREAD, cpce->f1_as_method());
//            Handle       appendix(   THREAD, cpce->appendix_if_resolved(_pool));
//            result.set_handle(vmClasses::MethodHandle_klass(), method, appendix, THREAD);
//            Exceptions::wrap_dynamic_exception(/* is_indy */ true, CHECK_false);
//            return true;
//        } else if (cpce->indy_resolution_failed()) {
//            int encoded_index = ResolutionErrorTable::encode_cpcache_index(_indy_index);
//            ConstantPool::throw_resolution_error(_pool, encoded_index, CHECK_false);
//            return true;
//        } else {
//            return false;
//        }
//    }
//    public boolean save_and_throw_indy_exc();
    // public void resolve_newly_linked_invokedynamic(CallInfo& result, );

    // pre-bootstrap resolution actions:
    public Object resolve_bsm() { // lazily compute _bsm and return it
        if (_bsm != null) {
            return _bsm;
        }
        boolean is_indy = is_method_call();
        // The tag at the bootstrap method index must be a valid method handle or a method handle in error.
        // If it is a MethodHandleInError, a resolution error will be thrown which will be wrapped if necessary
        // with a BootstrapMethodError.
        if (!(_pool.tag_at(bsm_index()) == MethodHandle ||
                _pool.tag_at(bsm_index()) == MethodHandleInError)) {
            throw new RuntimeException("MH not present, classfile structural constraint");
        }
        Object bsm_oop = _pool.resolve_possibly_cached_constant_at(bsm_index());
        //Exceptions::wrap_dynamic_exception(is_indy, CHECK_NH);
        //guarantee(java_lang_invoke_MethodHandle::is_instance(bsm_oop), );
        if (!(bsm_oop instanceof java.lang.invoke.MethodHandle)) {
            throw new RuntimeException("classfile must supply a valid BSM");
        }
        _bsm = bsm_oop;

        // Obtain NameAndType information
        resolve_bss_name_and_type();
        //Exceptions::wrap_dynamic_exception(is_indy, CHECK_NH);

        // Prepare static arguments
        resolve_args();
        //Exceptions::wrap_dynamic_exception(is_indy, CHECK_NH);

        return _bsm;
    }

    public void resolve_bss_name_and_type() { // lazily compute _name/_type
        if (_bsm == null) {
            throw new RuntimeException("resolve_bsm first");
        }
        Symbol name = this.name();
        Symbol type = this.signature();
        _name_arg = name.toString();
        if (type.char_at(0) == '(') {
            _type_arg = SystemDictionary.find_method_handle_type(type, caller());
        } else {
            _type_arg = SystemDictionary.find_java_mirror_for_type(type, caller(), SignatureStream.NCDFError);
        }
    }

    public void resolve_args() {  // compute arguments
        if (_bsm == null){
            throw new RuntimeException("resolve_bsm first");
        }

        // if there are no static arguments, return leaving _arg_values as null
        if (_argc == 0 && JVM.getFlag("UseBootstrapCallInfo").getInt() < 2) return;

        boolean use_BSCI;
        switch (JVM.getFlag("UseBootstrapCallInfo").getInt()) {
            default:
                use_BSCI = true;
                break;  // stress mode
            case 0:
                use_BSCI = false;
                break;  // stress mode
            case 1:                            // normal mode
                // If we were to support an alternative mode of BSM invocation,
                // we'd convert to pull mode here if the BSM could be a candidate
                // for that alternative mode.  We can't easily test for things
                // like varargs here, but we can get away with approximate testing,
                // since the JDK runtime will make up the difference either way.
                // For now, exercise the pull-mode path if the BSM is of arity 2,
                // or if there is a potential condy loop (see below).
                MethodType mt_oop = JavaClasses.MethodHandle.type((java.lang.invoke.MethodHandle) _bsm);
                use_BSCI = (JavaClasses.MethodType.ptype_count(mt_oop) == 2);
                break;
        }

        // Here's a reason to use BSCI even if it wasn't requested:
        // If a condy uses a condy argument, we want to avoid infinite
        // recursion (condy loops) in the C code.  It's OK in Java,
        // because Java has stack overflow checking, so we punt
        // potentially cyclic cases from C to Java.
        if (!use_BSCI && _pool.tag_at(_bss_index) == Dynamic) {
            boolean found_unresolved_condy = false;
            for (int i = 0; i < _argc; i++) {
                int arg_index = _pool.bootstrap_argument_index_at(_bss_index, i);
                if (_pool.tag_at(arg_index) == Dynamic) {
                    // potential recursion point condy -> condy
                    boolean[] found_it = new boolean[]{false};
                    _pool.find_cached_constant_at(arg_index, found_it);
                    if (!found_it[0]) {
                        found_unresolved_condy = true;
                        break;
                    }
                }
            }
            if (found_unresolved_condy)
                use_BSCI = true;
        }

        final int SMALL_ARITY = 5;
        if (use_BSCI && _argc <= SMALL_ARITY && JVM.getFlag("UseBootstrapCallInfo").getInt() <= 2) {
            // If there are only a few arguments, and none of them need linking,
            // push them, instead of asking the JDK runtime to turn around and
            // pull them, saving a JVM/JDK transition in some simple cases.
            boolean all_resolved = true;
            for (int i = 0; i < _argc; i++) {
                boolean[] found_it = new boolean[]{false};
                int arg_index = _pool.bootstrap_argument_index_at(_bss_index, i);
                _pool.find_cached_constant_at(arg_index, found_it);
                if (!found_it[0]) {
                    all_resolved = false;
                    break;
                }
            }
            if (all_resolved)
                use_BSCI = false;
        }

        if (!use_BSCI) {
            // return {arg...}; resolution of arguments is done immediately, before JDK code is called
            Object[] args_oop = new Object[_argc];
            _pool.copy_bootstrap_arguments_at(_bss_index, 0, _argc, args_oop, 0, true, null);
            Object arg_oop = ((_argc == 1) ? args_oop[0] : null);
            // try to discard the singleton array
            if (arg_oop != null && !arg_oop.getClass().isArray()) {
                // JVM treats arrays and nulls specially in this position,
                // but other things are just single arguments
                _arg_values = arg_oop;
            } else {
                _arg_values = args_oop;
            }
        } else {
            // return {arg_count, pool_index}; JDK code must pull the arguments as needed
            int[] ints_oop = new int[2];
            ints_oop[0] = _argc;
            ints_oop[1] = _bss_index;
            _arg_values = ints_oop;
        }
    }

    // setters for post-bootstrap results:
    public void set_resolved_value(Object value) {
        if (!(!is_resolved() && !is_method_call())) {
            throw new RuntimeException();
        }
        _is_resolved = true;
        _resolved_value = value;
    }

    public void set_resolved_method(Method method, Object appendix) {
        if (!(!is_resolved() && is_method_call())) {
            throw new RuntimeException();
        }
        _is_resolved = true;
        _resolved_method = method;
        _resolved_appendix = appendix;
    }
}
