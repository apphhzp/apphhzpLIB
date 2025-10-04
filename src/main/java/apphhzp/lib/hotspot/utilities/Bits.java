package apphhzp.lib.hotspot.utilities;

public class Bits {
    public static int setBits(int x, int m) {
        return x | m;
    }

    public static int clearBits(int x, int m) {
        return x & ~m;
    }

    public static int nthBit(int n) {
        return (n > 32) ? 0 : (1 << n);
    }

    public static int setNthBit(int x, int n) {
        return setBits(x, nthBit(n));
    }

    public static int clearNthBit(int x, int n) {
        return clearBits(x, nthBit(n));
    }

    public static boolean isSetNthBit(int word, int n) {
        return maskBits(word, nthBit(n)) != 0;
    }

    public static int rightNBits(int n) {
        return (nthBit(n) - 1);
    }

    public static int maskBits(int x, int m) {
        return x & m;
    }

    public static long maskBitsLong(long x, long m) {
        return x & m;
    }

    /** Returns integer round-up to the nearest multiple of s (s must be
     a power of two) */
    public static int roundTo(int x, int s) {
        int m = s - 1;
        return maskBits(x + m, ~m);
    }
}
