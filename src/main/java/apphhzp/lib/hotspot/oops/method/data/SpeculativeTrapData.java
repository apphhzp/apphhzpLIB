package apphhzp.lib.hotspot.oops.method.data;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

/** A SpeculativeTrapData is used to record traps due to type
 * speculation. It records the root of the compilation: that type
 * speculation is wrong in the context of one compilation (for
 * method1) doesn't mean it's wrong in the context of another one (for
 * method2). Type speculation could have more/different data in the
 * context of the compilation of method2 and it's worthwhile to try an
 * optimization that failed for compilation of method1 in the context
 * of compilation of method2.
 * Space for SpeculativeTrapData entries is allocated from the extra
 * data space in the MDO. If we run out of space, the trap data for
 * the ProfileData at that bci is updated.*/
public class SpeculativeTrapData extends ProfileData {
    protected static final int
        speculative_trap_method=0,
//#ifndef _LP64
        // The size of the area for traps is a multiple of the header
        // size, 2 cells on 32 bits. Packed at the end of this area are
        // argument info entries (with tag
        // DataLayout::arg_info_data_tag). The logic in
        // MethodData::bci_to_extra_data() that guarantees traps don't
        // overflow over argument info entries assumes the size of a
        // SpeculativeTrapData is twice the header size. On 32 bits, a
        // SpeculativeTrapData must be 4 cells.
        padding=1,
//#endif
                speculative_trap_cell_count= JVM.isLP64?1:2;

    public SpeculativeTrapData(DataLayout layout){
        super(layout);
        if (!(layout.tag() == DataLayout.speculative_trap_data_tag)){
            throw new IllegalArgumentException("wrong type");
        }
    }

    public boolean is_SpeculativeTrapData(){
        return true;
    }

    public static int static_cell_count() {
        return speculative_trap_cell_count;
    }

    public int cell_count(){
        return static_cell_count();
    }

    // Direct accessor
    public Method method(){
        return (Method.getOrCreate(intptr_at(speculative_trap_method)));
    }

    public void set_method(Method m) {
        if (m.is_old()){
            throw new IllegalArgumentException("cannot add old methods");
        }
        set_intptr_at(speculative_trap_method, m.address);
    }

    public static @RawCType("ByteSize")int method_offset() {
        return cell_offset(speculative_trap_method);
    }

    public void print_data_on(PrintStream st, String extra){
        print_shared(st, "SpeculativeTrapData", extra);
        tab(st,0);
        method().print_short_name(st);
        st.println();
    }
}
