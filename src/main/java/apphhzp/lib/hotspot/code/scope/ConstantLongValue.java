package apphhzp.lib.hotspot.code.scope;

import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;

public class ConstantLongValue extends ScopeValue {
    private long _value;
    public ConstantLongValue(long value)       { _value = value; }
    public long value()                  { return _value;  }
    public boolean is_constant_long()        { return true;    }
    public boolean equals(ScopeValue other) { return false;   }

    // Serialization of debugging information
    public ConstantLongValue(DebugInfoReadStream stream){
        _value = stream.readLong();
    }
    public void print_on(PrintStream st){
        st.print(value());
    }
}
