package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.method.data.DataLayout;
import apphhzp.lib.hotspot.util.RawCType;

public class Deoptimization {
    public static final Type TYPE=JVM.type("Deoptimization");
    public static final long TRAP_REASON_NAME_ADDRESS=TYPE.global("_trap_reason_name");
    public static final int Reason_many=JVM.intConstant("Deoptimization::Reason_many");
    public static final int Reason_LIMIT=JVM.intConstant("Deoptimization::Reason_LIMIT");
    public static final int Reason_RECORDED_LIMIT=JVM.intConstant("Deoptimization::Reason_RECORDED_LIMIT");

    public static String trap_reason_name(int reason) {
        if (reason == Reason_many)  return "many";
        if (Integer.compareUnsigned(reason,Reason_LIMIT)  < 0)
            return JVM.getStringRef(TRAP_REASON_NAME_ADDRESS+ (long) reason *JVM.oopSize);
        return "reason"+reason;
    }
    public static class UnrollBlock extends JVMObject{
        public static final Type TYPE= JVM.type("Deoptimization::UnrollBlock");
        public static final int SIZE=TYPE.size;
        public UnrollBlock(long addr) {
            super(addr);
        }
    }

    public static boolean reason_is_recorded_per_bytecode(@RawCType("DeoptReason")int reason) {
        return reason > 0 && reason <= Reason_RECORDED_LIMIT;
    }


    // Local derived constants.
    // Further breakdown of DataLayout::trap_state, as promised by DataLayout.
    private static final int DS_REASON_MASK   = (DataLayout.trap_mask) >>> 1;
    private static final int DS_RECOMPILE_BIT = DataLayout.trap_mask - DS_REASON_MASK;

    //---------------------------trap_state_reason---------------------------------
    public static @RawCType("DeoptReason")int trap_state_reason(int trap_state) {
        // This assert provides the link between the width of DataLayout::trap_bits
        // and the encoding of "recorded" reasons.  It ensures there are enough
        // bits to store all needed reasons in the per-BCI MDO profile.
        int recompile_bit = (trap_state & DS_RECOMPILE_BIT);
        trap_state -= recompile_bit;
        if (trap_state == DS_REASON_MASK) {
            return Reason_many;
        } else {
            return trap_state;
        }
    }
    //-------------------------trap_state_has_reason-------------------------------
    public static int trap_state_has_reason(int trap_state, int reason) {
//        assert(reason_is_recorded_per_bytecode((DeoptReason)reason), "valid reason");
//        assert(DS_REASON_MASK >= Reason_RECORDED_LIMIT, "enough bits");
        int recompile_bit = (trap_state & DS_RECOMPILE_BIT);
        trap_state -= recompile_bit;
        if (trap_state == DS_REASON_MASK) {
            return -1;  // true, unspecifically (bottom of state lattice)
        } else if (trap_state == reason) {
            return 1;   // true, definitely
        } else if (trap_state == 0) {
            return 0;   // false, definitely (top of state lattice)
        } else {
            return 0;   // false, definitely
        }
    }
    //-------------------------trap_state_add_reason-------------------------------
    public static int trap_state_add_reason(int trap_state, int reason) {
        //assert(reason_is_recorded_per_bytecode((DeoptReason)reason) || reason == Reason_many, "valid reason");
        int recompile_bit = (trap_state & DS_RECOMPILE_BIT);
        trap_state -= recompile_bit;
        if (trap_state == DS_REASON_MASK) {
            return trap_state + recompile_bit;     // already at state lattice bottom
        } else if (trap_state == reason) {
            return trap_state + recompile_bit;     // the condition is already true
        } else if (trap_state == 0) {
            return reason + recompile_bit;          // no condition has yet been true
        } else {
            return DS_REASON_MASK + recompile_bit;  // fall to state lattice bottom
        }
    }
    //-----------------------trap_state_is_recompiled------------------------------
    public static boolean trap_state_is_recompiled(int trap_state) {
        return (trap_state & DS_RECOMPILE_BIT) != 0;
    }
    //-----------------------trap_state_set_recompiled-----------------------------
    public static int trap_state_set_recompiled(int trap_state, boolean z) {
        if (z)  return trap_state |  DS_RECOMPILE_BIT;
        else    return trap_state & ~DS_RECOMPILE_BIT;
    }

    // This is used for debugging and diagnostics, including LogFile output.
    public static String format_trap_state(int trap_state) {
        @RawCType("DeoptReason")int reason      = trap_state_reason(trap_state);
        boolean        recomp_flag = trap_state_is_recompiled(trap_state);
        // Re-encode the state from its decoded components.
        int decoded_state = 0;
        if (reason_is_recorded_per_bytecode(reason) || reason == Reason_many)
            decoded_state = trap_state_add_reason(decoded_state, reason);
        if (recomp_flag)
            decoded_state = trap_state_set_recompiled(decoded_state, recomp_flag);
        // If the state re-encodes properly, format it symbolically.
        // Because this routine is used for debugging and diagnostics,
        // be robust even if the state is a strange value.
        if (decoded_state != trap_state) {
            // Random buggy state that doesn't decode??
            return "#"+trap_state;
        } else {
            return trap_reason_name(reason)+(recomp_flag ? " recompiled" : "");
        }
    }

}
