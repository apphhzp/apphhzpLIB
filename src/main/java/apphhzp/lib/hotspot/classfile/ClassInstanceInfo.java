package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.oop.Oop;

public class ClassInstanceInfo {
     InstanceKlass _dynamic_nest_host;
    Oop _class_data;

    public ClassInstanceInfo() {
        _dynamic_nest_host = null;
        _class_data = null;
    }
    ClassInstanceInfo(InstanceKlass dynamic_nest_host, Oop class_data) {
        _dynamic_nest_host = dynamic_nest_host;
        _class_data = class_data;
    }

    InstanceKlass dynamic_nest_host() { return _dynamic_nest_host; }
    Oop class_data() { return _class_data; }
}
