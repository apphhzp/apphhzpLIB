package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.util.RawCType;

public class ArgumentCount extends SignatureIterator {
    private int _size;

    protected void do_type(@RawCType("BasicType") int type) {
        _size++;
    }

    public ArgumentCount(Symbol signature) {
        super(signature);
        _size = 0;
        do_parameters_on(this);  // non-virtual template execution
    }

    public int size() {
        return _size;
    }
}
