package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/**Used for holding vtable chunks*/
public class VtableBlob extends BufferBlob{
    public static final Type TYPE= JVM.type("VtableBlob");
    public static final int SIZE=TYPE.size;
    public VtableBlob(long addr) {
        super(addr, TYPE);
    }

    @Override
    public boolean is_vtable_blob() {
        return true;
    }
}
