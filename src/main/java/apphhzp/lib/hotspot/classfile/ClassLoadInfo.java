package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.oop.Oop;

public class ClassLoadInfo {
    private final Oop _protection_domain;
    private final ClassInstanceInfo _class_hidden_info=new ClassInstanceInfo();
    private final boolean _is_hidden;
    private final boolean _is_strong_hidden;
    private final boolean _can_access_vm_annotations;

    public ClassLoadInfo(Oop protection_domain) {
        _protection_domain = protection_domain;
        _class_hidden_info._dynamic_nest_host = null;
        _class_hidden_info._class_data =null;
        _is_hidden = false;
        _is_strong_hidden = false;
        _can_access_vm_annotations = false;
    }

    ClassLoadInfo(Oop protection_domain, InstanceKlass dynamic_nest_host,
                  Oop class_data, boolean is_hidden, boolean is_strong_hidden,
                  boolean can_access_vm_annotations) {
        _protection_domain = protection_domain;
        _class_hidden_info._dynamic_nest_host = dynamic_nest_host;
        _class_hidden_info._class_data = class_data;
        _is_hidden = is_hidden;
        _is_strong_hidden = is_strong_hidden;
        _can_access_vm_annotations = can_access_vm_annotations;
    }

    Oop protection_domain() {
        return _protection_domain;
    }

    public ClassInstanceInfo class_hidden_info_ptr() {
        return _class_hidden_info;
    }

    public boolean is_hidden() {
        return _is_hidden;
    }

    public boolean is_strong_hidden() {
        return _is_strong_hidden;
    }

    public boolean can_access_vm_annotations() {
        return _can_access_vm_annotations;
    }
}
