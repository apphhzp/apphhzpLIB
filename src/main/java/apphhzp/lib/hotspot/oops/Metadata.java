package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.memory.MetaspaceObj;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;

public class Metadata extends JVMObject {
    public static final Type TYPE=JVM.type("Metadata");
    public Metadata(long addr) {
        super(addr);
    }
    public static Metadata getMetadata(long addr) {
        if (addr==0L){
            return null;
        }
        Type type=JVM.findDynamicTypeForAddress(addr,Metadata.TYPE);
        System.err.println(type.name);
        return switch (type.name) {
            case "Metadata" -> new Metadata(addr);
            case "Klass", "ArrayKlass", "ObjArrayKlass", "TypeArrayKlass" -> Klass.getOrCreate(addr);
            case "InstanceKlass", "InstanceClassLoaderKlass", "InstanceMirrorKlass", "InstanceRefKlass" ->
                    InstanceKlass.getOrCreate(addr);
            case "Method" -> Method.getOrCreate(addr);
            case "MethodData" -> new MethodData(addr);
            default ->
                    throw new RuntimeException("Unknown type: " + type.name + ", address: 0x" + Long.toHexString(addr));
        };
    }

    public boolean isShared() {
        if (JVM.usingSharedSpaces) {
            return MetaspaceObj.isShared(this.address);
        }
        return false;
    }
}
