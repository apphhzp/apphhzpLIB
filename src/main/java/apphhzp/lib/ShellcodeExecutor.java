package apphhzp.lib;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;

class ShellcodeExecutor {
    private static int x1;
    public static void main(final String[] args) {
        JVM.lookupSymbol("");
        for (int i=0;i<100000;i++){
            doit();
        }
        System.err.println(x1);
        InstanceKlass klass= Klass.asKlass(ShellcodeExecutor.class).asInstanceKlass();
        for (Method m : klass.getMethods()) {
            if (m.getConstMethod().getName().toString().equals("doit")){
                System.err.println("0x"+Long.toHexString(m.getFromCompiledEntry()));
                ClassHelperSpecial.unsafe.putByte(m.getFromCompiledEntry(), (byte) 0xc3);
                //ClassHelperSpecial.unsafe.putByte(m.getAdapter().getI2CEntry(), (byte) 0xc3);
            }
        }
        doit();
        System.err.println(x1);
    }

    public static void doit(){
        call();
    }
    public static void call(){
        ++x1;
    }
}
