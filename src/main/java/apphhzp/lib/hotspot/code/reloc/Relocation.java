package apphhzp.lib.hotspot.code.reloc;

import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.util.RawCType;

/** A Relocation is a flyweight object allocated within a RelocationHolder.
 * It represents the relocation data of relocation record.
 * So, the RelocIterator unpacks relocInfos into Relocations.*/
public class Relocation {

    // When a relocation has been created by a RelocIterator,
    // this field is non-null.  It allows the relocation to know
    // its context, such as the address to which it applies.
    private RelocIterator _binding;

    private @RawCType("relocInfo::relocType")int _rtype;

    protected RelocIterator binding(){
        if (_binding == null){
            throw new RuntimeException("must be bound");
        }
        return _binding;
    }
    protected void set_binding(RelocIterator b) {
        if (_binding!=null){
            throw new RuntimeException("must be unbound");
        }
        _binding = b;
        if (_binding==null){
            throw new RuntimeException("must now be bound");
        }
    }

    protected Relocation(@RawCType("relocInfo::relocType")int rtype){
        _binding=null;
        _rtype=(rtype);
    }

    protected static RelocationHolder newHolder() {
        return new RelocationHolder();
    }

    public static Relocation newRelocation(RelocationHolder holder,int rtype){
//        assert(size <= sizeof(holder._relocbuf), "Make _relocbuf bigger!");
//        assert((void* const *)holder.reloc() == &holder._relocbuf[0], "ptrs must agree");
        return holder.reloc()[0]=new Relocation(rtype);
    }

//    // make a generic relocation for a given type (if possible)
//    public static RelocationHolder spec_simple(@RawCType("relocInfo::relocType")int rtype){
//        if (rtype == RelocInfo.relocType.none)
//            return RelocationHolder.none;
//        RelocInfo ri = RelocInfo.create(rtype, 0);
//        RelocIterator itr=new RelocIterator();
//        itr.set_current(ri);
//        itr.reloc();
//        return itr._rh;
//    }
//
//    // here is the type-specific hook which writes relocation data:
//    //public virtual void pack_data_to(CodeSection* dest) { }
//
//    // here is the type-specific hook which reads (unpacks) relocation data:
//    public void unpack_data() {
//        if (!(datalen()==0 || type()==RelocInfo.relocType.none)){
//            throw new RuntimeException("no data here");
//        }
//    }
//
//
//    // Helper functions for pack_data_to() and unpack_data().
//
//    // Most of the compression logic is confined here.
//    // (The "immediate data" mechanism of relocInfo works independently
//    // of this stuff, and acts to further compress most 1-word data prefixes.)
//
//    // A variable-width int is encoded as a short if it will fit in 16 bits.
//    // The decoder looks at datalen to decide whether to unpack short or jint.
//    // Most relocation records are quite simple, containing at most two ints.
//
//    protected static boolean is_short(int x) { return x == (short)x; }
//    protected static @RawCType("short*")long add_short(@RawCType("short*")long p, int x){
//        unsafe.putShort(p++, (short) x);
//        return p;
//    }
//    protected static @RawCType("short*")long add_jint (@RawCType("short*")long p, int x) {
//        unsafe.putShort(p++, (short) RelocInfo.data0_from_int(x));
//        unsafe.putShort(p++, (short) RelocInfo.data1_from_int(x));
//        return p;
//    }
//    protected static @RawCType("short*")long add_var_int(@RawCType("short*")long p, int x) {   // add a variable-width int
//        if (is_short(x)) {
//            p = add_short(p, x);
//        } else {
//            p = add_jint (p, x);
//        }
//        return p;
//    }

//    protected static @RawCType("short*")long pack_1_int_to(@RawCType("short*")long p, int x0) {
//        // Format is one of:  [] [x] [Xx]
//        if (x0 != 0)  p = add_var_int(p, x0);
//        return p;
//    }
//    protected int unpack_1_int(){
//        if (!(datalen() <= 2)){
//            throw new RuntimeException("too much data");
//        }
//        return RelocInfo.jint_data_at(0, data(), datalen());
//    }
//
//    // With two ints, the short form is used only if both ints are short.
//    protected @RawCType("short*")long pack_2_ints_to(@RawCType("short*")long p, int x0, int x1) {
//        // Format is one of:  [] [x y?] [Xx Y?y]
//        if (x0 == 0 && x1 == 0) {
//            // no halfwords needed to store zeroes
//        } else if (is_short(x0) && is_short(x1)) {
//            // 1-2 halfwords needed to store shorts
//            p = add_short(p, x0); if (x1!=0) p = add_short(p, x1);
//        } else {
//            // 3-4 halfwords needed to store jints
//            p = add_jint(p, x0);             p = add_var_int(p, x1);
//        }
//        return p;
//    }
//    protected void unpack_2_ints(int[] x0, int[] x1) {
//        int    dlen = datalen();
//        @RawCType("short*")long dp  = data();
//        if (dlen <= 2) {
//            x0[0] = RelocInfo.short_data_at(0, dp, dlen);
//            x1[0] = RelocInfo.short_data_at(1, dp, dlen);
//        } else {
//            if (!(dlen <= 4)){
//                throw new RuntimeException("too much data");
//            }
//            x0[0] = RelocInfo.jint_data_at(0, dp, dlen);
//            x1[0] = RelocInfo.jint_data_at(2, dp, dlen);
//        }
//    }
//
//
//    // platform-independent utility for patching constant section
//    protected void const_set_data_value(@RawCType("address")long x){
//        if (JVM.isLP64){
//            if (format() == RelocInfo.narrow_oop_in_const) {
//                unsafe.putInt(addr(), (int) OopDesc.encodeOop(x));
//            } else {
//                unsafe.putAddress(addr(),x);
//            }
//        }else {
//            unsafe.putAddress(addr(),x);
//        }
//    }
//    protected void const_verify_data_value(@RawCType("address")long x){
//        if (JVM.isLP64){
//            if (format() == RelocInfo.narrow_oop_in_const) {
//                if (!((unsafe.getInt(addr())&0xffffffffL) == OopDesc.encodeOop((x)))){
//                    throw new RuntimeException("must agree");
//                }
//            } else {
//                if (!(unsafe.getAddress(addr()) == x)){
//                    throw new RuntimeException("must agree");
//                }
//            }
//        }else {
//            if (!(unsafe.getAddress(addr()) == x)){
//                throw new RuntimeException("must agree");
//            }
//        }
//    }
//    protected void pd_set_data_value(@RawCType("address")long x, @RawCType("intptr_t")long off){
//        pd_set_data_value(x,off,false);
//    }
//    // platform-dependent utilities for decoding and patching instructions
//    protected void pd_set_data_value(@RawCType("address")long x, @RawCType("intptr_t")long off, boolean verify_only){// a set or mem-ref
//        if (PlatformInfo.isX86()){
//            X86Relocation.pd_set_data_value(this,x,off,verify_only);
//        }else {
//            throw new UnsupportedOperationException();
//        }
//    }
//    protected void pd_verify_data_value(@RawCType("address")long x, @RawCType("intptr_t")long off) {
//        pd_set_data_value(x, off, true);
//    }
//    protected @RawCType("address")long pd_call_destination(){
//        return this.pd_call_destination(0L);
//    }
//    protected @RawCType("address")long pd_call_destination(@RawCType("address")long orig_addr){
//        if (PlatformInfo.isX86_64()){
//            return X86Relocation.pd_call_destination(this,orig_addr);
//        }else {
//            throw new UnsupportedOperationException();
//        }
//    }
//    protected void pd_set_call_destination (@RawCType("address")long x);
//
//    // this extracts the address of an address in the code stream instead of the reloc data
//    protected @RawCType("address*")long pd_address_in_code       ();
//
//    // this extracts an address from the code stream instead of the reloc data
//    protected @RawCType("address")long  pd_get_address_from_code ();
//
//    // these convert from byte offsets, to scaled offsets, to addresses
//    protected static int scaled_offset(@RawCType("address")long x, @RawCType("address")long base) {
//        int byte_offset = (int) (x - base);
//        int offset = -byte_offset / RelocInfo.addr_unit();
//        assert(address_from_scaled_offset(offset, base) == x, "just checkin'");
//        return offset;
//    }
//    protected static int scaled_offset_null_special(@RawCType("address")long x, @RawCType("address")long base) {
//        // Some relocations treat offset=0 as meaning NULL.
//        // Handle this extra convention carefully.
//        if (x == 0L) {
//            return 0;
//        }
//        if (x==base){
//            throw new IllegalArgumentException("offset must not be zero");
//        }
//        return scaled_offset(x, base);
//    }
//    protected static @RawCType("address")long address_from_scaled_offset(int offset, @RawCType("address")long base) {
//        int byte_offset = -( offset * RelocInfo.addr_unit() );
//        return base + byte_offset;
//    }
//
//    // helpers for mapping between old and new addresses after a move or resize
////    @RawCType("address")long old_addr_for(@RawCType("address")long newa, const CodeBuffer* src, CodeBuffer* dest);
////    @RawCType("address")long new_addr_for(@RawCType("address")long olda, const CodeBuffer* src, CodeBuffer* dest);
////    void normalize_address(address& addr, const CodeSection* dest, bool allow_other_sections = false);
//
//
    // accessors which only make sense for a bound Relocation
    public @RawCType("address")long addr(){ return binding().addr(); }
    public CompiledMethod code() { return binding().code(); }
    public boolean addr_in_const() { return binding().addr_in_const(); }

    protected @RawCType("short*")long data(){ return binding().data(); }
    protected int      datalen() { return binding().datalen(); }
    protected int      format() { return binding().format(); }


    public @RawCType("relocInfo::relocType")int type(){ return _rtype; }

    // is it a call instruction?
    public boolean is_call()                         { return false; }

    // is it a data movement instruction?
    public boolean is_data()                         { return false; }
//
//    // some relocations can compute their own values
//    public @RawCType("address")long value(){
//
//    }
//
//    // all relocations are able to reassert their values
//    public void set_value(@RawCType("address")long x);
//
//    public boolean clear_inline_cache()              { return true; }

    // This method assumes that all virtual/static (inline) caches are cleared (since for static_call_type and
    // ic_call_type is not always posisition dependent (depending on the state of the cache)). However, this is
    // probably a reasonable assumption, since empty caches simplifies code reloacation.
    //public void fix_relocation_after_move(const CodeBuffer* src, CodeBuffer* dest) { }
}
