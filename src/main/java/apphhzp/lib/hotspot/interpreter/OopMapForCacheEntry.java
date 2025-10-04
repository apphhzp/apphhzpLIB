package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.oops.CellTypeStateList;
import apphhzp.lib.hotspot.oops.GenerateOopMap;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;

import java.util.List;

public class OopMapForCacheEntry extends GenerateOopMap {
    private OopMapCacheEntry _entry;
    private int               _bci;
    private int               _stack_top;
    public OopMapForCacheEntry(Method method, int bci, OopMapCacheEntry entry){
        super(method);
        _bci       = bci;
        _entry     = entry;
        _stack_top = -1;
    }
    public boolean report_results(){ return false; }
    public boolean compute_map(){
        if (method().is_native()){
            throw new RuntimeException("cannot compute oop map for native methods");
        }
        // First check if it is a method where the stackmap is always empty
        if (method().code_size() == 0 || method().max_locals() + method().max_stack() == 0) {
            _entry.set_mask_size(0);
        } else {
            if (!super.compute_map()) {
                throw new RuntimeException("Unrecoverable verification or out-of-memory error");
            }
            result_for_basicblock(_bci);
        }
        return true;
    }
    public boolean possible_gc_point(BytecodeStream bcs) {
        return false; // We are not reporting any result. We call result_for_basicblock directly
    }
    public void fill_stackmap_prolog(int nof_gc_points) {
        // Do nothing
    }


    public void fill_stackmap_epilog() {
        // Do nothing
    }


    public void fill_init_vars(List<Integer> init_vars) {
        // Do nothing
    }
    public void fill_stackmap_for_opcodes(BytecodeStream bcs,
                                                        CellTypeStateList vars,
                                          CellTypeStateList stack,
                                                        int stack_top) {
        // Only interested in one specific bci
        if (bcs.bci() == _bci) {
            _entry.set_mask(vars, stack, stack_top);
            _stack_top = stack_top;
        }
    }


    public int size(){
        if (_stack_top == -1){
            throw new RuntimeException("compute_map must be called first");
        }
        return ((method().is_static()) ? 0 : 1) + method().max_locals() + _stack_top;
    }
}
