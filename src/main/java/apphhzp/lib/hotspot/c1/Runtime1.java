package apphhzp.lib.hotspot.c1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.blob.CodeBlob;

import static apphhzp.lib.ClassHelper.unsafe;

public class Runtime1 {
    public static final Type TYPE= JVM.type("Runtime1");
    public static final long BLOBS_ADDRESS=TYPE.global("_blobs");
    public static final int limit;
    static {
        int val=0;
        while (unsafe.getAddress(BLOBS_ADDRESS+ (long) (val) *JVM.oopSize)!=0){
            ++val;
        }
        limit=val;
    }
    public static long entryFor(int id) {
        return blobFor(id).codeBegin();
    }


    public static CodeBlob blobFor(int id) {
        if (id<0||id>=limit){
            throw new ArrayIndexOutOfBoundsException(id);
        }
        long blobAddr = unsafe.getAddress(BLOBS_ADDRESS+ (long) id *JVM.oopSize);
        return CodeBlob.getCodeBlob(blobAddr);
    }
}
