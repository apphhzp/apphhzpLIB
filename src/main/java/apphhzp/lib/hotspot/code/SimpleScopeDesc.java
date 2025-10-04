package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;
import apphhzp.lib.hotspot.util.RawCType;

public class SimpleScopeDesc {
    private Method _method;
    private int _bci;
    private boolean _is_optimized_linkToNative;
    public SimpleScopeDesc(CompiledMethod code,@RawCType("address") long pc) {
        PcDesc pc_desc = code.pc_desc_at(pc);
        if (pc_desc==null){
            throw new RuntimeException("Must be able to find matching PcDesc");
        }
        // save this here so we only have to look up the PcDesc once
        _is_optimized_linkToNative = pc_desc.is_optimized_linkToNative();
        DebugInfoReadStream buffer=new DebugInfoReadStream(code, pc_desc.scope_decode_offset());
        int ignore_sender = buffer.read_int();
        _method           = buffer.read_method();
        _bci              = buffer.read_bci();
    }

    public Method method() { return _method; }
    public int bci() { return _bci; }
    public boolean is_optimized_linkToNative() { return _is_optimized_linkToNative; }
}
