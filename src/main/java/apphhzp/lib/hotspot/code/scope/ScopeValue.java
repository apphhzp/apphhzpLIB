package apphhzp.lib.hotspot.code.scope;

import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;

public abstract class ScopeValue {
    protected ScopeValue(){}
    // Testers
    public boolean is_location(){ return false; }
    public boolean is_object(){ return false; }
    public boolean is_auto_box(){ return false; }
    public boolean is_marker(){ return false; }
    public boolean is_constant_int(){ return false; }
    public boolean is_constant_double(){ return false; }
    public boolean is_constant_long(){ return false; }
    public boolean is_constant_oop(){ return false; }
    public boolean equals(ScopeValue other){ return false; }

    public ConstantOopReadValue as_ConstantOopReadValue() {
        if (!is_constant_oop()){
            throw new IllegalStateException("must be");
        }
        return (ConstantOopReadValue) this;
    }

    public ObjectValue as_ObjectValue() {
        if (!is_object()){
            throw new IllegalStateException("must be");
        }
        return (ObjectValue)this;
    }

    public LocationValue as_LocationValue(){
        if (!is_location()){
            throw new IllegalStateException("must be");
        }
        return (LocationValue)this;
    }
    public static final int LOCATION_CODE = 0, CONSTANT_INT_CODE = 1,  CONSTANT_OOP_CODE = 2,
            CONSTANT_LONG_CODE = 3, CONSTANT_DOUBLE_CODE = 4,
            OBJECT_CODE = 5,        OBJECT_ID_CODE = 6,
            AUTO_BOX_OBJECT_CODE = 7, MARKER_CODE = 8;
    public static ScopeValue read_from(DebugInfoReadStream stream){
        ScopeValue result = switch (stream.read_int()) {
            case LOCATION_CODE -> new LocationValue(stream);
            case CONSTANT_INT_CODE -> new ConstantIntValue(stream);
            case CONSTANT_OOP_CODE -> new ConstantOopReadValue(stream);
            case CONSTANT_LONG_CODE -> new ConstantLongValue(stream);
            case CONSTANT_DOUBLE_CODE -> new ConstantDoubleValue(stream);
            case OBJECT_CODE -> stream.read_object_value(false /*is_auto_box*/);
            case AUTO_BOX_OBJECT_CODE -> stream.read_object_value(true /*is_auto_box*/);
            case OBJECT_ID_CODE -> stream.get_cached_object();
            case MARKER_CODE -> new MarkerValue();
            default -> throw new RuntimeException("ShouldNotReachHere");
        };
        return result;
    }
    public void print_on(PrintStream ps){

    }
}
