package apphhzp.lib.hotspot.opto;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class Compile extends JVMObject {
    public static final Type TYPE= JVM.type("Compile");
    public static final int SIZE=TYPE.size;
    public static final long COMPILE_ID_OFFSET=TYPE.offset("_compile_id");
    public static final long ILT_OFFSET=TYPE.offset("_ilt");
    public Compile(long addr) {
        super(addr);
    }

    public int getCompileID(){
        return unsafe.getInt(this.address+COMPILE_ID_OFFSET);
    }

    @Override
    public String toString() {
        return "Compile@0x"+Long.toHexString(this.address);
    }
}
