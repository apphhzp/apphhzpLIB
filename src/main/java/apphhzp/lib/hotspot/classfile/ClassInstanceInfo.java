package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oop.InstanceKlass;
import apphhzp.lib.hotspot.oop.OopDesc;

public class ClassInstanceInfo {
     InstanceKlass _dynamic_nest_host;
     OopDesc _class_data;

    public ClassInstanceInfo() {
        _dynamic_nest_host = null;
        _class_data = new OopDesc(0L);
    }
    ClassInstanceInfo(InstanceKlass dynamic_nest_host, OopDesc class_data) {
        _dynamic_nest_host = dynamic_nest_host;
        _class_data = class_data;
    }

    InstanceKlass dynamic_nest_host() { return _dynamic_nest_host; }
    OopDesc class_data() { return _class_data; }
}
