package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.gc.OopClosure;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.runtime.JNIHandleBlock.SomeConstants.block_size_in_oops;

public class JNIHandleBlock extends JVMObject {
    public static final Type TYPE= JVM.type("JNIHandleBlock");
    public static final int SIZE= TYPE.size;
    public static final long HANDLES_OFFSET=TYPE.offset("_handles");
    public static final long TOP_OFFSET=TYPE.offset("_top");
    public static final long NEXT_OFFSET=TYPE.offset("_next");
    public static final long LAST_OFFSET=JVM.computeOffset(JVM.oopSize,NEXT_OFFSET+JVM.oopSize);
    public static final long POP_FRAME_LINK_OFFSET=JVM.computeOffset(JVM.oopSize,LAST_OFFSET+JVM.oopSize);
    public static final class SomeConstants {
        // Number of handles per handle block
        public static final int block_size_in_oops  = JVM.intConstant("JNIHandleBlock::block_size_in_oops");
    }
    public JNIHandleBlock(long addr) {
        super(addr);
    }
    // No more handles in the both the current and following blocks
    public void clear(){
        unsafe.putInt(this.address+TOP_OFFSET,0);
    }

    public JNIHandleBlock next(){
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        return new JNIHandleBlock(addr);
    }
    public void oops_do(OopClosure f) {
        JNIHandleBlock current_chain = this;
        // Iterate over chain of blocks, followed by chains linked through the
        // pop frame links.
        while (current_chain != null) {
            for (JNIHandleBlock current = current_chain; current != null; current = current.next()) {
                if (!(current == current_chain || current.pop_frame_link() == null)){
                    throw new RuntimeException("only blocks first in chain should have pop frame link set");
                }
                for (int index = 0,maxi=current.top(); index < maxi; index++) {
                    @RawCType("uintptr_t*")long addr = current.address+HANDLES_OFFSET+ (long) index *JVM.oopSize;
                    @RawCType("uintptr_t")long value = unsafe.getAddress(addr);
                    // traverse heap pointers only, not deleted handles or free list pointers
                    if (value != 0 && !is_tagged_free_list(value)) {
                        Oop root = new Oop(addr);
                        f.do_oop(root);
                    }
                }
                // the next handle block is valid only if current block is full
                if (current.top() < block_size_in_oops) {
                    break;
                }
            }
            current_chain = current_chain.pop_frame_link();
        }
    }

    public static boolean is_tagged_free_list(@RawCType("uintptr_t") long value) {
        return (value & 1) != 0;
    }

    public static @RawCType("uintptr_t") long tag_free_list(@RawCType("uintptr_t") long value) {
        return value | 1;
    }

    public static @RawCType("uintptr_t") long untag_free_list(@RawCType("uintptr_t") long value) {
        return value & ~1;
    }

    public JNIHandleBlock pop_frame_link(){
        long addr=unsafe.getAddress(this.address+POP_FRAME_LINK_OFFSET);
        if (addr==0L){
            return null;
        }
        return new JNIHandleBlock(addr);
    }

    public int top(){
        return unsafe.getInt(this.address+TOP_OFFSET);
    }
    public boolean contains(@RawCType("jobject")long handle){
        return (this.address+HANDLES_OFFSET<= handle && handle<this.address+HANDLES_OFFSET+ (long) JVM.oopSize *top());
    }
    public boolean chain_contains(@RawCType("jobject") long handle){
        for (JNIHandleBlock current = this; current != null; current = current.next()) {
            if (current.contains(handle)) {
                return true;
            }
        }
        return false;
    }


    public @RawCType("size_t") long length(){
        long result = 1;
        for(JNIHandleBlock current = next(); current != null; current = current.next()) {
            result++;
        }
        return result;
    }

    private static class CountJNIHandleClosure implements OopClosure {
        private int _count;
        public CountJNIHandleClosure(){
            _count=0;
        }
        public void do_oop(Oop ooph){
            _count++;
        }
        public int count() { return _count; }
    };

    public @RawCType("size_t") long get_number_of_live_handles() {
        CountJNIHandleClosure counter=new CountJNIHandleClosure();
        oops_do(counter);
        return counter.count();
    }

    // This method is not thread-safe, i.e., must be called while holding a lock on the structure.
    public @RawCType("size_t") long memory_usage(){
        return length()*SIZE;
    }
}
