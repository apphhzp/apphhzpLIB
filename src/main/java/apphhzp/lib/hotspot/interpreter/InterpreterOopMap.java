package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import java.util.Arrays;

public class InterpreterOopMap {
    public static final int
                N = 4,                // the number of words reserved
                // for inlined mask storage
                small_mask_limit = N * JVM.BitsPerWord,  // the maximum number of bits
                // available for small masks,
                // small_mask_limit can be set to 0
                // for testing bit_mask allocation

                bits_per_entry   = 2,
                dead_bit_number  = 1,
                oop_bit_number   = 0;

    private Method _method;         // the method for which the mask is valid
    private @RawCType("unsigned short")int _bci;            // the bci    for which the mask is valid
    private int            _mask_size;      // the mask size in bits
    private int            _expression_stack_size; // the size of the expression stack in slots
    // the bit mask if
    // mask_size <= small_mask_limit,
    // ptr to bit mask otherwise
    // "protected" so that sub classes can
    // access it without using trickery in
    // methd bit_mask().
    protected @RawCType("intptr_t[]")long[]       _bit_mask;
    // access methods
    protected Method method(){ return _method; }
    protected void           set_method(Method v){ _method = v; }
    protected int            bci(){ return _bci; }
    protected void           set_bci(int v){ _bci = v; }
    protected int            mask_size(){ return _mask_size; }
    protected void           set_mask_size(int v){ _mask_size = v; }
    // Test bit mask size and return either the in-line bit mask or allocated
    // bit mask.
    protected @RawCType("uintptr_t*")long[] bit_mask() {
        return _bit_mask;
    }

    // return the word size of_bit_mask.  mask_size() <= 4 * MAX_USHORT
    protected @RawCType("size_t")long mask_word_size() {
        return (mask_size() + JVM.BitsPerWord - 1) / JVM.BitsPerWord;
    }

    protected @RawCType("uintptr_t")long entry_at(int offset){
        int i = offset * bits_per_entry;
        return _bit_mask[i / JVM.BitsPerWord] >>> (i % JVM.BitsPerWord);
    }

    protected void set_expression_stack_size(int sz)         { _expression_stack_size = sz; }

    // Lookup
    protected boolean match(Method method, int bci){ return _method == method && _bci == bci; }
    protected boolean is_empty(){
        boolean result = _method == null;
        if (!(_method != null || (_bci == 0 &&
                (_mask_size == 0 || _mask_size == 65535) &&
                _bit_mask[0] == 0))){
            throw new RuntimeException("Should be completely empty");
        }
        return result;
    }

    // Initialization
    protected void initialize(){
        _method    = null;
        _mask_size = 65535;  // This value should cause a failure quickly
        _bci       = 0;
        _expression_stack_size = 0;
        _bit_mask=new long[N];
        for (int i = 0; i < N; i++) {
            _bit_mask[i] = 0;
        }
    }

    public InterpreterOopMap(){
        initialize();
    }

    // Copy the OopMapCacheEntry in parameter "from" into this
    // InterpreterOopMap.  If the _bit_mask[0] in "from" points to
    // allocated space (i.e., the bit mask was to large to hold
    // in-line), allocate the space from a Resource area.
    public void resource_copy(OopMapCacheEntry from){
        set_method(from.method());
        set_bci(from.bci());
        set_mask_size(from.mask_size());
        set_expression_stack_size(from.expression_stack_size());
        _bit_mask= Arrays.copyOf(from._bit_mask, (int) (mask_word_size()* JVM.BytesPerWord/JVM.oopSize));
    }

    //public void iterate_oop(OffsetClosure* oop_closure) const;
//    public void print() const;

    public int number_of_entries(){ return mask_size() / bits_per_entry; }
    public boolean is_dead(int offset){ return (entry_at(offset) & (1 << dead_bit_number)) != 0; }
    public boolean is_oop (int offset) {return (entry_at(offset) & (1 << oop_bit_number )) != 0; }

    public int expression_stack_size()               { return _expression_stack_size; }
}
