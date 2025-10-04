package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.hotspot.code.reloc.Relocation;
import apphhzp.lib.hotspot.util.RawCType;

public class X86Relocation {
    public static void pd_set_data_value(Relocation ths,@RawCType("address")long x, @RawCType("intptr_t")long o, boolean verify_only) {
        throw new UnsupportedOperationException();
//        if (PlatformInfo.isX86_64()){
//            x += o;
//            //typedef Assembler::WhichOperand WhichOperand;
//            @RawCType("WhichOperand")int which = ths.format(); // that is, disp32 or imm, call32, narrow oop
//            assert(which == X86Assembler.WhichOperand.disp32_operand ||
//                    which == X86Assembler.WhichOperand.narrow_oop_operand ||
//                    which == X86Assembler.WhichOperand.imm_operand, "format unpacks ok");
//            if (which == X86Assembler.WhichOperand.imm_operand) {
//                if (verify_only) {
//                    guarantee(*pd_address_in_code() == x, "instructions must match");
//                } else {
//      *pd_address_in_code() = x;
//                }
//            } else if (which == X86Assembler.WhichOperand.narrow_oop_operand) {
//                @RawCType("address")long disp = X86Assembler.locate_operand(addr(), which);
//                // both compressed oops and compressed classes look the same
//                if (CompressedOops::is_in((void*)x)) {
//                    uint32_t encoded = CompressedOops::narrow_oop_value(cast_to_oop(x));
//                    if (verify_only) {
//                        guarantee(*(uint32_t*) disp == encoded, "instructions must match");
//                    } else {
//        *(int32_t*) disp = encoded;
//                    }
//                } else {
//                    if (verify_only) {
//                        guarantee(*(uint32_t*) disp == CompressedKlassPointers::encode((Klass*)x), "instructions must match");
//                    } else {
//        *(int32_t*) disp = CompressedKlassPointers::encode((Klass*)x);
//                    }
//                }
//            } else {
//                // Note:  Use runtime_call_type relocations for call32_operand.
//                address ip = addr();
//                address disp = Assembler::locate_operand(ip, which);
//                address next_ip = Assembler::locate_next_instruction(ip);
//                if (verify_only) {
//                    guarantee(*(int32_t*) disp == (x - next_ip), "instructions must match");
//                } else {
//      *(int32_t*) disp = x - next_ip;
//                }
//            }
//        }else{
//            if (verify_only) {
//                guarantee(*pd_address_in_code() == (x + o), "instructions must match");
//            } else {
//                *pd_address_in_code() = x + o;
//            }
//        }
    }

    public static @RawCType("address")long pd_call_destination(Relocation relocation, @RawCType("address")long orig_addr) {
        @RawCType("intptr_t")long adj = 0;
        if (orig_addr != 0L) {
            // We just moved this call instruction from orig_addr to addr().
            // This means its target will appear to have grown by addr() - orig_addr.
            adj = -( relocation.addr() - orig_addr );
        }
        X86NativeInstruction ni = X86NativeInstruction.nativeInstruction_at(relocation.addr());
        if (ni.is_call()) {
            return X86NativeCall.nativeCall_at(relocation.addr()).destination() + adj;
        } else if (ni.is_jump()) {
            return X86NativeJump.nativeJump_at(relocation.addr()).jump_destination() + adj;
        } else if (ni.is_cond_jump()) {
            return X86NativeGeneralJump.nativeGeneralJump_at(relocation.addr()).jump_destination() + adj;
        } else if (ni.is_mov_literal64()) {
            return (new X86NativeMovConstReg(ni.address) ).data();
        } else {
            throw new RuntimeException("ShouldNotReachHere()");
        }
    }
    public static void pd_set_call_destination(Relocation relocation,@RawCType("address")long x) {
//        X86NativeInstruction ni = X86NativeInstruction.nativeInstruction_at(relocation.addr());
//        if (ni.is_call()) {
//            X86NativeCall.nativeCall_at(relocation.addr()).set_destination(x);
//        } else if (ni.is_jump()) {
//            X86NativeJump nj = X86NativeJump.nativeJump_at(relocation.addr());
//
//            // Unresolved jumps are recognized by a destination of -1
//            // However 64bit can't actually produce such an address
//            // and encodes a jump to self but jump_destination will
//            // return a -1 as the signal. We must not relocate this
//            // jmp or the ic code will not see it as unresolved.
//
//            if (nj.jump_destination() == -1L) {
//                x = relocation.addr(); // jump to self
//            }
//            nj.set_jump_destination(x);
//        } else if (ni.is_cond_jump()) {
//            // %%%% kludge this, for now, until we get a jump_destination method
//            @RawCType("address")long old_dest = X86NativeGeneralJump.nativeGeneralJump_at(relocation.addr()).jump_destination();
//            @RawCType("address")long disp = Assembler::locate_operand(relocation.addr(), X86Assembler.WhichOperand.call32_operand);
//            unsafe.putInt(disp, (int) (unsafe.getInt(disp)+(x - old_dest)));
//        } else if (ni.is_mov_literal64()) {
//            new X86NativeMovConstReg(ni.address).set_data(x);
//        } else {
//            throw new RuntimeException("ShouldNotReachHere()");
//        }
    }
}
