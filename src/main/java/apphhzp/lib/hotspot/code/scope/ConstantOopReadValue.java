package apphhzp.lib.hotspot.code.scope;

import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;

/**A ConstantOopReadValue is created by the VM when reading debug information*/
public class ConstantOopReadValue extends ScopeValue {
    private Oop _value;
    public ConstantOopReadValue(DebugInfoReadStream stream) {
        _value =  stream.read_oop();
    }
    public Oop value()                  { return _value;  }
    public boolean is_constant_oop()          { return true;    }
    public boolean equals(ScopeValue other){ return false;   }
    public void print_on(PrintStream st){
        if (value()!=null &&value().getJavaObject() != null) {
            value().get().print_value_on(st);
        } else {
            st.print("NULL");
        }
    }
}
