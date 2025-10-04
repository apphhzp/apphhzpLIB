package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.classfile.JavaClasses;
import apphhzp.lib.hotspot.classfile.VMClasses;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** CallInfo provides all the information gathered for a particular
 * linked call site after resolving it. A link is any reference
 * made from within the bytecodes of a method to an object outside of
 * that method. If the info is invalid, the link has not been resolved
 * successfully.*/
public class CallInfo {
    // Ways that a method call might be selected (or not) based on receiver type.
    // Note that an invokevirtual instruction might be linked with no_dispatch,
    // and an invokeinterface instruction might be linked with any of the three options
    public static final class CallKind {
        public static final int
        direct_call=0,                        // jump into resolved_method (must be concrete)
        vtable_call=1,                        // select recv.klass.method_at_vtable(index)
        itable_call=2,                        // select recv.klass.method_at_itable(resolved_method.holder, index)
        unknown_kind = -1;
    };

    private Klass _resolved_klass;         // static receiver klass, resolved from a symbolic reference
    private Method _resolved_method;        // static target method
    private Method _selected_method;        // dynamic (actual) target method
    private @RawCType("CallKind")int _call_kind;              // kind of call (static(=bytecode static/special +
    //               others inferred), vtable, itable)
    private int          _call_index;             // vtable or itable index of selected class method (if any)
    private Oop _resolved_appendix;      // extra argument in constant pool (if CPCE::has_appendix)
    private /*ResolvedMethodName*/ Object       _resolved_method_name;   // Object holding the ResolvedMethodName

    private void set_static(Klass resolved_klass, Method resolved_method){

    }
    private  void set_interface(Klass resolved_klass,
                     Method resolved_method,
                                Method selected_method,
                       int itable_index){

    }
    private void set_virtual(Klass resolved_klass,
                             Method resolved_method,
                             Method selected_method,
                     int vtable_index){

    }
    private void set_handle(Klass resolved_klass,
                  Method resolved_method,
                    Oop resolved_appendix){

    }
    private void set_common(Klass resolved_klass,
                  Method resolved_method,
                            Method selected_method,
                    @RawCType("CallKind")int kind,
                    int index){

    }


    public CallInfo() {
        if (JVM.ENABLE_EXTRA_CHECK){
            _call_kind  = CallInfo.CallKind.unknown_kind;
            _call_index = Method.VtableIndexFlag.garbage_vtable_index;
        }
    }

    // utility to extract an effective CallInfo from a method and an optional receiver limit
    // does not queue the method for compilation.  This also creates a ResolvedMethodName
    // object for the resolved_method.
    public CallInfo(Method resolved_method, Klass resolved_klass){
        Klass resolved_method_holder = resolved_method.method_holder();
        if (resolved_klass == null) { // 2nd argument defaults to holder of 1st
            resolved_klass = resolved_method_holder;
        }
        _resolved_klass  = resolved_klass;
        _resolved_method = resolved_method;
        _selected_method = resolved_method;
        // classify:
        @RawCType("CallKind")int kind = CallInfo.CallKind.unknown_kind;
        int index = resolved_method.vtable_index();
        if (resolved_method.can_be_statically_bound()) {
            kind = CallInfo.CallKind.direct_call;
        } else if (!resolved_method_holder.isInterface()) {
            // Could be an Object method inherited into an interface, but still a vtable call.
            kind = CallInfo.CallKind.vtable_call;
        } else if (!resolved_klass.isInterface()) {
            // A default or miranda method.  Compute the vtable index.
            index = LinkResolver.vtable_index_of_interface_method(resolved_klass, _resolved_method);
            if (!(index >= 0)){
                throw new RuntimeException("we should have valid vtable index at this point");
            }

            kind = CallInfo.CallKind.vtable_call;
        } else if (resolved_method.has_vtable_index()) {
            // Can occur if an interface redeclares a method of Object.
            if (JVM.ENABLE_EXTRA_CHECK){
                // Ensure that this is really the case.
                Klass object_klass = VMClasses.objectKlass();
                Method  object_resolved_method = object_klass.vtable().method_at(index);
                if (!(object_resolved_method.name().equals(resolved_method.name()))){
                    throw new RuntimeException("Object and interface method names should match at vtable index "+index
                            +", "+object_resolved_method.name().toString()+" != "+resolved_method.name().toString());
                }
                if (!(object_resolved_method.signature().equals(resolved_method.signature()))){
                    throw new RuntimeException("Object and interface method signatures should match at vtable index "+index
                            +", "+object_resolved_method.signature().toString()+" != "+resolved_method.signature().toString());
                }
            }
            kind = CallInfo.CallKind.vtable_call;
        } else {
            // A regular interface call.
            kind = CallInfo.CallKind.itable_call;
            index = resolved_method.itable_index();
        }
        if (!(index == Method.VtableIndexFlag.nonvirtual_vtable_index || index >= 0)){
            throw new RuntimeException("bad index "+index);
        }
        _call_kind  = kind;
        _call_index = index;
        _resolved_appendix = null;
        // Find or create a ResolvedMethod instance for this Method*
        set_resolved_method_name();
        if (JVM.ENABLE_EXTRA_CHECK){
            verify();
        }
    }

    public Klass  resolved_klass()                  { return _resolved_klass; }
    public Method resolved_method()                 { return _resolved_method; }
    public Method selected_method()                 { return _selected_method; }
    public Oop resolved_appendix()          { return _resolved_appendix; }
    public /*ResolvedMethodName*/ Object resolved_method_name()       { return _resolved_method_name; }
    // Materialize a java.lang.invoke.ResolvedMethodName for this resolved_method
    public void set_resolved_method_name(){
        if (_resolved_method==null){
            throw new NullPointerException("Should already have a Method*");
        }
        Object rmethod_name = JavaClasses.ResolvedMethodName.find_resolved_method(_resolved_method);
        _resolved_method_name = rmethod_name;
    }

    public @RawCType("BasicType")int result_type(){ return selected_method().result_type(); }
    public @RawCType("CallKind")int call_kind(){ return _call_kind; }
    public int vtable_index()   {
        // Even for interface calls the vtable index could be non-negative.
        // See CallInfo::set_interface.
        if (!(has_vtable_index() || is_statically_bound())){
            throw new RuntimeException();
        }
        if (!(call_kind() == CallKind.vtable_call || call_kind() == CallKind.direct_call)){
            throw new RuntimeException();
        }
        // The returned value is < 0 if the call is statically bound.
        // But, the returned value may be >= 0 even if the kind is direct_call.
        // It is up to the caller to decide which way to go.
        return _call_index;
    }
    public int itable_index(){
        if (!(call_kind() == CallKind.itable_call)){
            throw new IllegalStateException();
        }
        // The returned value is always >= 0, a valid itable index.
        return _call_index;
    }

    public boolean has_vtable_index(){ return _call_index >= 0 && _call_kind != CallInfo.CallKind.itable_call; }
    public boolean is_statically_bound(){ return _call_index == Method.VtableIndexFlag.nonvirtual_vtable_index; }
    public void verify(){
        if (JVM.ENABLE_EXTRA_CHECK){

        }
    }
    public void print(PrintStream os){
        String kindstr;
        switch (_call_kind) {
            case CallKind.direct_call: kindstr = "direct";  break;
            case CallKind.vtable_call: kindstr = "vtable";  break;
            case CallKind.itable_call: kindstr = "itable";  break;
            default         : kindstr = "unknown"; break;
        }
        os.println("Call "+kindstr+"@"+_call_index+" "+(_resolved_method==null ? "(none)" : _resolved_method.method_holder().external_name()+"."+_resolved_method.name()+_resolved_method.signature()));
    }
}
