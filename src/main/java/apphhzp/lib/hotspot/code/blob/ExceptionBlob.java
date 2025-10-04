package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
/**Used for stack unrolling(currently only used by Compiler 2)*/
public class ExceptionBlob extends SingletonBlob{
    public static final Type TYPE=JVM.usingServerCompiler?JVM.type("ExceptionBlob"):Type.EMPTY;
    public ExceptionBlob(long addr) {
        super(addr,TYPE);
    }

    @Override
    public boolean is_exception_stub() {
        return true;
    }
}
