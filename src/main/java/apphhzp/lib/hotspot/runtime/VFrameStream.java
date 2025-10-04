package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.util.RawCType;

public class VFrameStream extends VFrameStreamCommon{
    public VFrameStream(JavaThread thread){
        this(thread,false,true);
    }
    public VFrameStream(JavaThread thread, boolean stop_at_java_call_stub){
        this(thread,stop_at_java_call_stub,true);
    }
    public VFrameStream(JavaThread thread, boolean stop_at_java_call_stub ,boolean process_frames){
        super(thread,process_frames);
        _stop_at_java_call_stub = stop_at_java_call_stub;

        if (!thread.has_last_Java_frame()) {
            _mode = at_end_mode;
            return;
        }
        _frame = _thread.last_frame();
        while (!fill_from_frame()) {
            _prev_frame = _frame;
            _frame = _frame.sender(_reg_map);
        }
    }

    public VFrameStream(JavaThread thread,Frame top_frame){
        this(thread,top_frame,false);
    }
    // top_frame may not be at safepoint, start with sender
    public VFrameStream(JavaThread thread, @RawCType("frame") Frame top_frame, boolean stop_at_java_call_stub){
        super(thread,true);
        top_frame=top_frame.clone();
        _stop_at_java_call_stub = stop_at_java_call_stub;

        // skip top frame, as it may not be at safepoint
        _prev_frame = top_frame;
        _frame  = top_frame.sender(_reg_map);
        while (!fill_from_frame()) {
            _prev_frame = _frame;
            _frame = _frame.sender(_reg_map);
        }
    }
}
