package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.code.DebugInformationRecorder;
import apphhzp.lib.hotspot.code.PcDesc;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;
import apphhzp.lib.hotspot.util.RawCType;

public class VFrameStreamCommon {

    // common
    protected Frame        _prev_frame;
    protected Frame        _frame;
    protected JavaThread  _thread;
    protected RegisterMap  _reg_map;
    protected static final int  interpreted_mode=0, compiled_mode=1, at_end_mode=2;
    protected int _mode;
    // For compiled_mode
    protected int _decode_offset;
    protected int _sender_decode_offset;
    protected int _vframe_id;

    // Cached information
    protected Method _method;
    protected int       _bci;

    // Should VM activations be ignored or not
    protected boolean _stop_at_java_call_stub;

    protected boolean fill_in_compiled_inlined_sender(){
        if (_sender_decode_offset == DebugInformationRecorder.serialized_null) {
            return false;
        }
        fill_from_compiled_frame(_sender_decode_offset);
        ++_vframe_id;
        return true;
    }
    protected void fill_from_compiled_frame(int decode_offset){
        _mode = compiled_mode;
        _decode_offset = decode_offset;

        // Range check to detect ridiculous offsets.
        if (decode_offset == DebugInformationRecorder.serialized_null ||
                decode_offset < 0 ||
                decode_offset >= nm().scopes_data_size()) {

            // Provide a cheap fallback in product mode.  (See comment above.)
            fill_from_compiled_native_frame();
            return;
        }

        // Decode first part of scopeDesc
        DebugInfoReadStream buffer=new DebugInfoReadStream(nm(), decode_offset);
        _sender_decode_offset = buffer.read_int();
        _method               = buffer.read_method();
        _bci                  = buffer.read_bci();
    }
    // The native frames are handled specially. We do not rely on ScopeDesc info
    // since the pc might not be exact due to the _last_native_pc trick.
    protected void fill_from_compiled_native_frame(){
        _mode = compiled_mode;
        _sender_decode_offset = DebugInformationRecorder.serialized_null;
        _decode_offset = DebugInformationRecorder.serialized_null;
        _vframe_id = 0;
        _method = nm().method();
        _bci = 0;
    }

    protected void fill_from_interpreter_frame(){
        Method method = _frame.interpreter_frame_method();
        @RawCType("address")long   bcp    = _frame.interpreter_frame_bcp();
        int       bci    = method.validate_bci_from_bcp(bcp);
        // 6379830 AsyncGetCallTrace sometimes feeds us wild frames.
        // AsyncGetCallTrace interrupts the VM asynchronously. As a result
        // it is possible to access an interpreter frame for which
        // no Java-level information is yet available (e.g., becasue
        // the frame was being created when the VM interrupted it).
        // In this scenario, pretend that the interpreter is at the point
        // of entering the method.
        if (bci < 0) {
            bci = 0;
        }
        _mode   = interpreted_mode;
        _method = method;
        _bci    = bci;
    }
    protected boolean fill_from_frame(){
        // Interpreted frame
        if (_frame.is_interpreted_frame()) {
            fill_from_interpreter_frame();
            return true;
        }

        // Compiled frame

        if (cb() != null && cb().is_compiled()) {
            if (nm().is_native_method()) {
                // Do not rely on scopeDesc since the pc might be unprecise due to the _last_native_pc trick.
                fill_from_compiled_native_frame();
            } else {
                PcDesc pc_desc = nm().pc_desc_at(_frame.pc());
                int decode_offset;
                if (pc_desc == null) {
                    // Should not happen, but let fill_from_compiled_frame handle it.

                    // If we are trying to walk the stack of a thread that is not
                    // at a safepoint (like AsyncGetCallTrace would do) then this is an
                    // acceptable result. [ This is assuming that safe_for_sender
                    // is so bullet proof that we can trust the frames it produced. ]
                    //
                    // So if we see that the thread is not safepoint safe
                    // then simply produce the method and a bci of zero
                    // and skip the possibility of decoding any inlining that
                    // may be present. That is far better than simply stopping (or
                    // asserting. If however the thread is safepoint safe this
                    // is the sign of a compiler bug  and we'll let
                    // fill_from_compiled_frame handle it.


                    JavaThreadState state = _thread.getState();

                    // in_Java should be good enough to test safepoint safety
                    // if state were say in_Java_trans then we'd expect that
                    // the pc would have already been slightly adjusted to
                    // one that would produce a pcDesc since the trans state
                    // would be one that might in fact anticipate a safepoint

                    if (state == JavaThreadState.IN_JAVA) {
                        // This will get a method a zero bci and no inlining.
                        // Might be nice to have a unique bci to signify this
                        // particular case but for now zero will do.

                        fill_from_compiled_native_frame();

                        // There is something to be said for setting the mode to
                        // at_end_mode to prevent trying to walk further up the
                        // stack. There is evidence that if we walk any further
                        // that we could produce a bad stack chain. However until
                        // we see evidence that allowing this causes us to find
                        // frames bad enough to cause segv's or assertion failures
                        // we don't do it as while we may get a bad call chain the
                        // probability is much higher (several magnitudes) that we
                        // get good data.

                        return true;
                    }
                    decode_offset = DebugInformationRecorder.serialized_null;
                } else {
                    decode_offset = pc_desc.scope_decode_offset();
                }
                fill_from_compiled_frame(decode_offset);
                _vframe_id = 0;
            }
            return true;
        }

        // End of stack?
        if (_frame.is_first_frame() || (_stop_at_java_call_stub && _frame.is_entry_frame())) {
            _mode = at_end_mode;
            return true;
        }

        return false;
    }

//    // Helper routine for security_get_caller_frame
//    protected void skip_prefixed_method_and_wrappers();


    public VFrameStreamCommon(JavaThread thread, boolean process_frames){
        _reg_map=RegisterMap.create(thread, false, process_frames);
        _thread = thread;
    }

    // Accessors
    public Method method() { return _method; }
    public int bci() { return _bci; }
    public @RawCType("intptr_t*")long  frame_id(){
        return  _frame.id();
    }
    public @RawCType("address")long frame_pc() { return _frame.pc(); }

    public CodeBlob cb(){
        return _frame.cb();
    }
    public CompiledMethod nm(){
        if (!(cb() != null && cb().is_compiled())){
            throw new IllegalArgumentException("usage");
        }
        return (CompiledMethod)cb();
    }
    public boolean at_end(){
        return _mode == at_end_mode;
    }

    public boolean is_interpreted_frame()  { return _frame.is_interpreted_frame(); }

    public boolean is_entry_frame()        { return _frame.is_entry_frame(); }

    public void next() {
        // handle frames with inlining
        if (_mode == compiled_mode    && fill_in_compiled_inlined_sender()) return;
        // handle general case
        do {
            _prev_frame = _frame;
            _frame = _frame.sender(_reg_map);
        } while (!fill_from_frame());
    }
    public JavaVFrame asJavaVFrame() {
        JavaVFrame result = null;
        if (_mode == compiled_mode){
            if (!_frame.is_compiled_frame()){
                throw new RuntimeException("expected compiled Java frame");
            }
            // lazy update to register map
            boolean update_map = true;
            RegisterMap map=RegisterMap.create(_thread, update_map);
            Frame f = _prev_frame.sender(map);

            if (!f.is_compiled_frame()){
                throw new RuntimeException("expected compiled Java frame");
            }

            CompiledVFrame cvf = (CompiledVFrame)(VFrame.new_vframe(f, map, _thread));

            if (!(cvf.cb().equals(cb()))){
                throw new RuntimeException("wrong code blob");
            }

            // get the same scope as this stream
            cvf = cvf.at_scope(_decode_offset, _vframe_id);

            if (!(cvf.scope().decode_offset() == _decode_offset)){
                throw new RuntimeException("wrong scope");
            }
            if (!(cvf.scope().sender_decode_offset() == _sender_decode_offset)){
                throw new RuntimeException("wrong scope");
            }
            if (!(cvf.vframe_id() == _vframe_id)){
                throw new RuntimeException("wrong vframe");
            }

            result = cvf;
        } else {
            result = (JavaVFrame) (VFrame.new_vframe(_frame, _reg_map, _thread));
        }
        if (!result.method().equals(method())){
            throw new RuntimeException("wrong method");
        }
        return result;
    }
}
