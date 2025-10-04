package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;

public class JavaFieldStream extends FieldStreamBase {
    public JavaFieldStream(InstanceKlass k) {
        super(k.getFields(), k.getConstantPool(), 0, k.java_fields_count());
    }

    public int nameIndex() {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        return field().name_index();
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
        return field().signature_index();
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
        return field().initval_index();
    }

    public void setInitvalIndex(int index) {
        if (field().isInternal()) {
            throw new IllegalStateException("regular only");
        }
        field().setInitialValueIndex(index);
    }
}
