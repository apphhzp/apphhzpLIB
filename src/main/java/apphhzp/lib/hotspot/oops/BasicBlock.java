package apphhzp.lib.hotspot.oops;

public class BasicBlock {

    private boolean _changed;                 // Reached a fixpoint or not

    public static final class Constants {
        public static final int _dead_basic_block = -2,
                _unreached = -1;                 // Alive but not yet reached by analysis
        // >=0                                  // Alive and has a merged state
    }
    ;

    public int _bci;                     // Start of basic block
    public int _end_bci;                 // Bci of last instruction in basicblock
    public int _max_locals;              // Determines split between vars and stack
    public int _max_stack;               // Determines split between stack and monitors
    public  CellTypeStateList _state;                   // State (vars, stack) at entry.
    public int _stack_top;               // -1 indicates bottom stack value.
    public int _monitor_top;             // -1 indicates bottom monitor stack value.

    public CellTypeStateList vars() {
        return _state;
    }

    public CellTypeStateList stack() {
        return _state.subList(_max_locals, _state.size());
    }

    public boolean changed() {
        return _changed;
    }

    public void set_changed(boolean s) {
        _changed = s;
    }

    public boolean is_reachable() {
        return _stack_top >= 0;
    }  // Analysis has reached this basicblock

    // All basicblocks that are unreachable are going to have a _stack_top == _dead_basic_block.
    // This info. is setup in a pre-parse before the real abstract interpretation starts.
    public boolean is_dead(){
        return _stack_top == Constants._dead_basic_block;
    }

    public boolean is_alive(){
        return _stack_top != Constants._dead_basic_block;
    }

    public void mark_as_alive(){
        if (!is_dead()){
            throw new RuntimeException("must be dead");
        }
        _stack_top = Constants._unreached;
    }
}
