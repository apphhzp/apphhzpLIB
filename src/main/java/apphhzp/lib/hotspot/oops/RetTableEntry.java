package apphhzp.lib.hotspot.oops;

import apphhzp.lib.hotspot.util.RawCType;

import java.util.ArrayList;
import java.util.List;

public class RetTableEntry {

    private static final int _init_nof_jsrs=5;                      // Default size of jsrs list
    private int _target_bci;                                // Target PC address of jump (bytecode index)
    private @RawCType("GrowableArray<intptr_t>*")List<Integer> _jsrs;                     // List of return addresses  (bytecode index)
    private RetTableEntry _next;                           // Link to next entry

    public RetTableEntry(int target, RetTableEntry next){
        _target_bci = target;
        _jsrs = new ArrayList<>(_init_nof_jsrs);
        _next = next;
    }

    // Query
    public int target_bci(){ return _target_bci; }
    public int nof_jsrs(){ return _jsrs.size(); }
    public int jsrs(int i){
        if (!(i>=0 && i<nof_jsrs())){
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        return _jsrs.get(i);
    }

    // Update entry
    public void add_jsr    (int return_bci)            { _jsrs.add(return_bci); }
    public void add_delta  (int bci, int delta){
        if (_target_bci > bci) _target_bci += delta;

        for (int k = 0; k < _jsrs.size(); k++) {
            int jsr = _jsrs.get(k);
            if (jsr > bci) {
                _jsrs.set(k, jsr+delta);
            }
        }
    }
    public RetTableEntry  next(){ return _next; }
}
