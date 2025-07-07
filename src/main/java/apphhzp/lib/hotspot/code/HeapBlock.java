package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class HeapBlock extends JVMObject {
    public static final Type TYPE= JVM.type("HeapBlock");
    public static final int SIZE=TYPE.size;
    public static final long HEADER_OFFSET=TYPE.offset("_header");
    public final Header header;
    public HeapBlock(long addr) {
        super(addr);
        this.header=new Header(addr+HEADER_OFFSET);
    }

    public static class Header extends JVMObject{
        public static final Type TYPE=JVM.type("HeapBlock::Header");
        public static final int SIZE=TYPE.size;
        public static final long LENGTH_OFFSET=TYPE.offset("_length");
        public static final long USED_OFFSET=TYPE.offset("_used");
        public Header(long addr) {
            super(addr);
        }

        public long getLength(){
            return unsafe.getAddress(this.address+LENGTH_OFFSET);
        }

        public void setLength(long len){
            unsafe.putAddress(this.address+LENGTH_OFFSET,len);
        }

        public boolean isUsed(){
            return unsafe.getByte(this.address+USED_OFFSET)!=0;
        }

        public void setUsed(boolean used){
            unsafe.putByte(this.address+USED_OFFSET, (byte) (used?1:0));
        }
    }
}
