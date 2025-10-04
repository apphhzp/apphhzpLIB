package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

public class ReferenceArgumentCount extends SignatureIterator {
    protected int _refs;

    protected void do_type(@RawCType("BasicType") int type) {
        if (BasicType.is_reference_type(type)) {
            _refs++;
        }
    }

    public ReferenceArgumentCount(Symbol signature) {
        super(signature);
        _refs = 0;
        do_parameters_on(this);  // non-virtual template execution
    }

    public int count() {
        return _refs;
    }
}
