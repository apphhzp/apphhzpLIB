package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

public class Signature {
    private Signature(){
        throw new UnsupportedOperationException("AllStatic");
    }
    public static @RawCType("BasicType") int basic_type(Symbol signature) {
        return basic_type(signature.char_at(0));
    }

    // Returns T_ILLEGAL for an illegal signature char.
    public static @RawCType("BasicType") int basic_type(int ch){
        return BasicType.charToBasicType((char) ch);
    }

    // Assuming it is either a class name or signature,
    // determine if it in fact cannot be a class name.
    // This means it either starts with '[' or ends with ';'
    public static boolean not_class_name(Symbol signature) {
        return (signature.char_at(0)=='['||
                signature.char_at(signature.getLength()-1)== ';');
    }

    // Assuming it is either a class name or signature,
    // determine if it in fact is an array descriptor.
    public static boolean is_array(Symbol signature) {
        return (signature.getLength() > 1 &&
                signature.char_at(0) == '[' &&
                        is_valid_array_signature(signature));
    }


    public static boolean is_valid_array_signature(Symbol sig) {
        if (sig.getLength()<=1){
            throw new IllegalArgumentException("this should already have been checked");
        }
        if (sig.char_at(0)!='['){
            throw new IllegalArgumentException("this should already have been checked");
        }
        // The first character is already checked
        int i = 1;
        int len = sig.getLength();
        // First skip all '['s
        while(i < len - 1 && sig.char_at(i) == '[') i++;
        // Check type
        switch(sig.char_at(i)) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
                // If it is an array, the type is the last character
                return (i + 1 == len);
            case 'L':
                // If it is an object, the last character must be a ';'
                return sig.char_at(len - 1) == ';';
        }
        return false;
    }

    // Assuming it is either a class name or signature,
    // determine if it contains a class name plus ';'.
    public static boolean has_envelope(Symbol signature) {
        return ((signature.getLength() > 0) &&
                signature.char_at(signature.getLength()-1)==';' &&
                        has_envelope(signature.char_at(0)));
    }

    // Determine if this signature char introduces an
    // envelope, which is a class name plus ';'.
    public static boolean has_envelope(char signature_char) {
        return (signature_char == 'L');
    }

    // Assuming has_envelope is true, return the symbol
    // inside the envelope, by stripping 'L' and ';'.
    // Caller is responsible for decrementing the newly created
    // Symbol's refcount, use TempNewSymbol.
    public static Symbol strip_envelope(Symbol signature){
        if (!has_envelope(signature)){
            throw new IllegalArgumentException("precondition");
        }
        String s=signature.toString();
        return Symbol.newSymbol(s.substring(1,s.length()-2));
    }

    // Assuming it's either a field or method descriptor, determine
    // whether it is in fact a method descriptor:
    public static boolean is_method(Symbol signature) {
        return signature.char_at(0)=='(';
    }

    // Assuming it's a method signature, determine if it must
    // return void.
    public static boolean is_void_method(Symbol signature) {
        if (!is_method(signature)){
            throw new IllegalArgumentException("signature is not for a method");
        }
        return signature.char_at(signature.getLength()-1)=='V';
    }
}
