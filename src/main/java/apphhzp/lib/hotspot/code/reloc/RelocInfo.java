package apphhzp.lib.hotspot.code.reloc;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class RelocInfo extends JVMObject {
    public static final long VALUE_OFFSET=0L;
    public static final int SIZE=JVM.type("unsigned short").size;
    protected static final int format_width,offset_unit;
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



    public static final  int
            value_width             = JVM.type("unsigned short").size * JVM.BitsPerByte,
            type_width              = 4,   // == log2(type_mask+1)
            nontype_width           = value_width - type_width,
            datalen_width           = nontype_width-1,
            datalen_tag             = 1 << datalen_width,  // or-ed into _value
            datalen_limit           = 1 << datalen_width,
            datalen_mask            = (1 << datalen_width)-1;

    // Derived constant, based on format_width which is PD:
    protected static final int
        offset_width       = nontype_width - format_width,
                offset_mask        = (1<<offset_width) - 1,
                format_mask        = (1<<format_width) - 1;

    public static final int
//#ifdef _LP64
        // for use in format
        // format_width must be at least 1 on _LP64
        narrow_oop_in_const = 1,
//#endif
                // Conservatively large estimate of maximum length (in shorts)
                // of any relocation record.
                // Extended format is length prefix, data words, and tag/offset suffix.
                length_limit       = 1 + 1 + (3*JVM.BytesPerWord/JVM.BytesPerShort) + 1,
                have_format        = format_width > 0?1:0
    ;
    public static final class relocType{
        public static final int
        none                    =  0, // Used when no relocation should be generated
        oop_type                =  1, // embedded oop
        virtual_call_type       =  2, // a standard inline cache call for a virtual send
        opt_virtual_call_type   =  3, // a virtual call that has been statically bound (i.e., no IC cache)
        static_call_type        =  4, // a static send
        static_stub_type        =  5, // stub-entry for static send  (takes care of interpreter case)
        runtime_call_type       =  6, // call to fixed external routine
        external_word_type      =  7, // reference to fixed external address
        internal_word_type      =  8, // reference within the current code blob
        section_word_type       =  9, // internal, but a cross-section reference
        poll_type               = 10, // polling instruction for safepoints
        poll_return_type        = 11, // polling instruction for safepoints at return
        metadata_type           = 12, // metadata that used to be oops
        trampoline_stub_type    = 13, // stub-entry for trampoline
        runtime_call_w_cp_type  = 14, // Runtime call which may load its target from the constant pool
        data_prefix_tag         = 15, // tag for a prefix (carries data arguments)
        type_mask               = 15  // A mask which selects only the above values
        ;
    }

    private static @RawCType("relocType")int check_relocType(@RawCType("relocType")int type){
        if (JVM.ENABLE_EXTRA_CHECK){

        }
        return type;
    }

    private static void check_offset_and_format(int offset, int format){
        if (JVM.ENABLE_EXTRA_CHECK){
            if (!(offset >= 0 && offset < offset_limit())){
                throw new RuntimeException("offset out off bounds");
            }
            if (!(JVM.is_aligned(offset, offset_unit))){
                throw new RuntimeException("misaligned offset");
            }
            if (!((format & format_mask) == format)){
                throw new RuntimeException("wrong format");
            }
        }
    }

    private static int compute_bits(int offset, int format) {
        check_offset_and_format(offset, format);
        return (offset / offset_unit) + (format << offset_width);
    }
    private int value(){
        return unsafe.getShort(this.address+VALUE_OFFSET)&0xffff;
    }
    private static RelocInfo createRaw(@RawCType("relocType")int type, int bits){
        long addr=unsafe.allocateMemory(SIZE);
        unsafe.putShort(addr, (short) ((type << nontype_width) + bits));
        return new RelocInfo(addr);
    }
    static RelocInfo create(@RawCType("relocType")int type, int offset){
        return create(type,offset,0);
    }
    static RelocInfo create(@RawCType("relocType")int type, int offset, int format){
        return createRaw(check_relocType(type), compute_bits(offset, format));
    }
    public RelocInfo(long addr) {
        super(addr);
    }

    // accessors

    public @RawCType("relocType")int type(){
        return (value() >> nontype_width);
    }
    public int  format(){
        return format_mask==0? 0: format_mask &
            (value() >> offset_width);
    }
    public int addr_offset(){
        if (is_prefix()){
            throw new IllegalStateException("must have offset");
        }
        return (value() & offset_mask)*offset_unit;
    }


    protected @RawCType("const short*")long data(){
        if (!is_datalen()){
            throw new IllegalStateException("must have data");
        }
        return (this.address + SIZE);
    }
    protected int datalen(){
        if (!is_datalen()){
            throw new IllegalStateException("must have data");
        }
        return (value() & datalen_mask);
    }
    protected int immediate(){
        if (!is_immediate()){
            throw new IllegalStateException("must have immed");
        }
        return (value() & datalen_mask);
    }

    public static int addr_unit(){ return offset_unit; }
    public static int offset_limit(){ return (1 << offset_width) * offset_unit; }

    public void set_type(@RawCType("relocType")int t){
        int old_offset = addr_offset();
        int old_format = format();
        unsafe.putShort(this.address+VALUE_OFFSET, (short) ((check_relocType(t) << nontype_width) + compute_bits(old_offset, old_format)));
        if (!(type()==(int)t)){
            throw new RuntimeException("sanity check");
        }
        if (!(addr_offset()==old_offset)){
            throw new RuntimeException("sanity check");
        }
        if (!(format()==old_format)){
            throw new RuntimeException("sanity check");
        }
    }

    public void remove() { set_type(relocType.none); }
    protected boolean is_none(){ return type() == relocType.none; }
    protected boolean is_prefix() { return type() == relocType.data_prefix_tag; }
    protected boolean is_datalen() {
        if (!is_prefix()){
            throw new RuntimeException("must be prefix");
        }
        return (value() & datalen_tag) != 0;
    }
    protected boolean is_immediate(){
        if (!is_prefix()){
            throw new RuntimeException("must be prefix");
        }
        return (value() & datalen_tag) == 0;
    }

    // Occasionally records of type relocInfo::none will appear in the stream.
    // We do not bother to filter these out, but clients should ignore them.
    // These records serve as "filler" in three ways:
    //  - to skip large spans of unrelocated code (this is rare)
    //  - to pad out the relocInfo array to the required oop alignment
    //  - to disable old relocation information which is no longer applicable

    public RelocInfo filler_relocInfo(){
        return create(RelocInfo.relocType.none, RelocInfo.offset_limit() - RelocInfo.offset_unit);
    }

    // Every non-prefix relocation may be preceded by at most one prefix,
    // which supplies 1 or more halfwords of associated data.  Conventionally,
    // an int is represented by 0, 1, or 2 halfwords, depending on how
    // many bits are required to represent the value.  (In addition,
    // if the sole halfword is a 10-bit unsigned number, it is made
    // "immediate" in the prefix header word itself.  This optimization
    // is invisible outside this module.)

    public RelocInfo prefix_relocInfo(int datalen){
        if (!(fits_into_immediate(datalen))){
            throw new RuntimeException("datalen in limits");
        }
        return createRaw(RelocInfo.relocType.data_prefix_tag,RelocInfo.datalen_tag | datalen);
    }


    // an immediate relocInfo optimizes a prefix with one 10-bit unsigned value
    protected static RelocInfo immediate_relocInfo(int data0) {
        if (!(fits_into_immediate(data0))){
            throw new RuntimeException("data0 in limits");
        }
        return createRaw(RelocInfo.relocType.data_prefix_tag, data0);
    }
    protected static boolean fits_into_immediate(int data0) {
        return (data0 >= 0 && data0 < datalen_limit);
    }


    // Support routines for compilers.

    // This routine takes an infant relocInfo (unprefixed) and
    // edits in its prefix, if any.  It also updates dest.locs_end.
    //public void initialize(CodeSection* dest, Relocation* reloc);

    // This routine updates a prefix and returns the limit pointer.
    // It tries to compress the prefix from 32 to 16 bits, and if
    // successful returns a reduced "prefix_limit" pointer.
    public RelocInfo finish_prefix(@RawCType("short*")long prefix_limit){
        @RawCType("short*")long p = (this.address+SIZE);
        if (!(prefix_limit >= p)){
            throw new RuntimeException("must be a valid span of data");
        }
        int plen = (int) (prefix_limit - p);
        if (plen == 0) {
            //debug_only(_value = 0xFFFF);
            return this;                         // no data: remove self completely
        }
        if (plen == 1 && fits_into_immediate(unsafe.getShort(p))) {
            RelocInfo tmp=immediate_relocInfo(unsafe.getShort(p));
            unsafe.putShort(this.address+VALUE_OFFSET, (short) tmp.value());// move data inside self
            unsafe.freeMemory(tmp.address);
            return new RelocInfo(this.address+SIZE);
        }
        // cannot compact, so just update the count and return the limit pointer
        RelocInfo tmp=prefix_relocInfo(plen);
        unsafe.putShort(this.address+VALUE_OFFSET, (short) tmp.value());// write new datalen
        unsafe.freeMemory(tmp.address);
        if (!(data() + datalen() == prefix_limit)){
            throw new RuntimeException("pointers must line up");
        }
        return new RelocInfo(prefix_limit);
    }

    // bit-packers for the data array:

    // As it happens, the bytes within the shorts are ordered natively,
    // but the shorts within the word are ordered big-endian.
    // This is an arbitrary choice, made this way mainly to ease debugging.
    public static int data0_from_int(int x){
        return x >> value_width; }
    public static int data1_from_int(int x){
        return (short)x;
    }
    public static int jint_from_data(@RawCType("short*")long data) {
        return (unsafe.getShort(data) << value_width) + (unsafe.getShort(data+2)&0xffff);
    }

    public static int short_data_at(int n, @RawCType("short*")long data, int datalen) {
        return datalen > n ? unsafe.getShort(data+n*2L) : 0;
    }

    public static int jint_data_at(int n, @RawCType("short*")long data, int datalen) {
        return datalen > n+1 ? jint_from_data(data+n*2L) : short_data_at(n, data, datalen);
    }

    // Update methods for relocation information
    // (since code is dynamically patched, we also need to dynamically update the relocation info)
    // Both methods takes old_type, so it is able to performe sanity checks on the information removed.
    public static void change_reloc_info_for_address(RelocIterator itr, @RawCType("address")long pc, @RawCType("relocType")int old_type, @RawCType("relocType")int new_type){
        boolean found = false;
        while (itr.next() && !found) {
            if (itr.addr() == pc) {
                if (!(itr.type()==old_type)){
                    throw new RuntimeException("wrong relocInfo type found");
                }
                itr.current().set_type(new_type);
                found=true;
            }
        }
        if (!found){
            throw new RuntimeException("no relocInfo found for pc");
        }
    }
    public static boolean mustIterateImmediateOopsInCode(){
        if (JVM.isZERO){
            return true;
        }
        return switch (PlatformInfo.getCPU()){
            case "x86","amd64"->true;
            case "aarch64", "arm", "pcc64" ->false;
            default -> throw new UnsupportedOperationException();
        };
    }

}
