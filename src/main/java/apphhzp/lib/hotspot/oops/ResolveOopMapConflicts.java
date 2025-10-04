package apphhzp.lib.hotspot.oops;

import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;

import java.util.List;

/** Subclass of the GenerateOopMap Class that just do rewrites of the method, if needed.
 * It does not store any oopmaps.*/
public class ResolveOopMapConflicts extends GenerateOopMap {

    private boolean _must_clear_locals;

    public boolean report_results()      { return false; }
    @SuppressWarnings("RedundantMethodOverride")
    public boolean report_init_vars()    { return true;  }
    public boolean allow_rewrites()     { return true;  }
    public boolean possible_gc_point          (BytecodeStream bcs)           { return false; }
    public void fill_stackmap_prolog(int nof_gc_points)             {}
    public void fill_stackmap_epilog       ()                              {}
    public void fill_stackmap_for_opcodes  (BytecodeStream bcs,
                                             CellTypeStateList vars,
                                             CellTypeStateList stack,
                                             int stack_top)                 {}
    public void fill_init_vars             (List<Integer> init_vars) { _must_clear_locals = !init_vars.isEmpty(); }

    public ResolveOopMapConflicts(Method method){
        super(method);
        _must_clear_locals = false; };

    public Method do_potential_rewrite(){
        if (!compute_map()) {
            throw new RuntimeException(exception());
        }
        return method();
    }
    public boolean must_clear_locals() { return _must_clear_locals; }
}
