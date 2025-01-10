package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;

public class InternalFieldStream extends FieldStreamBase{
    public InternalFieldStream(InstanceKlass klass) {
        super(klass.getFields(), klass.getConstantPool(), klass.getFieldsCount(),0);
    }
}
