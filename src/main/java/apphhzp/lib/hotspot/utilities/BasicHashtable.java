package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.util.function.LongFunction;

import static apphhzp.lib.ClassHelper.unsafe;

public class BasicHashtable extends JVMObject {
    public static final Type TYPE= JVM.type("BasicHashtable<mtInternal>");
    public static final int SIZE=TYPE.size;
    public static final long TABLE_SIZE_OFFSET=TYPE.offset("_table_size");
    public static final long BUCKETS_OFFSET=TYPE.offset("_buckets");
    public static final long ENTRY_SIZE_OFFSET=BUCKETS_OFFSET+JVM.oopSize;
    public static final long NUMBER_OF_ENTRIES_OFFSET=ENTRY_SIZE_OFFSET+JVM.intSize;

    public BasicHashtable(long addr) {
        super(addr);
    }

    public int getTableSize(){
        return unsafe.getInt(this.address+TABLE_SIZE_OFFSET);
    }

    public void setTableSize(int size){
        unsafe.putInt(this.address+TABLE_SIZE_OFFSET, size);
    }

    public int entrySize(){
        return unsafe.getInt(this.address+ENTRY_SIZE_OFFSET);
    }

    public void setEntrySize(int size){
        unsafe.putInt(this.address+ENTRY_SIZE_OFFSET, size);
    }

    public int getNumberOfEntries(){
        return unsafe.getInt(this.address+NUMBER_OF_ENTRIES_OFFSET);
    }

    public void setNumberOfEntries(int size){
        unsafe.putInt(this.address+NUMBER_OF_ENTRIES_OFFSET, size);
    }


    public <T extends BasicHashtableEntry> T bucket(int index){
        if (index<0||index>=this.getTableSize()){
            throw new IllegalArgumentException("Invalid bucket id");
        }
        long addr=unsafe.getAddress(this.address+BUCKETS_OFFSET);
        addr+= (long) index *HashtableBucket.SIZE;
        return (T) new HashtableBucket(addr).getEntry(this.getHashtableEntryConstructor());
    }

    public void freeEntry(BasicHashtableEntry entry){
        this.unlinkEntry(entry);
    }

    public void unlinkEntry(BasicHashtableEntry entry) {
        entry.setNext(null);
        this.setNumberOfEntries(this.getNumberOfEntries()-1);
    }

    public void addEntry(int index, BasicHashtableEntry entry) {
        entry.setNext(bucket(index));
        HashtableBucket bucket=new HashtableBucket(unsafe.getAddress(this.address+BUCKETS_OFFSET)+(long) index*HashtableBucket.SIZE);
        bucket.setEntry(entry);
        this.setNumberOfEntries(this.getNumberOfEntries()+1);
    }



    public LongFunction<? extends BasicHashtableEntry> getHashtableEntryConstructor() {
        return BasicHashtableEntry::new;
    }

    @Override
    public String toString() {
        return "BasicHashtable@0x"+Long.toHexString(this.address);
    }
}
