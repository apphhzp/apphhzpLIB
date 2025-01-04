package apphhzp.lib.hotspot.classfile;

public class FieldLayoutInfo{
    public OopMapBlocksBuilder oop_map_blocks;
    public int _instance_size;
    public int _nonstatic_field_size;
    public int _static_field_size;
    public boolean  _has_nonstatic_fields;

    public FieldLayoutInfo(OopMapBlocksBuilder oop_map_blocks, int _instance_size, int _nonstatic_field_size, int _static_field_size, boolean _has_nonstatic_fields) {
        this.oop_map_blocks = oop_map_blocks;
        this._instance_size = _instance_size;
        this._nonstatic_field_size = _nonstatic_field_size;
        this._static_field_size = _static_field_size;
        this._has_nonstatic_fields = _has_nonstatic_fields;
    }
}
