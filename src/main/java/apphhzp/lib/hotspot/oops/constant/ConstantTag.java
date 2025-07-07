package apphhzp.lib.hotspot.oops.constant;

import java.util.NoSuchElementException;

public class ConstantTag {
    public static final byte Utf8 = 1;
    public static final byte Unicode =  2; // unused
    public static final byte Integer =  3;
    public static final byte Float =  4;
    public static final byte Long =  5;
    public static final byte Double =  6;
    public static final byte Class =  7;
    public static final byte String =  8;
    public static final byte Fieldref =  9;
    public static final byte Methodref =  10;
    public static final byte InterfaceMethodref = 11;
    public static final byte NameAndType =  12;
    public static final byte MethodHandle =  15;  // JSR 292
    public static final byte MethodType =  16;  // JSR 292
    public static final byte Dynamic =  17;  // JSR 292 early drafts only
    public static final byte InvokeDynamic =  18;  // JSR 292
    public static final byte Module=19;
    public static final byte Package=20;
    public static final byte ExternalMax=20;
    public static final byte Invalid =  0;   // For bad value initialization
    public static final byte InternalMin=100;
    public static final byte UnresolvedClass = 100; // Temporary tag until actual use
    public static final byte ClassIndex = 101; // Temporary tag while constructing constant pool
    public static final byte StringIndex = 102; // Temporary tag while constructing constant pool
    public static final byte UnresolvedClassInError = 103; // Resolution failed
    public static final byte MethodHandleInError = 104; // Error tag due to resolution error
    public static final byte MethodTypeInError = 105; // Error tag due to resolution error
    public static final byte DynamicInError = 106;
    public static final byte InternalMax = 106;
    public static void checkTag(byte tag) {
        if (tag < 0 || (tag > 12 && tag < 15) || (tag > ExternalMax && tag < InternalMin) || tag > InternalMax) {
            throw new IllegalArgumentException("Unknown tag:" + tag);
        }
    }

    public static boolean isInternalTag(byte tag){
        return InternalMin<=tag&&tag<=InternalMax;
    }

    public static String getTagName(int tag) {
        java.lang.String re = tag == Utf8 ? "Utf8" : tag == Unicode ? "Unicode(unused)" : tag == Integer ? "Integer" : tag == Float ? "Float" : tag == Long ? "Long" : tag == Double ? "Double" : tag == Class ? "Class" : tag == String ? "String" : tag == Fieldref ? "FieldRef" : tag == Methodref ? "MethodRef" : tag == InterfaceMethodref ? "InterfaceMethodRef" : tag == NameAndType ? "NameAndType" : tag == MethodHandle ? "MethodHandle" : tag == MethodType ? "MethodType" : tag == Dynamic ? "Dynamic" : tag == InvokeDynamic ? "InvokeDynamic" : tag == Module ? "Module" : tag == Package ? "Package" : tag == Invalid ? "InvalidTag" : tag == UnresolvedClass ? "UnresolvedClass" : tag == ClassIndex ? "ClassIndex" : tag == StringIndex ? "StringIndex" : tag == UnresolvedClassInError ? "UnresolvedClassInError" : tag == MethodHandleInError ? "MethodHandleInError" : tag == MethodTypeInError ? "MethodTypeInError" : tag == DynamicInError ? "DynamicInError" : null;
        if (re==null){
            throw new NoSuchElementException("Unknown tag: " + tag);
        }
        return re;
    }

    public static boolean has_bootstrap(byte tag){
        return (tag == Dynamic ||
                tag == DynamicInError ||
                tag == InvokeDynamic);
    }

    public static final int REF_getField = 1;
    public static final int REF_getStatic = 2;
    public static final int REF_putField = 3;
    public static final int REF_putStatic = 4;
    public static final int REF_invokeVirtual = 5;
    public static final int REF_invokeStatic = 6;
    public static final int REF_invokeSpecial = 7;
    public static final int REF_newInvokeSpecial = 8;
    public static final int REF_invokeInterface = 9;

    public static String getREFTypeName(int type) {
        java.lang.String re = type == REF_getField ? "getField" : type == REF_getStatic ? "getStatic" : type == REF_putField ? "putField" : type == REF_putStatic ? "putStatic" : type == REF_invokeVirtual ? "invokeVirtual" : type == REF_invokeStatic ? "invokeStatic" : type == REF_invokeSpecial ? "invokeSpecial" : type == REF_newInvokeSpecial ? "newInvokeSpecial" : type == REF_invokeInterface ? "invokeInterface" : null;
        if (re==null){
            throw new IllegalStateException("Unknown REF_type: " + type);
        }
        return re;
    }
}
