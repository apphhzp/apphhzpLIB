package apphhzp.lib.hotspot.stream;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class CompressedStream extends JVMObject {
    public static final Type TYPE= JVM.type("CompressedStream");
    public static final long BUFFER_OFFSET=TYPE.offset("_buffer");
    public static final long POSITION_OFFSET=TYPE.offset("_position");
    public static final int LogBitsPerByte = 3;
    public static final int BitsPerByte = 1 << 3;

    // Constants for UNSIGNED5 coding of Pack200
    public static final int lg_H = 6;
    public static final int H = 1<<lg_H;  // number of high codes (64)
    public static final int L = (1<<BitsPerByte) - H; // number of low codes (192)
    public static final int MAX_i = 4;      // bytes are numbered in (0..4)
    protected int pos;

    public CompressedStream(long addr,int st_pos){
        super(addr);
        this.pos=st_pos;
    }

    public int pos(){
        return this.pos;
    }

    public static int encodeSign(int value) {
        return (value << 1) ^ (value >> 31);
    }

    public static int decodeSign(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    // 32-bit self-inverse encoding of float bits
    // converts trailing zeros (common in floats) to leading zeros
    public static int reverseInt(int i) {
        i = (i & 0x55555555) << 1 | (i >>> 1) & 0x55555555;
        i = (i & 0x33333333) << 3 | (i >>> 2) & 0x33333333;
        i = (i & 0x0f0f0f0f) << 4 | (i >>> 4) & 0x0f0f0f0f;
        i = (i << 24) | ((i & 0xff00) << 8) | ((i >>> 8) & 0xff00) | (i >>> 24);
        return i;
    }
}
