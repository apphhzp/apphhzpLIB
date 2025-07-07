package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.Debugger;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

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
    public void c_finalize() {
        if (Debugger.isDebug){
            return;
        }
        throw new UnsupportedOperationException("ShouldNotCallThis();");
    }

    @Override
    public int getSize() {
        return unsafe.getInt(this.address+SIZE_OFFSET);
    }

    public String getDesc(){
        return JVM.getStringRef(this.address+DESC_OFFSET);
    }
    public int codeSizeToSize(int code_size){
        return code_size_to_size(code_size);
    }

    public static int code_size_to_size(int code_size){ return (int) (JVM.alignUp(SIZE, JVM.codeEntryAlignment) + code_size); }

    // Code info
    public long codeBegin(){ return this.address + JVM.alignUp(SIZE,JVM.codeEntryAlignment); }
    public long codeEnd(){ return this.address + this.getSize(); }
    public int getBytecode(){
        return (unsafe.getInt(this.address+BYTECODE_OFFSET));
    }
    @Override
    public String toString() {
        return "InterpreterCodelet0x"+Long.toHexString(this.address);
    }
}
