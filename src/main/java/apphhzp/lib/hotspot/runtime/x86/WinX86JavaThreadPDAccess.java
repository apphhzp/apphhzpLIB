package apphhzp.lib.hotspot.runtime.x86;

import apphhzp.lib.hotspot.runtime.Frame;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.runtime.JavaThreadPDAccess;

public class WinX86JavaThreadPDAccess implements JavaThreadPDAccess {
    @Override
    public Frame pd_last_frame(JavaThread thread){
        if (!thread.has_last_Java_frame()){
            throw new RuntimeException("must have last_Java_sp() when suspended");
        }
        if (thread.anchor.last_Java_pc() == 0L){
            throw new RuntimeException("not walkable");
        }
        return new X86Frame(thread.anchor.last_Java_sp(), ((X86JavaFrameAnchor)thread.anchor).last_Java_fp(), thread.anchor.last_Java_pc());
    }
}
