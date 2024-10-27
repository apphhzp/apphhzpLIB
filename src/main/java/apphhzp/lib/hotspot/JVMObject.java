package apphhzp.lib.hotspot;

public class JVMObject {
    public final long address;
    public JVMObject(long addr){
        this.address=addr;
    }

    @Override
    public boolean equals(Object obj) {
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
}
