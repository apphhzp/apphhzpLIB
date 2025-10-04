package apphhzp.lib.hotspot.runtime.bytecode;

import apphhzp.lib.PlatformInfo;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public final class Bytes {
    private Bytes(){
        throw new UnsupportedOperationException("All Static");
    }

    private static int byte_at(long addr){
        return unsafe.getByte(addr)&0xff;
    }
    private static void byte_put(long addr, byte value){
        unsafe.putByte(addr, value);
    }


    public static int get_Java_u2(long addr){
        return (byte_at(addr)<<8|byte_at(addr+1))&0xffff;
    }
    public static int get_Java_u4(long addr){
        return byte_at(addr)<<24 | byte_at(addr+1)<<16 | byte_at(addr+2)<<8 | byte_at(addr+3);
    }

    public static long get_Java_u8(long p) {
        return (byte_at(p) &0xffL)<< 56L |
                (byte_at(p+1)&0xffL) << 48L |
                (byte_at(p+2)&0xffL) << 40L |
                (byte_at(p+3)&0xffL) << 32L |
                (long) byte_at(p + 4) << 24L |
                (long) byte_at(p + 5) << 16L |
                (long) byte_at(p + 6) <<  8L |
                byte_at(p+7);
    }

    public static void put_Java_u2(long p,int x){
        byte_put(p, (byte) ((x&0xffff)>>>8));
        byte_put(p+1, (byte) (x&0xff));
    }

    public static void put_Java_u4(long p,int x) {
        byte_put(p, (byte) (x>>>24));
        byte_put(p+1, (byte) ((x>>>16)&0xff));
        byte_put(p+2,(byte) ((x>>>8)&0xff));
        byte_put(p+3, (byte) (x&0xff));
    }

    public static void put_Java_u8(long p, long x) {
        byte_put(p, (byte) (x>>>56L));
        byte_put(p+1, (byte) ((x>>>48L)&0xffL));
        byte_put(p+2,(byte) ((x>>>40L)&0xffL));
        byte_put(p+3,(byte) ((x>>>32L)&0xffL));
        byte_put(p+4,(byte) ((x>>>24L)&0xffL));
        byte_put(p+5,(byte) ((x>>>16L)&0xffL));
        byte_put(p+6,(byte) ((x>>>8L)&0xffL));
        byte_put(p+7,(byte) (x&0xffL));
    }


    public static int get_native_u2(long addr){
        return unsafe.getShort(addr)&0xffff;
    }

    public static int get_native_u4(long addr){
        return unsafe.getInt(addr);
    }

    public static long get_native_u8(long p){
        return unsafe.getLong(p);
    }

    public static void put_native_u2(long p,int x){
        unsafe.putShort(p, (short) (x&0xffff));
    }
    public static void put_native_u4(long p,int x) {
        unsafe.putInt(p, x);
    }
    public static void put_native_u8(long p,long x) {
        unsafe.putLong(p, x);
    }


    public int swap_u2(int x){
        if (true){
            throw new RuntimeException("More consideration is needed");
        }
        if (PlatformInfo.isBigEndian()){
            return x;
        }
        return ((((x&0xffff) >>> 8) & 0xFF) | ((x&0xffff) << 8))&0xffff;
    }

    public int swap_u4(int x) {
        if (true){
            throw new RuntimeException("More consideration is needed");
        }
        if (PlatformInfo.isBigEndian()){
            return x;
        }
        return (swap_u2(x&0xffff) << 16) | (swap_u2((x >>> 16)) & 0xFFFF);
    }

    public long swap_u8(long x) {
        if (true){
            throw new RuntimeException("More consideration is needed");
        }
        if (PlatformInfo.isBigEndian()){
            return x;
        }
        return (swap_u4((int)(x&0xffffffffL))&0xffffffffL)<<32L | (swap_u4((int) (x >>> 32L))&0xFFFFFFFFL);
    }
}
