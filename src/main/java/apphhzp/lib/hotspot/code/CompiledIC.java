package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.util.RawCType;


public class CompiledIC{
    private NativeCallWrapper _call;
    private @RawCType("NativeInstruction*") long _value;//patchable value cell for this IC
    private boolean _is_optimized;
    private CompiledMethod _method;
    private CompiledIC(RelocIterator iter){
        if (true){
            throw new UnsupportedOperationException("Incomplete");
        }
        this._method=iter.code();
        _call = _method.call_wrapper_at(iter.addr());
        long ic_call = _call.instruction_address();

        CompiledMethod nm = iter.code();
//        assert(ic_call != NULL, "ic_call address must be set");
//        assert(nm != NULL, "must pass compiled method");
//        assert(nm.contains(ic_call), "must be in compiled method");

        initialize_from_iter(iter);
    }

    private void initialize_from_iter(RelocIterator iter) {
        if (iter.addr()!=_call.instruction_address()){
            throw new IllegalStateException("must find ic_call");
        }
//        if (iter.type() == RelocInfo.Type.VIRTUAL_CALL_TYPE) {
//            virtual_call_Relocation* r = iter.virtual_call_reloc();
//            _is_optimized = false;
//            _value = _call.get_load_instruction(r);
//        } else {
//            if (iter.type() != RelocInfo.Type.OPT_VIRTUAL_CALL_TYPE){
//                throw new IllegalStateException("must be a virtual call");
//            }
//            _is_optimized = true;
//            _value = 0L;
//        }
    }
    public boolean is_optimized(){
        return _is_optimized;
    }
//    public boolean set_to_clean(boolean in_use) {
//
//        long/*address*/ entry = _call.get_resolve_call_stub(is_optimized());
//
//        // A zombie transition will always be safe, since the metadata has already been set to NULL, so
//        // we only need to patch the destination
//        boolean safe_transition =true;// _call->is_safe_for_patching() || !in_use || is_optimized() || SafepointSynchronize::is_at_safepoint();
//
//        if (safe_transition) {
//            // Kill any leftover stub we might have too
//            clear_ic_stub();
//            if (is_optimized()) {
//                set_ic_destination(entry);
//            } else {
//                set_ic_destination_and_value(entry, (void*)NULL);
//            }
//        } else {
//            // Unsafe transition - create stub.
//            if (!InlineCacheBuffer::create_transition_stub(this, NULL, entry)) {
//                return false;
//            }
//        }
//        // We can't check this anymore. With lazy deopt we could have already
//        // cleaned this IC entry before we even return. This is possible if
//        // we ran out of space in the inline cache buffer trying to do the
//        // set_next and we safepointed to free up space. This is a benign
//        // race because the IC entry was complete when we safepointed so
//        // cleaning it immediately is harmless.
//        // assert(is_clean(), "sanity check");
//        return true;
//    }

    public static CompiledIC CompiledIC_at(RelocIterator reloc_iter) {
        if (!(reloc_iter.type() == RelocInfo.Type.VIRTUAL_CALL_TYPE ||
                reloc_iter.type() == RelocInfo.Type.OPT_VIRTUAL_CALL_TYPE)){
            throw new IllegalArgumentException("wrong reloc. info");
        }
        return new CompiledIC(reloc_iter);
    }
}
