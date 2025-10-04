package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.scope.ObjectValue;
import apphhzp.lib.hotspot.code.scope.ScopeValue;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class ScopeDesc {
    private Method _method;
    private int           _bci;
    private boolean          _reexecute;
    private boolean          _rethrow_exception;
    private boolean          _return_oop;
    private boolean          _has_ea_local_in_scope;       // One or more NoEscape or ArgEscape objects exist in
    // any of the scopes at compiled pc.
    private boolean          _arg_escape;                  // Compiled Java call in youngest scope passes ArgEscape

    // Decoding offsets
    private int _decode_offset;
    private int _sender_decode_offset;
    private int _locals_decode_offset;
    private  int _expressions_decode_offset;
    private int _monitors_decode_offset;

    // Object pool
    private List<ScopeValue> _objects;

    // Nmethod information
    private CompiledMethod _code;
    public ScopeDesc(CompiledMethod code, PcDesc pd){
        this(code,pd,false);
    }

    public ScopeDesc(CompiledMethod code, PcDesc pd, boolean ignore_objects) {
        int obj_decode_offset = ignore_objects ? DebugInformationRecorder.serialized_null : pd.obj_decode_offset();
        _code          = code;
        _decode_offset = pd.scope_decode_offset();
        _objects       = decode_object_values(obj_decode_offset);
        _reexecute     = pd.should_reexecute();
        _rethrow_exception = pd.rethrow_exception();
        _return_oop    = pd.return_oop();
        _has_ea_local_in_scope = !ignore_objects && pd.has_ea_local_in_scope();
        _arg_escape    = !ignore_objects && pd.arg_escape();
        decode_body();
    }

    // Direct access to scope
    public ScopeDesc at_offset(int decode_offset) { return new ScopeDesc(this, decode_offset); }
    private void initialize(ScopeDesc parent, int decode_offset) {
        _code          = parent._code;
        _decode_offset = decode_offset;
        _objects       = parent._objects;
        _reexecute     = false; //reexecute only applies to the first scope
        _rethrow_exception = false;
        _return_oop    = false;
        _has_ea_local_in_scope = parent.has_ea_local_in_scope();
        _arg_escape    = false;
        decode_body();
    }

    private ScopeDesc(ScopeDesc parent) {
        initialize(parent, parent._sender_decode_offset);
    }

    private ScopeDesc(ScopeDesc parent, int decode_offset) {
        initialize(parent, decode_offset);
    }
    public void decode_body() {
        if (decode_offset() == DebugInformationRecorder.serialized_null) {
            // This is a sentinel record, which is only relevant to
            // approximate queries.  Decode a reasonable frame.
            _sender_decode_offset = DebugInformationRecorder.serialized_null;
            _method = _code.method();
            _bci = JVM.invocationEntryBci;
            _locals_decode_offset = DebugInformationRecorder.serialized_null;
            _expressions_decode_offset = DebugInformationRecorder.serialized_null;
            _monitors_decode_offset = DebugInformationRecorder.serialized_null;
        } else {
            // decode header
            DebugInfoReadStream stream  = stream_at(decode_offset());

            _sender_decode_offset = stream.read_int();
            _method = stream.read_method();
            _bci    = stream.read_bci();

            // decode offsets for body and sender
            _locals_decode_offset      = stream.read_int();
            _expressions_decode_offset = stream.read_int();
            _monitors_decode_offset    = stream.read_int();
        }
    }


    // JVM state
    public Method method()      { return _method; }
    public int          bci()       { return _bci;    }
    public boolean should_reexecute()  { return _reexecute; }
    public boolean rethrow_exception()  { return _rethrow_exception; }
    public boolean return_oop()        { return _return_oop; }
    // Returns true if one or more NoEscape or ArgEscape objects exist in
    // any of the scopes at compiled pc.
    public boolean has_ea_local_in_scope()  { return _has_ea_local_in_scope; }
    public boolean arg_escape()        { return _arg_escape; }
    // Returns where the scope was decoded
    public int decode_offset() { return _decode_offset; }

    public int sender_decode_offset() { return _sender_decode_offset; }

    public DebugInfoReadStream stream_at(int decode_offset){
        return new DebugInfoReadStream(_code, decode_offset, _objects);
    }


    public List<ScopeValue> locals() {
        return decode_scope_values(_locals_decode_offset);
    }

    public List<ScopeValue> expressions() {
        return decode_scope_values(_expressions_decode_offset);
    }

    public List<MonitorValue> monitors() {
        return decode_monitor_values(_monitors_decode_offset);
    }

    public List<ScopeValue> objects() {
        return _objects;
    }

    private List<ScopeValue> decode_scope_values(int decode_offset) {
        if (decode_offset == DebugInformationRecorder.serialized_null) {
            return null;
        }
        DebugInfoReadStream stream = stream_at(decode_offset);
        int length = stream.read_int();
        List<ScopeValue> result = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            result.add(ScopeValue.read_from(stream));
        }
        return result;
    }

    private List<ScopeValue> decode_object_values(int decode_offset) {
        if (decode_offset == DebugInformationRecorder.serialized_null) {
            return null;
        }
        List<ScopeValue> result = new ArrayList<>();
        DebugInfoReadStream stream = new DebugInfoReadStream(_code, decode_offset, result);
        int length = stream.read_int();
        for (int index = 0; index < length; index++) {
            // Objects values are pushed to 'result' array during read so that
            // object's fields could reference it (OBJECT_ID_CODE).
            ScopeValue.read_from(stream);
        }
        if (result.size() != length){
            throw new RuntimeException("inconsistent debug information");
        }
        return result;
    }

    private List<MonitorValue> decode_monitor_values(int decode_offset) {
        if (decode_offset == DebugInformationRecorder.serialized_null) {
            return null;
        }
        DebugInfoReadStream stream  = stream_at(decode_offset);
        int length = stream.read_int();
        List<MonitorValue> result = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            result.add(new MonitorValue(stream));
        }
        return result;
    }

    public boolean is_top() {
        return _sender_decode_offset == DebugInformationRecorder.serialized_null;
    }

    public ScopeDesc sender(){
        if (is_top()) {
            return null;
        }
        return new ScopeDesc(this);
    }

    public void print_value_on(PrintStream st){
        st.print("  ");
        method().print_short_name(st);
        int lineno = method().line_number_from_bci(bci());
        if (lineno != -1) {
            st.printf("@%d (line %d)", bci(), lineno);
        } else {
            st.printf("@%d", bci());
        }
        if (should_reexecute()) {
            st.print("  reexecute=true");
        }
        st.println();
    }
    public void print_on(PrintStream st) {
        print_on(st, null);
    }

    public void print_on(PrintStream st, PcDesc pd){
        // header
        if (pd != null) {
            st.println("ScopeDesc(pc=0x"+Long.toHexString(pd.real_pc(_code))+" offset="+pd.pc_offset()+"):");
        }
        print_value_on(st);
        // decode offsets
        if (JVM.getFlag("WizardMode").getBool()) {
            st.print("ScopeDesc["+_decode_offset+"]@0x" +Long.toHexString(_code.content_begin())+ " ");
            st.println(" offset:     "+_decode_offset);
            st.println(" bci:        "+bci());
            st.println(" reexecute:  "+(should_reexecute() ? "true" : "false"));
            st.println(" locals:     "+_locals_decode_offset    );
            st.println(" stack:      "+_expressions_decode_offset    );
            st.println(" monitor:    "+_monitors_decode_offset    );
            st.println(" sender:     "+_sender_decode_offset    );
        }
        // locals
        {
            List<ScopeValue> l = (this).locals();
            if (l != null) {
                st.println("   Locals");
                for (int index = 0; index < l.size(); index++) {
                    st.printf("    - l%d: ", index);
                    l.get(index).print_on(st);
                    st.println();
                }
            }
        }
        // expressions
        {
            List<ScopeValue> l = (this).expressions();
            if (l != null) {
                st.println("   Expression stack");
                for (int index = 0; index < l.size(); index++) {
                    st.printf("    - @%d: ", index);
                    l.get(index).print_on(st);
                    st.println();
                }
            }
        }
        // monitors
        {
            List<MonitorValue> l = (this).monitors();
            if (l != null) {
                st.println("   Monitor stack");
                for (int index = 0; index < l.size(); index++) {
                    st.printf("    - @%d: ", index);
                    l.get(index).print_on(st);
                    st.println();
                }
            }
        }

        if (JVM.usingServerCompiler||JVM.includeJVMCI){
            if ((JVM.includeJVMCI||JVM.getFlag("DoEscapeAnalysis").getBool())&&is_top()&& _objects != null) {
                st.println("   Objects");
                for (ScopeValue object : _objects) {
                    ObjectValue sv = (ObjectValue) object;
                    st.printf("    - %d: ", sv.id());
                    st.printf("%s ", Klass.asKlass((sv.klass().as_ConstantOopReadValue().value().getJavaObject())).external_name());
                    sv.print_fields_on(st);
                    st.println();
                }
            }
        }
    }
}
