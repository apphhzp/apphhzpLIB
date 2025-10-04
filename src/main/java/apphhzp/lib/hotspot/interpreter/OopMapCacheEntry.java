package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.CellTypeStateList;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.signature.NativeSignatureIterator;
import apphhzp.lib.hotspot.util.RawCType;

public class OopMapCacheEntry extends InterpreterOopMap {
    private OopMapCacheEntry _next;

    // Initialization
    protected void fill(Method method, int bci){
        // Flush entry to deallocate an existing entry
        flush();
        set_method(method);
        set_bci(bci);
        if (method.is_native()) {
            // Native method activations have oops only among the parameters and one
            // extra oop following the parameters (the mirror for static native methods).
            fill_for_native(method);
        } else {
            OopMapForCacheEntry gen=new OopMapForCacheEntry(method, bci, this);
            if (!gen.compute_map()) {
                throw new RuntimeException("Unrecoverable verification or out-of-memory error");
            }
        }
    }
    // fills the bit mask for native calls
    protected void fill_for_native(Method method){
        if (!method.is_native()){
            throw new RuntimeException("method must be native method");
        }
        set_mask_size(method.size_of_parameters() * bits_per_entry);
        allocate_bit_mask();
        // fill mask for parameters
        MaskFillerForNative mf=new MaskFillerForNative(method, bit_mask(), mask_size());
        mf.generate();
    }
    private static class MaskFillerForNative extends NativeSignatureIterator {

        private @RawCType("uintptr_t *")long[] _mask;                             // the bit mask to be filled
        private int _size;                             // the mask size in bits

        private void set_one(int i) {
            i *= InterpreterOopMap.bits_per_entry;
            if (!(0 <= i && i < _size)){
                throw new IndexOutOfBoundsException("offset out of bounds");
            }
            _mask[i / JVM.BitsPerWord] |= ((1L << InterpreterOopMap.oop_bit_number) << (i % JVM.BitsPerWord));
        }


        public void pass_byte()                               { /* ignore */ }
        public void pass_short()                              { /* ignore */ }
        public void pass_int()                                { /* ignore */ }
        public void pass_long()                               { /* ignore */ }
        public void pass_float()                              { /* ignore */ }
        public void pass_double()                             { /* ignore */ }
        public void pass_object()                             { set_one(offset()); }

        public MaskFillerForNative(Method method,@RawCType("uintptr_t*")long[] mask, int size){
            super(method);
            _mask   = mask;
            _size   = size;
            // initialize with 0
            int i = (size + JVM.BitsPerWord - 1) / JVM.BitsPerWord;
            while (i-- > 0) _mask[i] = 0;
        }

        public void generate() {
            iterate();
        }
    }
    protected void set_mask(CellTypeStateList vars, CellTypeStateList stack, int stack_top){
        // compute bit mask size
        int max_locals = method().max_locals();
        int n_entries = max_locals + stack_top;
        set_mask_size(n_entries * bits_per_entry);
        allocate_bit_mask();
        set_expression_stack_size(stack_top);

        // compute bits
        int word_index = 0;
        @RawCType("uintptr_t")long value = 0;
        @RawCType("uintptr_t")long mask = 1;

        CellTypeStateList cell = vars;
        for (int entry_index = 0,idx=0; entry_index < n_entries; entry_index++, mask <<= bits_per_entry, idx++) {
            // store last word
            if (mask == 0) {
                bit_mask()[word_index++] = value;
                value = 0;
                mask = 1;
            }

            // switch to stack when done with locals
            if (entry_index == max_locals) {
                cell = stack;
                idx=0;
            }

            // set oop bit
            if ( cell.get(idx).is_reference()) {
                value |= (mask << oop_bit_number );
            }

            // set dead bit
            if (!cell.get(idx).is_live()) {
                value |= (mask << dead_bit_number);
                if (cell.get(idx).is_reference()){
                    throw new RuntimeException("dead value marked as oop");
                }
            }
        }

        // make sure last word is stored
        bit_mask()[word_index] = value;

        // verify bit mask
//        assert(verify_mask(vars, stack, max_locals, stack_top), "mask could not be verified");
    }

    // Deallocate bit masks and initialize fields
    protected void flush(){
        deallocate_bit_mask();
        initialize();
    }

    // allocates the bit mask on C heap f necessary
    private void allocate_bit_mask(){
        if (mask_size() > small_mask_limit) {
            if (_bit_mask!=null){
                throw new RuntimeException("bit mask should be new or just flushed");
            }
            _bit_mask=new long[(int) mask_word_size()];
        }
    }
    // allocates the bit mask on C heap f necessary
    private void deallocate_bit_mask(){
        if (mask_size() > small_mask_limit && _bit_mask[0] != 0) {
        }
    }

    public OopMapCacheEntry(){
        _next = null;
    }
}
