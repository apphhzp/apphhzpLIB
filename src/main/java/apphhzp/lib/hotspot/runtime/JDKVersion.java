package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class JDKVersion extends JVMObject {
    public static final Type TYPE= JVM.type("JDK_Version");
    public static final int SIZE=TYPE.size;
    public static final long CURRENT_ADDRESS=TYPE.global("_current");
    public static final long MAJOR_OFFSET=TYPE.offset("_major");
    public static final JDKVersion current=new JDKVersion(CURRENT_ADDRESS);
    public JDKVersion(long addr) {
        super(addr);
    }

    public int getMajor(){
        return unsafe.getByte(this.address+MAJOR_OFFSET)&0xff;
    }

    public void setMajor(int version){
        unsafe.putByte(this.address+MAJOR_OFFSET,(byte) (version&0xff));
    }
}
