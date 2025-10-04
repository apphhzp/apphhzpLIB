package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

/**Used to handle uncommon traps(currently only used by Compiler 2)*/
public class UncommonTrapBlob extends SingletonBlob{
    public static final Type TYPE=JVM.usingServerCompiler?JVM.type("UncommonTrapBlob"):Type.EMPTY;
    public UncommonTrapBlob(long addr) {
        super(addr,TYPE);
    }

    @Override
    public boolean is_uncommon_trap_stub() {
        return true;
    }
}
