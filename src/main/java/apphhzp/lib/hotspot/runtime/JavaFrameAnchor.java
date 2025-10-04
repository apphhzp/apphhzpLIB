package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.runtime.x86.X86JavaFrameAnchor;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public abstract class JavaFrameAnchor extends JVMObject {
    public static final Type TYPE = JVM.type("JavaFrameAnchor");
    public static final int SIZE = TYPE.size;
    public static final long LAST_JAVA_SP_OFFSET = TYPE.offset("_last_Java_sp");
    public static final long LAST_JAVA_PC_OFFSET = TYPE.offset("_last_Java_pc");

    public static JavaFrameAnchor create(long addr) {
        if (PlatformInfo.isX86()) {
            return new X86JavaFrameAnchor(addr);
        }
        throw new UnsupportedOperationException();
    }

    protected JavaFrameAnchor(long addr) {
        super(addr);
    }

    // tells whether the last Java frame is set
    // It is important that when last_Java_sp != NULL that the rest of the frame
    // anchor (including platform specific) all be valid.
    public boolean has_last_Java_frame() {
        return unsafe.getAddress(this.address + LAST_JAVA_SP_OFFSET) != 0L;
    }

    // This is very dangerous unless sp == NULL
    // Invalidate the anchor so that has_last_frame is false
    // and no one should look at the other fields.
    public void zap() {
        unsafe.putAddress(this.address + LAST_JAVA_SP_OFFSET, 0L);
    }

    public @RawCType("intptr_t*") long last_Java_sp() {
        return unsafe.getAddress(this.address+LAST_JAVA_SP_OFFSET);
    }

    public @RawCType("address") long last_Java_pc() {
        return unsafe.getAddress(this.address+LAST_JAVA_PC_OFFSET);
    }
    public abstract void make_walkable();
    public abstract boolean walkable();
}
