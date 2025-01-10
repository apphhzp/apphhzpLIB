package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class ExceptionTableElement extends JVMObject {
    public static final Type TYPE= JVM.type("ExceptionTableElement");
    public static final int SIZE=TYPE.size;
    public static final long START_PC_OFFSET=TYPE.offset("start_pc");
    public ExceptionTableElement(long addr) {
        super(addr);
    }


}
