package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** Utility class describing elements in exception table.*/
public class ExceptionTableElement extends JVMObject {
    public static final Type TYPE= JVM.type("ExceptionTableElement");
    public static final int SIZE=TYPE.size;
    public static final long START_PC_OFFSET=TYPE.offset("start_pc");
    public static final long END_PC_OFFSET=TYPE.offset("end_pc");
    public static final long HANDLER_PC_OFFSET=TYPE.offset("handler_pc");
    public static final long CATCH_TYPE_INDEX_OFFSET=TYPE.offset("catch_type_index");
    public ExceptionTableElement(long addr) {
        super(addr);
    }

    public int start_pc(){
        return unsafe.getShort(this.address+START_PC_OFFSET)&0xffff;
    }

    public int end_pc(){
        return unsafe.getShort(this.address+END_PC_OFFSET)&0xffff;
    }

    public int handler_pc(){
        return unsafe.getShort(this.address+HANDLER_PC_OFFSET)&0xffff;
    }

    public int catch_type_index(){
        return unsafe.getShort(this.address+CATCH_TYPE_INDEX_OFFSET)&0xffff;
    }

    public void set_start_pc(int pc){
        unsafe.putShort(this.address+START_PC_OFFSET, (short) (pc&0xffff));
    }
    public void set_end_pc(int pc){
        unsafe.putShort(this.address+END_PC_OFFSET, (short) (pc&0xffff));
    }
    public void set_handler_pc(int pc){
        unsafe.putShort(this.address+HANDLER_PC_OFFSET, (short) (pc&0xffff));
    }
    public void set_catch_type_index(int index){
        unsafe.putShort(this.address+CATCH_TYPE_INDEX_OFFSET, (short) (index&0xffff));
    }

    @Override
    public String toString() {
        return "ExceptionTableElement@0x"+Long.toHexString(this.address);
    }
}
