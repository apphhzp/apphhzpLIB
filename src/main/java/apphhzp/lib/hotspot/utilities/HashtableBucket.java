package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.util.function.LongFunction;

import static apphhzp.lib.ClassHelper.unsafe;

public class HashtableBucket extends JVMObject {
    public static final Type TYPE= JVM.type("HashtableBucket<mtInternal>");
    public static final int SIZE=TYPE.size;
    public static final long ENTRY_OFFSET=TYPE.offset("_entry");
    public HashtableBucket(long addr) {
        super(addr);
    }

    public <T extends BasicHashtableEntry> T getEntry(LongFunction<T> constructor){
        long addr=unsafe.getAddress(this.address+ENTRY_OFFSET);
        return addr==0L?null:constructor.apply(addr);
    }

    public void setEntry(BasicHashtableEntry entry){
        unsafe.putAddress(this.address+ENTRY_OFFSET,entry.address);
    }

    @Override
    public String toString() {
        return "HashtableBucket@0x"+Long.toHexString(this.address);
    }
}
