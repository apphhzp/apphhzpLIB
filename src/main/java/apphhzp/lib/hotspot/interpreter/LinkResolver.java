package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;

public class LinkResolver {
    public static int vtable_index_of_interface_method(Klass klass, Method resolved_method) {
        InstanceKlass ik = klass.asInstanceKlass();
        return ik.vtable_index_of_interface_method(resolved_method);
    }
}
