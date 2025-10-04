package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/** Super-class for all blobs that exist in only one instance. Implements default behaviour.*/
public class SingletonBlob extends RuntimeBlob{
    public static final Type TYPE= JVM.type("SingletonBlob");
    public static final int SIZE=TYPE.size;
    public SingletonBlob(long addr, Type type) {
        super(addr, type);
    }


}
