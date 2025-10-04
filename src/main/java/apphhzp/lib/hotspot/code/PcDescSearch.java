package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.util.RawCType;

public class PcDescSearch {
    private @RawCType("address")long _code_begin;
    private PcDesc _lower;
    private PcDesc _upper;
    public PcDescSearch(@RawCType("address")long code, PcDesc lower, PcDesc upper) {
        //_code_begin(code), _lower(lower), _upper(upper)
        _code_begin = code;
        _lower = lower;
        _upper = upper;
    }

    public @RawCType("address")long code_begin()  { return _code_begin; }
    public PcDesc scopes_pcs_begin()  { return _lower; }
    public PcDesc scopes_pcs_end()  { return _upper; }
}
