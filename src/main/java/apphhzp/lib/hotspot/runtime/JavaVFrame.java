package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.hotspot.oops.method.Method;

import java.util.List;

public abstract class JavaVFrame extends VFrame{
    // JVM state
    public abstract Method method();
    public abstract int                          bci();
    public abstract StackValueCollection        locals();
    public abstract StackValueCollection        expressions();
    // the order returned by monitors() is from oldest -> youngest#4418568
    public abstract List<MonitorInfo> monitors();

    // Debugging support via JVMTI.
    // NOTE that this is not guaranteed to give correct results for compiled vframes.
    // Deoptimize first if necessary.
    public abstract void set_locals(StackValueCollection values);

    // Test operation
    public boolean is_java_frame()  { return true; }
    protected JavaVFrame(Frame fr, RegisterMap reg_map, JavaThread thread){
        super(fr, reg_map, thread);
    }
    protected JavaVFrame(Frame fr, JavaThread thread){
        super(fr, thread);
    }
    public List<MonitorInfo> locked_monitors() {
        //TODO
        throw new UnsupportedOperationException("TODO");
//        assert(SafepointSynchronize::is_at_safepoint() || JavaThread::current() == thread(),
//                "must be at safepoint or it's a java frame of the current thread");
//
//        GrowableArray<MonitorInfo*>* mons = monitors();
//        GrowableArray<MonitorInfo*>* result = new GrowableArray<MonitorInfo*>(mons->length());
//        if (mons->is_empty()) return result;
//
//        bool found_first_monitor = false;
//        // The ObjectMonitor* can't be async deflated since we are either
//        // at a safepoint or the calling thread is operating on itself so
//        // it cannot exit the ObjectMonitor so it remains busy.
//        ObjectMonitor *waiting_monitor = thread()->current_waiting_monitor();
//        ObjectMonitor *pending_monitor = NULL;
//        if (waiting_monitor == NULL) {
//            pending_monitor = thread()->current_pending_monitor();
//        }
//        oop pending_obj = (pending_monitor != NULL ? pending_monitor->object() : (oop) NULL);
//        oop waiting_obj = (waiting_monitor != NULL ? waiting_monitor->object() : (oop) NULL);
//
//        for (int index = (mons->length()-1); index >= 0; index--) {
//            MonitorInfo* monitor = mons->at(index);
//            if (monitor->eliminated() && is_compiled_frame()) continue; // skip eliminated monitor
//            oop obj = monitor->owner();
//            if (obj == NULL) continue; // skip unowned monitor
//            //
//            // Skip the monitor that the thread is blocked to enter or waiting on
//            //
//            if (!found_first_monitor && (obj == pending_obj || obj == waiting_obj)) {
//                continue;
//            }
//            found_first_monitor = true;
//            result->append(monitor);
//        }
//        return result;
    }
}
