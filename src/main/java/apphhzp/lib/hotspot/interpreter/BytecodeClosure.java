package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

public interface BytecodeClosure {
    void trace(Method method, @RawCType("address")long bcp, @RawCType("uintptr_t")long tos, @RawCType("uintptr_t")long tos2, PrintStream st);
    void trace(Method method, @RawCType("address")long bcp, PrintStream st);
}
