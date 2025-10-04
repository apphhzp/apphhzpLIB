package apphhzp.lib.hotspot.code.scope;

import apphhzp.lib.hotspot.code.Location;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;

import java.io.PrintStream;

/** A Location value describes a value in a given location; i.e. the corresponding
 * logical entity (e.g., a method temporary) lives in this location.*/
public class LocationValue extends ScopeValue {
    private Location _location;

    public LocationValue(Location location) {
        _location = location;
    }

    public LocationValue(DebugInfoReadStream stream) {
        _location =new Location(stream);
    }


    public boolean is_location() {
        return true;
    }

    public Location location() {
        return _location;
    }
    public void print_on(PrintStream st){
        location().print_on(st);
    }
}
