package apphhzp.lib.hotspot.gc;

import apphhzp.lib.hotspot.oops.oop.OopDesc;

public interface ObjectClosure {
    void doOop(OopDesc oop);
}
