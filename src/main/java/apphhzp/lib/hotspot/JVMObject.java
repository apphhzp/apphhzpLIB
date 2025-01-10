package apphhzp.lib.hotspot;

public class JVMObject {
    public final long address;
    public JVMObject(long addr){
        this.address=addr;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==this){
            return true;
        }
        if (obj instanceof JVMObject jvmObject){
            return jvmObject.address==this.address;
        }
        return false;
    }

    public static boolean isEqual(JVMObject object,long addr){
        if (object==null){
            return false;
        }
        return object.address==addr;
    }

    @Override
    public String toString() {
        return this.getClass().getName()+"@0x"+Long.toHexString(this.address);
    }
}
