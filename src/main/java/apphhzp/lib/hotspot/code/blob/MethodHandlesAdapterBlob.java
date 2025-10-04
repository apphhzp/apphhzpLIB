package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/**Used to hold MethodHandles adapters*/
public class MethodHandlesAdapterBlob extends BufferBlob{
    public static final Type TYPE= JVM.type("MethodHandlesAdapterBlob");
    public static final int SIZE=TYPE.size;
    public MethodHandlesAdapterBlob(long addr) {
        super(addr, TYPE);
    }

    @Override
    public boolean is_method_handles_adapter_blob() {
        return true;
    }
}
