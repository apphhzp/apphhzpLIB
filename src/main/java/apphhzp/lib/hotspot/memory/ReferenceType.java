package apphhzp.lib.hotspot.memory;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

public enum ReferenceType {
    NONE(0),
    OTHER(1),
    SOFT(2),
    WEAK(3),
    FINAL(4),
    PHANTOM(5);
    public static final Type TYPE = JVM.type("ReferenceType");
    public static final int SIZE = TYPE.size;
    public static final int REF_NONE = 0,      // Regular class
            REF_OTHER = 1,     // Subclass of java/lang/ref/Reference, but not subclass of one of the classes below
            REF_SOFT = 2,      // Subclass of java/lang/ref/SoftReference
            REF_WEAK = 3,      // Subclass of java/lang/ref/WeakReference
            REF_FINAL = 4,     // Subclass of java/lang/ref/FinalReference
            REF_PHANTOM = 5;    // Subclass of java/lang/ref/PhantomReference

    public final int value;

    ReferenceType(int value) {
        this.value = value;
    }

    public static String toString(int value) {
        return switch (value) {
            case REF_NONE -> "None reference";
            case REF_OTHER -> "Other reference";
            case REF_SOFT -> "Soft reference";
            case REF_WEAK -> "Weak reference";
            case REF_FINAL -> "Final reference";
            case REF_PHANTOM -> "Phantom reference";
            default -> throw new IllegalArgumentException("Unknown reference type: " + value);
        };
    }

    public static ReferenceType of(int value) {
        return switch (value) {
            case REF_NONE -> NONE;
            case REF_OTHER -> OTHER;
            case REF_SOFT -> SOFT;
            case REF_WEAK -> WEAK;
            case REF_FINAL -> FINAL;
            case REF_PHANTOM -> PHANTOM;
            default -> throw new IllegalArgumentException("Unknown reference type: " + value);
        };
    }
}
