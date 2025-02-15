package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public abstract class Stub extends JVMObject {
    public Stub(long addr) {
        super(addr);
    }
    public abstract void c_finalize();
    public abstract int getSize();// the total size of the stub in bytes (must be a multiple of CodeEntryAlignment)
    public abstract int codeSizeToSize(int code_size); // computes the total stub size in bytes given the code size in bytes
    public abstract long codeBegin(); // points to the first code byte
    public abstract long codeEnd();// points to the first byte after the code
}
