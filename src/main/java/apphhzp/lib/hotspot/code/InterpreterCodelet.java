package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

import static apphhzp.lib.ClassHelper.unsafe;

public class InterpreterCodelet extends Stub{
    public static final Type TYPE= JVM.type("InterpreterCodelet");
    public static final int SIZE=TYPE.size;
    public static final long SIZE_OFFSET=TYPE.offset("_size");
    public static final long DESC_OFFSET=TYPE.offset("_description");
    public static final long BYTECODE_OFFSET=TYPE.offset("_bytecode");
    public InterpreterCodelet(long addr) {
        super(addr);
    }

    @Override
    public int getSize() {
        return unsafe.getInt(this.address+SIZE_OFFSET);
    }

    public String getDesc(){
        return JVM.getStringRef(this.address+DESC_OFFSET);
    }
}
