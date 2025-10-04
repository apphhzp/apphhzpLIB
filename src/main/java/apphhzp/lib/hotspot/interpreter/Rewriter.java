package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.classfile.VMClasses;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantPoolCache;
import apphhzp.lib.hotspot.oops.constant.ConstantTag;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.prims.MethodHandles;
import apphhzp.lib.hotspot.prims.VMIntrinsics;
import apphhzp.lib.hotspot.runtime.bytecode.Bytes;
import apphhzp.lib.hotspot.runtime.bytecode.RawBytecodeStream;
import apphhzp.lib.hotspot.runtime.fieldDescriptor;
import apphhzp.lib.hotspot.runtime.signature.Signature;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.lang.invoke.VarHandle;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** The Rewriter adds caches to the constant pool and rewrites bytecode indices
 * pointing into the constant pool for better interpreter performance.*/
public class Rewriter {

    private InstanceKlass _klass;
    private ConstantPool _pool;
    private VMTypeArray<Method>_methods;
    private IntList _cp_map;
    private IntList _cp_cache_map;  // for Methodref, Fieldref,
    // InterfaceMethodref and InvokeDynamic
    private IntList _reference_map; // maps from cp index to resolved_refs index (or -1)
    private IntList _resolved_references_map; // for strings, methodHandle, methodType
    private IntList _invokedynamic_references_map; // for invokedynamic resolved refs
    private IntList _method_handle_invokers;
    private int _resolved_reference_limit;

    // For mapping invokedynamic bytecodes, which are discovered during method
    // scanning.  The invokedynamic entries are added at the end of the cpCache.
    // If there are any invokespecial/InterfaceMethodref special case bytecodes,
    // these entries are added before invokedynamic entries so that the
    // invokespecial bytecode 16 bit index doesn't overflow.
    private IntList _invokedynamic_cp_cache_map;

    // For patching.
    private @RawCType("GrowableArray<address>*") LongList _patch_invokedynamic_bcps;
    private @RawCType("GrowableArray<int>*") IntList _patch_invokedynamic_refs;

    private void init_maps(int length) {
        _cp_map=new IntArrayList(length+1);
        for (int i=0;i<=length;i++){
            _cp_map.add(i,-1);
        }

        _cp_cache_map.size(0);
        // Also cache resolved objects, in another different cache.
        _reference_map=new IntArrayList(length+1);
        for (int i=0;i<=length;i++){
            _reference_map.add(i,-1);
        }

        _method_handle_invokers.size(0);
        _resolved_references_map.size(0);
        _invokedynamic_references_map.size(0);
        _resolved_reference_limit = -1;
        _first_iteration_cp_cache_limit = -1;

        // invokedynamic specific fields
        _invokedynamic_cp_cache_map.size(0);
        _patch_invokedynamic_bcps = new LongArrayList(length / 4);
        _patch_invokedynamic_refs = new IntArrayList(length / 4);
    }

    private int _first_iteration_cp_cache_limit;

    private void record_map_limits() {
        // Record initial size of the two arrays generated for the CP cache
        // relative to walking the constant pool.
        _first_iteration_cp_cache_limit = _cp_cache_map.size();
        _resolved_reference_limit = _resolved_references_map.size();
    }

    private int cp_cache_delta() {
        // How many cp cache entries were added since recording map limits after
        // cp cache initialization?
        if (_first_iteration_cp_cache_limit == -1){
            throw new RuntimeException("only valid after first iteration");
        }
        return _cp_cache_map.size() - _first_iteration_cp_cache_limit;
    }

    private int cp_entry_to_cp_cache(int i) {
        if (!(has_cp_cache(i))){
            throw new IndexOutOfBoundsException("oob");
        }
        return _cp_map.getInt(i);
    }

    private boolean has_cp_cache(int i) {
        return Integer.compareUnsigned(i,_cp_map.size())  < 0  && _cp_map.getInt(i) >= 0;
    }

    private int add_map_entry(int cp_index, IntList cp_map, IntList cp_cache_map) {
        if (!(cp_map.getInt(cp_index) == -1)){
            throw new RuntimeException("not twice on same cp_index");
        }
        cp_cache_map.add(cp_index);
        int cache_index = cp_cache_map.size()-1;
        cp_map.set(cp_index, cache_index);
        return cache_index;
    }

    private int add_cp_cache_entry(int cp_index) {
        if (_pool.tag_at(cp_index) == ConstantTag.InvokeDynamic){
            throw new RuntimeException("use indy version");
        }
        if (!(_first_iteration_cp_cache_limit == -1)){
            throw new RuntimeException("do not add cache entries after first iteration");
        }
        int cache_index = add_map_entry(cp_index,  _cp_map, _cp_cache_map);
        assert (cp_entry_to_cp_cache(cp_index) == cache_index);
        assert (cp_cache_entry_pool_index(cache_index) == cp_index);
        return cache_index;
    }

    private int add_invokedynamic_cp_cache_entry(int cp_index) {
        if (!(_pool.tag_at(cp_index) == ConstantTag.InvokeDynamic)){
            throw new RuntimeException("use non-indy version");
        }
        if (!(_first_iteration_cp_cache_limit >= 0)){
            throw new RuntimeException("add indy cache entries after first iteration");
        }
        // add to the invokedynamic index map.
        _invokedynamic_cp_cache_map.add(cp_index);
        int cache_index = _invokedynamic_cp_cache_map.size()-1;
        // do not update _cp_map, since the mapping is one-to-many
        if (!(invokedynamic_cp_cache_entry_pool_index(cache_index) == cp_index)){
            throw new RuntimeException();
        }
        // this index starts at one but in the bytecode it's appended to the end.
        return cache_index + _first_iteration_cp_cache_limit;
    }

    private int invokedynamic_cp_cache_entry_pool_index(int cache_index) {
        int cp_index = _invokedynamic_cp_cache_map.getInt(cache_index);
        return cp_index;
    }

    // add a new CP cache entry beyond the normal cache for the special case of
    // invokespecial with InterfaceMethodref as cpool operand.
    private int add_invokespecial_cp_cache_entry(int cp_index) {
        if (!(_first_iteration_cp_cache_limit >= 0)){
            throw new RuntimeException("add these special cache entries after first iteration");
        }
        // Don't add InterfaceMethodref if it already exists at the end.
        for (int i = _first_iteration_cp_cache_limit; i < _cp_cache_map.size(); i++) {
            if (cp_cache_entry_pool_index(i) == cp_index) {
                return i;
            }
        }
        _cp_cache_map.add(cp_index);
        int cache_index = _cp_cache_map.size()-1;
        if (!(cache_index >= _first_iteration_cp_cache_limit)){
            throw new RuntimeException();
        }
        // do not update _cp_map, since the mapping is one-to-many
        if (!(cp_cache_entry_pool_index(cache_index) == cp_index)){
            throw new RuntimeException();
        }
        return cache_index;
    }

    private int cp_entry_to_resolved_references(int cp_index) {
        if (!(has_entry_in_resolved_references(cp_index))){
            throw new RuntimeException("oob");
        }
        return _reference_map.getInt(cp_index);
    }

    private boolean has_entry_in_resolved_references(int cp_index) {
        return Integer.compareUnsigned(cp_index,_reference_map.size())<0  && _reference_map.getInt(cp_index) >= 0;
    }

    // add a new entry to the resolved_references map
    private int add_resolved_references_entry(int cp_index) {
        int ref_index = add_map_entry(cp_index,  _reference_map, _resolved_references_map);
        if (!(cp_entry_to_resolved_references(cp_index) == ref_index)){
            throw new RuntimeException();
        }
        return ref_index;
    }

    // add a new entry to the resolved_references map (for invokedynamic and invokehandle only)
    private int add_invokedynamic_resolved_references_entry(int cp_index, int cache_index) {
        if (!(_resolved_reference_limit >= 0)){
            throw new RuntimeException("must add indy refs after first iteration");
        }
        _resolved_references_map.add(cp_index);
        int ref_index = _resolved_references_map.size()-1;  // many-to-one
        if (!(ref_index >= _resolved_reference_limit)){
            throw new RuntimeException();
        }
        while(ref_index>=_invokedynamic_references_map.size()){
            _invokedynamic_references_map.add(-1);
        }
        _invokedynamic_references_map.set(ref_index, cache_index);
        return ref_index;
    }

    private int resolved_references_entry_to_pool_index(int ref_index) {
        int cp_index = _resolved_references_map.getInt(ref_index);
        return cp_index;
    }

    // Access the contents of _cp_cache_map to determine CP cache layout.
    private int cp_cache_entry_pool_index(int cache_index) {
        int cp_index = _cp_cache_map.getInt(cache_index);
        return cp_index;
    }

    // All the work goes in here:
    private Rewriter(InstanceKlass klass, ConstantPool cpool, VMTypeArray<Method> methods){
        /* _klass(klass),
    _pool(cpool),
    _methods(methods),
    _cp_map(cpool->length()),
    _cp_cache_map(cpool->length() / 2),
    _reference_map(cpool->length()),
    _resolved_references_map(cpool->length() / 2),
    _invokedynamic_references_map(cpool->length() / 2),
    _method_handle_invokers(cpool->length()),
    _invokedynamic_cp_cache_map(cpool->length() / 4)*/
        _klass=klass;
        _pool=cpool;
        _methods=methods;
        _cp_map=new IntArrayList(cpool.length());
        _cp_cache_map=new IntArrayList(cpool.length()/2);
        _reference_map=new IntArrayList(cpool.length());
        _resolved_references_map=new IntArrayList(cpool.length() / 2);
        _invokedynamic_references_map=new IntArrayList(cpool.length() / 2);
        _method_handle_invokers=new IntArrayList(cpool.length());
        _invokedynamic_cp_cache_map=new IntArrayList(cpool.length() / 4);
        // Rewrite bytecodes - exception here exits.
        rewrite_bytecodes();

        // Stress restoring bytecodes
        if (JVM.getFlag("StressRewriter").getBool()) {
            restore_bytecodes();
            rewrite_bytecodes();
        }

        // allocate constant pool cache, now that we've seen all the bytecodes
        make_constant_pool_cache();

        // Restore bytecodes to their unrewritten state if there are exceptions
        // rewriting bytecodes or allocating the cpCache
//        if (HAS_PENDING_EXCEPTION) {
//            restore_bytecodes();
//            return;
//        }

        // Relocate after everything, but still do this under the is_rewritten flag,
        // so methods with jsrs in custom class lists in aren't attempted to be
        // rewritten in the RO section of the shared archive.
        // Relocated bytecodes don't have to be restored, only the cp cache entries
        int len = _methods.length();
        for (int i = len-1; i >= 0; i--) {
            Method m=(_methods.get(i));

            if (m.has_jsrs()) {
                m = rewrite_jsrs(m);
                // Restore bytecodes to their unrewritten state if there are exceptions
                // relocating bytecodes.  If some are relocated, that is ok because that
                // doesn't affect constant pool to cpCache rewriting.
//                if (HAS_PENDING_EXCEPTION) {
//                    restore_bytecodes(THREAD);
//                    return;
//                }
                // Method might have gotten rewritten.
                methods.set(i, m);
            }
        }
    }

    private void compute_index_maps(){
        final int length  = _pool.length();
        init_maps(length);
        boolean saw_mh_symbol = false;
        for (int i = 0; i < length; i++) {
            int tag = _pool.tag_at(i);
            switch (tag) {
                case ConstantTag.InterfaceMethodref:
                case ConstantTag.Fieldref          : // fall through
                case ConstantTag.Methodref         : // fall through
                    add_cp_cache_entry(i);
                    break;
                case ConstantTag.Dynamic:
                    if (!(_pool.has_dynamic_constant())){
                        throw new RuntimeException("constant pool's _has_dynamic_constant flag not set");
                    }
                    add_resolved_references_entry(i);
                    break;
                case ConstantTag.String            : // fall through
                case ConstantTag.MethodHandle      : // fall through
                case ConstantTag.MethodType        : // fall through
                    add_resolved_references_entry(i);
                    break;
                case ConstantTag.Utf8:
                    if (_pool.symbol_at(i).toString().equals("java/lang/invoke/MethodHandle")||
                        _pool.symbol_at(i).toString().equals("java/lang/invoke/VarHandle")){
                        saw_mh_symbol = true;
                    }
                    break;
            }
        }

        // Record limits of resolved reference map for constant pool cache indices
        record_map_limits();

        if (!(_cp_cache_map.size() - 1 <= 65535)){
            throw new RuntimeException("all cp cache indexes fit in a u2");
        }

        if (saw_mh_symbol) {
            //_method_handle_invokers.at_grow(length, 0);
            while (_method_handle_invokers.size()<=length){
                _method_handle_invokers.add(0);
            }
        }
    }
    private void make_constant_pool_cache(){
        ClassLoaderData loader_data = _pool.pool_holder().getClassLoaderData();
        ConstantPoolCache cache =
                ConstantPoolCache.allocate( _cp_cache_map,
                _invokedynamic_cp_cache_map,
                _invokedynamic_references_map);

        // initialize object cache in constant pool
        _pool.set_cache(cache);
        cache.set_constant_pool(_pool);

        // _resolved_references is stored in pool->cache(), so need to be done after
        // the above lines.
        _pool.initialize_resolved_references(_resolved_references_map,
                _resolved_reference_limit);

        // Clean up constant pool cache if initialize_resolved_references() failed.
//        if (HAS_PENDING_EXCEPTION) {
//            MetadataFactory::free_metadata(loader_data, cache);
//            _pool->set_cache(NULL);  // so the verifier isn't confused
//        } else {
//            DEBUG_ONLY(
//            if (DumpSharedSpaces) {
//                cache->verify_just_initialized();
//            })
//        }
    }
    private void scan_method(Method method, boolean reverse, boolean[] invokespecial_error){

        int nof_jsrs = 0;
        boolean has_monitor_bytecodes = false;
        @RawCType("Bytecodes::Code")int c;

        // Bytecodes and their length
        final @RawCType("address")long code_base = method.code_base();
        final int code_length = method.code_size();

        int bc_length;
        for (int bci = 0; bci < code_length; bci += bc_length) {
            @RawCType("address")long bcp = code_base + bci;
            int prefix_length = 0;
            c = unsafe.getByte(bcp)&0xff;

            // Since we have the code, see if we can get the length
            // directly. Some more complicated bytecodes will report
            // a length of zero, meaning we need to make another method
            // call to calculate the length.
            bc_length = Bytecodes.length_for(c);
            if (bc_length == 0) {
                bc_length = Bytecodes.length_at(method, bcp);

                // length_at will put us at the bytecode after the one modified
                // by 'wide'. We don't currently examine any of the bytecodes
                // modified by wide, but in case we do in the future...
                if (c == Bytecodes.Code._wide) {
                    prefix_length = 1;
                    c = unsafe.getByte(bcp+1)&0xff;
                }
            }

            // Continuing with an invalid bytecode will fail in the loop below.
            // So guarantee here.
            if (!(bc_length > 0)){
                throw new RuntimeException("Verifier should have caught this invalid bytecode");
            }

            switch (c) {
                case Bytecodes.Code._lookupswitch   : {
                    if (!JVM.isZERO){
                        Bytecode_lookupswitch bc=new Bytecode_lookupswitch(method, bcp);
                        unsafe.putByte(bcp, (byte) (bc.number_of_pairs() < JVM.getFlag("BinarySwitchThreshold").getIntx()
                                                        ? Bytecodes.Code._fast_linearswitch
                                                        : Bytecodes.Code._fast_binaryswitch));
                    }
                    break;
                }
                case Bytecodes.Code._fast_linearswitch:
                case Bytecodes.Code._fast_binaryswitch: {
                    if (!JVM.isZERO){
                        unsafe.putByte(bcp, (byte) Bytecodes.Code._lookupswitch);
                    }
                    break;
                }

                case Bytecodes.Code._invokespecial  : {
                    rewrite_invokespecial(bcp, prefix_length+1, reverse, invokespecial_error);
                    break;
                }

                case Bytecodes.Code._putstatic      :
                case Bytecodes.Code._putfield       : {
                    if (!reverse) {
                        // Check if any final field of the class given as parameter is modified
                        // outside of initializer methods of the class. Fields that are modified
                        // are marked with a flag. For marked fields, the compilers do not perform
                        // constant folding (as the field can be changed after initialization).
                        //
                        // The check is performed after verification and only if verification has
                        // succeeded. Therefore, the class is guaranteed to be well-formed.
                        InstanceKlass klass = method.method_holder();
                        @RawCType("u2")int bc_index = Bytes.get_Java_u2(bcp + prefix_length + 1);
                        ConstantPool cp=(method.constants());
                        Symbol ref_class_name = cp.klass_name_at(cp.klass_ref_index_at(bc_index));

                        if (klass.name() == ref_class_name) {
                            Symbol field_name = cp.name_ref_at(bc_index);
                            Symbol field_sig = cp.signature_ref_at(bc_index);

                            fieldDescriptor fd=new fieldDescriptor();
                            if (klass.find_field(field_name, field_sig, fd) != null) {
                                if (fd.access_flags().isFinal()) {
                                    if (fd.access_flags().isStatic()) {
                                        if (!method.is_static_initializer()) {
                                            fd.set_has_initialized_final_update(true);
                                        }
                                    } else {
                                        if (!method.is_object_initializer()) {
                                            fd.set_has_initialized_final_update(true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // fall through
                case Bytecodes.Code._getstatic      : // fall through
                case Bytecodes.Code._getfield       : // fall through
                case Bytecodes.Code._invokevirtual  : // fall through
                case Bytecodes.Code._invokestatic   :
                case Bytecodes.Code._invokeinterface:
                case Bytecodes.Code._invokehandle   : // if reverse=true
                    rewrite_member_reference(bcp, prefix_length+1, reverse);
                    break;
                case Bytecodes.Code._invokedynamic:
                    rewrite_invokedynamic(bcp, prefix_length+1, reverse);
                    break;
                case Bytecodes.Code._ldc:
                case Bytecodes.Code._fast_aldc:  // if reverse=true
                    maybe_rewrite_ldc(bcp, prefix_length+1, false, reverse);
                    break;
                case Bytecodes.Code._ldc_w:
                case Bytecodes.Code._fast_aldc_w:  // if reverse=true
                    maybe_rewrite_ldc(bcp, prefix_length+1, true, reverse);
                    break;
                case Bytecodes.Code._jsr            : // fall through
                case Bytecodes.Code._jsr_w          : nof_jsrs++;                   break;
                case Bytecodes.Code._monitorenter   : // fall through
                case Bytecodes.Code._monitorexit    : has_monitor_bytecodes = true; break;

                default: break;
            }
        }

        // Update access flags
        if (has_monitor_bytecodes) {
            method.set_has_monitor_bytecodes();
        }

        // The present of a jsr bytecode implies that the method might potentially
        // have to be rewritten, so we run the oopMapGenerator on the method
        if (nof_jsrs > 0) {
            method.set_has_jsrs();
            // Second pass will revisit this method.
            if (!(method.has_jsrs())){
                throw new RuntimeException("didn't we just set this?");
            }
        }
    }
    private void rewrite_Object_init(Method method){
        RawBytecodeStream bcs=new RawBytecodeStream(method);
        while (!bcs.is_last_bytecode()) {
            @RawCType("Bytecodes::Code")int opcode = bcs.raw_next();
            switch (opcode) {
                case Bytecodes.Code._return:
                    unsafe.putByte(bcs.bcp(), (byte) Bytecodes.Code._return_register_finalizer);
                    break;

                case Bytecodes.Code._istore:
                case Bytecodes.Code._lstore:
                case Bytecodes.Code._fstore:
                case Bytecodes.Code._dstore:
                case Bytecodes.Code._astore:
                    if (bcs.get_index() != 0)
                        continue;

                    // fall through
                case Bytecodes.Code._istore_0:
                case Bytecodes.Code._lstore_0:
                case Bytecodes.Code._fstore_0:
                case Bytecodes.Code._dstore_0:
                case Bytecodes.Code._astore_0:
                    throw new IncompatibleClassChangeError("can't overwrite local 0 in Object.<init>");
                default:
                    break;
            }
        }
    }
    private void rewrite_member_reference(@RawCType("address")long bcp, int offset, boolean reverse){
        @RawCType("address")long p = bcp + offset;
        if (!reverse) {
            int  cp_index    = Bytes.get_Java_u2(p);
            int  cache_index = cp_entry_to_cp_cache(cp_index);
            Bytes.put_native_u2(p, cache_index);
            if (!_method_handle_invokers.isEmpty())
                maybe_rewrite_invokehandle(p - 1, cp_index, cache_index, reverse);
        } else {
            int cache_index = Bytes.get_native_u2(p);
            int pool_index = cp_cache_entry_pool_index(cache_index);
            Bytes.put_Java_u2(p, pool_index);
            if (!_method_handle_invokers.isEmpty())
                maybe_rewrite_invokehandle(p - 1, pool_index, cache_index, reverse);
        }
    }
    private void maybe_rewrite_invokehandle(@RawCType("address")long opc, int cp_index, int cache_index, boolean reverse){
        if (!reverse) {
            if ((unsafe.getByte(opc)&0xff)== Bytecodes.Code._invokevirtual ||
                    // allow invokespecial as an alias, although it would be very odd:
                    (unsafe.getByte(opc)&0xff) == Bytecodes.Code._invokespecial) {
                if (!(_pool.tag_at(cp_index)==ConstantTag.Methodref)){
                    throw new RuntimeException("wrong index");
                }
                // Determine whether this is a signature-polymorphic method.
                if (cp_index >= _method_handle_invokers.size()) {
                    return;
                }
                int status = _method_handle_invokers.getInt(cp_index);
                if (!(status >= -1 && status <= 1)){
                    throw new RuntimeException("oob tri-state");
                }
                if (status == 0) {
                    if (_pool.klass_ref_at_noresolve(cp_index).equals(Symbol.getVMSymbol("java/lang/invoke/MethodHandle")) &&
                            MethodHandles.is_signature_polymorphic_name(VMClasses.methodHandleKlass(),
                            _pool.name_ref_at(cp_index))) {
                        // we may need a resolved_refs entry for the appendix
                        add_invokedynamic_resolved_references_entry(cp_index, cache_index);
                        status = +1;
                    } else if (_pool.klass_ref_at_noresolve(cp_index).equals(Symbol.getVMSymbol("java/lang/invoke/VarHandle")) &&
                            MethodHandles.is_signature_polymorphic_name(Klass.asKlass(VarHandle.class),
                            _pool.name_ref_at(cp_index))) {
                        // we may need a resolved_refs entry for the appendix
                        add_invokedynamic_resolved_references_entry(cp_index, cache_index);
                        status = +1;
                    } else {
                        status = -1;
                    }
                    _method_handle_invokers.set(cp_index,status);
                }
                // We use a special internal bytecode for such methods (if non-static).
                // The basic reason for this is that such methods need an extra "appendix" argument
                // to transmit the call site's intended call type.
                if (status > 0) {
                    unsafe.putByte(opc, (byte) Bytecodes.Code._invokehandle);
                }
            }
        } else {
            // Do not need to look at cp_index.
            if ((unsafe.getByte(opc)&0xff) == Bytecodes.Code._invokehandle) {
                unsafe.putByte(opc, (byte) Bytecodes.Code._invokevirtual);
                // Ignore corner case of original _invokespecial instruction.
                // This is safe because (a) the signature polymorphic method was final, and
                // (b) the implementation of MethodHandle will not call invokespecial on it.
            }
        }
    }
    private void rewrite_invokedynamic(@RawCType("address")long bcp, int offset, boolean reverse){
        @RawCType("address")long p = bcp + offset;
        if (!((unsafe.getByte(p-1)&0xff)==Bytecodes.Code._invokedynamic)){
            throw new RuntimeException("not invokedynamic bytecode");
        }
        if (!reverse) {
            int cp_index = Bytes.get_Java_u2(p);
            int cache_index = add_invokedynamic_cp_cache_entry(cp_index);
            int resolved_index = add_invokedynamic_resolved_references_entry(cp_index, cache_index);
            // Replace the trailing four bytes with a CPC index for the dynamic
            // call site.  Unlike other CPC entries, there is one per bytecode,
            // not just one per distinct CP entry.  In other words, the
            // CPC-to-CP relation is many-to-one for invokedynamic entries.
            // This means we must use a larger index size than u2 to address
            // all these entries.  That is the main reason invokedynamic
            // must have a five-byte instruction format.  (Of course, other JVM
            // implementations can use the bytes for other purposes.)
            // Note: We use native_u4 format exclusively for 4-byte indexes.
            Bytes.put_native_u4(p, ConstantPool.encode_invokedynamic_index(cache_index));
            // add the bcp in case we need to patch this bytecode if we also find a
            // invokespecial/InterfaceMethodref in the bytecode stream
            _patch_invokedynamic_bcps.add(p);
            _patch_invokedynamic_refs.add(resolved_index);
        } else {
            int cache_index = ConstantPool.decode_invokedynamic_index(
                    Bytes.get_native_u4(p));
            // We will reverse the bytecode rewriting _after_ adjusting them.
            // Adjust the cache index by offset to the invokedynamic entries in the
            // cpCache plus the delta if the invokedynamic bytecodes were adjusted.
            int adjustment = cp_cache_delta() + _first_iteration_cp_cache_limit;
            int cp_index = invokedynamic_cp_cache_entry_pool_index(cache_index - adjustment);
            if (!(_pool.tag_at(cp_index)==ConstantTag.InvokeDynamic)){
                throw new RuntimeException("wrong index");
            }
            // zero out 4 bytes
            Bytes.put_Java_u4(p, 0);
            Bytes.put_Java_u2(p, cp_index);
        }
    }
    private void maybe_rewrite_ldc(@RawCType("address")long bcp, int offset, boolean is_wide, boolean reverse){
        if (!reverse) {
            if (!((unsafe.getByte(bcp)&0xff) == (is_wide ? Bytecodes.Code._ldc_w : Bytecodes.Code._ldc))){
                throw new RuntimeException("not ldc bytecode");
            }
            @RawCType("address")long p = bcp + offset;
            int cp_index = is_wide ? Bytes.get_Java_u2(p) : (unsafe.getByte(p)&0xff);
            @RawCType("constantTag")int tag = _pool.tag_at(cp_index);

            if (tag==ConstantTag.MethodHandle ||
                    tag==ConstantTag.MethodType ||
                    tag==ConstantTag.String ||
                    (tag==ConstantTag.Dynamic &&
                            // keep regular ldc interpreter logic for condy primitives
                            BasicType.is_reference_type(Signature.basic_type(_pool.uncached_signature_ref_at(cp_index))))) {
                int ref_index = cp_entry_to_resolved_references(cp_index);
                if (is_wide) {
                    unsafe.putByte(bcp, (byte) Bytecodes.Code._fast_aldc_w);
                    if (!(ref_index == (ref_index&0xffff))){
                        throw new RuntimeException("index overflow");
                    }
                    Bytes.put_native_u2(p, ref_index);
                } else {
                    unsafe.putByte(bcp, (byte) Bytecodes.Code._fast_aldc);
                    if (!(ref_index == (ref_index&0xff))){
                        throw new RuntimeException("index overflow");
                    }
                    unsafe.putByte(p, (byte) ref_index);
                }
            }
        } else {
            @RawCType("Bytecodes::Code")int rewritten_bc =
                    (is_wide ? Bytecodes.Code._fast_aldc_w : Bytecodes.Code._fast_aldc);
            if ((unsafe.getByte(bcp)&0xff) == rewritten_bc) {
                @RawCType("address")long p = bcp + offset;
                int ref_index = is_wide ? Bytes.get_native_u2(p) : (unsafe.getByte(p)&0xff);
                int pool_index = resolved_references_entry_to_pool_index(ref_index);
                if (is_wide) {
                    unsafe.putByte(bcp, (byte) Bytecodes.Code._ldc_w);
                    if (!(pool_index == (pool_index&0xffff))){
                        throw new RuntimeException("index overflow");
                    }
                    Bytes.put_Java_u2(p, pool_index);
                } else {
                    unsafe.putByte(bcp, (byte) Bytecodes.Code._ldc);
                    if (!(pool_index == (pool_index&0xff))){
                        throw new RuntimeException("index overflow");
                    }
                    unsafe.putByte(p, (byte) pool_index);
                }
            }
        }
    }
    private void rewrite_invokespecial(@RawCType("address")long bcp, int offset, boolean reverse, boolean[] invokespecial_error){
        @RawCType("address")long p = bcp + offset;
        if (!reverse) {
            int cp_index = Bytes.get_Java_u2(p);
            if (_pool.tag_at(cp_index)==ConstantTag.InterfaceMethodref) {
                int cache_index = add_invokespecial_cp_cache_entry(cp_index);
                if (cache_index != (cache_index&0xffff)) {
                    invokespecial_error[0] = true;
                }
                Bytes.put_native_u2(p, cache_index);
            } else {
                rewrite_member_reference(bcp, offset, reverse);
            }
        } else {
            rewrite_member_reference(bcp, offset, reverse);
        }
    }

    private void patch_invokedynamic_bytecodes(){
        // If the end of the cp_cache is the same as after initializing with the
        // cpool, nothing needs to be done.  Invokedynamic bytecodes are at the
        // correct offsets. ie. no invokespecials added
        int delta = cp_cache_delta();
        if (delta > 0) {
            int length = _patch_invokedynamic_bcps.size();
            if (!(length == _patch_invokedynamic_refs.size())){
                throw new RuntimeException("lengths should match");
            }
            for (int i = 0; i < length; i++) {
                @RawCType("address")long p = _patch_invokedynamic_bcps.getLong(i);
                int cache_index = ConstantPool.decode_invokedynamic_index(
                        Bytes.get_native_u4(p));
                Bytes.put_native_u4(p, ConstantPool.encode_invokedynamic_index(cache_index + delta));

                // invokedynamic resolved references map also points to cp cache and must
                // add delta to each.
                int resolved_index = _patch_invokedynamic_refs.getInt(i);
                if (!(_invokedynamic_references_map.getInt(resolved_index) == cache_index)){
                    throw new RuntimeException("should be the same index");
                }
                _invokedynamic_references_map.set(resolved_index, cache_index + delta);
            }
        }
    }

    // Do all the work.
    private void rewrite_bytecodes(){
        if (_pool.getCache() != null){
            throw new RuntimeException("constant pool cache must not be set yet");
        }

        // determine index maps for Method* rewriting
        compute_index_maps();

        if (JVM.getFlag("RegisterFinalizersAtInit").getBool() && _klass.name().toString().equals("java/lang/Object")) {
            boolean did_rewrite = false;
            int i = _methods.length();
            while (i-- > 0) {
                Method method = _methods.get(i);
                if (method.intrinsic_id() == VMIntrinsics.Object_init) {
                    // rewrite the return bytecodes of Object.<init> to register the
                    // object for finalization if needed.
                    rewrite_Object_init(method);
                    did_rewrite = true;
                    break;
                }
            }
            if (!did_rewrite){
                throw new RuntimeException( "must find Object::<init> to rewrite it");
            }
        }

        // rewrite methods, in two passes
        int len = _methods.length();
        boolean[] invokespecial_error = {false};

        for (int i = len-1; i >= 0; i--) {
            Method method = _methods.get(i);
            scan_method(method, false, invokespecial_error);
            if (invokespecial_error[0]) {
                // If you get an error here, there is no reversing bytecodes
                // This exception is stored for this class and no further attempt is
                // made at verifying or rewriting.
                throw new InternalError("This classfile overflows invokespecial for interfaces and cannot be loaded");
            }
        }

        // May have to fix invokedynamic bytecodes if invokestatic/InterfaceMethodref
        // entries had to be added.
        patch_invokedynamic_bytecodes();
    }

    // Revert bytecodes in case of an exception.
    private void restore_bytecodes(){
        int len = _methods.length();
        boolean[] invokespecial_error = {false};
        for (int i = len-1; i >= 0; i--) {
            Method method = _methods.get(i);
            scan_method(method, true, invokespecial_error);
            if (invokespecial_error[0]){
                throw new RuntimeException("reversing should not get an invokespecial error");
            }
        }
    }

    private static Method rewrite_jsrs(Method method){
        ResolveOopMapConflicts romc=new ResolveOopMapConflicts(method);
        Method new_method = romc.do_potential_rewrite();
        // Update monitor matching info.
        if (romc.monitor_safe()) {
            new_method.set_guaranteed_monitor_matching();
        }

        return new_method;
    }
    // Driver routine:
    public static void rewrite(InstanceKlass klass){
        ConstantPool cpool=(klass.getConstantPool());
        new Rewriter(klass, cpool, klass.getMethods());
    }
}
