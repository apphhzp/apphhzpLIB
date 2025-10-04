package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class VFrameArray extends JVMObject {
    public static final Type TYPE= JVM.type("vframeArray");
    public static final int SIZE= TYPE.size;
    public static final long NEXT_OFFSET=TYPE.offset("_next");
    public static final long ORIGINAL_OFFSET=TYPE.offset("_original");
    public static final long CALLER_OFFSET=TYPE.offset("_caller");
    public static final long FRAMES_OFFSET=TYPE.offset("_frames");
    public VFrameArray(long addr) {
        super(addr);
    }

    // Accessories for instance variable
    public int frames(){
        return unsafe.getInt(this.address+FRAMES_OFFSET);
    }

    // Accessors for next
    public VFrameArray next(){
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        return new VFrameArray(addr);
    }
    public void set_next(VFrameArray value){
        unsafe.putAddress(this.address+NEXT_OFFSET,value==null?0L:value.address);
    }

    // Accessors for sp
    public @RawCType("intptr_t*")long sp(){
        return unsafe.getAddress(this.address+ORIGINAL_OFFSET+Frame.SP_OFFSET);
    }

    public @RawCType("address")long original_pc(){
        return unsafe.getAddress(this.address+ORIGINAL_OFFSET+Frame.PC_OFFSET);
    }

    public Frame original(){
        return Frame.of(this.address+ORIGINAL_OFFSET);
    }

    public Frame caller(){
        return Frame.of(this.address+CALLER_OFFSET);
    }
}
