package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.Utf8Constant;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;

public class FieldStreamBase {
    protected final U2Array _fields;
    protected final ConstantPool _constants;
    protected int _index;
    protected int _limit;
    protected int _generic_signature_slot;

    public FieldInfo field() {
        return FieldInfo.from_field_array (_fields, _index);
    }

    public int initGenericSignatureStartSlot() {
        int length = _fields.length();
        int num_fields = _index;
        int skipped_generic_signature_slots = 0;
        FieldInfo fi;
        AccessFlags flags;
        /* Scan from 0 to the current _index. Count the number of generic
       signature slots for field[0] to field[_index - 1]. */
        for (int i = 0; i < _index; i++) {
            fi = FieldInfo.from_field_array (_fields, i);
            flags = fi.getAccessFlags();
            if (flags.fieldHasGenericSignature()) {
                length--;
                skipped_generic_signature_slots++;
            }
        }
        /* Scan from the current _index. */
        for (int i = _index; i * FieldInfo.field_slots < length; i++) {
            fi = FieldInfo.from_field_array (_fields, i);
            flags = fi.getAccessFlags();
            if (flags.fieldHasGenericSignature()) {
                length--;
            }
            num_fields++;
        }
        _generic_signature_slot = length + skipped_generic_signature_slots;
        if (!(_generic_signature_slot <= _fields.length())) {
            throw new IllegalStateException();
        }
        return num_fields;
    }

    protected FieldStreamBase(U2Array fields, ConstantPool constants, int start, int limit) {
        _fields = fields;
        _constants = constants;
        _index = start;
        int num_fields = initGenericSignatureStartSlot();
        if (limit < start) {
            _limit = num_fields;
        } else {
            _limit = limit;
        }
    }

    protected FieldStreamBase(U2Array fields, ConstantPool constants) {
        _fields = fields;
        _constants = constants;
        _index = 0;
        _limit = initGenericSignatureStartSlot();
    }

    public FieldStreamBase(InstanceKlass klass) {
        _fields = klass.getFields();
        _constants = klass.getConstantPool();
        _index = 0;
        _limit = klass.java_fields_count();
        initGenericSignatureStartSlot();
        if (!klass.equals(getFieldHolder())) {
            throw new IllegalStateException();
        }
    }

    // accessors
    public int index() {
        return _index;
    }

    public InstanceKlass getFieldHolder() {
        return _constants.pool_holder();
    }

    public void next() {
        if (getAccessFlags().fieldHasGenericSignature()) {
            _generic_signature_slot++;
            if (_generic_signature_slot > _fields.length()) {
                throw new IllegalStateException();
            }
        }
        _index += 1;
    }

    public boolean done() {
        return _index >= _limit;
    }

    // Accessors for current field
    public AccessFlags getAccessFlags() {
        return field().getAccessFlags();
    }

    public void setAccessFlags(int flags) {
        field().setAccessFlags(flags);
    }

    public void setAccessFlags(AccessFlags flags) {
        setAccessFlags(flags.flags);
    }

    public Symbol name() {
        return field().name(this._constants);
    }

    public Symbol signature() {
        return field().signature(this._constants);
    }

    public Symbol generic_signature() {
        if (getAccessFlags().fieldHasGenericSignature()) {
            if (_generic_signature_slot >= _fields.length()) {
                throw new IllegalStateException("out of bounds");
            }
            int index = _fields.get(_generic_signature_slot);
            return ((Utf8Constant) this._constants.getConstant(index)).str;//_constants.symbol_at(index);
        } else {
            return null;
        }
    }

    public int getOffset() {
        return field().offset();
    }

    public void setOffset(int offset) {
        field().setOffset(offset);
    }

    public boolean isOffsetSet() {
        return field().isOffsetSet();
    }

    public boolean isContended() {
        return field().isContended();
    }

    public int contendedGroup() {
        return field().contendedGroup();
    }
}
