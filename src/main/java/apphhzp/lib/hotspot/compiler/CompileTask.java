package apphhzp.lib.hotspot.compiler;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class CompileTask extends JVMObject {
    public static final Type TYPE= JVM.type("CompileTask");
    public static final int SIZE=TYPE.size;
    public static final long COMP_LEVEL_OFFSET=TYPE.offset("_comp_level");
    public CompileTask(long addr) {
        super(addr);
    }

    public int getCompLevel(){
        return unsafe.getInt(this.address+COMP_LEVEL_OFFSET);
    }

    @Override
    public String toString() {
        return "CompileTask@0x"+Long.toHexString(this.address);
    }
}
