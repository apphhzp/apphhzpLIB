package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.util.RawCType;

public class VFrame {
    protected Frame _fr;      // Raw frame behind the virtual frame.
    protected RegisterMap _reg_map; // Register map for the raw frame (used to handle callee-saved registers).
    protected JavaThread _thread;  // The thread owning the raw frame.


    public VFrame(Frame fr, RegisterMap reg_map, JavaThread thread) {
        _reg_map=(reg_map);
        _thread=(thread);
        if (fr == null){
            throw new NullPointerException("must have frame");
        }
        _fr = fr.clone();
    }

    public VFrame(Frame fr, JavaThread thread) {
        _reg_map=RegisterMap.create(thread);
        _thread=(thread);
        if (fr==null){
            throw new NullPointerException("must have frame");
        }
        _fr = fr.clone();
    }

    public static VFrame new_vframe(@RawCType("StackFrameStream&")StackFrameStream fst, JavaThread thread) {
        if (fst.current().is_runtime_frame()) {
            fst.next();
        }
        if (fst.is_done()){
            throw new IllegalArgumentException("missing caller");
        }
        return new_vframe(fst.current(), fst.register_map(), thread);
    }

    public static VFrame new_vframe(Frame f, RegisterMap reg_map, JavaThread thread) {
        // Interpreter frame
        if (f.is_interpreted_frame()) {
            return new InterpretedVFrame(f, reg_map, thread);
        }

        // Compiled frame
        CodeBlob cb = f.cb();
        if (cb != null) {
            if (cb.is_compiled()) {
                CompiledMethod nm = (CompiledMethod)cb;
                return new CompiledVFrame(f, reg_map, thread, nm);
            }

            if (f.is_runtime_frame()) {
                // Skip this frame and try again.
                RegisterMap temp_map = reg_map.clone();
                Frame s = f.sender(temp_map);
                return new_vframe(s, temp_map, thread);
            }
        }

        // Entry frame
        if (f.is_entry_frame()) {
            return new EntryVFrame(f, reg_map, thread);
        }

        // External frame
        return new ExternalVFrame(f, reg_map, thread);
    }

    // Accessors
    public Frame fr() {
        return _fr;
    }

    public CodeBlob cb() {
        return _fr.cb();
    }

    public CompiledMethod nm() {
        if (!(cb() != null && cb().is_compiled())){
            throw new RuntimeException("usage");
        }
        return (CompiledMethod) cb();
    }

    // ???? Does this need to be a copy?
    public Frame frame_pointer() {
        return _fr;
    }

    public RegisterMap register_map() {
        return _reg_map;
    }

    public JavaThread thread() {
        return _thread;
    }
    // Answers if the this is the top vframe in the frame, i.e., if the sender vframe
    // is in the caller frame
    public boolean is_top(){
        return true;
    }

    public VFrame sender(){
        RegisterMap temp_map = register_map().clone();
        if (!is_top()){
            throw new RuntimeException("just checking");
        }
        if (_fr.is_entry_frame() && _fr.is_first_frame())
            return null;
        Frame s = _fr.real_sender(temp_map);
        if (s.is_first_frame()) return null;
        return new_vframe(s, temp_map, thread());
    }

    public VFrame top()  {
        VFrame vf = this;
        while (!vf.is_top())
            vf = vf.sender();
        return vf;
    }


    public JavaVFrame java_sender() {
        VFrame f = sender();
        while (f != null) {
            if (f.is_java_frame())
                return (JavaVFrame) (f);
            f = f.sender();
        }
        return null;
    }
    // Type testing operations
    public boolean is_entry_frame()        { return false; }
    public boolean is_java_frame()         { return false; }
    public boolean is_interpreted_frame()  { return false; }
    public boolean is_compiled_frame()     { return false; }
}
