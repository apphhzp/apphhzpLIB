package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** Utility class describing elements in method parameters.*/
public class MethodParametersElement extends JVMObject {
    public static final Type TYPE= JVM.type("MethodParametersElement");
    public static final int SIZE=TYPE.size;
    public static final long NAME_CP_INDEX_OFFSET=0;
    public static final long FLAGS_OFFSET=JVM.computeOffset(2,NAME_CP_INDEX_OFFSET+2);
    static {
        JVM.assertOffset(SIZE,JVM.computeOffset(2,FLAGS_OFFSET+2));
    }
    public MethodParametersElement(long addr) {
        super(addr);
    }

    public int name_cp_index(){
        return unsafe.getShort(this.address+NAME_CP_INDEX_OFFSET)&0xffff;
    }

    public void set_name_cp_index(int index){
        unsafe.putShort(this.address+NAME_CP_INDEX_OFFSET,(short)(index&0xffff));
    }

    public int flags(){
        return unsafe.getShort(this.address+FLAGS_OFFSET)&0xffff;
    }

    public void set_flags(int flags){
        unsafe.putShort(this.address+FLAGS_OFFSET,(short)(flags&0xffff));
    }

    @Override
    public String toString() {
        return "MethodParametersElement@0x"+Long.toHexString(this.address);
    }
}
