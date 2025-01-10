package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;

public class JavaFieldStream extends FieldStreamBase {
    public JavaFieldStream(InstanceKlass k) {
        super(k.getFields(), k.getConstantPool(), 0, k.getFieldsCount());
    }

    public int name_index() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        return field().getNameIndex();
    }

    public void set_name_index(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        field().setNameIndex(index);
    }

    public int signature_index() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        return field().getSignatureIndex();
    }

    public void set_signature_index(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        field().setSignatureIndex(index);
    }

    public int generic_signature_index() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        if (getAccessFlags().fieldHasGenericSignature()) {
            if (_generic_signature_slot>=_fields.length()){
                throw new IllegalStateException("out of bounds");
            }
            return _fields.get(_generic_signature_slot);
        } else {
            return 0;
        }
    }

    public void set_generic_signature_index(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        if (getAccessFlags().fieldHasGenericSignature()) {
            if (_generic_signature_slot>=_fields.length()){
                throw new IllegalStateException("out of bounds");
            }
            _fields.set(_generic_signature_slot, (short) index);
        }
    }

    public int initval_index() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        return field().getInitialValueIndex();
    }

    public void set_initval_index(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        field().setInitialValueIndex(index);
    }
}
