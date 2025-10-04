package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.util.RawCType;

/**Describes stubs used by compiled code to call a (static) C++ runtime routine*/
public class RuntimeStub extends RuntimeBlob{
    public static final Type TYPE= JVM.type("RuntimeStub");
    public static final int SIZE=TYPE.size;
    public RuntimeStub(long addr) {
        super(addr,TYPE);
    }

    @Override
    public boolean is_runtime_stub() {
        return true;
    }
    public @RawCType("address")long entry_point(){ return code_begin(); }
}
