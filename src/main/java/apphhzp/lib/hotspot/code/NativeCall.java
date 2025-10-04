package apphhzp.lib.hotspot.code;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.code.x86.X86NativeCall;
import apphhzp.lib.hotspot.util.RawCType;

public interface  NativeCall {
    long getAddress();
    long destination();
    long instruction_address();
    long next_instruction_address();
    long return_address();
    void verify();
    void verify_alignment();
    static NativeCall create(long addr){
        if (PlatformInfo.isX86()){
            return new X86NativeCall(addr);
        }
        throw new UnsupportedOperationException();
    }
    static int instruction_size(){
        if (PlatformInfo.isX86()){
            return X86NativeCall.Intel_specific_constants.instruction_size;
        }else {
            throw new UnsupportedOperationException();
        }
    }
    static boolean is_call_before(long addr){
        if (PlatformInfo.isX86()){
            return X86NativeCall.is_call_before(addr);
        }else {
            throw new UnsupportedOperationException();
        }
    }
    static NativeCall nativeCall_at(@RawCType("address")long address) {
        if (PlatformInfo.isX86()){
            return X86NativeCall.nativeCall_at(address);
        }else {
            throw new UnsupportedOperationException();
        }
    }
    static NativeCall nativeCall_before(long addr){
        if (PlatformInfo.isX86()){
            return X86NativeCall.nativeCall_before(addr);
        }else {
            throw new UnsupportedOperationException();
        }
    }
}
