package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;

public class G1CollectionSet extends JVMObject {
    public static final long G1H_OFFSET;
    public static final long POLICY_OFFSET;
    public static final long CANDIDATES_OFFSET;
    public static final long EDEN_REGION_LENGTH_OFFSET;
    public static final long SURVIVOR_REGION_LENGTH_OFFSET;
    public static final long OLD_REGION_LENGTH_OFFSET;
    public static final long COLLECTION_SET_REGIONS_OFFSET;
    public static final long COLLECTION_SET_CUR_LENGTH_OFFSET;
    public static final long COLLECTION_SET_MAX_LENGTH_OFFSET;
    public static final long NUM_OPTIONAL_REGIONS_OFFSET;
    public static final long BYTES_USED_BEFORE_OFFSET;
    public static final long RECORDED_RS_LENGTH_OFFSET;
    public static final long INC_BUILD_STATE_OFFSET;
    public static final long INC_PART_START_OFFSET;
    public static final long INC_COLLECTION_SET_STATS_OFFSET;
    public static final long INC_BYTES_USED_BEFORE_OFFSET;
    public static final long INC_RECORDED_RS_LENGTH_OFFSET;
    public static final long INC_RECORDED_RS_LENGTH_DIFF_OFFSET;
    public static final long INC_PREDICTED_NON_COPY_TIME_MS_OFFSET;
    public static final long INC_PREDICTED_NON_COPY_TIME_MS_DIFF_OFFSET;
    public static final int SIZE;
    static {
        long[] offsets=JVM.computeOffsets(false,
                new long[]{JVM.oopSize,JVM.oopSize,JVM.oopSize,JVM.intSize,JVM.intSize,JVM.intSize,JVM.oopSize,JVM.size_tSize,JVM.size_tSize,JVM.intSize,JVM.size_tSize,JVM.size_tSize,JVM.intSize,JVM.size_tSize,JVM.oopSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.doubleSize,JVM.doubleSize},
                new long[]{JVM.oopSize,JVM.oopSize,JVM.oopSize,JVM.intSize,JVM.intSize,JVM.intSize,JVM.oopSize,JVM.size_tSize,JVM.size_tSize,JVM.intSize,JVM.size_tSize,JVM.size_tSize,JVM.intSize,JVM.size_tSize,JVM.oopSize,JVM.size_tSize,JVM.size_tSize,JVM.size_tSize,JVM.doubleSize,JVM.doubleSize});
        G1H_OFFSET=offsets[0];
        POLICY_OFFSET=offsets[1];
        CANDIDATES_OFFSET=offsets[2];
        EDEN_REGION_LENGTH_OFFSET=offsets[3];
        SURVIVOR_REGION_LENGTH_OFFSET=offsets[4];
        OLD_REGION_LENGTH_OFFSET=offsets[5];
        COLLECTION_SET_REGIONS_OFFSET=offsets[6];
        COLLECTION_SET_CUR_LENGTH_OFFSET=offsets[7];
        COLLECTION_SET_MAX_LENGTH_OFFSET=offsets[8];
        NUM_OPTIONAL_REGIONS_OFFSET=offsets[9];
        BYTES_USED_BEFORE_OFFSET=offsets[10];
        RECORDED_RS_LENGTH_OFFSET=offsets[11];
        INC_BUILD_STATE_OFFSET=offsets[12];
        INC_PART_START_OFFSET=offsets[13];
        INC_COLLECTION_SET_STATS_OFFSET=offsets[14];
        INC_BYTES_USED_BEFORE_OFFSET=offsets[15];
        INC_RECORDED_RS_LENGTH_OFFSET=offsets[16];
        INC_RECORDED_RS_LENGTH_DIFF_OFFSET=offsets[17];
        INC_PREDICTED_NON_COPY_TIME_MS_OFFSET=offsets[18];
        INC_PREDICTED_NON_COPY_TIME_MS_DIFF_OFFSET=offsets[19];
        SIZE= (int) JVM.computeOffset(Math.max(JVM.oopSize,Math.max(JVM.size_tSize,JVM.doubleSize)),INC_PREDICTED_NON_COPY_TIME_MS_DIFF_OFFSET+JVM.doubleSize);
    }
    public G1CollectionSet(long addr) {
        super(addr);
    }


}
