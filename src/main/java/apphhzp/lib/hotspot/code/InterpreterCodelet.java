package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** An InterpreterCodelet is a piece of interpreter code. All
 * interpreter code is generated into little codelets which
 * contain extra information for debugging and printing purposes.*/
public class InterpreterCodelet extends Stub{
    public static final Type TYPE= JVM.type("InterpreterCodelet");
    public static final int SIZE=TYPE.size;
    public static final long SIZE_OFFSET=TYPE.offset("_size");
    public static final long DESC_OFFSET=TYPE.offset("_description");
    public static final long BYTECODE_OFFSET=TYPE.offset("_bytecode");
    public InterpreterCodelet(long addr) {
        super(addr);
    }

    // Initialization/finalization
    public void initialize(int size){
        unsafe.putInt(this.address+SIZE_OFFSET,size);
    }
    @Override
    public void c_finalize() {
        throw new UnsupportedOperationException("ShouldNotCallThis()");
    }

    @Override
    public int size() {
        return unsafe.getInt(this.address+SIZE_OFFSET);
    }

    public String getDesc(){
        return JVM.getStringRef(this.address+DESC_OFFSET);
    }
    public int code_size_to_size(int code_size){
        return InterpreterCodelet.static_code_size_to_size(code_size);
    }

    public static int static_code_size_to_size(int code_size){ return (int) (JVM.alignUp(SIZE, JVM.codeEntryAlignment) + code_size); }

    // Code info
    public long code_begin(){ return this.address + JVM.alignUp(SIZE,JVM.codeEntryAlignment); }
    public long code_end(){ return this.address + this.size(); }
    // Interpreter-specific attributes
    public int code_size(){
        return (int) (code_end() - code_begin());
    }
    public String description(){ return JVM.getStringRef(this.address+DESC_OFFSET); }
    public @RawCType("Bytecodes::Code")int bytecode(){
        return unsafe.getInt(this.address+BYTECODE_OFFSET);
    }

    public void print_on(PrintStream st){
//        if (JVM.getFlag("PrintInterpreter").getBool()) {
//            st.println();
//            st.println("----------------------------------------------------------------------");
//        }
        if (description() != null) {
            st.printf("%s  ", description());
        }
        if (bytecode()    >= 0   ) {
            st.printf("%d %s  ", bytecode(), Bytecodes.name(bytecode()));
        }
        st.println("[0x"+Long.toHexString(code_begin())+  ", 0x"+Long.toHexString(code_end())+  "]  "+code_size()+" bytes");

//        if (JVM.getFlag("PrintInterpreter").getBool()) {
//            st.println();
//            Disassembler::decode(code_begin(), code_end(), st NOT_PRODUCT(COMMA &_asm_remarks));
//        }
    }
    @Override
    public String toString() {
        return "InterpreterCodelet0x"+Long.toHexString(this.address);
    }
}
