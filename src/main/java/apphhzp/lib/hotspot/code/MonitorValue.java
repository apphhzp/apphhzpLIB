package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.code.scope.ScopeValue;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;

public class MonitorValue {
    private ScopeValue _owner;
    private Location    _basic_lock;
    private boolean        _eliminated;
    // Constructor
    public MonitorValue(ScopeValue owner, Location basic_lock){
        this(owner, basic_lock, false);
    }
    public MonitorValue(ScopeValue owner, Location basic_lock, boolean eliminated){
        _owner       = owner;
        _basic_lock  = basic_lock;
        _eliminated  = eliminated;
    }

    // Accessors
    public ScopeValue  owner()      { return _owner; }
    public Location     basic_lock() { return _basic_lock;  }
    public boolean         eliminated() { return _eliminated; }

    // Serialization of debugging information
    public MonitorValue(DebugInfoReadStream stream){
        _basic_lock  = new Location(stream);
        _owner       = ScopeValue.read_from(stream);
        _eliminated  = (stream.readBoolean());
    }

    // Printing
    public void print_on(PrintStream st){
        st.print("monitor{");
        owner().print_on(st);
        st.print(",");
        basic_lock().print_on(st);
        st.print("}");
        if (_eliminated) {
            st.print(" (eliminated)");
        }
    }
}
