package apphhzp.lib.hotspot.util;

import java.nio.charset.StandardCharsets;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class CString {
    private CString(){
        throw new UnsupportedOperationException("All Static");
    }
    public static long memchr(long buf,int ch,long count){
        if (buf==0L){
            throw new NullPointerException();
        }
        for (long i=0;i<count;i++){
            if ((unsafe.getByte(buf+i)&0xff)==ch){
                return buf+i;
            }
        }
        return 0;
    }

    public static int memcmp(long buf1,long buf2,long count){
        if (buf1==0||buf2==0){
            throw new NullPointerException();
        }
        for (long i=0;i<count;i++){
            byte b1=unsafe.getByte(buf1+i),b2=unsafe.getByte(buf2+i);
            if (b1<b2){
                return -1;
            }else if (b1>b2){
                return 1;
            }
        }
        return 0;
    }
    public static int memcmp(String buf1,long buf2,long count){
        if (buf1==null||buf2==0){
            throw new NullPointerException();
        }
        byte[] bs1=buf1.getBytes(StandardCharsets.UTF_8);
        for (long i=0;i<count;i++){
            byte b1=(i>=bs1.length?0:bs1[(int) i]),b2=unsafe.getByte(buf2+i);
            if (b1<b2){
                return -1;
            }else if (b1>b2){
                return 1;
            }
        }
        return 0;
    }
    public static int memcmp(long buf1,String buf2,long count){
        if (buf1==0L||buf2==null){
            throw new NullPointerException();
        }
        byte[] bs2=buf2.getBytes(StandardCharsets.UTF_8);
        for (long i=0;i<count;i++){
            byte b1=unsafe.getByte(buf1+i),b2=(i>=bs2.length?0:bs2[(int) i]);
            if (b1<b2){
                return -1;
            }else if (b1>b2){
                return 1;
            }
        }
        return 0;
    }
    public static int memcmp(String buf1,String buf2,long count){
        if (buf1==null||buf2==null){
            throw new NullPointerException();
        }
        byte[] bs1=buf1.getBytes(StandardCharsets.UTF_8),bs2=buf2.getBytes(StandardCharsets.UTF_8);
        for (long i=0;i<count;i++){
            byte b1=(i>=bs1.length?0:bs1[(int) i]),b2=(i>=bs2.length?0:bs2[(int) i]);
            if (b1<b2){
                return -1;
            }else if (b1>b2){
                return 1;
            }
        }
        return 0;
    }
}
