package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;

public class InternalFieldStream extends FieldStreamBase{
    public InternalFieldStream(InstanceKlass klass) {
        super(klass.getFields(), klass.getConstantPool(), klass.java_fields_count(),0);
    }
}
