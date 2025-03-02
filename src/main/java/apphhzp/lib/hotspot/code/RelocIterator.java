package apphhzp.lib.hotspot.code;

import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.oops.oop.OopDesc;

import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelper.unsafe;

public class RelocIterator {
    private long _limit;   // stop producing relocations after this _addr
    private RelocInfo _current; // the current relocation information
    private RelocInfo _end;     // end marker; we're done iterating when _current == _end
    private CompiledMethod _code;    // compiled method containing _addr
    private long _addr;    // instruction to which the relocation applies
    private short _databuf; // spare buffer for compressed data
    private long _data;    // pointer to the relocation's data
    private short _datalen; // number of halfwords in _data
    // Base addresses needed to compute targets of section_word_type relocs.
    private final long[] _section_start = new long[3];
    private final long[] _section_end = new long[3];
    public RelocIterator(){
        this.initialize_misc();
    }

    public RelocIterator(CompiledMethod nm) {
        this(nm, 0, 0);
    }

    public RelocIterator(CompiledMethod nm, long begin, long limit) {
        initialize_misc();
        if (nm == null && begin != 0L) {
            CodeBlob cb = CodeCache.findBlob(begin);
            nm = (cb != null) ? (cb instanceof CompiledMethod ? (CompiledMethod) cb : null) : null;
        }
        if (nm == null) {
            throw new IllegalArgumentException("Must be able to deduce nmethod from other arguments");
        }
        _code = nm;
        _current = new RelocInfo(nm.relocationBegin() - 1);
        _end = new RelocInfo(nm.relocationEnd());
        _addr = nm.contentBegin();
        _section_start[0] = nm.constsBegin();
        _section_start[1] = nm.instsBegin();
        _section_start[2] = nm.stubBegin();
        _section_end[0] = nm.constsEnd();
        _section_end[1] = nm.instsEnd();
        _section_end[2] = nm.stubEnd();
//        assert(!has_current(), "just checking");
//        assert(begin == NULL || begin >= nm->code_begin(), "in bounds");
//        assert(limit == NULL || limit <= nm->code_end(),   "in bounds");
        set_limits(begin, limit);
    }

    private void set_has_current(boolean b) {
        _datalen = (short) (!b ? -1 : 0);
        //debug_only(_data=0L);
    }

    public void set_current(RelocInfo ri) {
        _current = ri;
        set_has_current(true);
    }


    public boolean addr_in_const() {
        final int n = 0;
        return section_start(n) <= addr() && addr() < section_end(n);
    }


    public void set_limits(long begin, long limit) {
        _limit = limit;
        if (begin != 0L) {
            RelocInfo backup;
            long backup_addr;
            while (true) {
                backup = _current;
                backup_addr = _addr;
                if (!next() || addr() >= begin) break;
            }
            _current = backup;
            _addr = backup_addr;
            set_has_current(false);
        }
    }

    public void advance_over_prefix() {
        if (_current.is_datalen()) {
            _data = _current.data();
            _datalen = (short) _current.datalen();
            _current.setAddress(_current.getAddress()+(_datalen + 1)*RelocInfo.SIZE);   // skip the embedded data & header
        } else {
            _databuf = (short) _current.immediate();
            try {
                _data = OopDesc.getEncodedAddress(this)+ unsafe.objectFieldOffset(RelocIterator.class.getDeclaredField("_databuf"));
            }catch (Throwable t){
                throw new RuntimeException(t);
            }
            _datalen = 1;
            _current.setAddress(_current.getAddress()+RelocInfo.SIZE);
        }
    }

    public void initialize_misc() {
        set_has_current(false);
        for (int i = 0; i < 3; i++) {
            _section_start[i] = 0;
            _section_end[i] = 0;
        }
    }

    public RelocInfo current() {
        if (!has_current()) {
            throw new IllegalStateException("must have current");
        }
        return _current;
    }

    public long limit() {
        return _limit;
    }

    public RelocInfo.Type type() {
        return current().type();
    }

    //int          format()        { return (relocInfo::have_format) ? current().format():0; }
    public long addr() {
        return _addr;
    }

    public CompiledMethod code() {
        return _code;
    }

    public long data() {
        return _data;
    }

    public int datalen() {
        return _datalen;
    }

    public boolean has_current() {
        return _datalen >= 0;
    }

    long section_start(int n) {
        if (_section_start[n] == 0) {
            throw new IllegalStateException("must be initialized");
        }
        return _section_start[n];
    }

    long section_end(int n) {
        if (_section_end[n] == 0) {
            throw new IllegalStateException("must be initialized");
        }
        return _section_end[n];
    }

    public boolean next() {
        _current.setAddress(this._current.getAddress()+RelocInfo.SIZE);
        if (_current.getAddress() > _end.getAddress()){
            throw new NoSuchElementException("must not overrun RelocInfo");
        }
        if (_current == _end) {
            set_has_current(false);
            return false;
        }
        set_has_current(true);

        if (_current.is_prefix()) {
            advance_over_prefix();
            if (current().is_prefix()){
                throw new RuntimeException("only one prefix at a time");
            }
        }
        _addr += _current.addr_offset();
        if (_limit != 0L && _addr >= _limit) {
            set_has_current(false);
            return false;
        }

        return true;
    }
}
