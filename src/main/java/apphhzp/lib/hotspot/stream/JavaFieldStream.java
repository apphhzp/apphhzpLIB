package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;

public class JavaFieldStream extends FieldStreamBase {
    public JavaFieldStream(InstanceKlass k) {
        super(k.getFields(), k.getConstantPool(), 0, k.getFieldsCount());
    }

    public int nameIndex() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        return field().getNameIndex();
    }

    public void setNameIndex(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        field().setNameIndex(index);
    }

    public int signatureIndex() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        return field().getSignatureIndex();
    }

    public void setSignatureIndex(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        field().setSignatureIndex(index);
    }

    public int genericSignatureIndex() {
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

    public void setGenericSignatureIndex(int index) {
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

    public int initvalIndex() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        return field().getInitialValueIndex();
    }

    public void setInitvalIndex(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        field().setInitialValueIndex(index);
    }
}
