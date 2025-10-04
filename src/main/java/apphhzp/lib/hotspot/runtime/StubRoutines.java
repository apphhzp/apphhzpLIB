package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class StubRoutines {
    public static final Type TYPE= JVM.type("StubRoutines");
    public static final long CALL_STUB_RETURN_ADDRESS_ADDRESS=TYPE.global("_call_stub_return_address");
    public static boolean returns_to_call_stub(@RawCType("address")long return_pc){
        return return_pc== unsafe.getAddress(CALL_STUB_RETURN_ADDRESS_ADDRESS);
    }
//    public static boolean is_stub_code(@RawCType("address")long addr){
//        return contains(addr);
//    }
}
