package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.util.RawCType;

public abstract class Stub extends JVMObject {
    public Stub(long addr) {
        super(addr);
    }
    public abstract void initialize(int size); // called after creation (called twice if allocated via (request, commit))
    public abstract void c_finalize();
    // General info/converters
    public abstract int size(); // the total size of the stub in bytes (must be a multiple of CodeEntryAlignment)
    public abstract int code_size_to_size(int code_size); // computes the total stub size in bytes given the code size in bytes

    // Code info
    public abstract @RawCType("address")long code_begin(); // points to the first code byte
    public abstract @RawCType("address")long code_end(); // points to the first byte after the code

//    // Debugging
//    public abstract void    verify(Stub* self)                       = 0; // verifies the stub
//    public abstract void    print(Stub* self)                        = 0; // prints information about the stub
}
