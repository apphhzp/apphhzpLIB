package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class PerfDataPrologue extends JVMObject implements Iterable<PerfDataEntry>{
    public static final Type TYPE= JVM.type("PerfDataPrologue");
    public static final int SIZE=TYPE.size;
    public static final long MAGIC_OFFSET=TYPE.offset("magic");
    public static final long BYTE_ORDER_OFFSET=TYPE.offset("byte_order");
    public static final long MAJOR_VERSION_OFFSET=TYPE.offset("major_version");
    public static final long MINOR_VERSION_OFFSET=TYPE.offset("minor_version");
    public static final long ACCESSIBLE_OFFSET=TYPE.offset("accessible");
    public static final long USED_OFFSET=TYPE.offset("used");
    public static final long OVERFLOW_OFFSET=TYPE.offset("overflow");
    public static final long MOD_TIME_STAMP_OFFSET=TYPE.offset("mod_time_stamp");
    public static final long ENTRY_OFFSET_OFFSET=TYPE.offset("entry_offset");
    public static final long NUM_ENTRIES_OFFSET=TYPE.offset("num_entries");
    public PerfDataPrologue(long addr) {
        super(addr);
    }

    public int getMagic(){
        return unsafe.getInt(this.address+MAGIC_OFFSET);
    }

    public void setMagic(int magic){
        unsafe.putInt(this.address+MAGIC_OFFSET,magic);
    }

    public byte getByteOrder(){
        return unsafe.getByte(this.address+BYTE_ORDER_OFFSET);
    }

    public void setByteOrder(byte order){
        unsafe.putByte(this.address+BYTE_ORDER_OFFSET,order);
    }

    public byte getMajorVersion(){
        return unsafe.getByte(this.address+MAJOR_VERSION_OFFSET);
    }

    public void setMajorVersion(byte version){
        unsafe.putByte(this.address+MAJOR_VERSION_OFFSET,version);
    }

    public byte getMinorVersion(){
        return unsafe.getByte(this.address+MINOR_VERSION_OFFSET);
    }

    public void setMinorVersion(byte version){
        unsafe.putByte(this.address+MINOR_VERSION_OFFSET,version);
    }

    public boolean getAccessible(){
        return unsafe.getByte(this.address+ACCESSIBLE_OFFSET)!=0;
    }

    public void setAccessible(boolean accessible) {
        unsafe.putByte(this.address+ACCESSIBLE_OFFSET, (byte) (accessible?1:0));
    }

    public int getUsed(){
        return unsafe.getInt(this.address+USED_OFFSET);
    }

    public void setUsed(int used) {
        unsafe.putInt(this.address+USED_OFFSET,used);
    }

    public int getOverflow(){
        return unsafe.getInt(this.address+OVERFLOW_OFFSET);
    }

    public void setOverflow(int overflow) {
        unsafe.putInt(this.address+OVERFLOW_OFFSET,overflow);
    }

    public long getModTimeStamp(){
        return unsafe.getLong(this.address+MOD_TIME_STAMP_OFFSET);
    }

    public void setModTimeStamp(long timeStamp) {
        unsafe.putLong(this.address+MOD_TIME_STAMP_OFFSET,timeStamp);
    }

    public int getEntryOffset(){
        return unsafe.getInt(this.address+ENTRY_OFFSET_OFFSET);
    }

    public void setEntryOffset(int offset) {
        unsafe.putInt(this.address+ENTRY_OFFSET_OFFSET,offset);
    }

    public int getNumEntries(){
        return unsafe.getInt(this.address+NUM_ENTRIES_OFFSET);
    }

    public void setNumEntries(int numEntries) {
        unsafe.putInt(this.address+NUM_ENTRIES_OFFSET,numEntries);
    }
    @Override
    public String toString() {
        return "PerfDataPrologue@0x"+Long.toHexString(this.address);
    }

    @Override
    public Iterator<PerfDataEntry> iterator() {
        return new Iterator<>() {
            private final int num = PerfDataPrologue.this.getNumEntries();
            private final long addr = PerfDataPrologue.this.address;
            private int off = PerfDataPrologue.this.getEntryOffset();
            private int cnt=0;
            @Override
            public boolean hasNext() {
                return cnt<num;
            }

            @Override
            public PerfDataEntry next() {
                if (!hasNext()){
                    throw new NoSuchElementException();
                }
                PerfDataEntry re=new PerfDataEntry(addr+off);
                off+=re.entryLength();
                ++cnt;
                return re;
            }
        };
    }
}
