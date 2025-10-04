package apphhzp.lib.hotspot.code.scope;

import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;

public class ConstantIntValue extends ScopeValue {
    private int _value;
    public ConstantIntValue(int value)         { _value = value; }
    public int value()                   { return _value;  }
    public boolean is_constant_int()         { return true;    }
    public boolean equals(ScopeValue other) { return false;   }

    // Serialization of debugging information
    public ConstantIntValue(DebugInfoReadStream stream){
        _value = stream.readSignedInt();
    }
    public void print_on(PrintStream st){
        st.printf("%d", value());
    }
}
