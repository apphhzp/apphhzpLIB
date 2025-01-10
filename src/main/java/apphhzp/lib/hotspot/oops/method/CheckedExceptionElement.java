package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class CheckedExceptionElement extends JVMObject {
    public static final Type TYPE= JVM.type("CheckedExceptionElement");
    public static final int SIZE=TYPE.size;
    public static final long CLASS_CP_INDEX_OFFSET=TYPE.offset("class_cp_index");
    public CheckedExceptionElement(long addr) {
        super(addr);
    }

    //Class index in the ConstantPool
    public int getClassCPIndex(){
        return unsafe.getShort(this.address+CLASS_CP_INDEX_OFFSET)&0xffff;
    }

    public void setClassCPIndex(int index){
        unsafe.putShort(this.address+CLASS_CP_INDEX_OFFSET, (short) (index&0xffff));
    }
}
