package apphhzp.lib.hotspot.stream;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.code.scope.AutoBoxObjectValue;
import apphhzp.lib.hotspot.code.scope.ObjectValue;
import apphhzp.lib.hotspot.code.scope.ScopeValue;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.Oop;

import java.util.List;

/** DebugInfoReadStream specializes CompressedReadStream for reading
 * debugging information. Used by ScopeDesc.*/
public class DebugInfoReadStream extends CompressedReadStream{
    private final CompiledMethod _code;
    private CompiledMethod code(){
        return _code;
    }
    private List<ScopeValue> _obj_pool;
    public DebugInfoReadStream(CompiledMethod code, int offset){
        this(code, offset, null);
    }
    public DebugInfoReadStream(CompiledMethod code, int offset, List<ScopeValue> obj_pool){
        super(code.scopes_data_begin(),offset);
        _code = code;
        _obj_pool = obj_pool;
    }
    public Method read_method() {
        Method o = (Method)(code().metadata_at(this.read_int()));
        return o;
    }
    // BCI encoding is mostly unsigned, but -1 is a distinguished value
    public int read_bci() {
        return read_int() + JVM.invocationEntryBci;
    }
    public Oop read_oop() {
        NMethod nm = (code()).as_nmethod_or_null();
        Oop o;
        if (nm != null) {
            // Despite these oops being found inside nmethods that are on-stack,
            // they are not kept alive by all GCs (e.g. G1 and Shenandoah).
            o = nm.oop_at(read_int());
        } else {
            o = code().oop_at(read_int());
        }
        return o;
    }
    public ScopeValue read_object_value(boolean is_auto_box) {
        int id = read_int();
        ObjectValue result = is_auto_box ? new AutoBoxObjectValue(id) : new ObjectValue(id);
        // Cache the object since an object field could reference it.
        _obj_pool.add(result);
        result.read_object(this);
        return result;
    }
    public ScopeValue get_cached_object() {
        int id = read_int();
        if (_obj_pool==null){
            throw new RuntimeException("object pool does not exist");
        }
        for (int i = _obj_pool.size() - 1; i >= 0; i--) {
            ObjectValue ov = _obj_pool.get(i).as_ObjectValue();
            if (ov.id() == id) {
                return ov;
            }
        }
        throw new RuntimeException("ShouldNotReachHere");
    }
}
