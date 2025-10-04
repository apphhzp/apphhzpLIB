package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.Bytecode;

public class Bytecode_lookupswitch extends Bytecode {
    public Bytecode_lookupswitch(Method method, long bcp) {
        super(method, bcp);
        verify();
    }
    public void verify(){
        if (JVM.ENABLE_EXTRA_CHECK){
            switch (Bytecodes.java_code(code())) {
                case Bytecodes.Code._lookupswitch:
                { int i = number_of_pairs() - 1;
                    while (i-- > 0) {
                        if (!(pair_at(i).match() < pair_at(i+1).match())){
                            throw new RuntimeException("unsorted table entries");
                        }
                    }
                }
                break;
                default:
                    throw new RuntimeException("not a lookupswitch bytecode");
            }
        }
    }

    // Attributes
    public int  default_offset()                    { return get_aligned_Java_u4_at(1); }
    public int  number_of_pairs()                   { return get_aligned_Java_u4_at(1 + 4); }
    public LookupswitchPair pair_at(int i){
        if (!(0 <= i && i < number_of_pairs())){
            throw new IndexOutOfBoundsException("pair index out of bounds: "+i);
        }
        return new LookupswitchPair(aligned_addr_at(1 + (1 + i)*2*4));
    }
}
