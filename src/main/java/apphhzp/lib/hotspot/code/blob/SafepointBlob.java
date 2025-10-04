package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/**Used to handle illegal instruction exceptions*/
public class SafepointBlob extends SingletonBlob{
    public static final Type TYPE= JVM.type("SafepointBlob");
    public SafepointBlob(long addr) {
        super(addr, TYPE);
    }

    @Override
    public boolean is_safepoint_stub() {
        return true;
    }
}
