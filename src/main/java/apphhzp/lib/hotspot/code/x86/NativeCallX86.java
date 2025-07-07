package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.code.NativeCall;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class NativeCallX86 extends NativeCall {
    private static final boolean isAMD64 = PlatformInfo.getCPU().equals("amd64");
    public static final int //Intel_specific_constants
            nop_instruction_code = 0x90,
            nop_instruction_size = 1;

    public static final int //Intel_specific_constants
            instruction_code = 0xE8,
            instruction_size = 5,
            instruction_offset = 0,
            displacement_offset = 1,
            return_address_offset = 5;


    public NativeCallX86(long addr) {
        if (true){
            throw new UnsupportedOperationException("Incomplete");
        }
        this.address = addr;
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

    @Override
    public long instruction_address() {
        return addr_at(instruction_offset);
    }

    @Override
    public long next_instruction_address() {
        return addr_at(return_address_offset);
    }

    @Override
    public long return_address() {
        return addr_at(return_address_offset);
    }

    public int displacement() {
        return int_at(displacement_offset);
    }

    public boolean is_nop() {
        return ubyte_at(0) == nop_instruction_code;
    }

    public long addr_at(int offset) {
        return this.address + offset;
    }

    public byte sbyte_at(int offset) {
        return unsafe.getByte(addr_at(offset));
    }

    public @RawCType("u_char") short  ubyte_at(int offset) {
        return (short) (unsafe.getByte(addr_at(offset)) & 0xff);
    }

    public int int_at(int offset) {
        return unsafe.getInt(addr_at(offset));
    }

    public @RawCType("intptr_t") long ptr_at(int offset) {
        return unsafe.getAddress(addr_at(offset));
    }

    public Oop oop_at(int offset) {
        return new Oop(unsafe.getAddress(addr_at(offset)));
    }
}
