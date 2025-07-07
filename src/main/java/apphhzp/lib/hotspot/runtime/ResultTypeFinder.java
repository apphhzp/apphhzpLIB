package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.util.RawCType;

// Specialized SignatureIterator: Used to compute the result type.
public class ResultTypeFinder extends SignatureIterator{
    public @RawCType("BasicType")int type() { return return_type(); }
    public ResultTypeFinder(Symbol signature){
        super(signature);
    }
}
