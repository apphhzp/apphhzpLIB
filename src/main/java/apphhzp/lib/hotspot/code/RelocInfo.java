package apphhzp.lib.hotspot.code;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;

import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class RelocInfo {
    public static final int format_width,offset_unit;
    static {
        String cpu = PlatformInfo.getCPU();
        switch (cpu) {
            case "x86" -> {
                offset_unit = 1;
                format_width = 1;
            }
            case "amd64" -> {
                offset_unit = 1;
                format_width = 2;
            }
            case "ppc64" -> {
                offset_unit = 4;
                format_width = JVM.isLP64 ? 1 : 0;
            }
            case "aarch64" -> {
                offset_unit = 4;
                format_width = 1;
            }
            case "arm" -> {
                offset_unit = 4;
                format_width = 0;
            }
            default -> throw new RuntimeException("Should not reach here");
        }
    }
    public static final int
            value_width = 16,
            type_width = 4,   // == log2(type_mask+1)
            nontype_width = value_width - type_width,
            datalen_width = nontype_width - 1,
            datalen_tag = 1 << datalen_width,  // or-ed into _value
            datalen_limit = 1 << datalen_width,
            datalen_mask = (1 << datalen_width) - 1,
            offset_width       = nontype_width - format_width,
            offset_mask        = (1<<offset_width) - 1,
            format_mask        = (1<<format_width) - 1;
    public static final int narrow_oop_in_const=JVM.isLP64?1:0/*unused*/;
    public static final int length_limit=1 + 1 + (3*JVM.BytesPerWord/JVM.BytesPerShort) + 1;
    public static final boolean have_format        = format_width > 0;
    public static final int SIZE = 2;
    private long address;

    public RelocInfo(long addr) {
        this.address = addr;
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(long addr) {
        this.address = addr;
    }

    public int value() {
        return unsafe.getShort(this.address) & 0xffff;
    }

    public long data() {
        if (!is_datalen()) {
            throw new IllegalStateException("must have data");
        }
        return (this.address + 2);
    }

    public int datalen() {
        if (!is_datalen()) {
            throw new IllegalStateException("must have data");
        }
        return (this.value() & datalen_mask);
    }

    public int immediate() {
        if (!is_immediate()) {
            throw new IllegalStateException("must have immed");
        }
        return (this.value() & datalen_mask);
    }

    public Type type() {
        return Type.of(this.value() >>> nontype_width);
    }

    public int  format() {
        return format_mask==0? 0: format_mask & (this.value() >>> offset_width); }

    public boolean is_none() {
        return type() == Type.NONE;
    }

    public boolean is_prefix() {
        return type() == Type.DATA_PREFIX_TAG;
    }

    public boolean is_datalen() {
        if (!is_prefix()) {
            throw new IllegalArgumentException("must be prefix");
        }
        return (this.value() & datalen_tag) != 0;
    }

    public boolean is_immediate() {
        if (!is_prefix()) {
            throw new IllegalArgumentException("must be prefix");
        }
        return (this.value() & datalen_tag) == 0;
    }

    public int addr_offset(){
        if (is_prefix()){
            throw new IllegalStateException("must have offset");
        }
        return (this.value() & offset_mask)*offset_unit;//offset_unit;
    }

    public static int data0_from_int(int x)         { return x >> value_width; }
    public static int data1_from_int(int x)         { return (short)x; }
    public static int jint_from_data(long/* short* */ data) {
        return (unsafe.getShort(data)<< value_width) + (unsafe.getShort(data+2)&0xffff);
    }

    public static int short_data_at(int n, long/* short* */ data, int datalen) {
        return datalen > n ? unsafe.getShort(data+n*2L) : 0;
    }

    public static int jint_data_at(int n, long/* short* */ data, int datalen) {
        return datalen > n+1 ? jint_from_data(data+n*2L) : short_data_at(n, data, datalen);
    }

    public enum Type {
        NONE(0), // Used when no relocation should be generated
        OOP_TYPE(1), // embedded oop
        VIRTUAL_CALL_TYPE(2), // a standard inline cache call for a virtual send
        OPT_VIRTUAL_CALL_TYPE(3), // a virtual call that has been statically bound (i.e., no IC cache)
        STATIC_CALL_TYPE(4), // a static send
        STATIC_STUB_TYPE(5), // stub-entry for static send  (takes care of interpreter case)
        RUNTIME_CALL_TYPE(6), // call to fixed external routine
        EXTERNAL_WORD_TYPE(7), // reference to fixed external address
        INTERNAL_WORD_TYPE(8), // reference within the current code blob
        SECTION_WORD_TYPE(9), // internal, but a cross-section reference
        POLL_TYPE(10), // polling instruction for safepoints
        POLL_RETURN_TYPE(11), // polling instruction for safepoints at return
        METADATA_TYPE(12), // metadata that used to be oops
        TRAMPOLINE_STUB_TYPE(13), // stub-entry for trampoline
        RUNTIME_CALL_W_CP_TYPE(14), // Runtime call which may load its target from the constant pool
        DATA_PREFIX_TAG(15); // tag for a prefix (carries data arguments)
        public static final int TYPE_MASK = 15;// A mask which selects only the above values
        public final int id;

        Type(int val) {
            this.id = val;
        }

        public static Type of(int val) {
            if (val == NONE.id) {
                return NONE;
            } else if (val == OOP_TYPE.id) {
                return OOP_TYPE;
            } else if (val == VIRTUAL_CALL_TYPE.id) {
                return VIRTUAL_CALL_TYPE;
            } else if (val == OPT_VIRTUAL_CALL_TYPE.id) {
                return OPT_VIRTUAL_CALL_TYPE;
            } else if (val == STATIC_CALL_TYPE.id) {
                return STATIC_CALL_TYPE;
            } else if (val == STATIC_STUB_TYPE.id) {
                return STATIC_STUB_TYPE;
            } else if (val == RUNTIME_CALL_TYPE.id) {
                return RUNTIME_CALL_TYPE;
            } else if (val == EXTERNAL_WORD_TYPE.id) {
                return EXTERNAL_WORD_TYPE;
            } else if (val == INTERNAL_WORD_TYPE.id) {
                return INTERNAL_WORD_TYPE;
            } else if (val == SECTION_WORD_TYPE.id) {
                return SECTION_WORD_TYPE;
            } else if (val == POLL_TYPE.id) {
                return POLL_TYPE;
            } else if (val == POLL_RETURN_TYPE.id) {
                return POLL_RETURN_TYPE;
            } else if (val == METADATA_TYPE.id) {
                return METADATA_TYPE;
            } else if (val == TRAMPOLINE_STUB_TYPE.id) {
                return TRAMPOLINE_STUB_TYPE;
            } else if (val == RUNTIME_CALL_W_CP_TYPE.id) {
                return RUNTIME_CALL_W_CP_TYPE;
            } else if (val == DATA_PREFIX_TAG.id) {
                return DATA_PREFIX_TAG;
            }else {
                throw new NoSuchElementException("" + val);
            }
        }
    }
}
