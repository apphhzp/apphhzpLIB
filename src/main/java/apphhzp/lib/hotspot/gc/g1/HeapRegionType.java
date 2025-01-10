package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class HeapRegionType extends JVMObject {
    public static final Type TYPE = JVM.type("HeapRegionType");
    public static final int SIZE = TYPE.size;
    public static final long TAG_OFFSET = TYPE.offset("_tag");
    public static final int FreeTag = JVM.intConstant("HeapRegionType::FreeTag");
    public static final int YoungMask = JVM.intConstant("HeapRegionType::YoungMask");
    public static final int EdenTag = JVM.intConstant("HeapRegionType::EdenTag");
    public static final int SurvTag = JVM.intConstant("HeapRegionType::SurvTag");
    public static final int HumongousMask = JVM.intConstant("HeapRegionType::HumongousMask");
    public static final int PinnedMask = JVM.intConstant("HeapRegionType::PinnedMask");
    public static final int ArchiveMask = JVM.intConstant("HeapRegionType::ArchiveMask");
    public static final int StartsHumongousTag = JVM.intConstant("HeapRegionType::StartsHumongousTag");
    public static final int ContinuesHumongousTag = JVM.intConstant("HeapRegionType::ContinuesHumongousTag");
    public static final int OldMask = JVM.intConstant("HeapRegionType::OldMask");
    public static final int OldTag = OldMask;
    public static final int OpenArchiveTag = ArchiveMask | PinnedMask;
    public static final int ClosedArchiveTag = ArchiveMask | PinnedMask + 1;

    public HeapRegionType(long addr) {
        super(addr);
    }

    public int get(){
        return unsafe.getInt(this.address+TAG_OFFSET);
    }

    public void set(int tag) {
        if (!isValid(tag)){
            throw new IllegalArgumentException("invalid HR type: " + tag);
        }
        unsafe.putInt(this.address+TAG_OFFSET, tag);
    }

    // Sets the type to 'tag', expecting the type to be 'before'. This
    // is available for when we want to add sanity checking to the type
    // transition.
    public void setFrom(int tag, int before) {
        if (!isValid(tag)){
            throw new IllegalArgumentException("invalid HR type: " + tag);
        }
        if (!isValid(before)){
            throw new IllegalArgumentException("invalid HR type: " + tag);
        }
        if (this.get()!=before){
            throw new IllegalArgumentException("HR tag: "+this.get()+", expected: "+before+" new tag; "+tag);
        }
        unsafe.putInt(this.address+TAG_OFFSET, tag);
    }

    public boolean isFree() {
        return this.get() == FreeTag;
    }

    public boolean isYoung() {
        return (this.get() & YoungMask) != 0;
    }

    public boolean isEden() {
        return this.get() == EdenTag;
    }

    public boolean isSurvivor() {
        return this.get() == SurvTag;
    }

    public boolean isHumongous() {
        return (this.get() & HumongousMask) != 0;
    }

    public boolean isStartsHumongous() {
        return this.get() == StartsHumongousTag;
    }

    public boolean isContinuesHumongous() {
        return this.get() == ContinuesHumongousTag;
    }

    public boolean isArchive() {
        return (this.get() & ArchiveMask) != 0;
    }

    public boolean isOpenArchive() {
        return this.get() == OpenArchiveTag;
    }

    public boolean isClosedArchive() {
        return this.get() == ClosedArchiveTag;
    }

    // is_old regions may or may not also be pinned
    public boolean isOld() {
        return (this.get() & OldMask) != 0;
    }

    public boolean isOldOrHumongous() {
        return (this.get() & (OldMask | HumongousMask)) != 0;
    }

    public boolean isOldOrHumongousOrArchive() {
        return (this.get() & (OldMask | HumongousMask | ArchiveMask)) != 0;
    }

    // is_pinned regions may be archive or humongous
    public boolean isPinned() {
        return (this.get() & PinnedMask) != 0;
    }

    // Setters

    public void setFree() {
        this.set(FreeTag);
    }

    public void setEden() {
        this.setFrom(EdenTag, FreeTag);
    }

    public void setEdenPreGC() {
        this.setFrom(EdenTag, SurvTag);
    }

    public void setSurvivor() {
        this.setFrom(SurvTag, FreeTag);
    }

    public void setStartsHumongous() {
        this.setFrom(StartsHumongousTag, FreeTag);
    }

    public void setContinuesHumongous() {
        this.setFrom(ContinuesHumongousTag, FreeTag);
    }

    public void setOld() {
        this.set(OldTag);
    }

    public static boolean isValid(int tag) {
        return tag == FreeTag || tag == EdenTag || tag == SurvTag || tag == StartsHumongousTag || tag == ContinuesHumongousTag || tag == OldTag || tag == OpenArchiveTag || tag == ClosedArchiveTag;
    }

    public static String getStr(int tag) {
        if (tag == FreeTag) {
            return "FREE";
        } else if (tag == EdenTag) {
            return "EDEN";
        } else if (tag == SurvTag) {
            return "SURV";
        } else if (tag == StartsHumongousTag) {
            return "HUMS";
        } else if (tag == ContinuesHumongousTag) {
            return "HUMC";
        } else if (tag == OldTag) {
            return "OLD";
        } else if (tag == OpenArchiveTag) {
            return "OARC";
        } else if (tag == ClosedArchiveTag) {
            return "CARC";
        }
        throw new IllegalArgumentException("Invalid tag: " + tag);
    }

    public static String getShortStr(int tag) {
        if (tag == FreeTag) {
            return "F";
        } else if (tag == EdenTag) {
            return "E";
        } else if (tag == SurvTag) {
            return "S";
        } else if (tag == StartsHumongousTag) {
            return "HS";
        } else if (tag == ContinuesHumongousTag) {
            return "HC";
        } else if (tag == OldTag) {
            return "O";
        } else if (tag == OpenArchiveTag) {
            return "OA";
        } else if (tag == ClosedArchiveTag) {
            return "CA";
        }
        throw new IllegalArgumentException("Invalid tag: " + tag);
    }
}
