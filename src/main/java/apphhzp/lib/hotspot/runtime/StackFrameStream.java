package apphhzp.lib.hotspot.runtime;

public class StackFrameStream {
    private Frame       _fr;
    private RegisterMap _reg_map;
    private boolean        _is_done;
    public StackFrameStream(JavaThread thread, boolean update, boolean process_frames){
        _reg_map=RegisterMap.create(thread, update, process_frames);
        if (!thread.has_last_Java_frame()){
            throw new RuntimeException("sanity check");
        }
        _fr = thread.last_frame();
        _is_done = false;
    }

    public boolean is_done() {
        if (_is_done){
            return true;
        }
        _is_done = _fr.is_first_frame();
        return false;
    }
    public void next()                     { if (!_is_done) _fr = _fr.sender(_reg_map); }

    // Query
    public Frame current()                { return _fr; }
    public RegisterMap register_map()     { return _reg_map; }
}
