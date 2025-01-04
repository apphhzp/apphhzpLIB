package apphhzp.lib.hotspot.cds;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class FileMapInfo extends JVMObject {
    public static final Type TYPE=JVM.usingSharedSpaces?JVM.type("FileMapInfo"):Type.EMPTY;
    public static final int SIZE=TYPE.size;
    public static final long CURRENT_INFO_ADDRESS=TYPE.global("_current_info");
    public static final long HEADER_OFFSET=TYPE.offset("_header");
    private FileMapHeader headerCache;
    public FileMapInfo(long addr) {
        super(addr);
    }

    public FileMapHeader getHeader(){
        long addr=unsafe.getAddress(this.address+HEADER_OFFSET);
        if (!isEqual(this.headerCache,addr)){
            this.headerCache=new FileMapHeader(addr);
        }

        return this.headerCache;
    }

    private static FileMapInfo currentCache;
    @Nullable
    public static FileMapInfo getCurrent(){
        long addr= unsafe.getAddress(CURRENT_INFO_ADDRESS);
        if (addr==0L){
            return null;
        }
        if (!isEqual(currentCache,addr)){
            currentCache=new FileMapInfo(addr);
        }
        return currentCache;
    }
}
