package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

public class G1MonitoringSupport extends JVMObject {
    public static final Type TYPE= JVM.type("G1MonitoringSupport");
    public static final int SIZE= TYPE.size;
    public static final long OLD_GEN_COMMITTED_OFFSET= TYPE.offset("_old_gen_committed");
    public static final long EDEN_SPACE_COMMITTED_OFFSET=TYPE.offset("_eden_space_committed");
    public static final long EDEN_SPACE_USED_OFFSET=TYPE.offset("_eden_space_used");
    public static final long SURVIVOR_SPACE_COMMITTED_OFFSET=TYPE.offset("_survivor_space_committed");
    public static final long SURVIVOR_SPACE_USED_OFFSET=TYPE.offset("_survivor_space_used");
    public static final long OLD_GEN_USED_OFFSET=TYPE.offset("_old_gen_used");
    public G1MonitoringSupport(long addr) {
        super(addr);
    }

    public long oldGenCommitted() {
        return JVM.getSizeT(this.address+OLD_GEN_COMMITTED_OFFSET);
    }

    public void setOldGenCommitted(long value) {
        JVM.putSizeT(this.address+OLD_GEN_COMMITTED_OFFSET, value);
    }

    public long edenSpaceCommitted() {
        return JVM.getSizeT(this.address+EDEN_SPACE_COMMITTED_OFFSET);
    }

    public void setEdenSpaceCommitted(long value) {
        JVM.putSizeT(this.address+EDEN_SPACE_COMMITTED_OFFSET, value);
    }
    public long edenSpaceUsed() {
        return JVM.getSizeT(this.address+EDEN_SPACE_USED_OFFSET);
    }

    public void setEdenSpaceUsed(long value) {
        JVM.putSizeT(this.address+EDEN_SPACE_USED_OFFSET, value);
    }

    public long survivorSpaceCommitted() {
        return JVM.getSizeT(this.address+SURVIVOR_SPACE_COMMITTED_OFFSET);
    }

    public void setSurvivorSpaceCommitted(long value) {
        JVM.putSizeT(this.address+SURVIVOR_SPACE_COMMITTED_OFFSET, value);
    }

    public long survivorSpaceUsed() {
        return JVM.getSizeT(this.address+SURVIVOR_SPACE_USED_OFFSET);
    }

    public void setSurvivorSpaceUsed(long value) {
        JVM.putSizeT(this.address+SURVIVOR_SPACE_USED_OFFSET, value);
    }

    public long oldGenUsed() {
        return JVM.getSizeT(this.address+OLD_GEN_USED_OFFSET);
    }

    public void setOldGenUsed(long value) {
        JVM.putSizeT(this.address+OLD_GEN_USED_OFFSET, value);
    }


}
