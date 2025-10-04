package apphhzp.lib.hotspot.code.reloc;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.CodeCache;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class RelocIterator {
    private static final int SECT_LIMIT = 3;  // must be equal to CodeBuffer::SECT_LIMIT, checked in ctor

    private @RawCType("address") long _limit;   // stop producing relocations after this _addr
    private RelocInfo _current; // the current relocation information
    private RelocInfo _end;     // end marker; we're done iterating when _current == _end
    private CompiledMethod _code;    // compiled method containing _addr
    private @RawCType("address") long _addr;    // instruction to which the relocation applies
    private short _databuf; // spare buffer for compressed data
    private @RawCType("short*") long _data;    // pointer to the relocation's data
    private short _datalen; // number of halfwords in _data

    // Base addresses needed to compute targets of section_word_type relocs.
    private @RawCType("address") long[] _section_start = new long[SECT_LIMIT];
    private @RawCType("address") long[] _section_end = new long[SECT_LIMIT];

    private void set_has_current(boolean b) {
        _datalen = (short) (!b ? -1 : 0);
        if (JVM.ENABLE_EXTRA_CHECK) {
            _data = 0L;
        }
    }

    void set_current(RelocInfo ri) {
        _current = ri;
        set_has_current(true);
    }

    RelocationHolder _rh = new RelocationHolder(); // where the current relocation is allocated

    public RelocInfo current() {
        if (!(has_current())) {
            throw new IllegalStateException("must have current");
        }
        return _current;
    }

    private void set_limits(@RawCType("address") long begin, @RawCType("address") long limit) {
        _limit = limit;

        // the limit affects this next stuff:
        if (begin != 0L) {
            RelocInfo backup;
            @RawCType("address") long backup_addr;
            while (true) {
                backup = _current;
                backup_addr = _addr;
                if (!next() || addr() >= begin) break;
            }
            // At this point, either we are at the first matching record,
            // or else there is no such record, and !has_current().
            // In either case, revert to the immediatly preceding state.
            _current = backup;
            _addr = backup_addr;
            set_has_current(false);
        }
    }

    // All the strange bit-encodings are in here.
    // The idea is to encode relocation data which are small integers
    // very efficiently (a single extra halfword).  Larger chunks of
    // relocation data need a halfword header to hold their size.
    private static final long _databuf_offset;

    static {
        try {
            _databuf_offset = unsafe.objectFieldOffset(RelocIterator.class.getDeclaredField("_databuf"));
        } catch (NoSuchFieldException e) {
            ClassHelperSpecial.throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    private void advance_over_prefix() {
        if (_current.is_datalen()) {
            _data = _current.data();
            _datalen = (short) _current.datalen();
            _current = new RelocInfo(_current.address + (_datalen + 1L) * RelocInfo.SIZE); // skip the embedded data & header
        } else {
            unsafe.putShort(OopDesc.getAddress(this) + _databuf_offset, (short) _current.immediate());
            _data = OopDesc.getAddress(this) + _databuf_offset;
            _datalen = 1;
            _current = new RelocInfo(_current.address + RelocInfo.SIZE);                 // skip the header
        }
        // The client will see the following relocInfo, whatever that is.
        // It is the reloc to which the preceding data applies.
    }

    private void initialize_misc() {
        set_has_current(false);
        for (int i = (int) 0; i < (int) 3; i++) {
            _section_start[i] = 0L;  // these will be lazily computed, if needed
            _section_end[i] = 0L;
        }
    }

    private void initialize(CompiledMethod nm, @RawCType("address") long begin, @RawCType("address") long limit) {
        initialize_misc();
        if (nm == null && begin != 0L) {
            // allow nmethod to be deduced from beginning address
            CodeBlob cb = CodeCache.find_blob(begin);
            nm = (cb != null) ? cb.as_compiled_method_or_null() : null;
        }
        if (nm == null) {
            throw new NullPointerException("must be able to deduce nmethod from other arguments");
        }

        _code = nm;
        _current = new RelocInfo(nm.relocation_begin().address - RelocInfo.SIZE);
        _end = nm.relocation_end();
        _addr = nm.content_begin();

        // Initialize code sections.
        _section_start[0] = nm.consts_begin();
        _section_start[1] = nm.insts_begin();
        _section_start[2] = nm.stub_begin();

        _section_end[0] = nm.consts_end();
        _section_end[1] = nm.insts_end();
        _section_end[2] = nm.stub_end();

        if (has_current()) {
            throw new RuntimeException("just checking");
        }
        if (!(begin == 0L || begin >= nm.code_begin())) {
            throw new IllegalArgumentException("in bounds");
        }
        if (!(limit == 0L || limit <= nm.code_end())) {
            throw new IllegalArgumentException("in bounds");
        }
        set_limits(begin, limit);
    }

    RelocIterator() {
        initialize_misc();
    }


    // constructor
    public RelocIterator(CompiledMethod nm) {
        initialize(nm, 0, 0);
    }

    public RelocIterator(CompiledMethod nm, @RawCType("address") long begin) {
        initialize(nm, begin, 0L);
    }

    public RelocIterator(CompiledMethod nm, @RawCType("address") long begin, @RawCType("address") long limit) {
        initialize(nm, begin, limit);
    }
    //RelocIterator(CodeSection* cb, address begin = NULL, address limit = NULL);

    // get next reloc info, return !eos
    public boolean next() {
        _current = new RelocInfo(_current.address + RelocInfo.SIZE);
        if (!(_current.address <= _end.address)) {
            throw new RuntimeException("must not overrun relocInfo");
        }
        if (_current.equals(_end)) {
            set_has_current(false);
            return false;
        }
        set_has_current(true);

        if (_current.is_prefix()) {
            advance_over_prefix();
            if (current().is_prefix()) {
                throw new IllegalStateException("only one prefix at a time");
            }
        }

        _addr += _current.addr_offset();

        if (_limit != 0L && _addr >= _limit) {
            set_has_current(false);
            return false;
        }

        return true;
    }

    // accessors
    public @RawCType("address") long limit() {
        return _limit;
    }

    public @RawCType("relocType") int type() {
        return current().type();
    }

    public int format() {
        return (RelocInfo.have_format != 0) ? current().format() : 0;
    }

    public @RawCType("address") long addr() {
        return _addr;
    }

    public CompiledMethod code() {
        return _code;
    }

    public @RawCType("short*") long data() {
        return _data;
    }

    public int datalen() {
        return _datalen;
    }

    public boolean has_current() {
        return _datalen >= 0;
    }

    public boolean addr_in_const() {
        final int n = 0;
        return section_start(n) <= addr() && addr() < section_end(n);
    }

    public @RawCType("address") long section_start(int n) {
        if (_section_start[n] == 0) {
            throw new IllegalStateException("must be initialized");
        }
        return _section_start[n];
    }

    public @RawCType("address") long section_end(int n) {
        if (_section_end[n] == 0) {
            throw new IllegalStateException("must be initialized");
        }
        return _section_end[n];
    }

    // The address points to the affected displacement part of the instruction.
    // For RISC, this is just the whole instruction.
    // For Intel, this is an unaligned 32-bit word.

    // type-specific relocation accessors:  oop_Relocation* oop_reloc(), etc.
//  #define EACH_TYPE(name)                               \
//    inline name##_Relocation* name##_reloc();
//    APPLY_TO_RELOCATIONS(EACH_TYPE)
//  #undef EACH_TYPE
//    // generic relocation accessor; switches on type to call the above
//    Relocation* reloc();

    // We know all the xxx_Relocation classes, so now we can define these:
//#define EACH_CASE(name)                                         \
//    inline name##_Relocation* RelocIterator::name##_reloc() {       \
//        assert(type() == relocInfo::name##_type, "type must agree");  \
//        /* The purpose of the placed "new" is to re-use the same */   \
//        /* stack storage for each new iteration. */                   \
//        name##_Relocation* r = new(_rh) name##_Relocation();          \
//        r->set_binding(this);                                         \
//        r->name##_Relocation::unpack_data();                          \
//        return r;                                                     \
//    }
//    APPLY_TO_RELOCATIONS(EACH_CASE);
//#undef EACH_CASE
//
//    public Relocation reloc() {
//        // (take the "switch" out-of-line)
//        @RawCType("relocInfo::relocType")int t = type();
//  #define EACH_TYPE(name)                             \
//  else if (t == relocInfo::name##_type) {             \
//            return name##_reloc();                            \
//        }
//        APPLY_TO_RELOCATIONS(EACH_TYPE);
//  #undef EACH_TYPE
//        if (!(t == RelocInfo.relocType.none)){
//            throw new RuntimeException("must be padding");
//        }
//        return  Relocation.newRelocation(_rh,t);
//    }


    private static void copy(RelocIterator fr, RelocIterator to) {
        to._addr = fr._addr;
        to._current = fr._current;
        to._end = fr._end;
        to._limit = fr._limit;
        to._code = fr._code;
        to._data = fr._data;
        to._databuf = fr._databuf;
        to._datalen = fr._datalen;
        to._section_start = fr._section_start.clone();
        to._section_end = fr._section_end.clone();
        to._rh = fr._rh.clone();
    }

    public void print(PrintStream tty) {
        RelocIterator save_this = new RelocIterator();
        copy(this, save_this);
        RelocInfo scan = _current;
        if (!has_current()) {
            scan = new RelocInfo(scan.address + RelocInfo.SIZE);  // nothing to scan here!
        }

        boolean skip_next = has_current();
        boolean got_next;
        while (true) {
            got_next = (skip_next || next());
            skip_next = false;

            tty.print("         @0x" + Long.toHexString(scan.address) + ": ");
            RelocInfo newscan = new RelocInfo(_current.address + RelocInfo.SIZE);
            if (!has_current()) {
                newscan = new RelocInfo(newscan.address - RelocInfo.SIZE);  // nothing to scan here!
            }
            while (scan.address < newscan.address) {
                tty.printf("%04x", unsafe.getShort(scan.address) & 0xFFFF);
                scan = new RelocInfo(scan.address + RelocInfo.SIZE);
            }
            tty.println();

            if (!got_next) break;
            print_current();
        }
        copy(save_this, this);
    }

    public void print_current() {
        //TODO
    }

}
