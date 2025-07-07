package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/*
* typedef size_t idx_t;
* typedef uintptr_t bm_word_t;
* */
public class BitMap extends JVMObject {
    public static final long MAP_OFFSET=0L;
    public static final long SIZE_OFFSET= JVM.computeOffset(JVM.size_tSize,MAP_OFFSET+JVM.oopSize);
    public static final int SIZE= (int) JVM.computeOffset(Math.max(JVM.size_tSize,JVM.oopSize),SIZE_OFFSET+JVM.size_tSize);
    public BitMap(long addr) {
        super(addr);
    }

    public long map(){
        return unsafe.getAddress(this.address+MAP_OFFSET);
    }

    public long size(){
        return JVM.getSizeT(this.address+SIZE_OFFSET);
    }

    public long size_in_words(){ return calc_size_in_words(size()); }
    public long size_in_bytes(){ return size_in_words() * JVM.BytesPerWord; }


    public long map(long word){return this.map()+word*JVM.oopSize;}

    public long word_addr(long bit) {
        return map() + to_words_align_down(bit)*JVM.oopSize;
    }

    // Set a word to a specified value or to all ones; clear a word.
    public void setWord  (long word, long val) {
        unsafe.putAddress(this.address+word*JVM.oopSize,val);
    }
    public void setWord  (long word){
        this.setWord(word, -1);
    }
    public void clearWord(long word){
        unsafe.putAddress(this.address+word*JVM.oopSize,0);
    }

    public boolean at(long index) {
        verify_index(index);
        return (unsafe.getAddress(word_addr(index))&bit_mask(index)) != 0;
    }


    public static long bit_in_word(long bit) { return bit & (JVM.BitsPerWord - 1); }

    // Return a mask that will select the specified bit, when applied to the word
    // containing the bit.
    public static long bit_mask(long bit) { return 1L << bit_in_word(bit); }

    // Return the bit number of the first bit in the specified word.
    public static long bit_index(long word)  { return word << JVM.LogBitsPerWord; }

    public static long raw_to_words_align_up(long bit) {
        return raw_to_words_align_down(bit + (JVM.BitsPerWord - 1));
    }

    // Assumes relevant validity checking for bit has already been done.
    public static long raw_to_words_align_down(long bit) {
        return bit >>> JVM.LogBitsPerWord;
    }
    public static long calc_size_in_words(long size_in_bits) {
        verify_size(size_in_bits);
        return raw_to_words_align_up(size_in_bits);
    }

    // Word-aligns bit and converts it to a word offset.
    // precondition: bit <= size()
    public long to_words_align_up(long bit) {
        verify_limit(bit);
        return raw_to_words_align_up(bit);
    }

    // Word-aligns bit and converts it to a word offset.
    // precondition: bit <= size()
    public long to_words_align_down(long bit) {
        verify_limit(bit);
        return raw_to_words_align_down(bit);
    }

    public static long max_size_in_words() { return raw_to_words_align_down(-1); }
    public static long max_size_in_bits() { return max_size_in_words() * JVM.BitsPerWord; }

    public static void verify_size(long size_in_bits) {
        if (JVM.ENABLE_EXTRA_CHECK){
            if (size_in_bits>max_size_in_bits()){
                throw new AssertionError("out of bounds: "+size_in_bits);
            }
        }
    }

    public void verify_index(long bit) {
        if (JVM.ENABLE_EXTRA_CHECK){
            if (bit>=this.size()){
                throw new AssertionError("BitMap index out of bounds: "+bit+" >= "+this.size());
            }
        }
    }

    public void verify_limit(long bit) {

        if (JVM.ENABLE_EXTRA_CHECK){
            if (bit>this.size()){
                throw new AssertionError("BitMap limit out of bounds: "+bit+" > "+this.size());
            }
        }
    }

    public void verify_range(long beg, long end) {
        if (JVM.ENABLE_EXTRA_CHECK){
            if (beg>end){
                throw new AssertionError("BitMap range error: "+beg+" > "+end);
            }
            verify_limit(end);
        }

    }

    @Override
    public String toString() {
        return "BitMap@0x"+Long.toHexString(this.address);
    }
}
