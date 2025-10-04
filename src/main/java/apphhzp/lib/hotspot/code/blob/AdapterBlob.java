package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/**Used to hold C2I/I2C adapters*/
public class AdapterBlob extends BufferBlob{
    public static final Type TYPE= JVM.type("AdapterBlob");
    public static final int SIZE=TYPE.size;
    public AdapterBlob(long addr){
        super(addr,TYPE);
    }

    @Override
    public boolean is_adapter_blob() {
        return true;
    }
}
