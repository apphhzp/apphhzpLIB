package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.CodeCache;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.VMRegImpl;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.interpreter.AbstractInterpreter;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.x86.X86Frame;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.runtime.Frame.DeoptState.is_deoptimized;
import static apphhzp.lib.hotspot.runtime.Frame.DeoptState.unknown;

public abstract class Frame {
    public static final Type TYPE = JVM.type("frame");
    public static final int SIZE = TYPE.size;
    public static final long SP_OFFSET=0;
    public static final long PC_OFFSET=JVM.oopSize;
    public static final long CB_OFFSET=PC_OFFSET+JVM.oopSize;
    public static final long DEOPT_STATE_OFFSET=JVM.computeOffset(JVM.intSize,CB_OFFSET+JVM.oopSize);
    public static final class DeoptState {
        public static final int
                not_deoptimized = 0,
                is_deoptimized = 1,
                unknown = 2;
    }

    public static final int pc_return_offset = JVM.intConstant("frame::pc_return_offset");

    // Instance variables:
    protected @RawCType("intptr_t*") long _sp; // stack pointer (from Thread::last_Java_sp)
    protected @RawCType("address") long _pc; // program counter (the next instruction after the call)

    protected CodeBlob _cb; // CodeBlob that "owns" pc

    protected @RawCType("deopt_state") int _deopt_state;

    public static Frame create() {
        if (PlatformInfo.isX86()) {
            return new X86Frame();
        } else {
            throw new RuntimeException("Unsupported platform");
        }
    }

    public static Frame of(long addr){
        if (PlatformInfo.isX86()){
            return X86Frame.of(addr);
        }else {
            throw new RuntimeException("Unsupported platform");
        }
    }

    public static int interpreter_frame_expression_stack_direction(){
        if (PlatformInfo.isX86()){
            return X86Frame.interpreter_frame_expression_stack_direction();
        }
        throw new UnsupportedOperationException();
    }

    protected Frame() {
    }

    public abstract @RawCType("intptr_t*") long fp();
    // Accessors

    // pc: Returns the pc at which this frame will continue normally.
    // It must point at the beginning of the next instruction to execute.
    public @RawCType("address") long pc() {
        return _pc;
    }


    // This returns the pc that if you were in the debugger you'd see. Not
    // the idealized value in the frame object. This undoes the magic conversion
    // that happens for deoptimized frames. In addition it makes the value the
    // hardware would want to see in the native frame. The only user (at this point)
    // is deoptimization. It likely no one else should ever use it.
    public @RawCType("address") long raw_pc() {
        if (is_deoptimized_frame()) {
            CompiledMethod cm = cb().as_compiled_method_or_null();
            if (cm.is_method_handle_return(pc()))
                return cm.deopt_mh_handler_begin() - pc_return_offset;
            else
                return cm.deopt_handler_begin() - pc_return_offset;
        } else {
            return (pc() - pc_return_offset);
        }
    }

    // Change the pc in a frame object. This does not change the actual pc in
    // actual frame. To do that use patch_pc.
    public void set_pc(@RawCType("address") long newpc) {
        if (JVM.ENABLE_EXTRA_CHECK) {
            if (_cb != null && _cb.is_nmethod()) {
                if (((NMethod) _cb).is_deopt_pc(_pc)) {
                    throw new RuntimeException("invariant violation");
                }
            }
        }

        // Unsafe to use the is_deoptimized tester after changing pc
        _deopt_state = unknown;
        _pc = newpc;
        _cb = CodeCache.findBlobUnsafe(_pc);
    }

    public @RawCType("intptr_t*") long sp() {
        return _sp;
    }

    public void set_sp(@RawCType("intptr_t*") long newsp) {
        _sp = newsp;
    }


    public CodeBlob cb() {
        return _cb;
    }

    public abstract boolean is_interpreted_frame();
    public boolean is_entry_frame(){
        return StubRoutines.returns_to_call_stub(pc());
    }

    // type testers
    public boolean is_ignored_frame() {
        return false;  // FIXME: some LambdaForm frames should be ignored
    }

//    public boolean is_stub_frame(){
//        return StubRoutines.is_stub_code(pc()) || (_cb != null && _cb.is_adapter_blob());
//    }
    public boolean is_optimized_entry_frame() {
        return _cb != null && _cb.is_optimized_entry_blob();
    }

    public boolean is_deoptimized_frame() {
        if (_deopt_state == unknown) {
            throw new RuntimeException("not answerable");
        }
        return _deopt_state == is_deoptimized;
    }

    public boolean is_native_frame() {
        return (_cb != null &&
                _cb.is_nmethod() &&
                ((NMethod) _cb).is_native_method());
    }

    public boolean is_java_frame() {
        if (is_interpreted_frame()) {
            return true;
        }
        if (is_compiled_frame()) {
            return true;
        }
        return false;
    }


    public boolean is_compiled_frame() {
        if (_cb != null &&
                _cb.is_compiled() &&
                ((CompiledMethod) _cb).is_java_method()) {
            return true;
        }
        return false;
    }


    public boolean is_runtime_frame() {
        return (_cb != null && _cb.is_runtime_stub());
    }

    public boolean is_safepoint_blob_frame() {
        return (_cb != null && _cb.is_safepoint_stub());
    }

    public @RawCType("intptr_t*") long addr_at(int index) {
        return fp() + (long) index * JVM.oopSize;
    }

    public @RawCType("intptr_t") long at(int index) {
        return unsafe.getAddress(addr_at(index));
    }

    // accessors for locals
    public Oop obj_at(int offset) {
        return new Oop(obj_at_addr(offset));
    }

    public void obj_at_put(int offset, OopDesc value) {
        unsafe.putAddress(obj_at_addr(offset), value.address);
    }

    public int int_at(int offset) {
        return unsafe.getInt(int_at_addr(offset));
    }

    public void int_at_put(int offset, int value) {
        unsafe.putInt(int_at_addr(offset),value);
    }

    public @RawCType("oop*") long obj_at_addr(int offset) {
        return addr_at(offset);
    }

    private @RawCType("jint*")long    int_at_addr(int offset) {
        return addr_at(offset);
    }

    // The frame's original SP, before any extension by an interpreted callee;
    // used for packing debug info into vframeArray objects and vframeArray lookup.
    public abstract @RawCType("intptr_t*")long unextended_sp();

    // Link (i.e., the pointer to the previous frame)
    // might crash if the frame has no parent
    public abstract @RawCType("intptr_t*")long link();

    // Helper methods for better factored code in frame::sender
    public abstract Frame sender_for_compiled_frame(RegisterMap map);
    public abstract Frame sender_for_entry_frame(RegisterMap map);
    public abstract Frame sender_for_interpreter_frame(RegisterMap map);
    public abstract Frame sender_for_native_frame(RegisterMap map);
    public abstract Frame sender_for_optimized_entry_frame(RegisterMap map);

    public @RawCType("address")long oopmapreg_to_location(VMReg reg,RegisterMap reg_map){
        if(reg.is_reg()) {
            // If it is passed in a register, it got spilled in the stub frame.
            return reg_map.location(reg);
        } else {
            int sp_offset_in_bytes = reg.reg2stack() * VMRegImpl.stack_slot_size;
            return (unextended_sp()) + sp_offset_in_bytes;
        }
    }

    public @RawCType("oop*")long oopmapreg_to_oop_location(VMReg reg, RegisterMap reg_map){
        return oopmapreg_to_location(reg, reg_map);
    }

    public abstract @RawCType("intptr_t*")long interpreter_frame_sender_sp();
    // returns the sending frame
    public abstract  Frame sender(RegisterMap map);

    // testers
    public boolean is_first_frame() { // oldest frame? (has no sender)
        return (is_entry_frame() && entry_frame_is_first())
                // Optimized entry frames are only present on certain platforms
                || (is_optimized_entry_frame() && optimized_entry_frame_is_first());
    }
    public boolean is_first_java_frame(){// same for Java frame
        RegisterMap map=RegisterMap.create(JavaThread.current(), false); // No update
        Frame s=Frame.create();
        for (s = sender(map); !(s.is_java_frame() || s.is_first_frame()); s = s.sender(map));
        return s.is_first_frame();
    }

    // Entry frames
    public JavaCallWrapper entry_frame_call_wrapper(){
        return new JavaCallWrapper(unsafe.getAddress(entry_frame_call_wrapper_addr()));
    }
//    public JavaCallWrapper entry_frame_call_wrapper_if_safe(JavaThread thread){
//        @RawCType("JavaCallWrapper**")long jcw = entry_frame_call_wrapper_addr();
//        @RawCType("address")long addr = jcw;
//        // addr must be within the usable part of the stack
//        if (thread.is_in_usable_stack(addr)) {
//            return *jcw;
//        }
//        return null;
//    }
    public abstract @RawCType("JavaCallWrapper**") long entry_frame_call_wrapper_addr();

    // tells whether there is another chunk of Delta stack above
    public boolean entry_frame_is_first(){
        return entry_frame_call_wrapper().is_first_frame();
    }
    public boolean optimized_entry_frame_is_first(){
        throw new UnsupportedOperationException("TODO");
    }

    public Method interpreter_frame_method() {
        if (!is_interpreted_frame()){
            throw new RuntimeException("interpreted frame expected");
        }
        Method m = Method.getOrCreate(unsafe.getAddress(interpreter_frame_method_addr()));
        return m;
    }
    public abstract @RawCType("Method**")long interpreter_frame_method_addr();
    public Frame real_sender(RegisterMap map){
        Frame result = sender(map);
        while (result.is_runtime_frame() ||
                result.is_ignored_frame()) {
            result = result.sender(map);
        }
        return result;
    }

    @Override
    public abstract Frame clone();


    public @RawCType("address")long interpreter_frame_bcp(){
        if (!is_interpreted_frame()){
            throw new RuntimeException("interpreted frame expected");
        }
        @RawCType("address")long bcp = unsafe.getAddress(interpreter_frame_bcp_addr());
        return interpreter_frame_method().bcp_from(bcp);
    }

    public abstract @RawCType("intptr_t**")long interpreter_frame_locals_addr();
    public abstract @RawCType("intptr_t*")long  interpreter_frame_bcp_addr();
    public abstract @RawCType("intptr_t*")long  interpreter_frame_mdp_addr();

    public void interpreter_frame_set_bcp(@RawCType("address")long bcp){
        if (!is_interpreted_frame()){
            throw new RuntimeException("interpreted frame expected");
        }
        unsafe.putAddress(interpreter_frame_bcp_addr(),bcp);
    }


    public @RawCType("intptr_t*")long interpreter_frame_local_at(int index){
        final int n = AbstractInterpreter.local_offset_in_bytes(index)/JVM.wordSize;
        return unsafe.getAddress(interpreter_frame_locals_addr()) + (long) n *JVM.oopSize;
    }

    // Every frame needs to return a unique id which distinguishes it from all other frames.
    // For sparc and ia32 use sp. ia64 can have memory frames that are empty so multiple frames
    // will have identical sp values. For ia64 the bsp (fp) value will serve. No real frame
    // should have an id() of NULL so it is a distinguishing value for an unmatchable frame.
    // We also have relationals which allow comparing a frame to anoth frame's id() allow
    // us to distinguish younger (more recent activation) from older (less recent activations)
    // A NULL id is only valid when comparing for equality.

    public abstract @RawCType("intptr_t*")long id();
    public abstract boolean is_younger(@RawCType("intptr_t*")long id);
    public abstract boolean is_older(@RawCType("intptr_t*")long id);

    public @RawCType("intptr_t*")long interpreter_frame_expression_stack_at(int offset){
      final int i = offset * interpreter_frame_expression_stack_direction();
      final int n = i * AbstractInterpreter.stackElementWords;
        return (interpreter_frame_expression_stack()+ (long) n *JVM.oopSize);
    }
    public abstract @RawCType("intptr_t*")long interpreter_frame_expression_stack();
    // top of expression stack
    public abstract @RawCType("intptr_t*")long interpreter_frame_tos_address();
}
