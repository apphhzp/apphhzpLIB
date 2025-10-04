package apphhzp.lib.hotspot.runtime;

import java.util.ArrayList;
import java.util.List;

public class StackValueCollection {
    private List<StackValue> _values;

    public StackValueCollection()            { _values = new ArrayList<>(); }
    public StackValueCollection(int length)  { _values = new ArrayList<>(length); }
    public void add(StackValue val)    { _values.add(val); }
    public int  size()                 { return _values.size(); }
    public boolean is_empty()             { return (size() == 0); }
    public StackValue at(int i)       { return _values.get(i); }
}
