package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.memory.ReferenceType;
import apphhzp.lib.hotspot.oop.*;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.constant.ConstantTag;
import apphhzp.lib.hotspot.oop.method.Method;
import apphhzp.lib.hotspot.utilities.BasicType;

import java.util.ArrayList;
import java.util.Arrays;

public class ClassFileParser {
    public static final int JAVA_CLASSFILE_MAGIC = 0xCAFEBABE, JAVA_MIN_SUPPORTED_VERSION = 45, JAVA_PREVIEW_MINOR_VERSION = 65535
// Used for two backward compatibility reasons:
// - to check for new additions to the class file format in JDK1.5
// - to check for bug fixes in the format checker in JDK1.5
            , JAVA_1_5_VERSION = 49
// Used for backward compatibility reasons:
// - to check for javac bug fixes that happened after 1.5
// - also used as the max version when running in jdk6
            , JAVA_6_VERSION = 50
// Used for backward compatibility reasons:
// - to disallow argument and require ACC_STATIC for <clinit> methods
            , JAVA_7_VERSION = 51
// Extension method support.
            , JAVA_8_VERSION = 52, JAVA_9_VERSION = 53, JAVA_10_VERSION = 54, JAVA_11_VERSION = 55, JAVA_12_VERSION = 56, JAVA_13_VERSION = 57, JAVA_14_VERSION = 58, JAVA_15_VERSION = 59, JAVA_16_VERSION = 60, JAVA_17_VERSION = 61;


    //[Publicity]
    public static final int
            INTERNAL = 0,
            BROADCAST = 1;
    //[END]

    // "used to verify unqualified names"
    public static final int
            LegalClass = 0,
            LegalField = 1,
            LegalMethod = 2;
    //[END]

    private static final int fixed_buffer_size = 128;
    //typedef void unsafe_u2;

    ClassFileStream _stream; // Actual input stream
    Symbol _class_name;
    ClassLoaderData _loader_data;
    boolean _is_hidden;
    boolean _can_access_vm_annotations;
    int _orig_cp_size;

    // Metadata created before the instance klass is created.  Must be deallocated
    // if not transferred to the InstanceKlass upon successful class loading
    // in which case these pointers have been set to null.
    InstanceKlass _super_klass;
    ConstantPool _cp;
    U2Array _fields;
    VMTypeArray<Method> _methods;
    U2Array _inner_classes;
    U2Array _nest_members;
    int/*u2*/ _nest_host;
    U2Array _permitted_subclasses;
    VMTypeArray<RecordComponent> _record_components;
    VMTypeArray<InstanceKlass> _local_interfaces;
    VMTypeArray<InstanceKlass> _transitive_interfaces;
    Annotations _combined_annotations;
    U1Array/*AnnotationArray* */ _class_annotations;
    U1Array/*AnnotationArray* */ _class_type_annotations;
    VMTypeArray<U1Array>/*Array<AnnotationArray*>* */ _fields_annotations;
    VMTypeArray<U1Array>/*Array<AnnotationArray*>* */ _fields_type_annotations;
    InstanceKlass _klass;  // InstanceKlass* once created.
    InstanceKlass _klass_to_deallocate; // an InstanceKlass* to be destroyed
    //
    ClassAnnotationCollector/*ClassAnnotationCollector* */ _parsed_annotations;
    FieldAllocationCount /*FieldAllocationCount* */ _fac;
    FieldLayoutInfo _field_info;

    //intArray* _method_ordering;
    ArrayList<Integer> _method_ordering;

    //GrowableArray<Method*>* _all_mirandas;
    ArrayList<Method> _all_mirandas;

    byte[] /*u_char*/ _linenumbertable_buffer = new byte[fixed_buffer_size];

    // Size of Java vtable (in words)
    int _vtable_size;
    int _itable_size;

    int _num_miranda_methods;

    int /*ReferenceType*/ _rt;

    OopDesc _protection_domain;
    AccessFlags _access_flags;

    // for tracing and notifications
    int /*Publicity*/ _pub_level;

    // Used to keep track of whether a constant pool item 19 or 20 is found.  These
    // correspond to CONSTANT_Module and CONSTANT_Package tags and are not allowed
    // in regular class files.  For class file version >= 53, a CFE cannot be thrown
    // immediately when these are seen because a NCDFE must be thrown if the class's
    // access_flags have ACC_MODULE set.  But, the access_flags haven't been looked
    // at yet.  So, the bad constant pool item is cached here.  A value of zero
    // means that no constant pool item 19 or 20 was found.
    short _bad_constant_seen;

    // class attributes parsed before the instance klass is created:
    boolean _synthetic_flag;
    int _sde_length;
    char[] _sde_buffer;
    int/*u2*/ _sourcefile_index;
    int/*u2*/ _generic_signature_index;

    int/*u2*/ _major_version;
    int/*u2*/ _minor_version;
    int/*u2*/ _this_class_index;
    int/*u2*/ _super_class_index;
    int/*u2*/ _itfs_len;
    int/*u2*/ _java_fields_count;

    boolean _need_verify;
    boolean _relax_verify;

    boolean _has_nonstatic_concrete_methods;
    boolean _declares_nonstatic_concrete_methods;
    boolean _has_final_method;
    boolean _has_contended_fields;

    // precomputed flags
    boolean _has_finalizer;
    boolean _has_empty_finalizer;
    boolean _has_vanilla_constructor;
    int _max_bootstrap_specifier_index;  // detects BSS values

    public ClassFileParser(ClassFileStream stream,
                           Symbol name,
                           ClassLoaderData loader_data,
                                  ClassLoadInfo cl_info,
                           int /*Publicity*/ pub_level){
        this._stream=stream;
        this._class_name=null;
        this._loader_data=loader_data;
        this._is_hidden=cl_info.is_hidden();
        this._can_access_vm_annotations=cl_info.can_access_vm_annotations();
        this._orig_cp_size=0;
        this._super_klass=InstanceKlass.create();
        this._cp=null;
        this._fields=null;
        this._methods=null;
        this._inner_classes=null;
        this._nest_members=null;
        this._nest_host=0;
        this._permitted_subclasses=null;
        this._record_components=null;
        this._local_interfaces=null;
        this._transitive_interfaces=null;
        this._combined_annotations=null;
        this._class_annotations=null;
        this._class_type_annotations=null;
        this._fields_annotations=null;
        this._fields_type_annotations=null;
        this._klass=null;
        this._klass_to_deallocate=null;
        this._parsed_annotations=null;
        this._fac=null;
        this._field_info=null;
        this._method_ordering=null;
        this._all_mirandas=null;
        this._vtable_size=0;
        this._itable_size=0;
        this._num_miranda_methods=0;
        this._rt= ReferenceType.REF_NONE;
        this._protection_domain=cl_info.protection_domain();
        this._access_flags=AccessFlags.getOrCreate(0);
        this._pub_level=pub_level;
        this._bad_constant_seen=0;
        this._synthetic_flag=false;
        this._sde_length=0;
        this._sde_buffer=null;
        this._sourcefile_index=0;
        this._generic_signature_index=0;
        this._major_version=0;
        this._minor_version=0;
        this._this_class_index=0;
        this._super_class_index=0;
        this._itfs_len=0;
        this._java_fields_count=0;
        this._need_verify=false;
        this._relax_verify=false;
        this._has_nonstatic_concrete_methods=false;
        this._declares_nonstatic_concrete_methods=false;
        this._has_final_method=false;
        this._has_contended_fields=false;
        this._has_finalizer=false;
        this._has_empty_finalizer=false;
        this._has_vanilla_constructor=false;
        this._max_bootstrap_specifier_index=-1;

        this._class_name=name!=null?name:Symbol.getVMSymbol("<Unknown>");
        this._class_name.incrementRefCount();
//        assert(_loader_data != NULL, "invariant");
//        assert(stream != NULL, "invariant");
//        assert(_stream != NULL, "invariant");
//        assert(_stream.buffer() == _stream.current(), "invariant");
//        assert(_class_name != NULL, "invariant");
//        assert(0 == _access_flags.as_int(), "invariant");
        if (_loader_data == null||stream == null||_stream==null||_stream.current_offset()!=0||this._class_name==null||0 != _access_flags.flags){
            throw new IllegalStateException("invariant");
        }
        if (JVM.dumpSharedSpaces) {
            // verify == true means it's a 'remote' class (i.e., non-boot class)
            // Verification decision is based on BytecodeVerificationRemote flag
            // for those classes.
            _need_verify = (stream.need_verify()) ? JVM.bytecodeVerificationRemote :
                    JVM.bytecodeVerificationLocal;
        } else {
            _need_verify = (_loader_data.getClassLoader() == null || !stream.need_verify()) ?
                    JVM.bytecodeVerificationLocal : JVM.bytecodeVerificationRemote;
        }

        stream.set_verify(_need_verify);
        _relax_verify = relax_format_check_for(_loader_data);
//        parse_stream(stream);
//
//        post_process_parsed_stream(stream, _cp);
    }

    public static boolean relax_format_check_for(ClassLoaderData loader_data) {
        boolean trusted = loader_data.equals(ClassLoaderData.null_class_loader_data)||loader_data.getClassLoader()==null ||
                loader_data.getClassLoader()==ClassLoader.getPlatformClassLoader();
        boolean need_verify =
                // verifyAll
                (JVM.bytecodeVerificationLocal && JVM.bytecodeVerificationRemote) ||
                        // verifyRemote
                        (!JVM.bytecodeVerificationLocal && JVM.bytecodeVerificationRemote && !trusted);
        return !need_verify;
    }

//    public void post_process_parsed_stream(ClassFileStream stream,
//                                                     ConstantPool cp) {
//        assert(stream != NULL, "invariant");
//        assert(stream.at_eos(), "invariant");
//        assert(cp != NULL, "invariant");
//        assert(_loader_data != NULL, "invariant");
//
//        if (_class_name == vmSymbols::java_lang_Object()) {
//            check_property(_local_interfaces == Universe::the_empty_instance_klass_array(),
//                    "java.lang.Object cannot implement an interface in class file %s",
//                    CHECK);
//        }
//        // We check super class after class file is parsed and format is checked
//        if (_super_class_index > 0 && NULL == _super_klass) {
//            Symbol* const super_class_name = cp.klass_name_at(_super_class_index);
//            if (_access_flags.is_interface()) {
//                // Before attempting to resolve the superclass, check for class format
//                // errors not checked yet.
//                guarantee_property(super_class_name == vmSymbols::java_lang_Object(),
//                        "Interfaces must have java.lang.Object as superclass in class file %s",
//                        CHECK);
//            }
//            Handle loader(THREAD, _loader_data.class_loader());
//            _super_klass = (const InstanceKlass*)
//            SystemDictionary::resolve_super_or_fail(_class_name,
//                    super_class_name,
//                    loader,
//                    _protection_domain,
//                    true,
//                    CHECK);
//        }
//
//        if (_super_klass != NULL) {
//            if (_super_klass.has_nonstatic_concrete_methods()) {
//                _has_nonstatic_concrete_methods = true;
//            }
//
//            if (_super_klass.is_interface()) {
//                classfile_icce_error("class %s has interface %s as super class", _super_klass);
//                return;
//            }
//        }
//
//        // Compute the transitive list of all unique interfaces implemented by this class
//        _transitive_interfaces =
//                compute_transitive_interfaces(_super_klass,
//                        _local_interfaces,
//                        _loader_data);
//
//        assert(_transitive_interfaces != null, "invariant");
//
//        // sort methods
//        _method_ordering = sort_methods(_methods);
//
//        _all_mirandas = new GrowableArray<Method*>(20);
//
//        Handle loader(THREAD, _loader_data.class_loader());
//        klassVtable::compute_vtable_size_and_num_mirandas(&_vtable_size,
//                                                    &_num_miranda_methods,
//                _all_mirandas,
//                _super_klass,
//                _methods,
//                _access_flags,
//                _major_version,
//                loader,
//                _class_name,
//                _local_interfaces);
//
//        // Size of Java itable (in words)
//        _itable_size = _access_flags.isInterface() ? 0 :
//                klassItable::compute_itable_size(_transitive_interfaces);
//
//        assert(_fac != null, "invariant");
//        assert(_parsed_annotations != null, "invariant");
//
//        _field_info = new FieldLayoutInfo();
//        FieldLayoutBuilder lb(class_name(), super_klass(), _cp, _fields,
//                _parsed_annotations.is_contended(), _field_info);
//        lb.build_layout();
//
//        // Compute reference typ
//        _rt = (null ==_super_klass) ? ReferenceType.REF_NONE : _super_klass.getReferenceType().value;
//
//    }

//    public void clean() {
//        _class_name.decrementRefCount();
//
//        if (_cp != null) {
//            unsafe.freeMemory(this._cp.address);
////            MetadataFactory::free_metadata(_loader_data, _cp);
//        }
//        if (_fields != null) {
//            unsafe.freeMemory(this._fields.address);
//            //MetadataFactory::free_array<u2>(_loader_data, _fields);
//        }
//
//        if (_methods != null) {
//            // Free methods
//            InstanceKlass::deallocate_methods(_loader_data, _methods);
//        }
//
//        // beware of the Universe::empty_blah_array!!
//        if (_inner_classes != null && _inner_classes != Universe::the_empty_short_array()) {
//            //MetadataFactory::free_array<u2>(_loader_data, _inner_classes);
//            unsafe.freeMemory(this._inner_classes.address);
//        }
//
//        if (_nest_members != null && _nest_members != Universe::the_empty_short_array()) {
//            MetadataFactory::free_array<u2>(_loader_data, _nest_members);
//            unsafe.freeMemory(this._nest_members.address);
//        }
//
//        if (_record_components != null) {
//            InstanceKlass::deallocate_record_components(_loader_data, _record_components);
//        }
//
//        if (_permitted_subclasses != null && _permitted_subclasses != Universe::the_empty_short_array()) {
//            //MetadataFactory::free_array<u2>(_loader_data, _permitted_subclasses);
//            unsafe.freeMemory(this._permitted_subclasses.address);
//        }
//
//        // Free interfaces
//        InstanceKlass::deallocate_interfaces(_loader_data, _super_klass,
//                _local_interfaces, _transitive_interfaces);
//
//        if (_combined_annotations != null) {
//            // After all annotations arrays have been created, they are installed into the
//            // Annotations object that will be assigned to the InstanceKlass being created.
//
//            // Deallocate the Annotations object and the installed annotations arrays.
//            _combined_annotations.deallocate_contents(_loader_data);
//
//            // If the _combined_annotations pointer is non-null,
//            // then the other annotations fields should have been cleared.
//
//            if (!(_class_annotations==null&&_class_type_annotations  == null&&_fields_annotations== null&&_fields_type_annotations == null)){
//                throw new IllegalStateException("Should have been cleared");
//            }
//        } else {
//            // If the annotations arrays were not installed into the Annotations object,
//            // then they have to be deallocated explicitly.
////            MetadataFactory::free_array<u1>(_loader_data, _class_annotations);
////            MetadataFactory::free_array<u1>(_loader_data, _class_type_annotations);
//            unsafe.freeMemory(this._class_annotations.address);
//            unsafe.freeMemory(this._class_type_annotations.address);
//            Annotations::free_contents(_loader_data, _fields_annotations);
//            Annotations::free_contents(_loader_data, _fields_type_annotations);
//        }
//        clear_class_metadata();
//        _transitive_interfaces = null;
//        _local_interfaces = null;
//
//        // deallocate the klass if already created.  Don't directly deallocate, but add
//        // to the deallocate list so that the klass is removed from the CLD::_klasses list
//        // at a safepoint.
//        if (_klass_to_deallocate != null) {
//            _loader_data.add_to_deallocate_list(_klass_to_deallocate);
//        }
//    }


    public void set_class_bad_constant_seen(short bad_constant) {
        if (!((bad_constant == ConstantTag.Module ||
                bad_constant == ConstantTag.Package) && _major_version >= JAVA_9_VERSION)){
            throw new IllegalArgumentException("Unexpected bad constant pool entry");
        }
        if (_bad_constant_seen == 0) _bad_constant_seen = bad_constant;
    }

//    public void parse_constant_pool_entries( ClassFileStream  stream,
//                                                      ConstantPool cp,
//                                                   int length) {
//        if (stream==null||cp==null){
//            throw new IllegalArgumentException("invariant");
//        }
//        // Use a local copy of ClassFileStream. It helps the C++ compiler to optimize
//        // this function (_current can be allocated in a register, with scalar
//        // replacement of aggregates). The _current pointer is copied back to
//        // stream() when this function returns. DON'T call another method within
//        // this method that uses stream().
//       ClassFileStream cfs = stream.copy();
//
////        assert(cfs.allocated_on_stack(), "should be local");
//        //debug_only( byte/*u1*/*  old_current = stream.current();)
//
//        // Used for batching symbol allocations.
//        String[] names=new String[8];
//        int[] lengths=new int[8];
//        int[] indices=new int[8];
//        long[] hashValues=new long[8];
//        int names_count = 0;
//
//        // parsing  Index 0 is unused
//        for (int index = 1; index < length; index++) {
//            // Each of the following case guarantees one more byte in the stream
//            // for the following tag or the access_flags following constant pool,
//            // so we don't need bounds-check for reading tag.
//            byte/*u1*/ tag = cfs.get_u1_fast();
//            switch (tag) {
//                case ConstantTag.Class: {
//                    cfs.guarantee_more(3);  // name_index, tag/access_flags
//                    int/*u2*/ name_index = cfs.get_u2_fast();
//                    cp.klass_index_at_put(index, name_index);
//                    break;
//                }
//                case ConstantTag.Fieldref: {
//                    cfs.guarantee_more(5);  // class_index, name_and_type_index, tag/access_flags
//                    int/*u2*/ class_index = cfs.get_u2_fast();
//                    int/*u2*/ name_and_type_index = cfs.get_u2_fast();
//                    cp.fieldRef_at_put(index, class_index, name_and_type_index);
//                    break;
//                }
//                case ConstantTag.Methodref: {
//                    cfs.guarantee_more(5);  // class_index, name_and_type_index, tag/access_flags
//                    int/*u2*/ class_index = cfs.get_u2_fast();
//                    int/*u2*/ name_and_type_index = cfs.get_u2_fast();
//                    cp.methodRef_at_put(index, class_index, name_and_type_index);
//                    break;
//                }
//                case ConstantTag.InterfaceMethodref: {
//                    cfs.guarantee_more(5);  // class_index, name_and_type_index, tag/access_flags
//         int/*u2*/ class_index = cfs.get_u2_fast();
//         int/*u2*/ name_and_type_index = cfs.get_u2_fast();
//                    cp.interface_method_at_put(index, class_index, name_and_type_index);
//                    break;
//                }
//                case ConstantTag.String:
//                    cfs.guarantee_more(3);  // string_index, tag/access_flags
//                    int/*u2*/ string_index = cfs.get_u2_fast();
//                    cp.string_index_at_put(index, string_index);
//                    break;
//                case ConstantTag.MethodHandle:
//                case ConstantTag.MethodType:
////                    if (_major_version < Verifier::INVOKEDYNAMIC_MAJOR_VERSION) {
////                        classfile_parse_error(
////                                "Class file version does not support constant tag %u in class file %s",
////                                tag, THREAD);
////                        return;
////                    }
//                    if (tag == ConstantTag.MethodHandle) {
//                        cfs.guarantee_more(4);  // ref_kind, method_index, tag/access_flags
//           byte/*u1*/ ref_kind = cfs.get_u1_fast();
//           int/*u2*/ method_index = cfs.get_u2_fast();
//                        cp.method_handle_index_at_put(index, ref_kind, method_index);
//                    } else if (tag == ConstantTag.MethodType) {
//                        cfs.guarantee_more(3);  // signature_index, tag/access_flags
//           int/*u2*/ signature_index = cfs.get_u2_fast();
//                        cp.method_type_index_at_put(index, signature_index);
//                    } else {
//                        throw new RuntimeException("ShouldNotReachHere");
//                    }
//                    break;
//                case ConstantTag.Dynamic: {
////                    if (_major_version < Verifier::DYNAMICCONSTANT_MAJOR_VERSION) {
////                        classfile_parse_error(
////                                "Class file version does not support constant tag %u in class file %s",
////                                tag);
////                        return;
////                    }
//                    cfs.guarantee_more(5);  // bsm_index, nt, tag/access_flags
//         int/*u2*/ bootstrap_specifier_index = cfs.get_u2_fast();
//         int/*u2*/ name_and_type_index = cfs.get_u2_fast();
//                    if (_max_bootstrap_specifier_index < bootstrap_specifier_index) {
//                        _max_bootstrap_specifier_index = bootstrap_specifier_index;  // collect for later
//                    }
//                    cp.dynamic_constant_at_put(index, bootstrap_specifier_index, name_and_type_index);
//                    break;
//                }
//                case ConstantTag.InvokeDynamic :
////                    if (_major_version < Verifier::INVOKEDYNAMIC_MAJOR_VERSION) {
////                        classfile_parse_error(
////                                "Class file version does not support constant tag %u in class file %s",
////                                tag);
////                        return;
////                    }
//                    cfs.guarantee_more(5);  // bsm_index, nt, tag/access_flags
//                    int/*u2*/ bootstrap_specifier_index = cfs.get_u2_fast();
//                    int/*u2*/ name_and_type_index = cfs.get_u2_fast();
//                    if (_max_bootstrap_specifier_index < bootstrap_specifier_index) {
//                        _max_bootstrap_specifier_index = bootstrap_specifier_index;  // collect for later
//                    }
//                    cp.invoke_dynamic_at_put(index, bootstrap_specifier_index, name_and_type_index);
//                    break;
//                case  ConstantTag.Integer: {
//                    cfs.guarantee_more(5);  // bytes, tag/access_flags
//                    int/*u4*/ bytes = cfs.get_u4_fast();
//                    cp.int_at_put(index, bytes);
//                    break;
//                }
//                case ConstantTag.Float: {
//                    cfs.guarantee_more(5);  // bytes, tag/access_flags
//                    int/*u4*/ bytes = cfs.get_u4_fast();
//                    cp.float_at_put(index, Float.intBitsToFloat(bytes));
//                    break;
//                }
//                case ConstantTag.Long: {
//                    // A mangled type might cause you to overrun allocated memory
////                    guarantee_property(index + 1 < length,
////                            "Invalid constant pool entry %u in class file %s",
////                            index);
//                    cfs.guarantee_more(9);  // bytes, tag/access_flags
//                    long/*u8*/ bytes = cfs.get_u8_fast();
//                    cp.long_at_put(index, bytes);
//                    index++;   // Skip entry following eigth-byte constant, see JVM book p. 98
//                    break;
//                }
//                case ConstantTag.Double:
//                    // A mangled type might cause you to overrun allocated memory
////                    guarantee_property(index+1 < length,
////                            "Invalid constant pool entry %u in class file %s",
////                            index);
//                    cfs.guarantee_more(9);  // bytes, tag/access_flags
//                    long/*u8*/ bytes = cfs.get_u8_fast();
//                    cp.double_at_put(index, Double.longBitsToDouble(bytes));
//                    index++;   // Skip entry following eigth-byte constant, see JVM book p. 98
//                    break;
//                case ConstantTag.NameAndType:
//                    cfs.guarantee_more(5);  // name_index, signature_index, tag/access_flags
//                    int/*u2*/ name_index = cfs.get_u2_fast();
//                    int/*u2*/ signature_index = cfs.get_u2_fast();
//                    cp.name_and_type_at_put(index, name_index, signature_index);
//                    break;
//                case ConstantTag.Utf8:
//                    cfs.guarantee_more(2);  // utf8_length
//                    int/*u2*/  utf8_length = cfs.get_u2_fast();
//                    int/*u1* */ utf8_buffer_index = cfs.current();
//                    //assert(utf8_buffer != null, "null utf8 buffer");
//
//                    // Got utf8 string, guarantee utf8_length+1 bytes, set stream position forward.
//                    cfs.guarantee_more(utf8_length+1);  // utf8 string, tag/access_flags
//                    cfs.skip_u1_fast(utf8_length);
//                    // Before storing the symbol, make sure it's legal
//                    if (_need_verify) {
//                        verify_legal_utf8(utf8_buffer_index, utf8_length);
//                    }
//                    String str=new String(Arrays.copyOfRange(cfs.buffer(),utf8_buffer_index,utf8_buffer_index+utf8_length) );
//                    long hash;
//                    Symbol  result = Symbol.onlyLookup(str);//( char*)utf8_buffer_index, utf8_length, hash);
//                    if (result == null) {
//                        names[names_count] = str;//( char*)utf8_buffer_index;
//                        lengths[names_count] = utf8_length;
//                        indices[names_count] = index;
//                        //hashValues[] = hash;
//                        names_count++;
//                        if (names_count == 8) {
//                            Symbol.new_symbols(_loader_data,
//                                    constantPoolHandle(cp),
//                                    names_count,
//                                    names,
//                                    lengths,
//                                    indices,
//                                    hashValues);
//                            names_count = 0;
//                        }
//                    } else {
//                        cp.symbol_at_put(index, result);
//                    }
//                    break;
//                case ConstantTag.Module:
//                case ConstantTag.Package:
//                    // Record that an error occurred in these two cases but keep parsing so
//                    // that ACC_Module can be checked for in the access_flags.  Need to
//                    // throw NoClassDefFoundError in that case.
//                    if (_major_version >= JAVA_9_VERSION) {
//                        cfs.guarantee_more(3);
//                        cfs.get_u2_fast();
//                        set_class_bad_constant_seen(tag);
//                        break;
//                    }
//                default:
//                    throw new RuntimeException("Unknown constant tag %u in class file "+tag);
//                    return;
//            } // end of switch(tag)
//        } // end of for
//
//        // Allocate the remaining symbols
//        if (names_count > 0) {
//            SymbolTable::new_symbols(_loader_data,
//                    constantPoolHandle(cp),
//                    names_count,
//                    names,
//                    lengths,
//                    indices,
//                    hashValues)
//        }
//
//        // Copy _current pointer of local copy back to stream.
//        //assert(stream.current() == old_current, "non-exclusive use of stream");
//        stream.set_current(cfs.buffer(),cfs.current());
//
//    }


//    InstanceKlass create_instance_klass(boolean changed_by_loadhook, ClassInstanceInfo/*&*/ cl_inst_info) {
//        if (_klass != null) {
//            return _klass;
//        }
//
//        InstanceKlass  ik =
//                InstanceKlass.allocate_instance_klass(this);
//
//        if (is_hidden()) {
//            mangle_hidden_class_name(ik);
//        }
//
//        fill_instance_klass(ik, changed_by_loadhook, cl_inst_info);
//        if (_klass!=ik){
//            throw new IllegalStateException("invariant");
//        }
//        return ik;
//    }
//
//    public void mangle_hidden_class_name(InstanceKlass ik) {
//
//        update_class_name(SymbolTable::new_symbol(new_name));
//        // Add a Utf8 entry containing the hidden name.
//        if (_class_name==null){
//            throw new IllegalStateException("Unexpected null _class_name");
//        }
//        int hidden_index = _orig_cp_size; // this is an extra slot we added
//        _cp.utf8_at_put(hidden_index, _class_name);
//        // Update this_class_index's slot in the constant pool with the new Utf8 entry.
//        // We have to update the resolved_klass_index and the name_index together
//        // so extract the existing resolved_klass_index first.
//        CPKlassSlot cp_klass_slot = _cp->klass_slot_at(_this_class_index);
//        int resolved_klass_index = cp_klass_slot.resolved_klass_index();
//        _cp->unresolved_klass_at_put(_this_class_index, hidden_index, resolved_klass_index);
//        if (!(_cp.klass_slot_at(_this_class_index).name_index() == _orig_cp_size)){
//            throw new IllegalStateException("Bad name_index");
//        }
//    }



//    void fill_instance_klass(InstanceKlass ik,
//                                              boolean changed_by_loadhook,
//                                           ClassInstanceInfo/*&*/ cl_inst_info) {
//        if (ik==null){
//            throw new IllegalArgumentException("invariant");
//        }
//        // Set name and CLD before adding to CLD
//        ik.setClassLoaderData(_loader_data);
//        ik.setName(_class_name);
//        // Add all classes to our internal class loader list here,
//        // including classes in the bootstrap (null) class loader.
//        boolean publicize = !is_internal();
//
//        _loader_data.add_class(ik, publicize);
//
//        set_klass_to_deallocate(ik);
//
//        assert(_field_info != null, "invariant");
//        assert(ik.getStaticFieldSize() == _field_info._static_field_size, "sanity");
//        assert(ik.nonstatic_oop_map_count() == _field_info.oop_map_blocks._nonstatic_oop_map_count,
//        "sanity");
//        assert(ik.getSizeHelper() == _field_info._instance_size, "sanity");
//
//        // Fill in information already parsed
////        ik.set_should_verify_class(_need_verify);
//        ik.setMiscFlags(ik.getMiscFlags()|InstanceKlass.MiscFlags.SHOULD_VERIFY_CLASS);
//        // Not yet: supers are done below to support the new subtype-checking fields
//        ik.setNonstaticFieldSize(_field_info._nonstatic_field_size);
//        ik.set_has_nonstatic_fields(_field_info._has_nonstatic_fields);
//        if (_fac==null){
//            throw new IllegalStateException("invariant");
//        }
//        ik.setStaticOopFieldCount(_fac.count[STATIC_OOP]);
//
//        // this transfers ownership of a lot of arrays from
//        // the parser onto the InstanceKlass*
//        apply_parsed_class_metadata(ik, _java_fields_count);
//
//        // can only set dynamic nest-host after static nest information is set
//        if (cl_inst_info.dynamic_nest_host() != null) {
//            ik.set_nest_host(cl_inst_info.dynamic_nest_host());
//        }
//
//        // note that is not safe to use the fields in the parser from this point on
////        assert(null == _cp, "invariant");
////        assert(null == _fields, "invariant");
////        assert(null == _methods, "invariant");
////        assert(null == _inner_classes, "invariant");
////        assert(null == _nest_members, "invariant");
////        assert(null == _combined_annotations, "invariant");
////        assert(null == _record_components, "invariant");
////        assert(null == _permitted_subclasses, "invariant");
//        if (null != _cp||null != _fields||null != _methods||null != _inner_classes||null != _nest_members||null != _combined_annotations||null != _record_components||null != _permitted_subclasses){
//            throw new IllegalStateException("invariant");
//        }
//        if (_has_final_method) {
//            ik.setAccessFlags(ik.getAccessFlags().flags|AccessFlags.JVM_ACC_HAS_FINAL_METHOD);
//        }
//        ik.copy_method_ordering(_method_ordering);
//        // The InstanceKlass::_methods_jmethod_ids cache
//        // is managed on the assumption that the initial cache
//        // size is equal to the number of methods in the class. If
//        // that changes, then InstanceKlass::idnum_can_increment()
//        // has to be changed accordingly.
//        ik.set_initial_method_idnum(ik.methods().length());
//        ik.set_this_class_index(_this_class_index);
//        if (_is_hidden) {
//            // _this_class_index is a CONSTANT_Class entry that refers to this
//            // hidden class itself. If this class needs to refer to its own methods
//            // or fields, it would use a CONSTANT_MethodRef, etc, which would reference
//            // _this_class_index. However, because this class is hidden (it's
//            // not stored in SystemDictionary), _this_class_index cannot be resolved
//            // with ConstantPool::klass_at_impl, which does a SystemDictionary lookup.
//            // Therefore, we must eagerly resolve _this_class_index now.
//            ik.getConstantPool().klass_at_put(_this_class_index, ik);
//        }
//
//        ik.set_minor_version(_minor_version);
//        ik.set_major_version(_major_version);
//        ik.set_has_nonstatic_concrete_methods(_has_nonstatic_concrete_methods);
//        ik.set_declares_nonstatic_concrete_methods(_declares_nonstatic_concrete_methods);
//
//        if (_is_hidden) {
//            ik.set_is_hidden();
//        }
//
//        // Set PackageEntry for this_klass
//        Oop cl = ik.getClassLoaderData().getClassLoaderOop();
//        Handle clh = Handle(THREAD, java_lang_ClassLoader::non_reflection_class_loader(cl));
//        ClassLoaderData cld = ClassLoaderData.class_loader_data_or_null(clh());
//        ik.set_package(cld, null);
//        VMTypeArray<Method>  methods = ik.getMethods();
//        if (methods==null){
//            throw new IllegalStateException("invariant");
//        }
//        int methods_len = methods.length();
//
//        check_methods_for_intrinsics(ik, methods);
//
//        // Fill in field values obtained by parse_classfile_attributes
//        if (_parsed_annotations.has_any_annotations()) {
//            _parsed_annotations.apply_to(ik);
//        }
//
//        apply_parsed_class_attributes(ik);
//
//        // Miranda methods
//        if ((_num_miranda_methods > 0) ||
//                // if this class introduced new miranda methods or
//                (_super_klass != null && _super_klass.has_miranda_methods())
//            // super class exists and this class inherited miranda methods
//        ) {
//            ik.set_has_miranda_methods(); // then set a flag
//        }
//
//        // Fill in information needed to compute superclasses.
//        ik.initialize_supers((_super_klass), _transitive_interfaces);
//        ik.set_transitive_interfaces(_transitive_interfaces);
//        ik.setLocalInterfaces(_local_interfaces);
//        _transitive_interfaces = null;
//        _local_interfaces = null;
//
//        // Initialize itable offset tables
//        klassItable::setup_itable_offset_table(ik);
//
//        // Compute transitive closure of interfaces this class implements
//        // Do final class setup
//        OopMapBlocksBuilder oop_map_blocks = _field_info.oop_map_blocks;
//        if (oop_map_blocks._nonstatic_oop_map_count > 0) {
//            oop_map_blocks.copy(ik.start_of_nonstatic_oop_maps());
//        }
//
//        if (_has_contended_fields || _parsed_annotations.is_contended() ||
//                ( _super_klass != null && _super_klass.has_contended_annotations())) {
//            ik.set_has_contended_annotations(true);
//        }
//
//        // Fill in has_finalizer, has_vanilla_constructor, and layout_helper
//        set_precomputed_flags(ik);
//
//        // check if this class can access its super class
//        check_super_class_access(ik, CHECK);
//
//        // check if this class can access its superinterfaces
//        check_super_interface_access(ik, CHECK);
//
//        // check if this class overrides any final method
//        check_final_method_override(ik, CHECK);
//
//        // reject static interface methods prior to Java 8
//        if (ik.is_interface() && _major_version < JAVA_8_VERSION) {
//            check_illegal_static_method(ik, CHECK);
//        }
//
//        // Obtain this_klass' module entry
//        ModuleEntry module_entry = ik.module();
//        assert(module_entry != null, "module_entry should always be set");
//
//        // Obtain java.lang.Module
//        Oop module_handle( module_entry.module());
//
//        // Allocate mirror and initialize static fields
//        // The create_mirror() call will also call compute_modifiers()
//        java_lang_Class::create_mirror(ik, _loader_data.getClassLoaderOop(),
//        module_handle,
//                _protection_domain,
//                cl_inst_info.class_data());
//        if (_all_mirandas==null){
//            throw new IllegalStateException("invariant");
//        }
//        // Generate any default methods - default methods are public interface methods
//        // that have a default implementation.  This is new with Java 8.
//        if (_has_nonstatic_concrete_methods) {
//            DefaultMethods::generate_default_methods(ik,
//                    _all_mirandas);
//        }
//
//        // Add read edges to the unnamed modules of the bootstrap and app class loaders.
//        if (changed_by_loadhook && !module_handle.is_null() && module_entry.is_named() &&
//                !module_entry.has_default_read_edges()) {
//            if (!module_entry.set_has_default_read_edges()) {
//                // We won a potential race
//                JvmtiExport::add_default_read_edges(module_handle, THREAD);
//            }
//        }
//
//        ClassLoadingService::notify_class_loaded(ik, false /* not shared class */);
//
//        if (!is_internal()) {
//            ik.print_class_load_logging(_loader_data, module_entry, _stream);
//        }
//
//        JFR_ONLY(INIT_ID(ik);)
//
//        // If we reach here, all is well.
//        // Now remove the InstanceKlass* from the _klass_to_deallocate field
//        // in order for it to not be destroyed in the ClassFileParser destructor.
//        set_klass_to_deallocate(null);
//
//        // it's official
//        set_klass(ik);
//    }


    public int vtable_size() { return _vtable_size; }
    public int itable_size()  { return _itable_size; }

    public int this_class_index()  { return _this_class_index; }

    public boolean is_hidden()  { return _is_hidden; }
    public boolean is_interface()  { return _access_flags.isInterface(); }

    public ClassLoaderData loader_data()  { return _loader_data; }
    public Symbol class_name()  { return _class_name; }
    public InstanceKlass super_klass()  { return _super_klass; }

    public int/*ReferenceType*/ reference_type()  { return _rt; }
    public AccessFlags access_flags()  { return _access_flags; }

    public boolean is_internal()  { return INTERNAL == _pub_level; }

    public int static_field_size() {
        if (_field_info==null){
            throw new IllegalStateException("invariant");
        }
        return _field_info._static_field_size;
    }

    public int total_oop_map_count() {
        if (_field_info==null){
            throw new IllegalStateException("invariant");
        }
        return _field_info.oop_map_blocks._nonstatic_oop_map_count;
    }

    public int layout_size() {
        if (_field_info==null){
            throw new IllegalStateException("invariant");
        }
        return _field_info._instance_size;
    }


    private static class AnnotationCollector {
        //[Location]
        public static final int
                _in_field = 0,
                _in_method = 1,
                _in_class = 2;
        //[END]

        //[ID]
        public static final int
                _unknown = 0,
                _method_CallerSensitive = 1,
                _method_ForceInline = 2,
                _method_DontInline = 3,
                _method_InjectedProfile = 4,
                _method_LambdaForm_Compiled = 5,
                _method_Hidden = 6,
                _method_Scoped = 7,
                _method_IntrinsicCandidate = 8,
                _jdk_internal_vm_annotation_Contended = 9,
                _field_Stable = 10,
                _jdk_internal_vm_annotation_ReservedStackAccess = 11,
                _jdk_internal_ValueBased = 12,
                _annotation_LIMIT = 13;
        //[END]
        private final int _location;
        private int _annotations_present;
        private int/*u2*/ _contended_group;

        public AnnotationCollector(int location) {
            _location = location;
            _annotations_present = 0;
            _contended_group = 0;
            if (_annotation_LIMIT <= JVM.intSize * (1 << 3)) {
                throw new IllegalArgumentException();
            }
        }

        // If this annotation name has an ID, report it (or _none).
        public int/*ID*/ annotation_index(ClassLoaderData loader_data, Symbol name, boolean can_access_vm_annotations) {
            // Privileged code can use all annotations.  Other code silently drops some.
            boolean privileged = loader_data.equals(ClassLoaderData.null_class_loader_data) ||
                    loader_data.getClassLoader() == ClassLoader.getPlatformClassLoader() ||
                    can_access_vm_annotations;
            String s = name.toString();
            switch (s) {
                case "Ljdk/internal/reflect/CallerSensitive;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_CallerSensitive;
                case "Ljdk/internal/vm/annotation/ForceInline;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_ForceInline;
                case "Ljdk/internal/vm/annotation/DontInline;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_DontInline;
                case "Ljava/lang/invoke/InjectedProfile;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_InjectedProfile;
                case "Ljava/lang/invoke/LambdaForm$Compiled;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_LambdaForm_Compiled;
                case "Ljdk/internal/vm/annotation/Hidden;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_Hidden;
                case "Ljdk/internal/misc/ScopedMemoryAccess$Scoped;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_Scoped;
                case "Ljdk/internal/vm/annotation/IntrinsicCandidate;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (!privileged) break;  // only allow in privileged code
                    return _method_IntrinsicCandidate;
                case "Ljdk/internal/vm/annotation/Stable;":
                    if (_location != _in_field) break;  // only allow for fields
                    if (!privileged) break;  // only allow in privileged code
                    return _field_Stable;
                case "Ljdk/internal/vm/annotation/Contended;":
                    if (_location != _in_field && _location != _in_class) {
                        break;  // only allow for fields and classes
                    }
                    if (!JVM.enableContended || (JVM.restrictContended && !privileged)) {
                        break;  // honor privileges
                    }
                    return _jdk_internal_vm_annotation_Contended;
                case "Ljdk/internal/vm/annotation/ReservedStackAccess;":
                    if (_location != _in_method) break;  // only allow for methods
                    if (JVM.restrictReservedStack && !privileged) break; // honor privileges
                    return _jdk_internal_vm_annotation_ReservedStackAccess;
                case "Ljdk/internal/ValueBased;":
                    if (_location != _in_class) break;  // only allow for classes
                    if (!privileged) break;  // only allow in priviledged code
                    return _jdk_internal_ValueBased;
                default:
                    break;
            }
            return _unknown;
        }

        // Set the annotation name:
        void set_annotation(int/*ID*/ id) {
            if (!(id >= 0 && id < _annotation_LIMIT)) {
                throw new IllegalArgumentException("oob");
            }
            _annotations_present |= JVM.nthBit(id);
        }

        void remove_annotation(int/*ID*/ id) {
            if (!(id >= 0 && id < _annotation_LIMIT)) {
                throw new IllegalArgumentException("oob");
            }
            _annotations_present &= ~JVM.nthBit(id);
        }

        // Report if the annotation is present.
        boolean has_any_annotations() {
            return _annotations_present != 0;
        }

        boolean has_annotation(int/*ID*/ id) {
            return (JVM.nthBit(id) & _annotations_present) != 0;
        }

        void set_contended_group(int/*u2*/ group) {
            _contended_group = group;
        }

        int/*u2*/ contended_group() {
            return _contended_group;
        }

        boolean is_contended() {
            return has_annotation(_jdk_internal_vm_annotation_Contended);
        }

        void set_stable(boolean stable) {
            set_annotation(_field_Stable);
        }

        boolean is_stable() {
            return has_annotation(_field_Stable);
        }
    }

    private static class ClassAnnotationCollector extends AnnotationCollector {
        public ClassAnnotationCollector() {
            super(_in_class);
        }

        void apply_to(InstanceKlass ik) {
            if (ik == null) {
                throw new NullPointerException("invariant");
            }
            if (has_annotation(_jdk_internal_vm_annotation_Contended)) {
                ik.setIsContended(is_contended());
            }
            if (has_annotation(_jdk_internal_ValueBased)) {
                ik.setHasValueBasedClassAnnotation();
                if (JVM.diagnoseSyncOnValueBasedClasses!=0) {
                    ik.setAccessFlags(ik.getAccessFlags().flags | AccessFlags.JVM_ACC_IS_VALUE_BASED_CLASS);
                    ik.setPrototypeHeader(MarkWord.PROTOTYPE);
                }
            }
        }
    }

    private static class FieldAllocationCount {
        //[FieldAllocationType]
        public static final int STATIC_OOP = 0,           // Oops
                STATIC_BYTE = 1,          // Boolean, Byte, char
                STATIC_SHORT = 2,         // shorts
                STATIC_WORD = 3,          // ints
                STATIC_DOUBLE = 4,        // aligned long or double
                NONSTATIC_OOP = 5,
                NONSTATIC_BYTE = 6,
                NONSTATIC_SHORT = 7,
                NONSTATIC_WORD = 8,
                NONSTATIC_DOUBLE = 9,
                MAX_FIELD_ALLOCATION_TYPE = 10,
                BAD_ALLOCATION_TYPE = -1;
        //[END]

        public final static int[]/*FieldAllocationType*/ _basic_type_to_atype = new int[]{
                BAD_ALLOCATION_TYPE, // 0
                BAD_ALLOCATION_TYPE, // 1
                BAD_ALLOCATION_TYPE, // 2
                BAD_ALLOCATION_TYPE, // 3
                NONSTATIC_BYTE,     // T_BOOLEAN     =  4,
                NONSTATIC_SHORT,     // T_CHAR        =  5,
                NONSTATIC_WORD,      // T_FLOAT       =  6,
                NONSTATIC_DOUBLE,    // T_DOUBLE      =  7,
                NONSTATIC_BYTE,      // T_BYTE        =  8,
                NONSTATIC_SHORT,     // T_SHORT       =  9,
                NONSTATIC_WORD,      // T_INT         = 10,
                NONSTATIC_DOUBLE,    // T_LONG        = 11,
                NONSTATIC_OOP,       // T_OBJECT      = 12,
                NONSTATIC_OOP,       // T_ARRAY       = 13,
                BAD_ALLOCATION_TYPE, // T_VOID        = 14,
                BAD_ALLOCATION_TYPE, // T_ADDRESS     = 15,
                BAD_ALLOCATION_TYPE, // T_NARROWOOP   = 16,
                BAD_ALLOCATION_TYPE, // T_METADATA    = 17,
                BAD_ALLOCATION_TYPE, // T_NARROWKLASS = 18,
                BAD_ALLOCATION_TYPE, // T_CONFLICT    = 19,
                BAD_ALLOCATION_TYPE, // 0
                BAD_ALLOCATION_TYPE, // 1
                BAD_ALLOCATION_TYPE, // 2
                BAD_ALLOCATION_TYPE, // 3
                STATIC_BYTE,        // T_BOOLEAN     =  4,
                STATIC_SHORT,        // T_CHAR        =  5,
                STATIC_WORD,         // T_FLOAT       =  6,
                STATIC_DOUBLE,       // T_DOUBLE      =  7,
                STATIC_BYTE,         // T_BYTE        =  8,
                STATIC_SHORT,        // T_SHORT       =  9,
                STATIC_WORD,         // T_INT         = 10,
                STATIC_DOUBLE,       // T_LONG        = 11,
                STATIC_OOP,          // T_OBJECT      = 12,
                STATIC_OOP,          // T_ARRAY       = 13,
                BAD_ALLOCATION_TYPE, // T_VOID        = 14,
                BAD_ALLOCATION_TYPE, // T_ADDRESS     = 15,
                BAD_ALLOCATION_TYPE, // T_NARROWOOP   = 16,
                BAD_ALLOCATION_TYPE, // T_METADATA    = 17,
                BAD_ALLOCATION_TYPE, // T_NARROWKLASS = 18,
                BAD_ALLOCATION_TYPE, // T_CONFLICT    = 19,
        };

        public int[] count = new int[MAX_FIELD_ALLOCATION_TYPE];

        public FieldAllocationCount() {
            Arrays.fill(count, 0);
        }

        public static int /*FieldAllocationType*/ basic_type_to_atype(boolean is_static, int /*BasicType*/ type) {
            if (!(type >= BasicType.T_BOOLEAN && type < BasicType.T_VOID)) {
                throw new IllegalArgumentException("only allowable values");
            }
            int result = _basic_type_to_atype[type + (is_static ? (BasicType.T_CONFLICT + 1) : 0)];
            if (result == BAD_ALLOCATION_TYPE) {
                throw new RuntimeException("bad type");
            }
            return result;
        }

        public void update(boolean is_static, int/*BasicType*/ type) {
            int atype = basic_type_to_atype(is_static, type);
            if (atype != BAD_ALLOCATION_TYPE) {
                // Make sure there is no overflow with injected fields.
                if (count[atype] >= 0xFFFF) {
                    throw new IllegalStateException("More than 65535 fields");
                }
                count[atype]++;
            }
        }
    }
}
