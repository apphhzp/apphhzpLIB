package apphhzp.lib.hotspot.classfile;

import java.util.Arrays;

public class ClassFileStream {
//    /**
//     * code{u1*}
//     */
//    private long _buffer_start; // Buffer bottom
    /**
     * code{u1*}
     */
    private int _buffer_length;   // Buffer top (one past last element)
    /**
     * {u1*}
     */
    private byte[] _current;    // Current buffer position
    private int _current_pos;
    //char*
    private char[] _source;     // Source of stream (directory name, ZIP/JAR archive name)
    private boolean _need_verify;             // True if verification is on for the class file
    private boolean _from_boot_loader_modules_image;  // True if this was created by ClassPathImageEntry.
    //void truncated_file_error(TRAPS)
    //;
    public static final boolean verify = false;

    public ClassFileStream(byte[] buffer, int length, char[] source, boolean verify_stream) {
        this(buffer, length, source, verify_stream, false);
    }
    public ClassFileStream(byte[] buffer, int length, char[] source, boolean verify_stream, boolean from_boot_loader_modules_image) {
        if (buffer == null) {
            throw new NullPointerException("caller should throw NPE");
        }
        _current = buffer;
        _current_pos = 0;
        _buffer_length = length;
        _source = source;
        _need_verify = verify_stream;
        _from_boot_loader_modules_image = from_boot_loader_modules_image;
    }

    public byte[] clone_buffer() {
        return Arrays.copyOf(_current, _buffer_length);
    }

    public char[] clone_source() {
        char[] src = source();
        char[] source_copy = null;
        if (src != null) {
            source_copy=Arrays.copyOf(src, src.length);
        }
        return source_copy;
    }

    public ClassFileStream _clone() {
        return new ClassFileStream(clone_buffer(),
                length(),
                clone_source(),
                need_verify(),
                from_boot_loader_modules_image());
    }

    public byte[] buffer() {
        return _current;
    }

    int length() {
        return (int) _buffer_length;
    }

//    public int current() {
//        return _current_pos;
//    }

    void set_current(byte[] buffer,int pos) {
        if (!(pos >= 0 && pos <= _buffer_length)) {
            throw new IllegalArgumentException("invariant");
        }
        _current=buffer;
        _current_pos = pos;
    }

    // for relative positioning
    public long current_offset() {
        return _current_pos;
    }

    public char[] source() {
        return _source;
    }

    public boolean need_verify() {
        return _need_verify;
    }

    public void set_verify(boolean flag) {
        _need_verify = flag;
    }

    public boolean from_boot_loader_modules_image() {
        return _from_boot_loader_modules_image;
    }

    public void check_truncated_file(boolean b) {
        if (b) {
            throw new ClassFormatError("Truncated class file");
        }
    }

    public void guarantee_more(int size) {
        long remaining = _buffer_length - _current_pos;
        long usize = size & 0xffffffffL;
        check_truncated_file(usize > remaining);
    }

    // Read u1 from stream
    byte get_u1_fast() {
        return _current[_current_pos++];
    }

    byte get_u1() {
        if (_need_verify) {
            guarantee_more(1);
        } else {
            if (!(1 <= _buffer_length - _current_pos)){
                throw new IllegalStateException("buffer overflow");
            }
        }
        return get_u1_fast();
    }

    // Read u2 from stream
    short get_u2_fast() {
        short res = (short)((_current[_current_pos]&0xff)<<8 | _current[_current_pos+1]&0xff);
        _current_pos += 2;
        return res;
    }

    short get_u2() {
        if (_need_verify) {
            guarantee_more(2);
        } else {
            if (!(2 <= _buffer_length - _current_pos)){
                throw new IllegalStateException("buffer overflow");
            }
        }
        return get_u2_fast();
    }

    // Read u4 from stream
    int get_u4_fast() {
        int res = (_current[_current_pos]&0xff)<<24 | (_current[_current_pos+1]&0xff)<<16 | (_current[_current_pos+2]&0xff)<<8 | _current[_current_pos+3]&0xff;
        _current_pos += 4;
        return res;
    }

    // Read u8 from stream
    long get_u8_fast() {
        long res =(_current[_current_pos]&0xffL)<<56L|(_current[_current_pos+1]&0xffL)<<48L|(_current[_current_pos+2]&0xffL)<<40L|(_current[_current_pos+3]&0xffL)<<32L|(_current[_current_pos+4]&0xffL)<<24L|(_current[_current_pos+5]&0xffL)<<16L|(_current[_current_pos+6]&0xffL)<<8L|(_current[_current_pos+7]&0xffL);// Bytes::get_Java_u8 (address) _current;
        _current_pos += 8;
        return res;
    }

    // Skip length elements from stream
    void skip_u1(int length) {
        if (_need_verify) {
            guarantee_more(length);
        }
        skip_u1_fast(length);
    }

    void skip_u1_fast(int length) {
        _current_pos += length;
    }

    void skip_u2_fast(int length) {
        _current_pos += 2 * length;
    }

    void skip_u4_fast(int length) {
        _current_pos += 4 * length;
    }

    // Tells whether eos is reached
    boolean at_eos() {
        return _current_pos == _buffer_length;
    }

    public ClassFileStream copy(){
        ClassFileStream re=new ClassFileStream(_current,_buffer_length,_source,_need_verify,_from_boot_loader_modules_image);
        re._current_pos=this._current_pos;
        re._buffer_length=this._buffer_length;
        re._current=this.clone_buffer();
        re._source=this.clone_source();
        re._from_boot_loader_modules_image=this._from_boot_loader_modules_image;
        re._need_verify=this._need_verify;
        return re;
    }
}
