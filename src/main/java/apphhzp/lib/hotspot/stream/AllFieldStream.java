package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oop.InstanceKlass;
import apphhzp.lib.hotspot.oop.U2Array;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;

public class AllFieldStream extends FieldStreamBase{
    public AllFieldStream(U2Array fields, ConstantPool constants) {
        super(fields, constants);
    }

    public AllFieldStream(InstanceKlass klass) {
        super(klass.getFields(), klass.getConstantPool());
    }
}
