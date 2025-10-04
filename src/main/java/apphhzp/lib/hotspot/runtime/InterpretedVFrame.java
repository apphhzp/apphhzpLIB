package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.interpreter.InterpreterOopMap;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

import java.util.List;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class InterpretedVFrame extends JavaVFrame{

    // JVM state
    public Method method(){
        return fr().interpreter_frame_method();
    }
    public int bci(){
        return method().bci_from(bcp());
    }
    public StackValueCollection        locals(){
        return stack_data(false);
    }
    public StackValueCollection        expressions(){
        return stack_data(true);
    }
    public List<MonitorInfo> monitors(){
        //TODO
        throw new UnsupportedOperationException("TODO");
//        List<MonitorInfo> result = new ArrayList<>(5);
//        for (BasicObjectLock* current = (fr().previous_monitor_in_interpreter_frame(fr().interpreter_frame_monitor_begin()));
//             current >= fr().interpreter_frame_monitor_end();
//             current = fr().previous_monitor_in_interpreter_frame(current)) {
//            result->push(new MonitorInfo(current->obj(), current->lock(), false, false));
//        }
//        return result;
    }
    // Test operation
    public boolean is_interpreted_frame()  { return true; }

    protected InterpretedVFrame(Frame fr, RegisterMap reg_map, JavaThread thread){
        super(fr, reg_map, thread);
    }
    // Accessors for Byte Code Pointer
    public @RawCType("u_char*")long bcp(){
        return fr().interpreter_frame_bcp();
    }
    public void set_bcp(@RawCType("u_char*")long bcp){
        fr().interpreter_frame_set_bcp(bcp);
    }
    public @RawCType("intptr_t*")long locals_addr_at(int offset){
        if (!fr().is_interpreted_frame()){
            throw new RuntimeException("frame should be an interpreted frame");
        }
        return fr().interpreter_frame_local_at(offset);
    }

    public void set_locals(StackValueCollection values) {
        if (values == null || values.size() == 0) {
            return;
        }

        // If the method is native, max_locals is not telling the truth.
        // maxlocals then equals the size of parameters
        final int max_locals = method().is_native() ?
                method().size_of_parameters() : method().max_locals();

        if (!(max_locals == values.size())){
            throw new IllegalArgumentException("Mismatch between actual stack format and supplied data");
        }

        // handle locals
        for (int i = 0; i < max_locals; i++) {
            // Find stack location
            @RawCType("intptr_t*")long addr = locals_addr_at(i);

            // Depending on oop/int put it in the right package
            StackValue sv = values.at(i);
            if (sv == null){
                throw new NullPointerException("sanity check");
            }
            if (sv.type() == BasicType.T_OBJECT) {
                if (sv.get_obj().isFake){
                    throw new RuntimeException();
                }
                unsafe.putAddress(addr,sv.get_obj().address);
            } else {                   // integer
                 unsafe.putAddress(addr,sv.get_int());
            }
        }
    }

    /*
     * Worker routine for fetching references and/or values
     * for a particular bci in the interpretedVFrame.
     *
     * Returns data for either "locals" or "expressions",
     * using bci relative oop_map (oop_mask) information.
     *
     * @param expressions  bool switch controlling what data to return
                           (false == locals / true == expression)
     *
     */
    private StackValueCollection stack_data(boolean expressions){

        InterpreterOopMap oop_mask=new InterpreterOopMap();
        method().mask_for(bci(), oop_mask);
        final int mask_len = oop_mask.number_of_entries();

        // If the method is native, method()->max_locals() is not telling the truth.
        // For our purposes, max locals instead equals the size of parameters.
        final int max_locals = method().is_native() ?
                method().size_of_parameters() : method().max_locals();

        if (!(mask_len >= max_locals)){
            throw new RuntimeException("invariant");
        }

        final int length = expressions ? mask_len - max_locals : max_locals;
        if (!(length >= 0)){
            throw new RuntimeException("invariant");
        }

        StackValueCollection result = new StackValueCollection(length);

        if (0 == length) {
            return result;
        }

        if (expressions) {
            stack_expressions(result, length, max_locals, oop_mask, fr());
        } else {
            stack_locals(result, length, oop_mask, fr());
        }

        if (!(length == result.size())){
            throw new RuntimeException("invariant");
        }
        return result;
    }

    private static StackValue create_stack_value_from_oop_map(InterpreterOopMap oop_mask,
                                                              int index, @RawCType("const intptr_t* const")long addr) {

        if (!(index >= 0 &&
                index < oop_mask.number_of_entries())){
            throw new RuntimeException("invariant");
        }

        // categorize using oop_mask
        if (oop_mask.is_oop(index)) {
            // reference (oop) "r"
            Oop h=addr != 0L ? (new Oop((addr))) : null;
            return new StackValue(h);
        }
        // value (integer) "v"
        return new StackValue(addr != 0L ? unsafe.getAddress(addr) : 0);
    }

    private static boolean is_in_expression_stack(Frame fr, @RawCType("const intptr_t* const")long addr) {
        if (addr==0L){
            throw new RuntimeException("invariant");
        }

        // Ensure to be 'inside' the expresion stack (i.e., addr >= sp for Intel).
        // In case of exceptions, the expression stack is invalid and the sp
        // will be reset to express this condition.
        if (Frame.interpreter_frame_expression_stack_direction() > 0) {
            return addr <= fr.interpreter_frame_tos_address();
        }

        return addr >= fr.interpreter_frame_tos_address();
    }

    static void stack_locals(StackValueCollection result,
                             int length,
                          InterpreterOopMap oop_mask,
                          Frame fr) {
        if (result==null){
            throw new NullPointerException("invariant");
        }

        for (int i = 0; i < length; ++i) {
            @RawCType("intptr_t*")long addr = fr.interpreter_frame_local_at(i);
            if (addr==0L){
                throw new RuntimeException("invariant");
            }
            if (!(addr >= fr.sp())){
                throw new RuntimeException("must be inside the frame");
            }

            StackValue sv = create_stack_value_from_oop_map(oop_mask, i, addr);

            result.add(sv);
        }
    }

    static void stack_expressions(StackValueCollection result,
                                  int length,
                                  int max_locals, InterpreterOopMap oop_mask, Frame fr) {
        if (result==null){
            throw new NullPointerException("invariant");
        }

        for (int i = 0; i < length; ++i) {
            @RawCType("intptr_t*")long addr = fr.interpreter_frame_expression_stack_at(i);
            if (addr==0L){
                throw new RuntimeException("invariant");
            }
            if (!is_in_expression_stack(fr, addr)) {
                // Need to ensure no bogus escapes.
                addr = 0L;
            }

            StackValue sv = create_stack_value_from_oop_map(oop_mask,
                    i + max_locals,
                    addr);
            result.add(sv);
        }
    }
}
