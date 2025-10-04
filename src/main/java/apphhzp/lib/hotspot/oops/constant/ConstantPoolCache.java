package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.U2Array;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.util.RawCType;
import it.unimi.dsi.fastutil.ints.IntList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class ConstantPoolCache extends JVMObject implements Iterable<ConstantPoolCacheEntry> {
    public static final Type TYPE = JVM.type("ConstantPoolCache");
    public static final int SIZE = TYPE.size;
    public static final long LENGTH_OFFSET = TYPE.offset("_length");
    public static final long CONSTANT_POOL_OFFSET = TYPE.offset("_constant_pool");
    public static final long RESOLVED_REFERENCES_OFFSET = TYPE.offset("_resolved_references");
    public static final long REFERENCE_MAP_OFFSET = TYPE.offset("_reference_map");
    public static final long ARCHIVED_REFERENCES_INDEX_OFFSET=JVM.computeOffset(JVM.intSize,REFERENCE_MAP_OFFSET+JVM.oopSize);
    private ConstantPool holderCache;
    private OopDesc resolvedReferencesCache;
    private U2Array referenceMapCache;
    private ConstantPoolCacheEntry[] entries;


    public static ConstantPoolCache allocate(
            @RawCType("intStack&") IntList index_map,
            @RawCType("intStack&") IntList invokedynamic_index_map,
            @RawCType("intStack&") IntList invokedynamic_map) {

        final int length = index_map.size() + invokedynamic_index_map.size();
        int size = ConstantPoolCache.size(length);
        long addr=unsafe.allocateMemory((long) size *JVM.wordSize);
        unsafe.setMemory(addr,(long) size *JVM.wordSize, (byte) 0);
        ConstantPoolCache re=new ConstantPoolCache(addr);//(length, index_map, invokedynamic_index_map, invokedynamic_map)
        re.setLength(length);
        re.set_constant_pool(null);
        if (JVM.includeCDSJavaHeap){
            unsafe.putInt(addr+ARCHIVED_REFERENCES_INDEX_OFFSET,-1);
        }
        re.initialize(index_map, invokedynamic_index_map,
                invokedynamic_map);
//        for (int i = 0; i < length; i++) {
//            assert(entry_at(i)->is_f1_null(), "Failed to clear?");
//
//        }
        return re;
    }

    public void initialize(@RawCType("intStack&") IntList inverse_index_map,
                           @RawCType("intStack&") IntList invokedynamic_inverse_index_map,
                           @RawCType("intStack&") IntList invokedynamic_references_map) {
        for (int i = 0; i < inverse_index_map.size(); i++) {
            ConstantPoolCacheEntry e = this.entry_at(i);
            int original_index = inverse_index_map.getInt(i);
            e.initialize_entry(original_index);
//            assert(entry_at(i) == e, "sanity");
        }

        // Append invokedynamic entries at the end
        int invokedynamic_offset = inverse_index_map.size();
        for (int i = 0; i < invokedynamic_inverse_index_map.size(); i++) {
            int offset = i + invokedynamic_offset;
            ConstantPoolCacheEntry e = entry_at(offset);
            int original_index = invokedynamic_inverse_index_map.getInt(i);
            e.initialize_entry(original_index);
//            assert(entry_at(offset) == e, "sanity");
        }
        for (int ref = 0; ref < invokedynamic_references_map.size(); ref++) {
            final int cpci = invokedynamic_references_map.getInt(ref);
            if (cpci >= 0) {
                entry_at(cpci).initialize_resolved_reference_index(ref);
            }
        }
    }

    public ConstantPoolCache(long addr) {
        super(addr);
        this.entries = new ConstantPoolCacheEntry[this.getLength()];
    }

    public int getLength() {
        return unsafe.getInt(this.address + LENGTH_OFFSET);
    }

    public void setLength(int length) {
        unsafe.putInt(this.address + LENGTH_OFFSET, length);
    }

    public ConstantPool getConstantPool() {
        long addr = unsafe.getAddress(this.address + CONSTANT_POOL_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.holderCache, addr)) {
            this.holderCache = ConstantPool.getOrCreate(addr);
        }
        return this.holderCache;
    }

    public void set_constant_pool(ConstantPool pool) {
        unsafe.putAddress(this.address + CONSTANT_POOL_OFFSET,pool==null?0L: pool.address);
    }

    @Nullable
    public Object[] getResolvedReferences() {
        long addr = OopDesc.fromOopHandle(this.address + RESOLVED_REFERENCES_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.resolvedReferencesCache, addr)) {
            this.resolvedReferencesCache = OopDesc.of(addr);
        }
        return this.resolvedReferencesCache.getObject();
    }

    public void setResolvedReferences(OopDesc val) {
        unsafe.putAddress(unsafe.getAddress(this.address + RESOLVED_REFERENCES_OFFSET), val.address);
    }

    public U2Array getReferenceMap() {
        long addr = unsafe.getAddress(this.address + REFERENCE_MAP_OFFSET);
        if (!isEqual(this.referenceMapCache, addr)) {
            this.referenceMapCache = new U2Array(addr);
        }
        return this.referenceMapCache;
    }

    public void set_reference_map(U2Array array) {
        unsafe.putAddress(this.address + REFERENCE_MAP_OFFSET, array.address);
    }

    public int objectToCpcIndex(int index) {
        return this.getReferenceMap().get(index);
    }

    public int cpcToObjectIndex(int which) {
        return this.getReferenceMap().find((short) (which & 0xffff));
    }

    public ConstantPoolCacheEntry entry_at(int index) {
        if (JVM.ENABLE_EXTRA_CHECK && (index < 0 || index >= this.getLength())) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (index >= entries.length) {
            entries = Arrays.copyOf(entries, this.getLength());
        }
        return entries[index] == null ? entries[index] = new ConstantPoolCacheEntry(this.address + SIZE + (long) index * ConstantPoolCacheEntry.SIZE, this) : entries[index];
    }

    public void clearResolvedCacheEntry() {
        int len = this.getLength();
        for (int i = 0; i < len; i++) {
            long val = unsafe.getAddress(this.address + SIZE + (long) i * ConstantPoolCacheEntry.SIZE);
            unsafe.putAddress(this.address + SIZE + (long) i * ConstantPoolCacheEntry.SIZE, val & 0xFFFFL);
        }
    }

    private static int header_size() {
        return ConstantPoolCache.SIZE / JVM.wordSize;
    }

    private static int size(int length) {
        return (int) JVM.align_metadata_size(header_size() + (long) length * (ConstantPoolCacheEntry.size()));
    }

    public int size() {
        return size(this.getLength());
    }

    @Override
    public String toString() {
        return "ConstantPoolCache@0x" + Long.toHexString(this.address);
    }

    @Nonnull
    @Override
    public Iterator<ConstantPoolCacheEntry> iterator() {
        if (this.getLength() >= entries.length) {
            entries = Arrays.copyOf(entries, this.getLength());
        }
        return new Iterator<>() {
            private final int len = ConstantPoolCache.this.getLength();
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < len;
            }

            @Override
            public ConstantPoolCacheEntry next() {
                if (index >= len) {
                    throw new NoSuchElementException();
                }
                ConstantPoolCacheEntry re = entries[index] == null ? entries[index] = new ConstantPoolCacheEntry(ConstantPoolCache.this.address + SIZE + (long) index * ConstantPoolCacheEntry.SIZE, ConstantPoolCache.this) : entries[index];
                ++index;
                return re;
            }
        };
    }
}
