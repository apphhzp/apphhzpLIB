package apphhzp.lib.hotspot.stream;

import apphhzp.lib.hotspot.oop.*;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.constant.Utf8Constant;

public class FieldStreamBase {
    protected final U2Array _fields;
    protected final ConstantPool _constants;
    protected int _index;
    protected int _limit;
    protected int _generic_signature_slot;

    public FieldInfo field() {
        return FieldInfo.from_field_array (_fields, _index);
    }

    public int init_generic_signature_start_slot() {
        int length = _fields.length();
        int num_fields = _index;
        int skipped_generic_signature_slots = 0;
        FieldInfo fi = null;
        AccessFlags flags = null;
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

    public FieldStreamBase(U2Array fields, ConstantPool constants, int start, int limit) {
        _fields = fields;
        _constants = constants;
        _index = start;
        int num_fields = init_generic_signature_start_slot();
        if (limit < start) {
            _limit = num_fields;
        } else {
            _limit = limit;
        }
    }

    public FieldStreamBase(U2Array fields, ConstantPool constants) {
        _fields = fields;
        _constants = constants;
        _index = 0;
        _limit = init_generic_signature_start_slot();
    }

    public FieldStreamBase(InstanceKlass klass) {
        _fields = klass.getFields();
        _constants = klass.getConstantPool();
        _index = 0;
        _limit = klass.getFieldsCount();
        init_generic_signature_start_slot();
        if (!klass.equals(field_holder())) {
            throw new IllegalStateException();
        }
    }

    // accessors
    public int index() {
        return _index;
    }

    public InstanceKlass field_holder() {
        return _constants.getHolder();
    }

    public void next() {
        if (access_flags().fieldHasGenericSignature()) {
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
    public AccessFlags access_flags() {
        return field().getAccessFlags();
    }

    public void set_access_flags(int flags) {
        field().setAccessFlags(flags);
    }

    public void set_access_flags(AccessFlags flags) {
        set_access_flags(flags.flags);
    }

    public Symbol name() {
        return field().getName(this._constants);
    }

    public Symbol signature() {
        return field().getSignature(this._constants);
    }

    public Symbol generic_signature() {
        if (access_flags().fieldHasGenericSignature()) {
            if (_generic_signature_slot >= _fields.length()) {
                throw new IllegalStateException("out of bounds");
            }
            int index = _fields.get(_generic_signature_slot);
            return ((Utf8Constant) this._constants.getConstant(index)).str;//_constants.symbol_at(index);
        } else {
            return null;
        }
    }

    public int offset() {
        return field().getOffset();
    }

    public void set_offset(int offset) {
        field().setOffset(offset);
    }

    public boolean is_offset_set() {
        return field().isOffsetSet();
    }

    public boolean is_contended() {
        return field().is_contended();
    }

    public int contended_group() {
        return field().contended_group();
    }
}
