package apphhzp.lib.hotspot.gc;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.util.RawCType;

import java.util.function.Predicate;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class OopStorage extends JVMObject {
    public static final Type TYPE= JVM.type("OopStorage");
    public static final int SIZE= TYPE.size;
    public static final long NAME_OFFSET=0;
    public static final long ACTIVE_ARRAY_OFFSET=JVM.computeOffset(JVM.oopSize,NAME_OFFSET+JVM.oopSize);
    public static final long ALLOCATION_LIST_OFFSET=JVM.computeOffset(JVM.oopSize,ACTIVE_ARRAY_OFFSET+JVM.oopSize);
    public final AllocationList allocation_list;
    public OopStorage(long addr) {
        super(addr);
        allocation_list=new AllocationList(addr+ALLOCATION_LIST_OFFSET);
    }
    public String name() {
        return JVM.getStringRef(this.address+NAME_OFFSET);
    }
    public ActiveArray active_array(){
        return new ActiveArray(unsafe.getAddress(this.address+ACTIVE_ARRAY_OFFSET));
    }

    public static boolean iterate_impl(Predicate<Oop> f, OopStorage storage){
        ActiveArray blocks = storage.active_array();
        @RawCType("size_t")long limit = blocks.block_count();
        for(@RawCType("size_t")long i = 0; Long.compareUnsigned(i,limit) < 0; ++i) {
            Block block = blocks.at(i);
            if (!block.iterate(f)) {
                return false;
            }
        }
        return true;
    }
    public void oops_do(OopClosure cl) {
        iterate_impl(oop -> {
            cl.do_oop(oop);
            return true;
        }, this);
    }

    public static class ActiveArray extends JVMObject {
        public static final long SIZE_OFFSET=0;
        public static final long BLOCK_COUNT_OFFSET=JVM.computeOffset(JVM.size_tSize,SIZE_OFFSET+JVM.size_tSize);
        public static final long REFCOUNT_OFFSET=JVM.computeOffset(JVM.intSize,BLOCK_COUNT_OFFSET+JVM.size_tSize);
        public static final int SIZE= (int) JVM.computeOffset(Math.max(JVM.size_tSize,JVM.intSize),REFCOUNT_OFFSET+JVM.intSize);
        public ActiveArray(long addr) {
            super(addr);
        }

        public @RawCType("size_t") long size(){
            return JVM.getSizeT(this.address+SIZE_OFFSET);
        }

        public @RawCType("size_t") long block_count(){
            return JVM.getSizeT(this.address+BLOCK_COUNT_OFFSET);
        }

//        public void increment_refcount(){
//            int new_value = Atomic::add(&_refcount, 1);
//            assert(new_value >= 1, "negative refcount %d", new_value - 1);
//        }

//        bool OopStorage::ActiveArray::decrement_refcount() const {
//            int new_value = Atomic::sub(&_refcount, 1);
//            assert(new_value >= 0, "negative refcount %d", new_value);
//            return new_value == 0;
//        }
        public static @RawCType("size_t") long blocks_offset() {
            return JVM.alignUp(SIZE, JVM.oopSize);
        }

        public @RawCType("OopStorage::Block* const*") long base_ptr(){
            @RawCType("const void*") long ptr = (this.address) + blocks_offset();
            return (ptr);
        }

        public @RawCType("OopStorage::Block**") long block_ptr(@RawCType("size_t") long index) {
            return base_ptr()+ index*JVM.oopSize;
        }

        public OopStorage.Block at(@RawCType("size_t") long index){
            if (Long.compareUnsigned(index,block_count()) >=0){
                throw new IndexOutOfBoundsException("precondition");
            }
            long addr=unsafe.getAddress(block_ptr(index));
            if (addr==0L){
                return null;
            }
            return new Block(addr);
        }
    }
    /** Fixed-sized array of oops, plus bookkeeping data.
     * All blocks are in the storage's _active_array, at the block's _active_index.
     * Non-full blocks are in the storage's _allocation_list, linked through the
     * block's _allocation_list_entry.  Empty blocks are at the end of that list.*/
    public static class Block extends JVMObject {
        public static final long DATA_OFFSET=0;
        public static final long ALLOCATED_BITMASK_OFFSET=JVM.computeOffset(JVM.oopSize,DATA_OFFSET+ (long) JVM.oopSize *JVM.BitsPerWord);
        public static final long OWNER_ADDRESS_OFFSET=JVM.computeOffset(JVM.oopSize,ALLOCATED_BITMASK_OFFSET+JVM.oopSize);
        public static final long MEMORY_OFFSET=JVM.computeOffset(JVM.oopSize,OWNER_ADDRESS_OFFSET+JVM.oopSize);
        public static final long ACTIVE_INDEX_OFFSET=JVM.computeOffset(JVM.size_tSize,MEMORY_OFFSET+JVM.oopSize);
        public static final long ALLOCATION_LIST_ENTRY_OFFSET=JVM.computeOffset(JVM.oopSize,ACTIVE_INDEX_OFFSET+JVM.size_tSize);
        public static final long DEFERRED_UPDATES_NEXT_OFFSET=JVM.computeOffset(JVM.oopSize,ALLOCATION_LIST_ENTRY_OFFSET+AllocationListEntry.SIZE);
        public static final long RELEASE_REFCOUNT_OFFSET=JVM.computeOffset(JVM.oopSize,DEFERRED_UPDATES_NEXT_OFFSET+JVM.oopSize);
        public static final int SIZE= (int) JVM.computeOffset(Math.max(JVM.oopSize,JVM.size_tSize),RELEASE_REFCOUNT_OFFSET+JVM.oopSize);

        public final AllocationListEntry allocation_list_entry;
        public Block(long addr) {
            super(addr);
            if (addr==0L){
                throw new NullPointerException();
            }
            allocation_list_entry=new AllocationListEntry(this.address+ALLOCATION_LIST_ENTRY_OFFSET);
        }

        private void check_index(@RawCType("unsigned")int index){
            if (Integer.compareUnsigned(index,JVM.BitsPerWord)>=0){
                throw new IndexOutOfBoundsException("Index out of bounds: "+Integer.toUnsignedString(index));
            }
        }

        public Oop get_pointer(@RawCType("unsigned")int index) {
            check_index(index);
            return new Oop(this.address+DATA_OFFSET+ (long) index *JVM.oopSize);
        }


        public Block deferred_updates_next(){
            long addr=unsafe.getAddress(this.address+DEFERRED_UPDATES_NEXT_OFFSET);
            if (addr==0L){
                return null;
            }
            return new Block(addr);
        }
        public void set_deferred_updates_next(Block block){
            unsafe.putAddress(this.address+DEFERRED_UPDATES_NEXT_OFFSET,block==null?0L:block.address);
        }
        public boolean contains(Oop ptr){
            return contains(ptr.address);
        }
        public boolean contains(@RawCType("oop*")long ptr){
            long base = get_pointer(0).address;
            return (Long.compareUnsigned(base,ptr) <= 0) && (Long.compareUnsigned(ptr,base + (long) JVM.BitsPerWord *JVM.oopSize) < 0);
        }

        public @RawCType("size_t")long active_index() {
            return JVM.getSizeT(this.address+ACTIVE_INDEX_OFFSET);
        }

        public void set_active_index(@RawCType("size_t")long index) {
            JVM.putSizeT(this.address+ACTIVE_INDEX_OFFSET,index);
        }
        public @RawCType("unsigned")int get_index(Oop ptr){
            return get_index(ptr.address);
        }
        public @RawCType("unsigned")int get_index(@RawCType("oop*") long ptr){
            if (!contains(ptr)){
                throw new IllegalArgumentException("0x"+Long.toHexString(ptr)+" not in block 0x"+Long.toHexString(this.address));
            }
            return (int) ((ptr-get_pointer(0).address)/JVM.oopSize);
        }
        public @RawCType("uintx")long allocated_bitmask(){
            return unsafe.getAddress(this.address+ALLOCATED_BITMASK_OFFSET);
        }
        public @RawCType("uintx")long bitmask_for_index(@RawCType("unsigned")int index){
            check_index(index);
            return 1L << Integer.toUnsignedLong(index);
        }
        public static boolean iterate_impl(Predicate<Oop> f, Block block) {
            @RawCType("uintx")long bitmask = block.allocated_bitmask();
            while (bitmask != 0) {
                @RawCType("unsigned")int index = Long.numberOfTrailingZeros(bitmask);
                bitmask ^= block.bitmask_for_index(index);
                if (!f.test(block.get_pointer(index))) {
                    return false;
                }
            }
            return true;
        }
        public boolean iterate(Predicate<Oop> f) {
            return iterate_impl(f, this);
        }
    }
    public static class AllocationListEntry extends JVMObject{
        public static final long PREV_OFFSET=0;
        public static final long NEXT_OFFSET=JVM.computeOffset(JVM.oopSize,PREV_OFFSET+JVM.oopSize);
        public static final int SIZE= (int) JVM.computeOffset(JVM.oopSize,NEXT_OFFSET+JVM.oopSize);
        public AllocationListEntry(long addr) {
            super(addr);
        }
        public Block prev(){
            long addr=unsafe.getAddress(this.address+PREV_OFFSET);
            if (addr==0L){
                return null;
            }
            return new Block(addr);
        }
        public Block next(){
            long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
            if (addr==0L){
                return null;
            }
            return new Block(addr);
        }
        public void set_prev(Block val){
            unsafe.putAddress(this.address+PREV_OFFSET,val==null?0L:val.address);
        }
        public void set_next(Block val){
            unsafe.putAddress(this.address+NEXT_OFFSET,val==null?0L:val.address);
        }
    }
    /** Doubly-linked list of Blocks.  For all operations with a block
     * argument, the block must be from the list's OopStorage.*/
    public static class AllocationList extends JVMObject{
        public static final long HEAD_OFFSET=0;
        public static final long TAIL_OFFSET=JVM.computeOffset(JVM.oopSize,HEAD_OFFSET+JVM.oopSize);
        public static final int SIZE= (int) JVM.computeOffset(JVM.oopSize,TAIL_OFFSET+JVM.oopSize);
        public AllocationList(long addr) {
            super(addr);
        }
        public Block head() {
            long addr=unsafe.getAddress(this.address+HEAD_OFFSET);
            if (addr==0L){
                return null;
            }
            return new Block(addr);
        }

        public Block tail() {
            long addr=unsafe.getAddress(this.address+TAIL_OFFSET);
            if (addr==0L){
                return null;
            }
            return new Block(addr);
        }

        private void set_head(Block val) {
            unsafe.putAddress(this.address+HEAD_OFFSET,val==null?0L:val.address);
        }

        private void set_tail(Block val) {
            unsafe.putAddress(this.address+TAIL_OFFSET,val==null?0L:val.address);
        }

        public Block prev(Block block) {
            return (block.allocation_list_entry.prev());
        }

        public Block next(Block block) {
            return (block.allocation_list_entry.next());
        }

        public void push_front(Block block) {
            Block old = head();
            if (old == null){
                if (tail()!=null){
                    throw new RuntimeException("invariant");
                }
                set_head(block);
                set_tail(block);
            } else {
                block.allocation_list_entry.set_next(old);
                old.allocation_list_entry.set_prev(block);
                set_head(block);
            }
        }

        public void push_back( Block block) {
            Block old=tail();
            if (old==null){
                if (head()!=null){
                    throw new RuntimeException("invariant");
                }
                set_head(block);
                set_tail(block);
            }else{
                old.allocation_list_entry.set_next(block);
                block.allocation_list_entry.set_prev(old);
                set_tail(block);
            }
        }

        public void unlink(Block block) {
            AllocationListEntry block_entry = block.allocation_list_entry;
            Block prev_blk = block_entry.prev();
            Block next_blk = block_entry.next();
            block_entry.set_prev(null);
            block_entry.set_next(null);
            if ((prev_blk == null) && (next_blk == null)){
                if (!head().equals(block)){
                    throw new RuntimeException("invariant");
                }
                if (!tail().equals(block)){
                    throw new RuntimeException("invariant");
                }
                set_head(null);
                set_tail(null);
            } else if (prev_blk == null) {
                if (!head().equals(block)){
                    throw new RuntimeException("invariant");
                }
                next_blk.allocation_list_entry.set_prev(null);
                set_head(next_blk);
            } else if (next_blk == null) {
                if (!tail().equals(block)){
                    throw new RuntimeException("invariant");
                }
                prev_blk.allocation_list_entry.set_next(null);
                set_tail(prev_blk);
            } else {
                next_blk.allocation_list_entry.set_prev(prev_blk);
                prev_blk.allocation_list_entry.set_next(next_blk);
            }
        }

        public boolean contains(Block block){
            return (next(block) != null) || (tail().equals(block));
        }
    }
}
