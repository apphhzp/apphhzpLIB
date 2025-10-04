package apphhzp.lib.hotspot.runtime.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.runtime.JavaFrameAnchor;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class X86JavaFrameAnchor extends JavaFrameAnchor {
    public static final long LAST_JAVA_FP_OFFSET=TYPE.offset("_last_Java_fp");
    public X86JavaFrameAnchor(long addr) {
        super(addr);
    }

    public boolean walkable(){ return unsafe.getAddress(this.address+LAST_JAVA_SP_OFFSET) != 0L && unsafe.getAddress(this.address+LAST_JAVA_PC_OFFSET)!= 0L; }
    @Override
    public void make_walkable() {
        // last frame set?
        if (last_Java_sp() == 0L) {
            return;
        }
        // already walkable?
        if (walkable()) {
            return;
        }
        if (!(last_Java_pc() == 0L)){
            throw new RuntimeException("already walkable");
        }
        unsafe.putAddress(this.address+LAST_JAVA_PC_OFFSET,unsafe.getAddress(this.address+LAST_JAVA_SP_OFFSET-JVM.oopSize));
        if (!walkable()){
            throw new RuntimeException("something went wrong");
        }
    }

    public @RawCType("intptr_t*")long   last_Java_fp() {
        return unsafe.getAddress(this.address+LAST_JAVA_FP_OFFSET);
    }
}
