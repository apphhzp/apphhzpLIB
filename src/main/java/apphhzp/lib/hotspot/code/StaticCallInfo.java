package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

public class StaticCallInfo {

    protected @RawCType("address")long _entry;          // Entrypoint
    protected Method _callee;         // Callee (used when calling interpreter)
    protected boolean         _to_interpreter; // call to interpreted method (otherwise compiled)

    public @RawCType("address")long entry(){ return _entry;  }
    public Method callee(){ return _callee; }
}
