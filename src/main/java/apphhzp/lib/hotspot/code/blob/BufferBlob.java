package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/**Used for non-relocatable code such as interpreter, stubroutines, etc.*/
public class BufferBlob extends RuntimeBlob{
    public static final Type TYPE= JVM.type("BufferBlob");
    public static final int SIZE=TYPE.size;
    public BufferBlob(long addr, Type type) {
        super(addr,type);
    }

    @Override
    public boolean is_buffer_blob() {
        return true;
    }
}
