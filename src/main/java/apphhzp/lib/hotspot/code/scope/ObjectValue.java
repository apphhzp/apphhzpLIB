package apphhzp.lib.hotspot.code.scope;

import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**An ObjectValue describes an object eliminated by escape analysis.*/
public class ObjectValue extends ScopeValue {
    protected int _id;
    protected ScopeValue _klass;
    protected List<ScopeValue> _field_values;
    protected Oop _value;
    protected boolean _visited;

    public ObjectValue(int id, ScopeValue klass) {
//        : _id(id)
//                , _klass(klass)
//                , _field_values()
//                , _value()
//                , _visited(false)
        _id = id;
        _klass = klass;
        _field_values = new ArrayList<>();
        _value = null;
        _visited = false;
        if (!klass.is_constant_oop()) {
            throw new IllegalArgumentException("should be constant java mirror oop");
        }
    }

    public ObjectValue(int id) {
        _id = id;
        _klass = null;
        _field_values = new ArrayList<>();
        _value = null;
        _visited = false;
    }

    // Accessors
    public boolean is_object() {
        return true;
    }

    public int id() {
        return _id;
    }

    public ScopeValue klass() {
        return _klass;
    }

    public List<ScopeValue> field_values() {
        return _field_values;
    }

    public ScopeValue field_at(int i) {
        return _field_values.get(i);
    }

    public int field_size() {
        return _field_values.size();
    }

    public Oop value() {
        return _value;
    }

    public boolean is_visited() {
        return _visited;
    }
    public void set_value(Oop value) {
        _value = value;
    }
    public void set_visited(boolean visited) { _visited = visited; }

    public void read_object(DebugInfoReadStream stream) {
        _klass = read_from(stream);
        if (!_klass.is_constant_oop()){
            throw new RuntimeException("should be constant java mirror oop");
        }
        int length = stream.read_int();
        for (int i = 0; i < length; i++) {
            ScopeValue val = read_from(stream);
            _field_values.add(val);
        }
    }

    public void print_on(PrintStream st){
        st.printf("%s[%d]", is_auto_box() ? "box_obj" : "obj", _id);
    }
    public void print_fields_on(PrintStream st) {
        if (!_field_values.isEmpty()) {
            _field_values.get(0).print_on(st);
        }
        for (int i = 1; i < _field_values.size(); i++) {
            st.print(", ");
            _field_values.get(i).print_on(st);
        }
    }
}
