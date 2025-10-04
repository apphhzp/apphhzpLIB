package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.NativeCall;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.code.x86.X86NativeCall.Intel_specific_constants.instruction_code;
import static apphhzp.lib.hotspot.code.x86.X86NativeCall.Intel_specific_constants.return_address_offset;

public class X86NativeCall extends X86NativeInstruction implements NativeCall {
    public static final class Intel_specific_constants {
        public static final int
                instruction_code            = 0xE8,
                instruction_size            =    5,
                instruction_offset          =    0,
                displacement_offset         =    1,
                return_address_offset       =    5;
    }

    public X86NativeCall(long addr) {
        super(addr);
    }

    @Override
    public long getAddress() {
        return this.address;
    }

    @Override
    public long destination() {
        // Getting the destination of a call isn't safe because that call can
        // be getting patched while you're calling this.  There's only special
        // places where this can be called but not automatically verifiable by
        // checking which locks are held.  The solution is true atomic patching
        // on x86, nyi.
        return return_address() + displacement();
    }

    public @RawCType("address")long instruction_address(){ return addr_at(Intel_specific_constants.instruction_offset); }
    public @RawCType("address")long next_instruction_address(){ return addr_at(return_address_offset); }
    public int  displacement(){
        return int_at(Intel_specific_constants.displacement_offset);
    }
    public @RawCType("address")long displacement_address(){ return addr_at(Intel_specific_constants.displacement_offset); }
    public @RawCType("address")long return_address(){ return addr_at(return_address_offset); }

    public static boolean is_call_at(@RawCType("address")long instr) {
        return (unsafe.getByte(instr)&0xFF) == instruction_code;
    }

    public static boolean is_call_before(@RawCType("address")long return_address) {
        return is_call_at(return_address - return_address_offset);
    }
    public void verify_alignment(){
        if (!is_displacement_aligned()){
            throw new RuntimeException("displacement of call is not aligned");
        }
    }

    public void verify() {
        // Make sure code pattern is actually a call imm32 instruction.
        int inst = ubyte_at(0);
        if (inst != instruction_code) {
            System.err.printf("Addr: 0x"+Long.toHexString(instruction_address()) + " Code: 0x%x\n", inst);
            throw new RuntimeException("not a call disp32");
        }
    }

    public static X86NativeCall nativeCall_at(@RawCType("address")long address) {
        X86NativeCall call = new X86NativeCall(address - Intel_specific_constants.instruction_offset);
        if (JVM.ENABLE_EXTRA_CHECK){
            call.verify();
        }
        return call;
    }

    public static X86NativeCall nativeCall_before(@RawCType("address")long return_address) {
        X86NativeCall call = new X86NativeCall(return_address - Intel_specific_constants.return_address_offset);
        if (JVM.ENABLE_EXTRA_CHECK){
            call.verify();
        }
        return call;
    }

    // Inserts a native call instruction at a given pc
    public void insert(@RawCType("address")long code_pos, @RawCType("address")long entry) {
        @RawCType("intptr_t")long disp = entry - (code_pos + 1 + 4);
        if (PlatformInfo.isX86_64()){
            if (!(disp == (long)(int)disp)){
                throw new RuntimeException("must be 32-bit offset");
            }
        }
        unsafe.putByte(code_pos, (byte) Intel_specific_constants.instruction_code);
        unsafe.putInt((code_pos+1), (int) disp);
        //ICache::invalidate_range(code_pos, Intel_specific_constants.instruction_size);
    }

    public void print(PrintStream os) {
        os.println("0x"+Long.toHexString(instruction_address())+": call 0x"+Long.toHexString(destination()));
    }
    public boolean is_displacement_aligned() {
        return Long.remainderUnsigned(displacement_address(),4L)== 0;
    }

    public static boolean is_call_to(@RawCType("address")long instr, @RawCType("address")long target) {
        return nativeInstruction_at(instr).is_call() &&
                nativeCall_at(instr).destination() == target;
    }
}
