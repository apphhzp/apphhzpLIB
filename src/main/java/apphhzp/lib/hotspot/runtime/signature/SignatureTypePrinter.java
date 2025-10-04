package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.hotspot.oops.Symbol;

import java.io.PrintStream;

public class SignatureTypePrinter extends SignatureTypeNames {
    private PrintStream _st;
    private boolean _use_separator;

    @Override
    protected void type_name(String name) {
        if (_use_separator) _st.print(", ");
        _st.print(name);
        _use_separator = true;
    }

    public SignatureTypePrinter(Symbol signature, PrintStream st) {
        super(signature);
        _st = st;
        _use_separator = false;
    }

    public void print_parameters() {
        _use_separator = false;
        do_parameters_on(this);
    }

    public void print_returntype() {
        _use_separator = false;
        do_type(return_type());
    }
}
