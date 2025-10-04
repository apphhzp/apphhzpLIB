package apphhzp.lib.hotspot.cpu.x86;

import apphhzp.lib.helfy.JVM;

public class X86Assembler {

    public static final class Condition {                     // The x86 condition codes used for conditional jumps/moves.
        public static final int
        zero          = 0x4,
        notZero       = 0x5,
        equal         = 0x4,
        notEqual      = 0x5,
        less          = 0xc,
        lessEqual     = 0xe,
        greater       = 0xf,
        greaterEqual  = 0xd,
        below         = 0x2,
        belowEqual    = 0x6,
        above         = 0x7,
        aboveEqual    = 0x3,
        overflow      = 0x0,
        noOverflow    = 0x1,
        carrySet      = 0x2,
        carryClear    = 0x3,
        negative      = 0x8,
        positive      = 0x9,
        parity        = 0xa,
        noParity      = 0xb;
    };

    public static final class Prefix {
        public static final int
        // segment overrides
        CS_segment = 0x2e,
        SS_segment = 0x36,
        DS_segment = 0x3e,
        ES_segment = 0x26,
        FS_segment = 0x64,
        GS_segment = 0x65,

        REX        = 0x40,

        REX_B      = 0x41,
        REX_X      = 0x42,
        REX_XB     = 0x43,
        REX_R      = 0x44,
        REX_RB     = 0x45,
        REX_RX     = 0x46,
        REX_RXB    = 0x47,

        REX_W      = 0x48,

        REX_WB     = 0x49,
        REX_WX     = 0x4A,
        REX_WXB    = 0x4B,
        REX_WR     = 0x4C,
        REX_WRB    = 0x4D,
        REX_WRX    = 0x4E,
        REX_WRXB   = 0x4F,

        VEX_3bytes = 0xC4,
        VEX_2bytes = 0xC5,
        EVEX_4bytes = 0x62,
        Prefix_EMPTY = 0x0;
    };

    public static final class VexPrefix {
        public static final int
        VEX_B = 0x20,
        VEX_X = 0x40,
        VEX_R = 0x80,
        VEX_W = 0x80;
    };

    public static final class ExexPrefix {
        public static final int
        EVEX_F  = 0x04,
        EVEX_V  = 0x08,
        EVEX_Rb = 0x10,
        EVEX_X  = 0x40,
        EVEX_Z  = 0x80;
    };

    public static final class VexSimdPrefix {
        public static final int
        VEX_SIMD_NONE = 0x0,
        VEX_SIMD_66   = 0x1,
        VEX_SIMD_F3   = 0x2,
        VEX_SIMD_F2   = 0x3;
    };

    public static final class VexOpcode {
        public static final int
        VEX_OPCODE_NONE  = 0x0,
        VEX_OPCODE_0F    = 0x1,
        VEX_OPCODE_0F_38 = 0x2,
        VEX_OPCODE_0F_3A = 0x3,
        VEX_OPCODE_MASK  = 0x1F;
    };

    public static final class AvxVectorLen {
        public static final int
        AVX_128bit = 0x0,
        AVX_256bit = 0x1,
        AVX_512bit = 0x2,
        AVX_NoVec  = 0x4;
    };

    public static final class EvexTupleType {
        public static final int
        EVEX_FV   = 0,
        EVEX_HV   = 4,
        EVEX_FVM  = 6,
        EVEX_T1S  = 7,
        EVEX_T1F  = 11,
        EVEX_T2   = 13,
        EVEX_T4   = 15,
        EVEX_T8   = 17,
        EVEX_HVM  = 18,
        EVEX_QVM  = 19,
        EVEX_OVM  = 20,
        EVEX_M128 = 21,
        EVEX_DUP  = 22,
        EVEX_ETUP = 23;
    };

    public static final class EvexInputSizeInBits {
        public static final int
        EVEX_8bit  = 0,
        EVEX_16bit = 1,
        EVEX_32bit = 2,
        EVEX_64bit = 3,
        EVEX_NObit = 4;
    };

    public static final class WhichOperand {
        public static final int
        // input to locate_operand, and format code for relocations
        imm_operand  = 0,            // embedded 32-bit|64-bit immediate operand
        disp32_operand = 1,          // embedded 32-bit displacement or address
        call32_operand = 2,          // embedded 32-bit self-relative displacement
        narrow_oop_operand = 3,     // embedded 32-bit immediate narrow oop
        _WhichOperand_limit= JVM.isLP64?4:3;
    }

    // Comparison predicates for integral types & FP types when using SSE
    public static final class ComparisonPredicate {
        public static final int
                eq = 0,
        lt = 1,
        le = 2,
        _false = 3,
        neq = 4,
        nlt = 5,
        nle = 6,
        _true = 7;
    };

    // Comparison predicates for FP types when using AVX
    // O means ordered. U is unordered. When using ordered, any NaN comparison is false. Otherwise, it is true.
    // S means signaling. Q means non-signaling. When signaling is true, instruction signals #IA on NaN.
    public static final class ComparisonPredicateFP {
        public static final int
                EQ_OQ = 0,
        LT_OS = 1,
        LE_OS = 2,
        UNORD_Q = 3,
        NEQ_UQ = 4,
        NLT_US = 5,
        NLE_US = 6,
        ORD_Q = 7,
        EQ_UQ = 8,
        NGE_US = 9,
        NGT_US = 0xA,
        FALSE_OQ = 0XB,
        NEQ_OQ = 0xC,
        GE_OS = 0xD,
        GT_OS = 0xE,
        TRUE_UQ = 0xF,
        EQ_OS = 0x10,
        LT_OQ = 0x11,
        LE_OQ = 0x12,
        UNORD_S = 0x13,
        NEQ_US = 0x14,
        NLT_UQ = 0x15,
        NLE_UQ = 0x16,
        ORD_S = 0x17,
        EQ_US = 0x18,
        NGE_UQ = 0x19,
        NGT_UQ = 0x1A,
        FALSE_OS = 0x1B,
        NEQ_OS = 0x1C,
        GE_OQ = 0x1D,
        GT_OQ = 0x1E,
        TRUE_US =0x1F;
    };

    public static final class Width {
        public static final int
                B = 0,
        W = 1,
        D = 2,
        Q = 3;
    };
}
