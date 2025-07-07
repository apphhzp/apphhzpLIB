package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.util.RawCType;

public class BasicType {
    public static final int T_BOOLEAN= JVM.intConstant("T_BOOLEAN");
    public static final int T_CHAR= JVM.intConstant("T_CHAR");
    public static final int T_FLOAT= JVM.intConstant("T_FLOAT");
    public static final int T_DOUBLE= JVM.intConstant("T_DOUBLE");
    public static final int T_BYTE= JVM.intConstant("T_BYTE");
    public static final int T_SHORT= JVM.intConstant("T_SHORT");
    public static final int T_INT= JVM.intConstant("T_INT");
    public static final int T_LONG= JVM.intConstant("T_LONG");
    public static final int T_OBJECT= JVM.intConstant("T_OBJECT");
    public static final int T_ARRAY= JVM.intConstant("T_ARRAY");
    public static final int T_VOID= JVM.intConstant("T_VOID");
    public static final int T_ADDRESS= JVM.intConstant("T_ADDRESS");
    public static final int T_NARROWOOP= JVM.intConstant("T_NARROWOOP");
    public static final int T_METADATA= JVM.intConstant("T_METADATA");
    public static final int T_NARROWKLASS= JVM.intConstant("T_NARROWKLASS");
    public static final int T_CONFLICT= JVM.intConstant("T_CONFLICT");
    public static final int T_ILLEGAL= JVM.intConstant("T_ILLEGAL");
    public static final int T_BOOLEAN_size=JVM.intConstant("T_BOOLEAN_size");
    public static final int T_CHAR_size=JVM.intConstant("T_CHAR_size");
    public static final int T_FLOAT_size=JVM.intConstant("T_FLOAT_size");
    public static final int T_DOUBLE_size=JVM.intConstant("T_DOUBLE_size");
    public static final int T_BYTE_size=JVM.intConstant("T_BYTE_size");
    public static final int T_SHORT_size=JVM.intConstant("T_SHORT_size");
    public static final int T_INT_size=JVM.intConstant("T_INT_size");
    public static final int T_LONG_size=JVM.intConstant("T_LONG_size");
    public static final int T_OBJECT_size=JVM.intConstant("T_OBJECT_size");
    public static final int T_ARRAY_size=JVM.intConstant("T_ARRAY_size");
    public static final int T_NARROWOOP_size=JVM.intConstant("T_NARROWOOP_size");
    public static final int T_NARROWKLASS_size=JVM.intConstant("T_NARROWKLASS_size");
    public static final int T_VOID_size=JVM.intConstant("T_VOID_size");

    public int value;
    public BasicType(int value) {
        this.value = value;
    }

    public static int charToBasicType(char c) {
        return switch (c) {
            case 'B' -> T_BYTE;
            case 'C' -> T_CHAR;
            case 'D' -> T_DOUBLE;
            case 'F' -> T_FLOAT;
            case 'I' -> T_INT;
            case 'J' -> T_LONG;
            case 'S' -> T_SHORT;
            case 'Z' -> T_BOOLEAN;
            case 'V' -> T_VOID;
            case 'L' -> T_OBJECT;
            case '[' -> T_ARRAY;
            default -> T_ILLEGAL;
        };
    }
    public String getName() {
        return getName(value);
    }
    public static String getName(int value) {
        if (value == T_BOOLEAN) {
            return "boolean";
        } else if (value == T_CHAR) {
            return "char";
        } else if (value == T_FLOAT) {
            return "float";
        } else if (value == T_DOUBLE) {
            return "double";
        } else if (value == T_BYTE) {
            return "byte";
        } else if (value == T_SHORT) {
            return "short";
        } else if (value == T_INT) {
            return "int";
        } else if (value == T_LONG) {
            return "long";
        } else if (value == T_OBJECT) {
            return "object";
        } else if (value == T_ARRAY) {
            return "array";
        } else if (value == T_VOID) {
            return "void";
        } else if (value == T_ADDRESS) {
            return "address";
        } else if (value == T_NARROWOOP) {
            return "narrow oop";
        } else if (value == T_METADATA) {
            return "metadata";
        } else if (value == T_NARROWKLASS) {
            return "narrow klass";
        } else if (value == T_CONFLICT) {
            return "conflict";
        } else {
            return "ILLEGAL TYPE";
        }
    }

    public static  boolean is_java_type(@RawCType("BasicType")int t) {
        return T_BOOLEAN <= t && t <= T_VOID;
    }

    public static  boolean is_java_primitive(@RawCType("BasicType")int t) {
        return T_BOOLEAN <= t && t <= T_LONG;
    }

    public static  boolean is_subword_type(@RawCType("BasicType")int t) {
        // these guys are processed exactly like T_INT in calling sequences:
        return (t == T_BOOLEAN || t == T_CHAR || t == T_BYTE || t == T_SHORT);
    }

    public static  boolean is_signed_subword_type(@RawCType("BasicType")int t) {
        return (t == T_BYTE || t == T_SHORT);
    }

    public static  boolean is_double_word_type(@RawCType("BasicType")int t) {
        return (t == T_DOUBLE || t == T_LONG);
    }

    public static  boolean is_reference_type(@RawCType("BasicType")int t) {
        return (t == T_OBJECT || t == T_ARRAY);
    }

    public static  boolean is_integral_type(@RawCType("BasicType")int t) {
        return is_subword_type(t) || t == T_INT || t == T_LONG;
    }

    public static  boolean is_floating_point_type(@RawCType("BasicType")int t) {
        return (t == T_FLOAT || t == T_DOUBLE);
    }
}
