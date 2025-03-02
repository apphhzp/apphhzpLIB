package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class Deoptimization {
    public static class UnrollBlock extends JVMObject{
        public static final Type TYPE= JVM.type("Deoptimization::UnrollBlock");
        public static final int SIZE=TYPE.size;
        public UnrollBlock(long addr) {
            super(addr);
        }
    }
}
