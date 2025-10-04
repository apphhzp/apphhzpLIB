package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.code.x86.X86NativePltCall.Intel_specific_constants.*;

public class X86NativePltCall extends X86NativeInstruction{
    public X86NativePltCall(long addr) {
        super(addr);
    }


    public static final class Intel_specific_constants {
        public static final int
        instruction_code           = 0xE8,
        instruction_size           =    5,
        instruction_offset         =    0,
        displacement_offset        =    1,
        return_address_offset      =    5;
    };
    public @RawCType("address")long instruction_address()  { return addr_at(instruction_offset); }
    public @RawCType("address")long next_instruction_address()  { return addr_at(return_address_offset); }
    public @RawCType("address")long displacement_address()  { return addr_at(displacement_offset); }
    public int displacement()  { return  int_at(displacement_offset); }
    public @RawCType("address")long return_address() { return addr_at(return_address_offset); }

    public @RawCType("address")long destination(){
        X86NativeGotJump  jump = X86NativeGotJump.nativeGotJump_at(plt_jump());
        return jump.destination();
    }

    public @RawCType("address")long plt_entry(){
        return return_address() + displacement();
    }

    public @RawCType("address")long plt_jump(){
        @RawCType("address")long entry = plt_entry();
        // Virtual PLT code has move instruction first
        if (new X86NativeGotJump(entry).is_GotJump()) {
            return entry;
        } else {
            return X86NativeLoadGot.nativeLoadGot_at(entry).next_instruction_address();
        }
    }
    public @RawCType("address")long plt_load_got(){
        @RawCType("address")long entry = plt_entry();
        if (!new X86NativeGotJump(entry).is_GotJump()) {
            // Virtual PLT code has move instruction first
            return entry;
        } else {
            // Static PLT code has move instruction second (from c2i stub)
            return X86NativeGotJump.nativeGotJump_at(entry).next_instruction_address();
        }
    }

    public @RawCType("address")long plt_c2i_stub(){
        @RawCType("address")long entry = plt_load_got();
        // This method should be called only for static calls which has C2I stub.
        X86NativeLoadGot load = X86NativeLoadGot.nativeLoadGot_at(entry);
        return entry;
    }

    public @RawCType("address")long plt_resolve_call(){
        X86NativeGotJump jump = X86NativeGotJump.nativeGotJump_at(plt_jump());
        @RawCType("address")long entry = jump.next_instruction_address();
        if (new X86NativeGotJump(entry).is_GotJump()) {
            return entry;
        } else {
            // c2i stub 2 instructions
            entry = X86NativeLoadGot.nativeLoadGot_at(entry).next_instruction_address();
            return X86NativeGotJump.nativeGotJump_at(entry).next_instruction_address();
        }
    }

    public void reset_to_plt_resolve_call() {
        set_destination_mt_safe(plt_resolve_call());
    }

    public void set_destination_mt_safe(@RawCType("address")long dest) {
        // rewriting the value in the GOT, it should always be aligned
        X86NativeGotJump jump = X86NativeGotJump.nativeGotJump_at(plt_jump());
        @RawCType("address*")long got = jump.got_address();
        unsafe.putAddress(got,dest);
    }

    public void set_stub_to_clean() {
        X86NativeLoadGot method_loader = X86NativeLoadGot.nativeLoadGot_at(plt_c2i_stub());
        X86NativeGotJump jump          = X86NativeGotJump.nativeGotJump_at(method_loader.next_instruction_address());
        method_loader.set_data(0);
        jump.set_jump_destination(-1L);
    }
    public void verify(){
        // Make sure code pattern is actually a call rip+off32 instruction.
        int inst = ubyte_at(0);
        if (inst != instruction_code) {
            System.err.printf("Addr: 0x"+Long.toHexString(instruction_address()) +" Code: 0x%x\n", inst);
            throw new RuntimeException("not a call rip+off32");
        }
    }

    public static X86NativePltCall nativePltCall_at(@RawCType("address")long address) {
        X86NativePltCall call = new X86NativePltCall(address);
        if (JVM.ENABLE_EXTRA_CHECK) {
            call.verify();
        }
        return call;
    }

    public static X86NativePltCall nativePltCall_before(@RawCType("address")long addr) {
        @RawCType("address")long at = addr - X86NativePltCall.Intel_specific_constants.instruction_size;
        return nativePltCall_at(at);
    }
}
