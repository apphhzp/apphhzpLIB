package apphhzp.lib.hotspot.runtime.x86;

import apphhzp.lib.hotspot.asm.x86.X86ConcreteRegisterImpl;
import apphhzp.lib.hotspot.asm.x86.X86XMMRegister;
import apphhzp.lib.hotspot.asm.x86.X86XMMRegisterImpl;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.runtime.RegisterMap;
import apphhzp.lib.hotspot.util.RawCType;

public class X86RegisterMap extends RegisterMap {
    public X86RegisterMap(JavaThread thread, boolean update_map, boolean process_frames) {
        super(thread, update_map, process_frames);
    }

    public X86RegisterMap(RegisterMap map) {
        super(map);
    }

    // This is the hook for finding a register in an "well-known" location,
    // such as a register block of a predetermined format.
    private @RawCType("address") long pd_location(X86VMReg reg){
        if (reg.is_XMMRegister()) {
            int reg_base = reg.value() - X86ConcreteRegisterImpl.max_fpr;
            int base_reg_enc = (reg_base / X86XMMRegisterImpl.max_slots_per_register);
            if (!(base_reg_enc >= 0 && base_reg_enc < X86XMMRegisterImpl.number_of_registers)){
                throw new RuntimeException("invalid XMMRegister: "+base_reg_enc);
            }
            VMReg base_reg = new X86XMMRegister(base_reg_enc).as_VMReg();
            @RawCType("intptr_t")int offset_in_bytes = (reg.value() - base_reg.value()) * X86VMRegImpl.stack_slot_size;
            if (base_reg_enc > 15) {
                if (offset_in_bytes == 0) {
                    return 0; // ZMM16-31 are stored in full.
                }
            } else {
                if (offset_in_bytes == 0 || offset_in_bytes == 16 || offset_in_bytes == 32) {
                    // Reads of the low and high 16 byte parts should be handled by location itself because
                    // they have separate callee saved entries (see RegisterSaver::save_live_registers()).
                    return 0;
                }
                // The upper part of YMM0-15 and ZMM0-15 registers are saved separately in the frame.
                if (offset_in_bytes > 32) {
                    base_reg = base_reg.next(8);
                    offset_in_bytes -= 32;
                } else if (offset_in_bytes > 16) {
                    base_reg = base_reg.next(4);
                    offset_in_bytes -= 16;
                } else {
                    // XMM0-15 case (0 < offset_in_bytes < 16). No need to adjust base register (or offset).
                }
            }
            @RawCType("address") long base_location = location(base_reg);
            if (base_location != 0) {
                return base_location + offset_in_bytes;
            }
        }
        return 0;
    }

    @Override
    public @RawCType("address") long pd_location(VMReg reg) {
        if (!(reg instanceof X86VMReg)){
            throw new IllegalArgumentException();
        }
        return pd_location((X86VMReg)reg);
    }

    @Override
    public @RawCType("address") long pd_location(VMReg base_reg, int slot_idx){
        return location(base_reg.next(slot_idx));
    }

    @Override
    public void check_location_valid() {}
    // no PD state to clear or copy:
    public void pd_clear() {}
    public void pd_initialize() {}
    public void pd_initialize_from(RegisterMap map) {}

    @Override
    public RegisterMap clone() {
        return new X86RegisterMap(this);
    }
}
