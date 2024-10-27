package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.InterpreterCodelet;
import apphhzp.lib.hotspot.code.StubQueue;

import static apphhzp.lib.ClassHelper.unsafe;

public class AbstractInterpreter {
    public static final Type TYPE= JVM.type("AbstractInterpreter");
    public static final long CODE_ADDRESS=TYPE.global("_code");
    private static StubQueue<InterpreterCodelet> queueCache;
    private AbstractInterpreter(){}

    public static StubQueue<InterpreterCodelet> getCode(){
        long addr= unsafe.getAddress(CODE_ADDRESS);
        if (!JVMObject.isEqual(queueCache,addr)){
            queueCache=new StubQueue<>(addr,InterpreterCodelet::new);
        }
        return queueCache;
    }
}
