package apphhzp.lib.hotspot.stream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class CompressedReadStream extends CompressedStream{

    public CompressedReadStream(long addr, int st_pos) {
        super(addr, st_pos);
    }

    public boolean readBoolean() {
        return (read() != 0);
    }

    public byte readByte() {
        return (byte) read();
    }

    public char readChar() {
        return (char) read_int();
    }

    public short readShort() {
        return (short) readSignedInt();
    }

    public int readSignedInt() {
        return decodeSign(read_int());
    }

    public int read_int() {
        int b0 = read();
        if (b0 < L) {
            return b0;
        } else {
            return readIntMb(b0);
        }
    }


    public float readFloat() {
        return Float.intBitsToFloat(reverseInt(read_int()));
    }

    public double readDouble() {
        int rh = read_int();
        int rl = read_int();
        int h = reverseInt(rh);
        int l = reverseInt(rl);
        return Double.longBitsToDouble(((long)h << 32) | ((long)l & 0x00000000FFFFFFFFL));
    }

    public long readLong() {
        long low = readSignedInt() & 0xFFFFFFFFL;
        long high = readSignedInt();
        return (high << 32) | low;
    }


    // This encoding, called UNSIGNED5, is taken from J2SE Pack200.
    // It assumes that most values have lots of leading zeroes.
    // Very small values, in the range [0..191], code in one byte.
    // Any 32-bit value (including negatives) can be coded, in
    // up to five bytes.  The grammar is:
    //    low_byte  = [0..191]
    //    high_byte = [192..255]
    //    any_byte  = low_byte | high_byte
    //    coding = low_byte
    //           | high_byte low_byte
    //           | high_byte high_byte low_byte
    //           | high_byte high_byte high_byte low_byte
    //           | high_byte high_byte high_byte high_byte any_byte
    // Each high_byte contributes six bits of payload.
    // The encoding is one-to-one (except for integer overflow)
    // and easy to parse and unparse.
    private int readIntMb(int b0) {
        int pos = this.pos - 1;
        int sum = b0;
        // must collect more bytes: b[1]...b[4]
        int lg_H_i = lg_H;
        for (int i = 0;;) {
            int b_i = read(pos + (++i));
            sum += b_i << lg_H_i; // sum += b[i]*(64**i)
            if (b_i < L || i == MAX_i) {
                this.pos=(pos+i+1);
                return sum;
            }
            lg_H_i += lg_H;
        }
    }

    private short read(int index) {
        return (short)(unsafe.getByte(this.address+index)&0xff);
    }

    private short read() {
        return (short) (unsafe.getByte(this.address+(pos++))&0xff);
    }
}
