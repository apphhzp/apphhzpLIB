package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.hotspot.util.RawCType;

/** TosState describes the top-of-stack state before and after the execution of
 * a bytecode or method. The top-of-stack value may be cached in one or more CPU
 * registers. The TosState corresponds to the 'machine representation' of this cached
 * value. There's 4 states corresponding to the JAVA types int, long, float & double
 * as well as a 5th state in case the top-of-stack value is actually on the top
 * of stack (in memory) and thus not cached. The atos state corresponds to the itos
 * state when it comes to machine representation but is used separately for (oop)
 * type specific operations (e.g. verification code).*/
public class TosState {
    // describes the tos cache contents
    public static final int btos = 0,             // byte, bool tos cached
    ztos = 1,             // byte, bool tos cached
    ctos = 2,             // char tos cached
    stos = 3,             // short tos cached
    itos = 4,             // int tos cached
    ltos = 5,             // long tos cached
    ftos = 6,             // float tos cached
    dtos = 7,             // double tos cached
    atos = 8,             // object cached
    vtos = 9,             // tos not cached
    number_of_states=10,
    ilgl=11                  // illegal state: should not occur
    ;
    public static @RawCType("TosState")int as_TosState(@RawCType("BasicType")int type) {
        if (type == BasicType.T_BYTE) {
            return btos;
        } else if (type == BasicType.T_BOOLEAN) {
            return ztos;
        } else if (type == BasicType.T_CHAR) {
            return ctos;
        } else if (type == BasicType.T_SHORT) {
            return stos;
        } else if (type == BasicType.T_INT) {
            return itos;
        } else if (type == BasicType.T_LONG) {
            return ltos;
        } else if (type == BasicType.T_FLOAT) {
            return ftos;
        } else if (type == BasicType.T_DOUBLE) {
            return dtos;
        } else if (type == BasicType.T_VOID) {
            return vtos;
        } else if (type == BasicType.T_ARRAY || type == BasicType.T_OBJECT) {
            return atos;
        }
        return ilgl;
    }

}
