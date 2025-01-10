package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class MethodParametersElement extends JVMObject {
    public static final Type TYPE= JVM.type("MethodParametersElement");
    public static final int SIZE=TYPE.size;
    public MethodParametersElement(long addr) {
        super(addr);
    }

}
