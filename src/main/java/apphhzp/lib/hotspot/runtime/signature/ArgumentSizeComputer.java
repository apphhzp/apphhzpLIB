package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

public class ArgumentSizeComputer extends SignatureIterator {
    private int _size;

    protected void do_type(@RawCType("BasicType") int type) {
        _size += BasicType.parameter_type_word_count(type);
    }

    public ArgumentSizeComputer(Symbol signature) {
        super(signature);
        _size = 0;
        do_parameters_on(this);  // non-virtual template execution
    }

    public int size() {
        return _size;
    }
}
