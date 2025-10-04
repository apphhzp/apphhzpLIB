package apphhzp.lib.hotspot.gc;

import apphhzp.lib.hotspot.oops.oop.Oop;

public interface OopClosure {
    void do_oop(Oop oop);
}
