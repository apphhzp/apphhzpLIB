package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

public class BytecodeTracer {
    private static BytecodeClosure _closure;
    public static BytecodeClosure std_closure(){ // a printing closure
        return new BytecodePrinter();
    }
    public static BytecodeClosure closure()                                                   { return _closure; }
    public static void             set_closure(BytecodeClosure closure) { _closure = closure; }
    public static void trace(Method method, @RawCType("address")long bcp, @RawCType("uintptr_t")long tos, @RawCType("uintptr_t")long tos2, PrintStream st) {
        //if (JVM.getFlag("TraceBytecodes").getBool()  ) {//&& BytecodeCounter::counter_value() >= TraceBytecodesAt
            //ttyLocker ttyl;  // 5065316: keep the following output coherent
            // The ttyLocker also prevents races between two threads
            // trying to use the single instance of BytecodePrinter.
            // Using the ttyLocker prevents the system from coming to
            // a safepoint within this code, which is sensitive to Method*
            // movement.
            //
            // There used to be a leaf mutex here, but the ttyLocker will
            // work just as well, as long as the printing operations never block.
            //
            // We put the locker on the static trace method, not the
            // virtual one, because the clients of this module go through
            // the static method.
            _closure.trace(method, bcp, tos, tos2, st);
        //}
    }

    public static void trace(Method method, @RawCType("address")long bcp, PrintStream st) {
        //ttyLocker ttyl;  // 5065316: keep the following output coherent
        _closure.trace(method, bcp, st);
    }

}
