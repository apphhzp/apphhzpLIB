package apphhzp.lib.hotspot.gc.g1;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.memory.MemRegion;

import javax.annotation.Nullable;
import java.util.Iterator;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

//class G1HeapRegionTable : public G1BiasedMappedArray<HeapRegion*>;
public class G1HeapRegionTable extends G1BiasedMappedArray {

    public G1HeapRegionTable(long addr) {
        super(addr);
    }



    public HeapRegion getByIndex(long index){
        verifyIndex(index);
        long addr=unsafe.getAddress(this.base()+index*JVM.oopSize);
        return addr==0L?null:new HeapRegion(addr);
    }

    public void setByIndex(long index,@Nullable HeapRegion value) {
        verifyIndex(index);
        unsafe.putAddress(this.base()+index*JVM.oopSize,value==null?0L:value.address);
    }

    public HeapRegion getByAddress(long /*(HeapWord*)==(address**) */ value) {
        long biased_index = ((value) >>> this.shiftBy());
        this.verifyBiasedIndex(biased_index);
        long addr=unsafe.getAddress(this.biasedBase()+biased_index*JVM.oopSize);
        return addr==0L?null:new HeapRegion(addr);
    }

    public long/*(HeapRegion**)*/ getRefByIndex(long index){
        verifyIndex(index);
        return this.base()+index*JVM.oopSize;
    }

    // Return the index of the element of the given array that covers the given
    // word in the heap.
    public long getIndexByAddress(long/*(HeapWord*)==(address**) */ value) {
        long biased_index = ((value) >>> this.shiftBy());
        this.verifyBiasedIndex(biased_index);
        return biased_index - this.bias()*JVM.oopSize;
    }

    // Set the value of the array entry that corresponds to the given array.
    public void setByAddress(long/*(HeapWord*)==(address**) */ address,@Nullable HeapRegion value) {
        long biased_index = ((address) >>> this.shiftBy());
        this.verifyBiasedIndex(biased_index);
        unsafe.putAddress(this.biasedBase()+biased_index*JVM.oopSize,value==null?0L:value.address);
    }

    public void setByAddress(MemRegion range,@Nullable HeapRegion value) {
        long biased_start = ((range.start()) >>> this.shiftBy());
        long biased_last = ((range.last()) >>> this.shiftBy());
        this.verifyBiasedIndex(biased_start);
        this.verifyBiasedIndex(biased_last);
        for (long i = biased_start; i <= biased_last; i++) {
            unsafe.putAddress(this.biasedBase()+i*JVM.oopSize,value==null?0L:value.address);
        }
    }

    // Returns the address of the element the given address maps to
    long/*(HeapRegion**)*/ addressMappedTo(long/*(HeapWord*)==(address**) */ address) {
        long biased_index = ((address) >>> this.shiftBy());
        this.verifyBiasedIndexInclusiveEnd(biased_index);
        return this.biasedBase() + biased_index*JVM.oopSize;
    }

    long /*(HeapWord*)==(address**) */ bottomAddressMapped() {
        return (this.bias() << this.shiftBy());
    }

    // Return the highest address (exclusive) in the heap that this array covers.
    long /*(HeapWord*)==(address**) */ endAddressMapped() {
        return ((this.bias() + this.length()) << this.shiftBy());
    }

    private void verifyIndex(long index){
        if (!(index<this.length())){
            throw new IndexOutOfBoundsException("Index out of bounds index: "+index+" length: "+this.length());
        }
    }

    private void verifyBiasedIndex(long biased_index) {
        if (!(biased_index >= bias() && biased_index < (bias() + length()))){
            throw new IndexOutOfBoundsException("Biased index out of bounds, index: "+ biased_index+" bias: " +this.bias()+ " length: " +this.length());
        }
    }

    private void verifyBiasedIndexInclusiveEnd(long biased_index) {
        if (!(biased_index >= bias() && biased_index <= (bias() + length()))){
            throw new IndexOutOfBoundsException("Biased index out of inclusive bounds, index: "+biased_index+ " bias: " +this.bias()+ " length: "+this.length());
        }
    }

    private class HeapRegionIterator implements Iterator<HeapRegion> {
        private long index;
        private final long length;
        private HeapRegion next;

        public HeapRegion positionToNext() {
            HeapRegion result = next;
            while (index < length && G1HeapRegionTable.this.getByIndex(index) == null) {
                index++;
            }
            if (index < length) {
                next = G1HeapRegionTable.this.getByIndex(index);
                index++; // restart search at next element
            } else {
                next = null;
            }
            return result;
        }

        @Override
        public boolean hasNext() { return next != null;     }

        @Override
        public HeapRegion next() { return positionToNext(); }

        @Override
        public void remove()     { /* not supported */      }

        private HeapRegionIterator(long totalLength) {
            index = 0;
            length = totalLength;
            positionToNext();
        }
    }

    public Iterator<HeapRegion> heapRegionIterator(long committedLength) {
        return new HeapRegionIterator(committedLength);
    }

    public void clear() {
        for (long i = 0,base=this.base(); i < length(); i++) {
            unsafe.putAddress(base+i*JVM.oopSize,0L);
        }
    }

    @Override
    public String toString() {
        return "G1HeapRegionTable@0x"+Long.toHexString(this.address);
    }
}
