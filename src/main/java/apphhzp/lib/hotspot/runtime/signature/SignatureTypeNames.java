package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

public abstract class SignatureTypeNames extends SignatureIterator {
    protected abstract void type_name(String name);

    protected void do_type(@RawCType("BasicType") int type) {
        if (type == BasicType.T_BOOLEAN) {
            type_name("jboolean");
        } else if (type == BasicType.T_CHAR) {
            type_name("jchar");
        } else if (type == BasicType.T_FLOAT) {
            type_name("jfloat");
        } else if (type == BasicType.T_DOUBLE) {
            type_name("jdouble");
        } else if (type == BasicType.T_BYTE) {
            type_name("jbyte");
        } else if (type == BasicType.T_SHORT) {
            type_name("jshort");
        } else if (type == BasicType.T_INT) {
            type_name("jint");
        } else if (type == BasicType.T_LONG) {
            type_name("jlong");
        } else if (type == BasicType.T_VOID) {
            type_name("void");
        } else if (type == BasicType.T_ARRAY || type == BasicType.T_OBJECT) {
            type_name("jobject");
        } else {
            throw new RuntimeException("ShouldNotReachHere");
        }
    }

    public SignatureTypeNames(Symbol signature) {
        super(signature);
    }
}
