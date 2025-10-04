package apphhzp.lib.hotspot.code.scope;

import java.io.PrintStream;

/** A placeholder value that has no concrete meaning other than helping constructing
 other values.*/
public class MarkerValue extends ScopeValue{
    public boolean      is_marker()                { return true; }
    // Printing
    public void print_on(PrintStream st){
        st.print("marker");
    }
}
