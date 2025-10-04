package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.interpreter.Bytecode_invoke;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

/** Entries to collect type information at a call: contains arguments
 * (TypeStackSlotEntries), a return type (ReturnTypeEntry) and a
 * number of cells. Because the number of cells for the return type is
 * smaller than the number of cells for the type of an arguments, the
 * number of cells is used to tell how many arguments are profiled and
 * whether a return value is profiled. See has_arguments() and
 * has_return().*/
public class TypeEntriesAtCall {
    private static int stack_slot_local_offset(int i) {
        return header_cell_count() + TypeStackSlotEntries.stack_slot_local_offset(i);
    }

    private static int argument_type_local_offset(int i) {
        return header_cell_count() + TypeStackSlotEntries.type_local_offset(i);
    }
    public static int header_cell_count() {
        return 1;
    }

    public static int cell_count_local_offset() {
        return 0;
    }
    public static void initialize(DataLayout dl, int base, int cell_count) {
        int off = base + cell_count_local_offset();
        dl.set_cell_at(off, cell_count - base - header_cell_count());
    }
    public static int compute_cell_count(BytecodeStream stream) {
        if (!Bytecodes.is_invoke(stream.code())){
            throw new IllegalArgumentException("should be invoke");
        }
        if (TypeStackSlotEntries.per_arg_count() > ReturnTypeEntry.static_cell_count()){
            throw new RuntimeException("code to test for arguments/results broken");
        }
        final Method m = stream.method();
        int bci = stream.bci();
        Bytecode_invoke inv=new Bytecode_invoke(m, bci);
        int args_cell = 0;
        if (MethodData.profile_arguments_for_invoke(m, bci)) {
            args_cell = TypeStackSlotEntries.compute_cell_count(inv.signature(), false, (int) JVM.getFlag("TypeProfileArgsLimit").getIntx());
        }
        int ret_cell = 0;
        if (MethodData.profile_return_for_invoke(m, bci) && BasicType.is_reference_type(inv.result_type())) {
            ret_cell = ReturnTypeEntry.static_cell_count();
        }
        int header_cell = 0;
        if (args_cell + ret_cell > 0) {
            header_cell = header_cell_count();
        }

        return header_cell + args_cell + ret_cell;
    }
    public static boolean return_profiling_enabled() {
        return MethodData.profile_return();
    }

    public static boolean arguments_profiling_enabled() {
        return MethodData.profile_arguments();
    }
    public static @RawCType("ByteSize")int args_data_offset() {
        return (header_cell_count() * DataLayout.cell_size);
    }
}
