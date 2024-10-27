package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

import static apphhzp.lib.ClassHelper.unsafe;

public class RuntimeStub extends CodeBlob{
    public static final Type TYPE= JVM.type("RuntimeStub");
    public static final int SIZE=TYPE.size;
    public static final long CALLER_MUST_GC_ARGS_OFFSET=TYPE.offset("_caller_must_gc_arguments");
    public RuntimeStub(long addr) {
        super(addr,TYPE);
    }

    public boolean callerMustGCArgs(){
        return unsafe.getByte(this.address+CALLER_MUST_GC_ARGS_OFFSET)!=0;
    }

    public void setCallerMustGCArgs(boolean val){
        unsafe.putByte(this.address+CALLER_MUST_GC_ARGS_OFFSET,(byte) (val?1:0));
    }
}
