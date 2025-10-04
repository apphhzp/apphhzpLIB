package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.CodeCache;
import apphhzp.lib.hotspot.code.Location;
import apphhzp.lib.hotspot.code.ScopeDesc;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.code.scope.ScopeValue;
import apphhzp.lib.hotspot.oops.method.Method;

import java.util.List;

public class CompiledVFrame extends JavaVFrame{
    protected ScopeDesc _scope;
    protected int _vframe_id;

    //StackValue resolve(ScopeValue* sv) const;
    protected BasicLock resolve_monitor_lock(Location location){
        return StackValue.resolve_monitor_lock(_fr, location);
    }
    protected StackValue create_stack_value(ScopeValue sv){
        return StackValue.create_stack_value(_fr, register_map(), sv);
    }
    @Override
    public Method method() {
        if (scope() == null) {
            // native nmethods have no scope the method is implied
            NMethod nm = code().as_nmethod();
            if (!nm.is_native_method()){
                throw new RuntimeException("must be native");
            }
            return nm.method();
        }
        return scope().method();
    }

    @Override
    public int bci() {
        int raw = raw_bci();
        return raw == JVM.invocationEntryBci ? 0 : raw;
    }
    public boolean should_reexecute(){
        if (scope() == null) {
            // native nmethods have no scope the method/bci is implied
            NMethod nm = code().as_nmethod();
            if (!nm.is_native_method()){
                throw new RuntimeException("must be native");
            }
            return false;
        }
        return scope().should_reexecute();
    }

    @Override
    public StackValueCollection locals() {
        // Natives has no scope
        if (scope() == null) {
            return new StackValueCollection(0);
        }
        List<ScopeValue>  scv_list = scope().locals();
        if (scv_list == null) {
            return new StackValueCollection(0);
        }

        // scv_list is the list of ScopeValues describing the JVM stack state.
        // There is one scv_list entry for every JVM stack state in use.
        int length = scv_list.size();
        StackValueCollection result = new StackValueCollection(length);
        for (ScopeValue scopeValue : scv_list) {
            result.add(create_stack_value(scopeValue));
        }
        return result;
    }

    @Override
    public StackValueCollection expressions() {
        // Natives has no scope
        if (scope() == null) {
            return new StackValueCollection(0);
        }
        List<ScopeValue>  scv_list = scope().expressions();
        if (scv_list == null) {
            return new StackValueCollection(0);
        }
        // scv_list is the list of ScopeValues describing the JVM stack state.
        // There is one scv_list entry for every JVM stack state in use.
        int length = scv_list.size();
        StackValueCollection result = new StackValueCollection(length);
        for (ScopeValue scopeValue : scv_list) {
            result.add(create_stack_value(scopeValue));
        }
        return result;
    }

    @Override
    public List<MonitorInfo> monitors() {
        //TODO
        throw new UnsupportedOperationException("TODO");
//        // Natives has no scope
//        if (scope() == null) {
//            CompiledMethod nm = code();
//            Method method = nm.method();
//            if (!method.is_native()){
//                throw new RuntimeException("Expect a native method");
//            }
//            if (!method.is_synchronized()) {
//                return new ArrayList<>(0);
//            }
//            // This monitor is really only needed for UseBiasedLocking, but
//            // return it in all cases for now as it might be useful for stack
//            // traces and tools as well
//            ArrayList<MonitorInfo> monitors = new ArrayList<>(1);
//            // Casting away const
//            Frame fr =  _fr;
//            MonitorInfo info = new MonitorInfo(
//                    fr.get_native_receiver(), fr.get_native_monitor(), false, false);
//            monitors.add(info);
//            return monitors;
//        }
//        List<MonitorValue> monitors = scope().monitors();
//        if (monitors == null) {
//            return new ArrayList<>(0);
//        }
//        List<MonitorInfo> result = new ArrayList<>(monitors.size());
//        for (int index = 0; index < monitors.size(); index++) {
//            MonitorValue mv = monitors.get(index);
//            ScopeValue   ov = mv.owner();
//            StackValue owner_sv = create_stack_value(ov); // it is an oop
//            if (ov.is_object() && owner_sv.obj_is_scalar_replaced()) { // The owner object was scalar replaced
//                assert(mv.eliminated(), "monitor should be eliminated for scalar replaced object");
//                // Put klass for scalar replaced object.
//                ScopeValue kv = ((ObjectValue)ov).klass();
//                assert(kv.is_constant_oop(), "klass should be oop constant for scalar replaced object");
//                Oop k=(((ConstantOopReadValue)kv).value());
//                if (!(k.getJavaObject() instanceof Class)){
//                    throw new RuntimeException("must be");
//                }
//                result.add(new MonitorInfo(k.getJavaObject(), resolve_monitor_lock(mv.basic_lock()),
//                        mv.eliminated(), true));
//            } else {
//                result.add(new MonitorInfo(owner_sv.get_obj()(), resolve_monitor_lock(mv.basic_lock()),
//                        mv.eliminated(), false));
//            }
//        }
//
//        return result;
    }
    public int vframe_id(){
        return _vframe_id;
        }

    public boolean has_ea_local_in_scope(){
        if (scope() == null) {
            // native nmethod, all objs escape
            NMethod nm = code().as_nmethod();
            if (!nm.is_native_method()){
                throw new RuntimeException("must be native");
            }
            return false;
        }
        return (scope().objects() != null) || scope().has_ea_local_in_scope();
    }
    // at call with arg escape in parameter list
    public boolean arg_escape(){
        if (scope() == null) {
            // native nmethod, all objs escape
            NMethod nm = code().as_nmethod();
            if (!nm.is_native_method()){
                throw new RuntimeException("must be native");
            }
            return false;
        }
        return scope().arg_escape();
    }

    @Override
    public void set_locals(StackValueCollection values) {
        throw new UnsupportedOperationException("Should use update_local for each local update");
    }

    // Constructors
    public CompiledVFrame(Frame fr, RegisterMap reg_map, JavaThread thread, CompiledMethod nm){
        super(fr, reg_map, thread);
        _scope  = null;
        _vframe_id = 0;
        // Compiled method (native stub or Java code)
        // native wrappers have no scope data, it is implied
        if (!nm.is_compiled() || !nm.as_compiled_method().is_native_method()) {
            _scope  = nm.scope_desc_at(_fr.pc());
        }
    }

    // Virtuals defined in vframe
    public boolean is_compiled_frame(){ return true; }
    public VFrame sender(){
        Frame f = fr().clone();
        if (scope() == null) {
            // native nmethods have no scope the method/bci is implied
            NMethod nm = code().as_nmethod();
            if (!nm.is_native_method()){
                throw new RuntimeException("must be native");
            }
            return super.sender();
        } else {
            return scope().is_top()
                    ? super.sender() : new CompiledVFrame(f, register_map(), thread(), scope().sender(), vframe_id() + 1);
        }
    }
    public boolean is_top(){
        // FIX IT: Remove this when new native stubs are in place
        if (scope() == null) {
            return true;
        }
        return scope().is_top();
    }
    // Returns the active nmethod
    public CompiledMethod  code(){
        return CodeCache.find_compiled(_fr.pc());
    }

    // Returns the scopeDesc
    public ScopeDesc scope(){
        return _scope;
    }

    // Return the compiledVFrame for the desired scope
    public CompiledVFrame at_scope(int decode_offset, int vframe_id){
        if (scope().decode_offset() != decode_offset) {
            ScopeDesc scope = this.scope().at_offset(decode_offset);
            return new CompiledVFrame(frame_pointer(), register_map(), thread(), scope, vframe_id);
        }
        if (!(_vframe_id == vframe_id)){
            throw new RuntimeException("wrong frame id");
        }
        return this;
    }

    // Returns SynchronizationEntryBCI or bci() (used for synchronization)
    public int raw_bci(){
        if (scope() == null) {
            // native nmethods have no scope the method/bci is implied
            NMethod nm = code().as_nmethod();
            if (!nm.is_native_method()){
                throw new RuntimeException("must be native");
            }
            return 0;
        }
        return scope().bci();
    }

    private CompiledVFrame(Frame fr,  RegisterMap reg_map, JavaThread thread, ScopeDesc scope, int vframe_id){
        super(fr, reg_map, thread);
        if (_scope==null){
            throw new NullPointerException("scope must be present");
        }
        _scope  = scope;
        _vframe_id = vframe_id;
    }

//    public void update_local(@RawCType("BasicType")int type, int index, jvalue value) {
//        assert(index >= 0 && index < method()->max_locals(), "out of bounds");
//        update_deferred_value(type, index, value);
//    }
//
//    public void update_stack(@RawCType("BasicType")int type, int index, jvalue value) {
//        assert(index >= 0 && index < method()->max_stack(), "out of bounds");
//        update_deferred_value(type, index + method()->max_locals(), value);
//    }
//
//    public void update_monitor(int index, MonitorInfo* val) {
//        assert(index >= 0, "out of bounds");
//        jvalue value;
//        value.l = cast_from_oop<jobject>(val->owner());
//        update_deferred_value(T_OBJECT, index + method()->max_locals() + method()->max_stack(), value);
//    }
//
//    public void update_deferred_value(BasicType type, int index, jvalue value) {
//        assert(fr().is_deoptimized_frame(), "frame must be scheduled for deoptimization");
//        GrowableArray<jvmtiDeferredLocalVariableSet*>* deferred = JvmtiDeferredUpdates::deferred_locals(thread());
//        jvmtiDeferredLocalVariableSet* locals = NULL;
//        if (deferred != NULL ) {
//            // See if this vframe has already had locals with deferred writes
//            for (int f = 0; f < deferred->length(); f++ ) {
//                if (deferred->at(f)->matches(this)) {
//                    locals = deferred->at(f);
//                    break;
//                }
//            }
//            // No matching vframe must push a new vframe
//        } else {
//            // No deferred updates pending for this thread.
//            // allocate in C heap
//            JvmtiDeferredUpdates::create_for(thread());
//            deferred = JvmtiDeferredUpdates::deferred_locals(thread());
//        }
//        if (locals == NULL) {
//            locals = new jvmtiDeferredLocalVariableSet(method(), bci(), fr().id(), vframe_id());
//            deferred->push(locals);
//            assert(locals->id() == fr().id(), "Huh? Must match");
//        }
//        locals->set_value_at(index, type, value);
//    }
}
