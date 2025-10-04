package apphhzp.lib.hotspot.code.scope;

public class AutoBoxObjectValue extends ObjectValue {
    private boolean _cached;

    public AutoBoxObjectValue(int id, ScopeValue klass) {
        super(id, klass);
        _cached = (false);
    }

    public AutoBoxObjectValue(int id) {
        super(id);
        _cached = (false);
    }

    public boolean is_auto_box() {
        return true;
    }

    public boolean is_cached() {
        return _cached;
    }

    public void set_cached(boolean cached) {
        _cached = cached;
    }
}
