package apphhzp.lib.hotspot.compiler;

import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.runtime.Frame;
import apphhzp.lib.hotspot.runtime.RegisterMap;
import apphhzp.lib.hotspot.util.RawCType;

public class OopMapSet {
    // Update callee-saved register info for the following frame
    public static void update_register_map(Frame fr, RegisterMap reg_map) {
        CodeBlob cb = fr.cb();
        if (cb == null){
            throw new RuntimeException("no codeblob");
        }
        // Check if caller must update oop argument
        if (!((reg_map.include_argument_oops() ||
                !cb.caller_must_gc_arguments(reg_map.thread())))){
            throw new RuntimeException("include_argument_oops should already be set");
        }

        // Scan through oopmap and find location of all callee-saved registers
        // (we do not do update in place, since info could be overwritten)

        @RawCType("address")long pc = fr.pc();
        ImmutableOopMap map  = cb.oop_map_for_return_address(pc);
        if (map==null){
            throw new RuntimeException("no ptr map found");
        }
        for (OopMapStream oms=new OopMapStream(map); !oms.is_done(); oms.next()) {
            OopMapValue omv = oms.current();
            if (omv.type() == OopMapValue.OopTypes.callee_saved_value) {
                VMReg reg = omv.content_reg();
                @RawCType("oop*")long loc = fr.oopmapreg_to_oop_location(omv.reg(), reg_map);
                reg_map.set_location(reg,  loc);
            }
        }
    }
}
