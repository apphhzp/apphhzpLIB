package apphhzp.lib.hotspot.oop;

import apphhzp.lib.hotspot.JVMObject;

public class RecordComponent extends JVMObject {
    private final U1Array/*AnnotationArray*  */ _annotations;
    private final U1Array/*AnnotationArray*  */ _type_annotations;
    private int _name_index;
    private int _descriptor_index;
    private final int _attributes_count;

    // generic_signature_index gets set if the Record component has a Signature
    // attribute.  A zero value indicates that there was no Signature attribute.
    private int _generic_signature_index;

    public RecordComponent(int _name_index, int _descriptor_index, int _attributes_count, int _generic_signature_index, U1Array _annotations, U1Array _type_annotations) {
        //fake pointer
        super(0);
        this._name_index = _name_index;
        this._descriptor_index = _descriptor_index;
        this._attributes_count = _attributes_count;
        this._generic_signature_index = _generic_signature_index;
        this._annotations = _annotations;
        this._type_annotations = _type_annotations;
    }


    public int name_index() {
        return _name_index;
    }

    public void set_name_index(int name_index) {
        _name_index = name_index;
    }

    public int descriptor_index() {
        return _descriptor_index;
    }

    public void set_descriptor_index(int descriptor_index) {
        _descriptor_index = descriptor_index;
    }

    public int attributes_count() {
        return _attributes_count;
    }

    public int generic_signature_index() {
        return _generic_signature_index;
    }

    public void set_generic_signature_index(int generic_signature_index) {
        _generic_signature_index = generic_signature_index;
    }

    public U1Array annotations() {
        return _annotations;
    }

    public U1Array type_annotations() {
        return _type_annotations;
    }

    @Override
    public String toString() {
        return "RecordComponent@We needn't to know";
    }
}
