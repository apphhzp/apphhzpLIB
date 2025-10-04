package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.runtime.bytecode.Bytes;
import apphhzp.lib.hotspot.util.RawCType;

public class LookupswitchPair {
    private final @RawCType("address")long _bcp;

    private @RawCType("address")long  addr_at            (int offset)             { return _bcp + offset; }
    private int     get_Java_u4_at     (int offset)             { return Bytes.get_Java_u4(addr_at(offset)); }
    public LookupswitchPair(@RawCType("address")long bcp){
        _bcp = bcp;
    }
    public int  match()                              { return get_Java_u4_at(0); }
    public int  offset()                             { return get_Java_u4_at(4); }
}
