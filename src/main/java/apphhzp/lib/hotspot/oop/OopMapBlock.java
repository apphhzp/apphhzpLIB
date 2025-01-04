package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;

public class OopMapBlock {
    public static final int SIZE=JVM.type("uint").size+JVM.intSize;
    private int _offset;
    private int _count;

    public OopMapBlock(final int offset, final int count) {
        _offset = offset;
        _count = count;
    }

    public int offset() {
        return _offset;
    }

    public void set_offset(int offset) {
        _offset = offset;
    }

    // Number of oops in this block.
    public int count() {
        return _count;
    }

    public void set_count(int count) {
        _count = count;
    }

    public void increment_count(int diff) {
        _count += diff;
    }

    public int offset_span() {
        return _count *  JVM.heapOopSize;
    }

    public int end_offset() {
        return offset() + offset_span();
    }

    public boolean is_contiguous(int another_offset) {
        return another_offset == end_offset();
    }

    // sizeof(OopMapBlock) in words.
    public static int size_in_words() {
        return (int) (JVM.alignUp(SIZE, JVM.oopSize) >>
                        JVM.LogBytesPerWord);
    }

    public static int compare_offset(OopMapBlock a, OopMapBlock b) {
        return a.offset() - b.offset();
    }

}
