package apphhzp.lib.hotspot.runtime.x86;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.CodeCache;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.compiler.OopMapSet;
import apphhzp.lib.hotspot.interpreter.AbstractInterpreter;
import apphhzp.lib.hotspot.runtime.Frame;
import apphhzp.lib.hotspot.runtime.RegisterMap;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.runtime.Frame.DeoptState.*;

public class X86Frame extends Frame {
    public static final long FP_OFFSET=JVM.computeOffset(JVM.oopSize,Frame.DEOPT_STATE_OFFSET+JVM.intSize);
    public static final long UNEXTENDED_SP_OFFSET=FP_OFFSET+JVM.oopSize;

    public static final int entry_frame_call_wrapper_offset= JVM.intConstant("frame::entry_frame_call_wrapper_offset");
    public static final int // All frames
    link_offset                                      =  0,
    return_addr_offset                               =  1,
    // non-interpreter frames
    sender_sp_offset                                 =  2,

    // Interpreter frames
    interpreter_frame_result_handler_offset          =  3, // for native calls only
    interpreter_frame_oop_temp_offset                =  2, // for native calls only

    interpreter_frame_sender_sp_offset               = -1,
    // outgoing sp before a call to an invoked method
    interpreter_frame_last_sp_offset                 = interpreter_frame_sender_sp_offset - 1,
    interpreter_frame_method_offset                  = interpreter_frame_last_sp_offset - 1,
    interpreter_frame_mirror_offset                  = interpreter_frame_method_offset - 1,
    interpreter_frame_mdp_offset                     = interpreter_frame_mirror_offset - 1,
    interpreter_frame_cache_offset                   = interpreter_frame_mdp_offset - 1,
    interpreter_frame_locals_offset                  = interpreter_frame_cache_offset - 1,
    interpreter_frame_bcp_offset                     = interpreter_frame_locals_offset - 1,
    interpreter_frame_initial_sp_offset              = interpreter_frame_bcp_offset - 1,

    interpreter_frame_monitor_block_top_offset       = interpreter_frame_initial_sp_offset,
    interpreter_frame_monitor_block_bottom_offset    = interpreter_frame_initial_sp_offset,
            // Entry frames
    entry_frame_after_call_words                     = PlatformInfo.isX86_64()?(ClassHelperSpecial.isWindows&&ClassHelperSpecial.is64BitJVM?60:13): -114514,
    arg_reg_save_area_bytes                          =  PlatformInfo.isX86_64()?(ClassHelperSpecial.isWindows&&ClassHelperSpecial.is64BitJVM?32:0):-114514; // Register argument save area


    // an additional field beyond _sp and _pc:
    private @RawCType("intptr_t*")long   _fp; // frame pointer
    // The interpreter and adapters will extend the frame of the caller.
    // Since oopMaps are based on the sp of the caller before extension
    // we need to know that value. However in order to compute the address
    // of the return address we need the real "raw" sp. By convention we
    // use sp() to mean "raw" sp and unextended_sp() to mean the caller's
    // original sp.

    private @RawCType("intptr_t*")long _unextended_sp;
    static {
        if (PlatformInfo.isX86()){
            JVM.assertOffset(Frame.SIZE,JVM.computeOffset(JVM.oopSize,UNEXTENDED_SP_OFFSET+JVM.oopSize));
        }
    }

    public static Frame of(long addr){
        X86Frame re=new X86Frame();
        re._sp=unsafe.getAddress(addr+SP_OFFSET);
        re._cb= CodeBlob.getCodeBlob(unsafe.getAddress(addr+CB_OFFSET));
        re._pc=unsafe.getAddress(addr+PC_OFFSET);
        re._deopt_state=unsafe.getInt(addr+DEOPT_STATE_OFFSET);
        re._fp=unsafe.getAddress(addr+FP_OFFSET);
        re._unextended_sp=unsafe.getAddress(addr+UNEXTENDED_SP_OFFSET);
        return re;
    }

    public X86Frame(){
        if (!PlatformInfo.isX86()){
            throw new RuntimeException();
        }
        _pc = 0L;
        _sp = 0L;
        _unextended_sp = 0L;
        _fp = 0L;
        _cb = null;
        _deopt_state = DeoptState.unknown;
    }
    public X86Frame(@RawCType("intptr_t*")long sp, @RawCType("intptr_t*")long fp, @RawCType("address")long pc){
        if (!PlatformInfo.isX86()){
            throw new RuntimeException();
        }
        init(sp, fp, pc);
    }

    public X86Frame(@RawCType("intptr_t*")long sp, @RawCType("intptr_t*")long unextended_sp, @RawCType("intptr_t*")long fp, @RawCType("address")long pc){
        if (!PlatformInfo.isX86()){
            throw new RuntimeException();
        }
        _sp = sp;
        _unextended_sp = unextended_sp;
        _fp = fp;
        _pc = pc;
        if (pc == 0L){
            throw new RuntimeException("no pc?");
        }
        _cb = CodeCache.find_blob(pc);

        @RawCType("address")long original_pc = CompiledMethod.get_deopt_original_pc(this);
        if (original_pc != 0L) {
            _pc = original_pc;
            if (!(_cb.as_compiled_method().insts_contains_inclusive(_pc))){
                throw new RuntimeException("original PC must be in the main code section of the the compiled method (or must be immediately following it)");
            }
            _deopt_state = is_deoptimized;
        } else {
            if (_cb.is_deoptimization_stub()) {
                _deopt_state = is_deoptimized;
            } else {
                _deopt_state = not_deoptimized;
            }
        }
    }

    public X86Frame(@RawCType("intptr_t*")long sp, @RawCType("intptr_t*")long fp){
        if (!PlatformInfo.isX86()){
            throw new RuntimeException();
        }
        _sp = sp;
        _unextended_sp = sp;
        _fp = fp;
        _pc = unsafe.getAddress(sp-JVM.oopSize);

        // Here's a sticky one. This constructor can be called via AsyncGetCallTrace
        // when last_Java_sp is non-null but the pc fetched is junk. If we are truly
        // unlucky the junk value could be to a zombied method and we'll die on the
        // find_blob call. This is also why we can have no asserts on the validity
        // of the pc we find here. AsyncGetCallTrace -> pd_get_top_frame_for_signal_handler
        // -> pd_last_frame should use a specialized version of pd_last_frame which could
        // call a specialized frame constructor instead of this one.
        // Then we could use the assert below. However this assert is of somewhat dubious
        // value.
        // UPDATE: this constructor is only used by trace_method_handle_stub() now.
        // assert(_pc != NULL, "no pc?");

        _cb = CodeCache.find_blob(_pc);

        @RawCType("address")long original_pc = CompiledMethod.get_deopt_original_pc(this);
        if (original_pc != 0L) {
            _pc = original_pc;
            _deopt_state = is_deoptimized;
        } else {
            _deopt_state = not_deoptimized;
        }
    }


    public @RawCType("intptr_t")long ptr_at(int offset){
        return unsafe.getAddress(ptr_at_addr(offset));
    }

    public void ptr_at_put(int offset, @RawCType("intptr_t")long value) {
         unsafe.putAddress(ptr_at_addr(offset),value);
    }

    public @RawCType("intptr_t*") long ptr_at_addr(int offset){
        return addr_at(offset);
    }

    // accessors for the instance variables
    // Note: not necessarily the real 'frame pointer' (see real_fp)
    @Override
    public long fp() {
        return _fp;
    }

    @Override
    public boolean is_interpreted_frame() {
        return AbstractInterpreter.getCode().contains(pc());
    }

    public static int interpreter_frame_expression_stack_direction() {
        return -1;
    }

    public @RawCType("address*")long sender_pc_addr(){
        return addr_at( return_addr_offset);
    }
    public @RawCType("address")long  sender_pc(){
        return unsafe.getAddress(sender_pc_addr());
    }

    public @RawCType("intptr_t*")long sender_sp(){
        return addr_at(sender_sp_offset);
    }

    public void init(@RawCType("intptr_t*")long sp, @RawCType("intptr_t*")long fp, @RawCType("address")long pc) {
        _sp = sp;
        _unextended_sp = sp;
        _fp = fp;
        _pc = pc;
        if (pc == 0L){
            throw new RuntimeException("no pc?");
        }

        _cb = CodeCache.find_blob(pc);
        @RawCType("address")long original_pc = CompiledMethod.get_deopt_original_pc(this);
        if (original_pc != 0L) {
            _pc = original_pc;
            _deopt_state = is_deoptimized;
        } else {
            _deopt_state = not_deoptimized;
        }
    }

    @Override
    public @RawCType("intptr_t*") long unextended_sp() {
        return _unextended_sp;
    }

    public Frame sender_raw(RegisterMap map){
        // Default is we done have to follow them. The sender_for_xxx will
        // update it accordingly
        map.set_include_argument_oops(false);

        if (is_entry_frame())        return sender_for_entry_frame(map);
        if (is_optimized_entry_frame()) return sender_for_optimized_entry_frame(map);
        if (is_interpreted_frame())  return sender_for_interpreter_frame(map);
        if (JVM.ENABLE_EXTRA_CHECK&&!_cb.equals(CodeCache.find_blob(pc()))){
            throw new RuntimeException("Must be the same");
        }

        if (_cb != null) {
            return sender_for_compiled_frame(map);
        }
        // Must be native-compiled frame, i.e. the marshaling code for native
        // methods that exists in the core system.
        return new X86Frame(sender_sp(), link(), sender_pc());
    }

    public Frame sender(RegisterMap map) {
        Frame result = sender_raw(map);
        return result;
    }

    @Override
    public long link() {
        return unsafe.getAddress(addr_at(link_offset));
    }

    public Frame sender_for_compiled_frame(RegisterMap map){
        if (map==null){
            throw new NullPointerException("map must be set");
        }
        // frame owned by optimizing compiler
        if (!(_cb.frame_size() >= 0)){
            throw new RuntimeException("must have non-zero frame size");
        }
        @RawCType("intptr_t*")long sender_sp = unextended_sp() + (long) _cb.frame_size() *JVM.oopSize;
        @RawCType("intptr_t*")long unextended_sp = sender_sp;

        // On Intel the return_address is always the word on the stack
        @RawCType("address")long sender_pc =  unsafe.getAddress(sender_sp-JVM.oopSize);

        // This is the saved value of EBP which may or may not really be an FP.
        // It is only an FP if the sender is an interpreter frame (or C1?).
        @RawCType("intptr_t**")long saved_fp_addr = (sender_sp - (long) sender_sp_offset *JVM.oopSize);

        if (map.update_map()) {
            // Tell GC to use argument oopmaps for some runtime stubs that need it.
            // For C1, the runtime stub might not have oop maps, so set this flag
            // outside of update_register_map.
            map.set_include_argument_oops(_cb.caller_must_gc_arguments(map.thread()));
            if (_cb.oop_maps() != null) {
                OopMapSet.update_register_map(this, map);
            }
            // Since the prolog does the save and restore of EBP there is no oopmap
            // for it so we must fill in its location as if there was an oopmap entry
            // since if our caller was compiled code there could be live jvm state in it.
            update_map_with_saved_link(map, saved_fp_addr);
        }
        if (sender_sp == sp()){
            throw new RuntimeException("must have changed");
        }
        return new X86Frame(sender_sp, unextended_sp, unsafe.getAddress(saved_fp_addr), sender_pc);
    }
    public Frame sender_for_entry_frame(RegisterMap map){
        //TODO
        throw new UnsupportedOperationException("TODO");
//        if (map==null){
//            throw new NullPointerException("map must be set");
//        }
//        // Java frame called from C; skip all C frames and return top C
//        // frame of that chunk as the sender
//        JavaFrameAnchor jfa = entry_frame_call_wrapper()->anchor();
//        assert(!entry_frame_is_first(), "next Java fp must be non zero");
//        assert(jfa->last_Java_sp() > sp(), "must be above this frame on stack");
//        // Since we are walking the stack now this nested anchor is obviously walkable
//        // even if it wasn't when it was stacked.
//        jfa->make_walkable();
//        map->clear();
//        assert(map->include_argument_oops(), "should be set by clear");
//        frame fr(jfa->last_Java_sp(), jfa->last_Java_fp(), jfa->last_Java_pc());
//
//        return fr;
    }
    public Frame sender_for_interpreter_frame(RegisterMap map){
        // SP is the raw SP from the sender after adapter or interpreter
        // extension.
        @RawCType("intptr_t*")long sender_sp = this.sender_sp();

        // This is the sp before any possible extension (adapter/locals).
        @RawCType("intptr_t*")long unextended_sp = interpreter_frame_sender_sp();

        if ((JVM.usingServerCompiler||JVM.includeJVMCI)&&map.update_map()) {
            update_map_with_saved_link(map, addr_at(link_offset));
        }
        return new X86Frame(sender_sp, unextended_sp, link(), sender_pc());
    }
    public Frame sender_for_native_frame(RegisterMap map){
        throw new UnsupportedOperationException();
    }
    public Frame sender_for_optimized_entry_frame(RegisterMap map){
        //TODO
        throw new UnsupportedOperationException("TODO");
    }

    public  @RawCType("intptr_t*")long interpreter_frame_sender_sp(){
        if (!is_interpreted_frame()){
            throw new RuntimeException("interpreted frame expected");
        }
        return at(interpreter_frame_sender_sp_offset);
    }

    private static final VMReg rbp;
    static {
        if (JVM.oopSize == 4) {
            rbp = VMReg.create(5);
        } else {
            rbp = VMReg.create(5 << 1);
        }
    }
    // helper to update a map with callee-saved RBP
    public static void update_map_with_saved_link(RegisterMap map, @RawCType("intptr_t**")long link_addr){
// The interpreter and compiler(s) always save EBP/RBP in a known
        // location on entry. We must record where that location is
        // so this if EBP/RBP was live on callout from c2 we can find
        // the saved copy no matter what it called.

        // Since the interpreter always saves EBP/RBP if we record where it is then
        // we don't have to always save EBP/RBP on entry and exit to c2 compiled
        // code, on entry will be enough.
        map.set_location(rbp,  link_addr);
        // this is weird "H" ought to be at a higher address however the
        // oopMaps seems to have the "H" regs at the same address and the
        // vanilla register.
        // XXXX make this go away
        if(PlatformInfo.isX86_64()){
            map.set_location(rbp.next(), link_addr);
        }
    }

    @Override
    public @RawCType("Method**") long interpreter_frame_method_addr() {
        return addr_at(interpreter_frame_method_offset);
    }

    @Override
    public X86Frame clone() {
        X86Frame re=new X86Frame();
        re._fp=this._fp;
        re._cb=this._cb;
        re._unextended_sp=this._unextended_sp;
        re._deopt_state=this._deopt_state;
        re._sp=this._sp;
        re._pc=this._pc;
        return re;
    }

    @Override
    public long interpreter_frame_mdp_addr() {
        return addr_at(interpreter_frame_mdp_offset);
    }

    @Override
    public long interpreter_frame_bcp_addr() {
        return addr_at(interpreter_frame_bcp_offset);
    }

    @Override
    public long interpreter_frame_locals_addr() {
        return addr_at(interpreter_frame_locals_offset);
    }
    // Return unique id for this frame. The id must have a value where we can distinguish
    // identity and younger/older relationship. NULL represents an invalid (incomparable)
    // frame.
    public @RawCType("intptr_t*")long id(){
        return unextended_sp();
    }

    public boolean is_younger(@RawCType("intptr_t*")long id){
        throw new UnsupportedOperationException("No implementation in JVM source code????");
    }

    // Return true if the frame is older (less recent activation) than the frame represented by id
    public boolean is_older(@RawCType("intptr_t*")long id){
        if (!(this.id() != 0L && id != 0L)){
            throw new RuntimeException("NULL frame id");
        }
        return Long.compareUnsigned(this.id(),id) > 0;
    }

    public @RawCType("BasicObjectLock*")long interpreter_frame_monitor_end() {
        @RawCType("BasicObjectLock*")long result = unsafe.getAddress(addr_at(interpreter_frame_monitor_block_top_offset));
        // make sure the pointer points inside the frame
        if (!(Long.compareUnsigned(sp(),result) <= 0)){
            throw new RuntimeException("monitor end should be above the stack pointer");
        }
        if (!(Long.compareUnsigned(result,fp())  < 0)){
            throw new RuntimeException("monitor end should be strictly below the frame pointer");
        }
        return result;
    }

    @Override
    public long interpreter_frame_expression_stack() {
        @RawCType("intptr_t*")long monitor_end = interpreter_frame_monitor_end();
        return monitor_end-JVM.oopSize;
    }

    @Override
    public long entry_frame_call_wrapper_addr() {
        return addr_at(entry_frame_call_wrapper_offset);
    }
    public @RawCType("intptr_t*")long interpreter_frame_last_sp() {
        return unsafe.getAddress(addr_at(interpreter_frame_last_sp_offset));
    }

    // top of expression stack
    public @RawCType("intptr_t*")long interpreter_frame_tos_address() {
        @RawCType("intptr_t*")long last_sp = interpreter_frame_last_sp();
        if (last_sp == 0L) {
            return sp();
        } else {
            // sp() may have been extended or shrunk by an adapter.  At least
            // check that we don't fall behind the legal region.
            // For top deoptimized frame last_sp == interpreter_frame_monitor_end.
            if (!(last_sp <=  interpreter_frame_monitor_end())){
                throw new RuntimeException("bad tos");
            }
            return last_sp;
        }
    }
}
