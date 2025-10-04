package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/**Non-compiled method code; generated glue code*/
public class RuntimeBlob extends CodeBlob{
    public static final Type TYPE= JVM.type("RuntimeBlob");
    public static final int SIZE=TYPE.size;
    public RuntimeBlob(long addr,Type type) {
        super(addr, type);
    }

}
