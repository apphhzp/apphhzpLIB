package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.Bytecode;

public class Bytecode_tableswitch extends Bytecode {
    public Bytecode_tableswitch(Method method, long bcp) {
        super(method, bcp);
    }
    public void verify(){
        if (!JVM.ENABLE_EXTRA_CHECK){
            return;
        }
        switch (Bytecodes.java_code(code())) {
            case Bytecodes.Code._tableswitch:
            { int lo = low_key();
                int hi = high_key();
                if (!(hi >= lo)){
                    throw new RuntimeException("incorrect hi/lo values in tableswitch");
                }
                int i  = hi - lo - 1 ;
                while (i-- > 0) {
                    // no special check needed
                }
            }
            break;
            default:
                throw new RuntimeException("not a tableswitch bytecode");
        }
    }
    // Attributes
    public int  default_offset()                    { return get_aligned_Java_u4_at(1); }
    public int  low_key()                           { return get_aligned_Java_u4_at(1 + 4); }
    public int  high_key()                          { return get_aligned_Java_u4_at(1 + 2*4); }
    public int  dest_offset_at(int i){
        return get_aligned_Java_u4_at(1 + (3 + i)*4);
    }
    public int  length()                                  { return high_key()-low_key()+1; }
}
