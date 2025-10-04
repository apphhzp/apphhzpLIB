package apphhzp.lib.hotspot.code.scope;

import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;

public class ConstantDoubleValue extends ScopeValue{
    private double _value;
    public ConstantDoubleValue(double value)   { _value = value; }
    public double value()                { return _value;  }
    public boolean is_constant_double()      { return true;    }
    public boolean equals(ScopeValue other) { return false;   }

    // Serialization of debugging information
    public ConstantDoubleValue(DebugInfoReadStream stream){
        _value = stream.readDouble();
    }

    public void print_on(PrintStream st) {
        st.printf("%f", value());
    }
}
