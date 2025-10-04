package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.interpreter.Bytecode_lookupswitch;
import apphhzp.lib.hotspot.interpreter.Bytecode_tableswitch;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.interpreter.LookupswitchPair;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.method.ExceptionTable;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;
import apphhzp.lib.hotspot.runtime.bytecode.Bytecode_loadconstant;
import apphhzp.lib.hotspot.runtime.signature.Signature;
import apphhzp.lib.hotspot.runtime.signature.SignatureIterator;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;
import apphhzp.lib.hotspot.utilities.ResourceBitMap;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** Main class used to compute the pointer-maps in a Method*/
public class GenerateOopMap {


    // _monitor_top is set to this constant to indicate that a monitor matching
    // problem was encountered prior to this point in control flow.
    protected static final int  bad_monitors = -1;

    // Main variables
    protected Method _method;                     // The method we are examine
    protected RetTable     _rt;                         // Contains the return address mappings
    protected int          _max_locals;                 // Cached value of no. of locals
    protected int          _max_stack;                  // Cached value of max. stack depth
    protected int          _max_monitors;               // Cached value of max. monitor stack depth
    protected int          _has_exceptions;             // True, if exceptions exist for method
    protected boolean        _got_error;                  // True, if an error occurred during interpretation.
    protected String       _exception;                  // Exception if got_error is true.
    protected boolean         _did_rewriting;              // was bytecodes rewritten
    protected boolean         _did_relocation;             // was relocation neccessary
    protected boolean         _monitor_safe;               // The monitors in this method have been determined
    // to be safe.

    // Working Cell type state
    protected int            _state_len;                // Size of states
    protected @RawCType("CellTypeState*") CellTypeStateList _state;                    // list of states
    protected char[]          _state_vec_buf;            // Buffer used to print a readable version of a state
    protected int            _stack_top;
    protected int            _monitor_top;

//    // Timing and statistics
//    protected static elapsedTimer _total_oopmap_time;   // Holds cumulative oopmap generation time
//    protected static uint64_t     _total_byte_count;    // Holds cumulative number of bytes inspected

    // Cell type methods
    protected void init_state(){
        _state_len     = _max_locals + _max_stack + _max_monitors;
        _state=new CellTypeStateList(_state_len);
        int count = Math.max(_max_locals, Math.max(_max_stack, _max_monitors)) + 1/*for null terminator char */;
        _state_vec_buf=new char[count];
    }
    protected void make_context_uninitialized (){
        CellTypeStateList vs = vars();

        for (int i = 0; i < _max_locals; i++)
            vs.set(i,CellTypeState.uninit);

        _stack_top = 0;
        _monitor_top = 0;
    }
    private static class ComputeEntryStack extends SignatureIterator {
        private CellTypeStateList _effect;
        private int _idx;

        private void set(CellTypeState state)         { _effect.set(_idx++,state); }
        private int  length()                         { return _idx; };
        protected void do_type(@RawCType("BasicType")int type) {
            this.do_type(type,false);
        }
        private void do_type(@RawCType("BasicType")int type,boolean for_return) {
            if (for_return && type == BasicType.T_VOID) {
                set(CellTypeState.bottom);
            } else if (BasicType.is_reference_type(type)) {
                set(CellTypeState.make_slot_ref(_idx));
            } else {
                if (!BasicType.is_java_primitive(type)){
                    throw new RuntimeException();
                }
                set(CellTypeState.value);
                if (BasicType.is_double_word_type(type)) {
                    set(CellTypeState.value);
                }
            }
        }

        public ComputeEntryStack(Symbol signature){
            super(signature);
        };

        // Compute methods
        public int compute_for_parameters(boolean is_static, CellTypeStateList effect) {
            _idx    = 0;
            _effect = effect;

            if (!is_static)
                effect.set(_idx++,CellTypeState.make_slot_ref(0));
            do_parameters_on(this);

            return length();
        };

        public int compute_for_returntype(CellTypeStateList effect) {
            _idx    = 0;
            _effect = effect;
            do_type(return_type(), true);
            set(CellTypeState.bottom);  // Always terminate with a bottom state, so ppush works
            return length();
        }
    };
    protected int methodsig_to_effect(Symbol signature, boolean is_static, CellTypeStateList effect){
        ComputeEntryStack ces=new ComputeEntryStack(signature);
        return ces.compute_for_parameters(is_static, effect);
    }
    protected boolean merge_local_state_vectors(CellTypeStateList cts, CellTypeStateList bbts){
        int i;
        int len = _max_locals + _stack_top;
        boolean change = false;

        for (i = len - 1; i >= 0; i--) {
            CellTypeState v = cts.get(i).merge(bbts.get(i), i);
            change = change || !v.equal(bbts.get(i));
            bbts.set(i,v);
        }
        return change;
    }
    protected boolean merge_monitor_state_vectors(CellTypeStateList cts, CellTypeStateList bbts){
        boolean change = false;
        if (_max_monitors > 0 && _monitor_top != bad_monitors) {
            // If there are no monitors in the program, or there has been
            // a monitor matching error before this point in the program,
            // then we do not merge in the monitor state.

            int base = _max_locals + _max_stack;
            int len = base + _monitor_top;
            for (int i = len - 1; i >= base; i--) {
                CellTypeState v = cts.get(i).merge(bbts.get(i), i);

                // Can we prove that, when there has been a change, it will already
                // have been detected at this point?  That would make this equal
                // check here unnecessary.
                change = change || !v.equal(bbts.get(i));
                bbts.set(i,v);
            }
        }
        return change;
    }
    protected void copy_state(CellTypeStateList dst, CellTypeStateList src){
        int len = _max_locals + _stack_top;
        for (int i = 0; i < len; i++) {
            if (src.get(i).is_nonlock_reference()) {
                dst.set(i,CellTypeState.make_slot_ref(i));
            } else {
                dst.set(i,src.get(i));
            }
        }
        if (_max_monitors > 0 && _monitor_top != bad_monitors) {
            int base = _max_locals + _max_stack;
            len = base + _monitor_top;
            for (int i = base; i < len; i++) {
                dst.set(i,src.get(i));
            }
        }
    }
    protected void merge_state_into_bb(BasicBlock bb){
        if (bb==null){
            throw new NullPointerException("null basicblock");
        }
        if (!(bb.is_alive())){
            throw new RuntimeException("merging state into a dead basicblock");
        }
        if (_stack_top == bb._stack_top) {
            // always merge local state even if monitors don't match.
            if (merge_local_state_vectors(_state, bb._state)) {
                bb.set_changed(true);
            }
            if (_monitor_top == bb._monitor_top) {
                // monitors still match so continue merging monitor states.
                if (merge_monitor_state_vectors(_state, bb._state)) {
                    bb.set_changed(true);
                }
            } else {
                // When the monitor stacks are not matched, we set _monitor_top to
                // bad_monitors.  This signals that, from here on, the monitor stack cannot
                // be trusted.  In particular, monitorexit bytecodes may throw
                // exceptions.  We mark this block as changed so that the change
                // propagates properly.
                bb._monitor_top = bad_monitors;
                bb.set_changed(true);
                _monitor_safe = false;
            }
        } else if (!bb.is_reachable()) {
            // First time we look at this  BB
            copy_state(bb._state, _state);
            bb._stack_top = _stack_top;
            bb._monitor_top = _monitor_top;
            bb.set_changed(true);
        } else {
            throw new RuntimeException("stack height conflict: "+_stack_top+" vs. "+bb._stack_top);
        }
    }
    protected static void merge_state(GenerateOopMap gom, int bci, int[] data){
        gom.merge_state_into_bb(gom.get_basic_block_at(bci));
    }
    protected void set_var(int localNo, CellTypeState cts){
        if (!(cts.is_reference() || cts.is_value() || cts.is_address())){
            throw new RuntimeException("wrong celltypestate");
        }
        if (localNo < 0 || localNo > _max_locals) {
            throw new RuntimeException("variable write error: r"+localNo);
        }
        vars().set(localNo,cts);
    }
    protected CellTypeState get_var(int localNo){
        if (!(localNo < _max_locals + _nof_refval_conflicts)){
            throw new RuntimeException("variable read error");
        }
        if (localNo < 0 || localNo > _max_locals) {
            throw new RuntimeException("variable read error: r"+localNo);
        }
        return vars().get(localNo);
    }
    protected CellTypeState pop(){
        if ( _stack_top <= 0) {
            throw new RuntimeException("stack underflow");
        }
        return  stack().get(--_stack_top);
    }
    protected void push(CellTypeState cts){
        if ( _stack_top >= _max_stack) {
            throw new RuntimeException("stack overflow");
        }
        stack().set(_stack_top++,cts);
    }
    protected CellTypeState monitor_pop(){
        if (_monitor_top == bad_monitors){
            throw new RuntimeException("monitor_pop called on error monitor stack");
        }
        if (_monitor_top == 0) {
            // We have detected a pop of an empty monitor stack.
            _monitor_safe = false;
            _monitor_top = bad_monitors;

//            if (log_is_enabled(Info, monitormismatch)) {
//                report_monitor_mismatch("monitor stack underflow");
//            }
            return CellTypeState.ref; // just to keep the analysis going.
        }
        return  monitors().get(--_monitor_top);
    }
    protected void monitor_push(CellTypeState cts){
        if (_monitor_top == bad_monitors){
            throw new RuntimeException("monitor_push called on error monitor stack");
        }
        if (_monitor_top >= _max_monitors) {
            // Some monitorenter is being executed more than once.
            // This means that the monitor stack cannot be simulated.
            _monitor_safe = false;
            _monitor_top = bad_monitors;

//            if (log_is_enabled(Info, monitormismatch)) {
//                report_monitor_mismatch("monitor stack overflow");
//            }
            return;
        }
        monitors().set(_monitor_top++,cts);;
    }
    protected CellTypeStateList vars(){ return _state; }
    protected CellTypeStateList stack(){ return _state.subList(_max_locals,_state.size()); }
    protected CellTypeStateList monitors(){ return _state.subList(_max_locals+_max_stack,_state.size()); }

    protected void            replace_all_CTS_matches    (CellTypeState match,
                                                CellTypeState replace){
        int i;
        int len = _max_locals + _stack_top;
        boolean change = false;

        for (i = len - 1; i >= 0; i--) {
            if (match.equal(_state.get(i))) {
                _state.set(i,replace);;
            }
        }

        if (_monitor_top > 0) {
            int base = _max_locals + _max_stack;
            len = base + _monitor_top;
            for (i = len - 1; i >= base; i--) {
                if (match.equal(_state.get(i))) {
                    _state.set(i,replace);;
                }
            }
        }
    }
    protected void            print_states               (PrintStream os, CellTypeStateList vector, int num){
        for (int i = 0; i < num; i++) {
            vector.get(i).print(os);
        }
    }
    protected  void            print_current_state        (PrintStream  os,
                                                BytecodeStream currentBC,
                                                boolean            detailed){
        if (detailed) {
            os.printf("     %4d vars     = ", currentBC.bci());
            print_states(os, vars(), _max_locals);
            os.printf("    %s", Bytecodes.name(currentBC.code()));
        } else {
            os.printf("    %4d  vars = '%s' ", currentBC.bci(), (state_vec_to_string(vars(), _max_locals)));
            os.printf("     stack = '%s' ", state_vec_to_string(stack(), _stack_top));
            if (_monitor_top != bad_monitors) {
                os.printf("  monitors = '%s'  \t%s", state_vec_to_string(monitors(), _monitor_top), Bytecodes.name(currentBC.code()));
            } else {
                os.print("  [bad monitor stack]");
            }
        }

        switch(currentBC.code()) {
            case Bytecodes.Code._invokevirtual:
            case Bytecodes.Code._invokespecial:
            case Bytecodes.Code._invokestatic:
            case Bytecodes.Code._invokedynamic:
            case Bytecodes.Code._invokeinterface: {
                int idx = currentBC.has_index_u4() ? currentBC.get_index_u4() : currentBC.get_index_u2_cpcache();
                ConstantPool cp      = method().constants();
                int nameAndTypeIdx    = cp.name_and_type_ref_index_at(idx);
                int signatureIdx      = cp.signature_ref_index_at(nameAndTypeIdx);
                Symbol signature     = cp.symbol_at(signatureIdx);
                os.printf("%s", signature.toString());
            }
            default:
                break;
        }
        if (detailed) {
            os.println();
            os.print("          stack    = ");
            print_states(os, stack(), _stack_top);
            os.println();
            if (_monitor_top != bad_monitors) {
                os.print("          monitors = ");
                print_states(os, monitors(), _monitor_top);
            } else {
                os.print("          [bad monitor stack]");
            }
        }
        os.println();
    }
    protected void report_monitor_mismatch    (String msg){
        System.err.print("Monitor mismatch in method ");
        method().print_short_name(System.err);
        System.err.println(": "+msg);
    }

    // Basicblock info
    protected BasicBlock[]    _basic_blocks;             // Array of basicblock info
    protected int             _gc_points;
    protected int             _bb_count;
    protected ResourceBitMap _bb_hdr_bits;

    // Basicblocks methods
    protected void          initialize_bb               (){
        _gc_points = 0;
        _bb_count  = 0;
        _bb_hdr_bits=new ResourceBitMap( _method.code_size());
    }
    protected void mark_bbheaders_and_count_gc_points(){
        initialize_bb();

        boolean fellThrough = false;  // False to get first BB marked.

        // First mark all exception handlers as start of a basic-block
        ExceptionTable excps=new ExceptionTable(method());
        for(int i = 0; i < excps.length(); i ++) {
            bb_mark_fct(this, excps.handler_pc(i), null);
        }

        // Then iterate through the code
        BytecodeStream bcs=new BytecodeStream(_method);
        @RawCType("Bytecodes::Code")int bytecode;

        while( (bytecode = bcs.next()) >= 0) {
            int bci = bcs.bci();

            if (!fellThrough) {
                bb_mark_fct(this, bci, null);
            }

            fellThrough = jump_targets_do(bcs, GenerateOopMap::bb_mark_fct, null);

            /* We will also mark successors of jsr's as basic block headers. */
            switch (bytecode) {
                case Bytecodes.Code._jsr, Bytecodes.Code._jsr_w:
                    if (fellThrough){
                        throw new RuntimeException("should not happen");
                    }
                    bb_mark_fct(this, bci + Bytecodes.length_for(bytecode), null);
                    break;
                default:
                    break;
            }

            if (possible_gc_point(bcs)) {
                _gc_points++;
            }
        }
    }
    protected boolean          is_bb_header                (int bci)    {
        return _bb_hdr_bits.at(bci);
    }
    protected int           gc_points                   ()                           { return _gc_points; }
    protected int           bb_count                    ()                           { return _bb_count; }
    protected void          set_bbmark_bit              (int bci){
        _bb_hdr_bits.atPut(bci, true);
    }
    protected BasicBlock  get_basic_block_at          (int bci){
        BasicBlock bb = get_basic_block_containing(bci);
        if (!(bb._bci == bci)){
            throw new RuntimeException("should have found BB");
        }
        return bb;
    }
    protected BasicBlock  get_basic_block_containing  (int bci){
        BasicBlock[] bbs = _basic_blocks;
        int lo = 0, hi = _bb_count - 1;

        while (lo <= hi) {
            int m = (lo + hi) / 2;
            int mbci = bbs[m]._bci;
            int nbci;

            if ( m == _bb_count-1) {
                if (!(bci >= mbci && bci < method().code_size())){
                    throw new RuntimeException("sanity check failed");
                }
                return bbs[m];
            } else {
                nbci = bbs[m+1]._bci;
            }

            if ( mbci <= bci && bci < nbci) {
                return bbs[m];
            } else if (mbci < bci) {
                lo = m + 1;
            } else {
                if (!(mbci > bci)){
                    throw new RuntimeException("sanity check");
                }
                hi = m - 1;
            }
        }
        throw new RuntimeException("should have found BB");
    }

    private int bbIndex(BasicBlock bb) {
        for (int i = 0; i < _basic_blocks.length; i++) {
            if (_basic_blocks[i] == bb) {
                return i;
            }
        }
        throw new RuntimeException("Should have found block");
    }
    protected void          interp_bb                   (BasicBlock bb){
        // We do not want to do anything in case the basic-block has not been initialized. This
        // will happen in the case where there is dead-code hang around in a method.
        if (!bb.is_reachable()){
            throw new RuntimeException("should be reachable or deadcode exist");
        }
        restore_state(bb);

        BytecodeStream itr=new BytecodeStream(_method);

        // Set iterator interval to be the current basicblock
        int lim_bci = next_bb_start_pc(bb);
        itr.set_interval(bb._bci, lim_bci);
        if (lim_bci == bb._bci){
            throw new RuntimeException("must be at least one instruction in a basicblock");
        }
        itr.next(); // read first instruction

        // Iterates through all bytecodes except the last in a basic block.
        // We handle the last one special, since there is controlflow change.
        while(itr.next_bci() < lim_bci && !_got_error) {
            if (_has_exceptions!=0 || _monitor_top != 0) {
                // We do not need to interpret the results of exceptional
                // continuation from this instruction when the method has no
                // exception handlers and the monitor stack is currently
                // empty.
                do_exception_edge(itr);
            }
            interp1(itr);
            itr.next();
        }

        // Handle last instruction.
        if (!_got_error){
            if (!(itr.next_bci() == lim_bci)){
                throw new RuntimeException("must point to end");
            }
            if (_has_exceptions!=0 || _monitor_top != 0) {
                do_exception_edge(itr);
            }
            interp1(itr);

            boolean fall_through = jump_targets_do(itr, GenerateOopMap::merge_state, null);
            if (_got_error)  return;

            if (itr.code() == Bytecodes.Code._ret){
                if (fall_through){
                    throw new RuntimeException("cannot be set if ret instruction");
                }
                // Automatically handles 'wide' ret indicies
                ret_jump_targets_do(itr, GenerateOopMap::merge_state, itr.get_index(), null);
            } else if (fall_through) {
                // Hit end of BB, but the instr. was a fall-through instruction,
                // so perform transition as if the BB ended in a "jump".
                if (lim_bci != _basic_blocks[bbIndex(bb)+1]._bci){
                    throw new RuntimeException("bytecodes fell through last instruction");
                }
                merge_state_into_bb(_basic_blocks[bbIndex(bb)+1]);
            }
        }
    }
    protected void restore_state (BasicBlock bb){
        for (int i = 0; i < _state_len; i++) {
            _state.set(i,bb._state.get(i));
        }
        _stack_top = bb._stack_top;
        _monitor_top = bb._monitor_top;
    }
    protected  int           next_bb_start_pc            (BasicBlock bb){
        int bbNum = bbIndex(bb) + 1;
        if (bbNum == _bb_count)
            return method().code_size();

        return _basic_blocks[bbNum]._bci;
    }
    protected void          update_basic_blocks         (int bci, int delta, int new_method_size){
        if (!(new_method_size >= method().code_size() + delta)){
            throw new RuntimeException("new method size is too small");
        }
        _bb_hdr_bits=new ResourceBitMap(new_method_size);

        for(int k = 0; k < _bb_count; k++) {
            if (_basic_blocks[k]._bci > bci) {
                _basic_blocks[k]._bci     += delta;
                _basic_blocks[k]._end_bci += delta;
            }
            _bb_hdr_bits.atPut(_basic_blocks[k]._bci, true);
        }
    }
    protected static void   bb_mark_fct(GenerateOopMap c, int bci, int[] data){
        if (!(bci>= 0 && bci < c.method().code_size())){
            throw new RuntimeException("index out of bounds");
        }
        if (c.is_bb_header(bci))
            return;
        c.set_bbmark_bit(bci);
        c._bb_count++;
    }

    // Dead code detection
    protected  void mark_reachable_code(){
        int[] change = new int[]{1}; // int to get function pointers to work

        // Mark entry basic block as alive and all exception handlers
        _basic_blocks[0].mark_as_alive();
        ExceptionTable excps=new ExceptionTable(method());
        for(int i = 0; i < excps.length(); i++) {
            BasicBlock bb = get_basic_block_at(excps.handler_pc(i));
            // If block is not already alive (due to multiple exception handlers to same bb), then
            // make it alive
            if (bb.is_dead())
                bb.mark_as_alive();
        }

        BytecodeStream bcs=new BytecodeStream(_method);

        // Iterate through all basic blocks until we reach a fixpoint
        while (change[0]!=0) {
            change[0] = 0;

            for (int i = 0; i < _bb_count; i++) {
                BasicBlock bb = _basic_blocks[i];
                if (bb.is_alive()) {
                    // Position bytecodestream at last bytecode in basicblock
                    bcs.set_start(bb._end_bci);
                    bcs.next();
                    @RawCType("Bytecodes::Code")int bytecode = bcs.code();
                    int bci = bcs.bci();
                    if (!(bci == bb._end_bci)){
                        throw new RuntimeException("wrong bci");
                    }

                    boolean fell_through = jump_targets_do(bcs, GenerateOopMap::reachable_basicblock, change);

                    // We will also mark successors of jsr's as alive.
                    switch (bytecode) {
                        case Bytecodes.Code._jsr:
                        case Bytecodes.Code._jsr_w:
                            if (fell_through){
                                throw new RuntimeException("should not happen");
                            }
                            reachable_basicblock(this, bci + Bytecodes.length_for(bytecode), change);
                            break;
                        default:
                            break;
                    }
                    if (fell_through) {
                        // Mark successor as alive
                        if (_basic_blocks[i+1].is_dead()) {
                            _basic_blocks[i+1].mark_as_alive();
                            change[0] = 1;
                        }
                    }
                }
            }
        }
    }
    protected static void reachable_basicblock(GenerateOopMap c, int bci, int[] data){
        if (!(bci>= 0 && bci < c.method().code_size())){
            throw new RuntimeException("index out of bounds");
        }
        BasicBlock bb = c.get_basic_block_at(bci);
        if (bb.is_dead()) {
            bb.mark_as_alive();
            data[0] = 1; // Mark basicblock as changed
        }
    }

    // Interpretation methods (primary)
    protected void  do_interpretation(){
        // "i" is just for debugging, so we can detect cases where this loop is
        // iterated more than once.
        int i = 0;
        do {
            _conflict = false;
            _monitor_safe = true;
            // init_state is now called from init_basic_blocks.  The length of a
            // state vector cannot be determined until we have made a pass through
            // the bytecodes counting the possible monitor entries.
            if (!_got_error) init_basic_blocks();
            if (!_got_error) setup_method_entry_state();
            if (!_got_error) interp_all();
            if (!_got_error) rewrite_refval_conflicts();
            i++;
        } while (_conflict && !_got_error);
    }
    protected void  init_basic_blocks(){
        // Note: Could consider reserving only the needed space for each BB's state
        // (entry stack may not be of maximal height for every basic block).
        // But cumbersome since we don't know the stack heights yet.  (Nor the
        // monitor stack heights...)

        _basic_blocks = new BasicBlock[_bb_count];
        for (int i = 0; i < _bb_count; i++) {
            _basic_blocks[i] = new BasicBlock();
        }

        // Make a pass through the bytecodes.  Count the number of monitorenters.
        // This can be used an upper bound on the monitor stack depth in programs
        // which obey stack discipline with their monitor usage.  Initialize the
        // known information about basic blocks.
        BytecodeStream j=new BytecodeStream(_method);
        @RawCType("Bytecodes::Code")int bytecode;

        int bbNo = 0;
        int monitor_count = 0;
        int prev_bci = -1;
        while((bytecode = j.next()) >= 0) {
            if (j.code() == Bytecodes.Code._monitorenter) {
                monitor_count++;
            }

            int bci = j.bci();
            if (is_bb_header(bci)) {
                // Initialize the basicblock structure
                BasicBlock bb   = _basic_blocks[bbNo];
                bb._bci         = bci;
                bb._max_locals  = _max_locals;
                bb._max_stack   = _max_stack;
                bb.set_changed(false);
                bb._stack_top   = BasicBlock.Constants._dead_basic_block; // Initialize all basicblocks are dead.
                bb._monitor_top = bad_monitors;

                if (bbNo > 0) {
                    _basic_blocks[bbNo - 1]._end_bci = prev_bci;
                }

                bbNo++;
            }
            // Remember prevous bci.
            prev_bci = bci;
        }
        // Set
        _basic_blocks[bbNo-1]._end_bci = prev_bci;


        // Check that the correct number of basicblocks was found
        if (bbNo !=_bb_count) {
            if (bbNo < _bb_count) {
                throw new RuntimeException("jump into the middle of instruction?");
            } else {
                throw new RuntimeException("extra basic blocks - should not happen?");
            }
        }

        _max_monitors = monitor_count;

        // Now that we have a bound on the depth of the monitor stack, we can
        // initialize the CellTypeState-related information.
        init_state();



        CellTypeStateList basicBlockState=new CellTypeStateList(bbNo * _state_len);

        // Make a pass over the basicblocks and assign their state vectors.
        for (int blockNum=0; blockNum < bbNo; blockNum++) {
            BasicBlock bb = _basic_blocks[blockNum];
            bb._state = basicBlockState.subList(blockNum * _state_len,(blockNum + 1) * _state_len);

            if (JVM.ENABLE_EXTRA_CHECK&&blockNum + 1 < bbNo) {
                @RawCType("address")long bcp = _method.bcp_from(bb._end_bci);
                int bc_len = Bytecodes.java_length_at(_method, bcp);
                if (!(bb._end_bci + bc_len == _basic_blocks[blockNum+1]._bci)){
                    throw new RuntimeException("unmatched bci info in basicblock");
                }
            }
        }

        if (JVM.ENABLE_EXTRA_CHECK){
            BasicBlock bb = _basic_blocks[bbNo-1];
            @RawCType("address")long bcp = _method.bcp_from(bb._end_bci);
            int bc_len = Bytecodes.java_length_at(_method, bcp);
            if (!(bb._end_bci + bc_len == _method.code_size())){
                throw new RuntimeException("wrong end bci");
            }
        }


        // Mark all alive blocks
        mark_reachable_code();
    }
    protected void  setup_method_entry_state(){
        // Initialize all locals to 'uninit' and set stack-height to 0
        make_context_uninitialized();

        // Initialize CellState type of arguments
        methodsig_to_effect(method().signature(), method().is_static(), vars());

        // If some references must be pre-assigned to null, then set that up
        initialize_vars();

        // This is the start state
        merge_state_into_bb(_basic_blocks[0]);

        if (!_basic_blocks[0].changed()){
            throw new RuntimeException("we are not getting off the ground");
        }
    }
    protected  void  interp_all(){
        boolean change = true;

        while (change && !_got_error) {
            change = false;
            for (int i = 0; i < _bb_count && !_got_error; i++) {
                BasicBlock bb = _basic_blocks[i];
                if (bb.changed()) {
                    if (_got_error) return;
                    change = true;
                    bb.set_changed(false);
                    interp_bb(bb);
                }
            }
        }
    }


    // Commonly used constants
    static CellTypeState[] epsilonCTS = { CellTypeState.bottom };
    static CellTypeState   refCTS   = CellTypeState.ref;
    static CellTypeState   valCTS   = CellTypeState.value;
    static CellTypeState[] vCTS = { CellTypeState.value, CellTypeState.bottom };
    static CellTypeState[] rCTS = { CellTypeState.ref,   CellTypeState.bottom };
    static CellTypeState[] rrCTS = { CellTypeState.ref,   CellTypeState.ref,   CellTypeState.bottom };
    static CellTypeState[] vrCTS = { CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
    static CellTypeState[] vvCTS = { CellTypeState.value, CellTypeState.value, CellTypeState.bottom };
    static CellTypeState[] rvrCTS = { CellTypeState.ref,   CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
    static CellTypeState[] vvrCTS = { CellTypeState.value, CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
    static CellTypeState[] vvvCTS = { CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.bottom };
    static CellTypeState[] vvvrCTS = { CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
    static CellTypeState[] vvvvCTS = { CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.bottom };

    // Interpretation methods (secondary)
    /** Sets the current state to be the state after executing the
     * current instruction, starting in the current state.*/
    protected void  interp1(BytecodeStream itr){
        // Should we report the results? Result is reported *before* the instruction at the current bci is executed.
        // However, not for calls. For calls we do not want to include the arguments, so we postpone the reporting until
        // they have been popped (in method ppl).
        if (_report_result) {
            switch(itr.code()) {
                case Bytecodes.Code._invokevirtual:
                case Bytecodes.Code._invokespecial:
                case Bytecodes.Code._invokestatic:
                case Bytecodes.Code._invokedynamic:
                case Bytecodes.Code._invokeinterface:
                    _itr_send = itr;
                    _report_result_for_send = true;
                    break;
                default:
                    fill_stackmap_for_opcodes(itr, vars(), stack(), _stack_top);
                    break;
            }
        }

        // abstract interpretation of current opcode
        switch(itr.code()) {
            case Bytecodes.Code._nop:                                           break;
            case Bytecodes.Code._goto:                                          break;
            case Bytecodes.Code._goto_w:                                        break;
            case Bytecodes.Code._iinc:                                          break;
            case Bytecodes.Code._return:
                do_return_monitor_check();
                break;

            case Bytecodes.Code._aconst_null:
            case Bytecodes.Code._new:
                ppush1(CellTypeState.make_line_ref(itr.bci()));
                break;

            case Bytecodes.Code._iconst_m1:
            case Bytecodes.Code._iconst_0:
            case Bytecodes.Code._iconst_1:
            case Bytecodes.Code._iconst_2:
            case Bytecodes.Code._iconst_3:
            case Bytecodes.Code._iconst_4:
            case Bytecodes.Code._iconst_5:
            case Bytecodes.Code._fconst_0:
            case Bytecodes.Code._fconst_1:
            case Bytecodes.Code._fconst_2:
            case Bytecodes.Code._bipush:
            case Bytecodes.Code._sipush:            ppush1(valCTS);             break;

            case Bytecodes.Code._lconst_0:
            case Bytecodes.Code._lconst_1:
            case Bytecodes.Code._dconst_0:
            case Bytecodes.Code._dconst_1:          ppush(vvCTS);               break;

            case Bytecodes.Code._ldc2_w:            ppush(vvCTS);               break;

            case Bytecodes.Code._ldc:               // fall through:
            case Bytecodes.Code._ldc_w:             do_ldc(itr.bci());         break;

            case Bytecodes.Code._iload:
            case Bytecodes.Code._fload:             ppload(vCTS, itr.get_index()); break;

            case Bytecodes.Code._lload:
            case Bytecodes.Code._dload:             ppload(vvCTS,itr.get_index()); break;

            case Bytecodes.Code._aload:             ppload(rCTS, itr.get_index()); break;

            case Bytecodes.Code._iload_0:
            case Bytecodes.Code._fload_0:           ppload(vCTS, 0);            break;
            case Bytecodes.Code._iload_1:
            case Bytecodes.Code._fload_1:           ppload(vCTS, 1);            break;
            case Bytecodes.Code._iload_2:
            case Bytecodes.Code._fload_2:           ppload(vCTS, 2);            break;
            case Bytecodes.Code._iload_3:
            case Bytecodes.Code._fload_3:           ppload(vCTS, 3);            break;

            case Bytecodes.Code._lload_0:
            case Bytecodes.Code._dload_0:           ppload(vvCTS, 0);           break;
            case Bytecodes.Code._lload_1:
            case Bytecodes.Code._dload_1:           ppload(vvCTS, 1);           break;
            case Bytecodes.Code._lload_2:
            case Bytecodes.Code._dload_2:           ppload(vvCTS, 2);           break;
            case Bytecodes.Code._lload_3:
            case Bytecodes.Code._dload_3:           ppload(vvCTS, 3);           break;

            case Bytecodes.Code._aload_0:           ppload(rCTS, 0);            break;
            case Bytecodes.Code._aload_1:           ppload(rCTS, 1);            break;
            case Bytecodes.Code._aload_2:           ppload(rCTS, 2);            break;
            case Bytecodes.Code._aload_3:           ppload(rCTS, 3);            break;

            case Bytecodes.Code._iaload:
            case Bytecodes.Code._faload:
            case Bytecodes.Code._baload:
            case Bytecodes.Code._caload:
            case Bytecodes.Code._saload:            pp(vrCTS, vCTS); break;

            case Bytecodes.Code._laload:            pp(vrCTS, vvCTS);  break;
            case Bytecodes.Code._daload:            pp(vrCTS, vvCTS); break;

            case Bytecodes.Code._aaload:            pp_new_ref(vrCTS, itr.bci()); break;

            case Bytecodes.Code._istore:
            case Bytecodes.Code._fstore:            ppstore(vCTS, itr.get_index()); break;

            case Bytecodes.Code._lstore:
            case Bytecodes.Code._dstore:            ppstore(vvCTS, itr.get_index()); break;

            case Bytecodes.Code._astore:            do_astore(itr.get_index());     break;

            case Bytecodes.Code._istore_0:
            case Bytecodes.Code._fstore_0:          ppstore(vCTS, 0);           break;
            case Bytecodes.Code._istore_1:
            case Bytecodes.Code._fstore_1:          ppstore(vCTS, 1);           break;
            case Bytecodes.Code._istore_2:
            case Bytecodes.Code._fstore_2:          ppstore(vCTS, 2);           break;
            case Bytecodes.Code._istore_3:
            case Bytecodes.Code._fstore_3:          ppstore(vCTS, 3);           break;

            case Bytecodes.Code._lstore_0:
            case Bytecodes.Code._dstore_0:          ppstore(vvCTS, 0);          break;
            case Bytecodes.Code._lstore_1:
            case Bytecodes.Code._dstore_1:          ppstore(vvCTS, 1);          break;
            case Bytecodes.Code._lstore_2:
            case Bytecodes.Code._dstore_2:          ppstore(vvCTS, 2);          break;
            case Bytecodes.Code._lstore_3:
            case Bytecodes.Code._dstore_3:          ppstore(vvCTS, 3);          break;

            case Bytecodes.Code._astore_0:          do_astore(0);               break;
            case Bytecodes.Code._astore_1:          do_astore(1);               break;
            case Bytecodes.Code._astore_2:          do_astore(2);               break;
            case Bytecodes.Code._astore_3:          do_astore(3);               break;

            case Bytecodes.Code._iastore:
            case Bytecodes.Code._fastore:
            case Bytecodes.Code._bastore:
            case Bytecodes.Code._castore:
            case Bytecodes.Code._sastore:           ppop(vvrCTS);               break;
            case Bytecodes.Code._lastore:
            case Bytecodes.Code._dastore:           ppop(vvvrCTS);              break;
            case Bytecodes.Code._aastore:           ppop(rvrCTS);               break;

            case Bytecodes.Code._pop:               ppop_any(1);                break;
            case Bytecodes.Code._pop2:              ppop_any(2);                break;

            case Bytecodes.Code._dup:               ppdupswap(1, "11");         break;
            case Bytecodes.Code._dup_x1:            ppdupswap(2, "121");        break;
            case Bytecodes.Code._dup_x2:            ppdupswap(3, "1321");       break;
            case Bytecodes.Code._dup2:              ppdupswap(2, "2121");       break;
            case Bytecodes.Code._dup2_x1:           ppdupswap(3, "21321");      break;
            case Bytecodes.Code._dup2_x2:           ppdupswap(4, "214321");     break;
            case Bytecodes.Code._swap:              ppdupswap(2, "12");         break;

            case Bytecodes.Code._iadd:
            case Bytecodes.Code._fadd:
            case Bytecodes.Code._isub:
            case Bytecodes.Code._fsub:
            case Bytecodes.Code._imul:
            case Bytecodes.Code._fmul:
            case Bytecodes.Code._idiv:
            case Bytecodes.Code._fdiv:
            case Bytecodes.Code._irem:
            case Bytecodes.Code._frem:
            case Bytecodes.Code._ishl:
            case Bytecodes.Code._ishr:
            case Bytecodes.Code._iushr:
            case Bytecodes.Code._iand:
            case Bytecodes.Code._ior:
            case Bytecodes.Code._ixor:
            case Bytecodes.Code._l2f:
            case Bytecodes.Code._l2i:
            case Bytecodes.Code._d2f:
            case Bytecodes.Code._d2i:
            case Bytecodes.Code._fcmpl:
            case Bytecodes.Code._fcmpg:             pp(vvCTS, vCTS); break;

            case Bytecodes.Code._ladd:
            case Bytecodes.Code._dadd:
            case Bytecodes.Code._lsub:
            case Bytecodes.Code._dsub:
            case Bytecodes.Code._lmul:
            case Bytecodes.Code._dmul:
            case Bytecodes.Code._ldiv:
            case Bytecodes.Code._ddiv:
            case Bytecodes.Code._lrem:
            case Bytecodes.Code._drem:
            case Bytecodes.Code._land:
            case Bytecodes.Code._lor:
            case Bytecodes.Code._lxor:              pp(vvvvCTS, vvCTS); break;

            case Bytecodes.Code._ineg:
            case Bytecodes.Code._fneg:
            case Bytecodes.Code._i2f:
            case Bytecodes.Code._f2i:
            case Bytecodes.Code._i2c:
            case Bytecodes.Code._i2s:
            case Bytecodes.Code._i2b:               pp(vCTS, vCTS); break;

            case Bytecodes.Code._lneg:
            case Bytecodes.Code._dneg:
            case Bytecodes.Code._l2d:
            case Bytecodes.Code._d2l:               pp(vvCTS, vvCTS); break;

            case Bytecodes.Code._lshl:
            case Bytecodes.Code._lshr:
            case Bytecodes.Code._lushr:             pp(vvvCTS, vvCTS); break;

            case Bytecodes.Code._i2l:
            case Bytecodes.Code._i2d:
            case Bytecodes.Code._f2l:
            case Bytecodes.Code._f2d:               pp(vCTS, vvCTS); break;

            case Bytecodes.Code._lcmp:              pp(vvvvCTS, vCTS); break;
            case Bytecodes.Code._dcmpl:
            case Bytecodes.Code._dcmpg:             pp(vvvvCTS, vCTS); break;

            case Bytecodes.Code._ifeq:
            case Bytecodes.Code._ifne:
            case Bytecodes.Code._iflt:
            case Bytecodes.Code._ifge:
            case Bytecodes.Code._ifgt:
            case Bytecodes.Code._ifle:
            case Bytecodes.Code._tableswitch:       ppop1(valCTS);
                break;
            case Bytecodes.Code._ireturn:
            case Bytecodes.Code._freturn:           do_return_monitor_check();
                ppop1(valCTS);
                break;
            case Bytecodes.Code._if_icmpeq:
            case Bytecodes.Code._if_icmpne:
            case Bytecodes.Code._if_icmplt:
            case Bytecodes.Code._if_icmpge:
            case Bytecodes.Code._if_icmpgt:
            case Bytecodes.Code._if_icmple:         ppop(vvCTS);
                break;

            case Bytecodes.Code._lreturn:           do_return_monitor_check();
                ppop(vvCTS);
                break;

            case Bytecodes.Code._dreturn:           do_return_monitor_check();
                ppop(vvCTS);
                break;

            case Bytecodes.Code._if_acmpeq:
            case Bytecodes.Code._if_acmpne:         ppop(rrCTS);                 break;

            case Bytecodes.Code._jsr:               do_jsr(itr.dest());         break;
            case Bytecodes.Code._jsr_w:             do_jsr(itr.dest_w());       break;

            case Bytecodes.Code._getstatic:         do_field(true,  true,  itr.get_index_u2_cpcache(), itr.bci()); break;
            case Bytecodes.Code._putstatic:         do_field(false, true,  itr.get_index_u2_cpcache(), itr.bci()); break;
            case Bytecodes.Code._getfield:          do_field(true,  false, itr.get_index_u2_cpcache(), itr.bci()); break;
            case Bytecodes.Code._putfield:          do_field(false, false, itr.get_index_u2_cpcache(), itr.bci()); break;

            case Bytecodes.Code._invokevirtual:
            case Bytecodes.Code._invokespecial:     do_method(false, false, itr.get_index_u2_cpcache(), itr.bci()); break;
            case Bytecodes.Code._invokestatic:      do_method(true,  false, itr.get_index_u2_cpcache(), itr.bci()); break;
            case Bytecodes.Code._invokedynamic:     do_method(true,  false, itr.get_index_u4(),         itr.bci()); break;
            case Bytecodes.Code._invokeinterface:   do_method(false, true,  itr.get_index_u2_cpcache(), itr.bci()); break;
            case Bytecodes.Code._newarray:
            case Bytecodes.Code._anewarray:         pp_new_ref(vCTS, itr.bci()); break;
            case Bytecodes.Code._checkcast:         do_checkcast(); break;
            case Bytecodes.Code._arraylength:
            case Bytecodes.Code._instanceof:        pp(rCTS, vCTS); break;
            case Bytecodes.Code._monitorenter:      do_monitorenter(itr.bci()); break;
            case Bytecodes.Code._monitorexit:       do_monitorexit(itr.bci()); break;

            case Bytecodes.Code._athrow:            // handled by do_exception_edge() BUT ...
                // vlh(apple): do_exception_edge() does not get
                // called if method has no exception handlers
                if ((_has_exceptions==0) && (_monitor_top > 0)) {
                    _monitor_safe = false;
                }
                break;

            case Bytecodes.Code._areturn:           do_return_monitor_check();
                ppop1(refCTS);
                break;
            case Bytecodes.Code._ifnull:
            case Bytecodes.Code._ifnonnull:         ppop1(refCTS); break;
            case Bytecodes.Code._multianewarray:    do_multianewarray(unsafe.getByte(itr.bcp()+3)&0xff, itr.bci()); break;

            case Bytecodes.Code._wide:              throw new RuntimeException("Iterator should skip this bytecode");
            case Bytecodes.Code._ret:                                           break;

            // Java opcodes
            case Bytecodes.Code._lookupswitch:      ppop1(valCTS);             break;

            default:
                System.err.print("unexpected opcode: "+itr.code()+"\n");
                throw new RuntimeException("ShouldNotReachHere()");
        }
    }
    protected  void  do_exception_edge(BytecodeStream itr){
        // Only check exception edge, if bytecode can trap
        if (!Bytecodes.can_trap(itr.code())) return;
        switch (itr.code()) {
            case Bytecodes.Code._aload_0:
                // These bytecodes can trap for rewriting.  We need to assume that
                // they do not throw exceptions to make the monitor analysis work.
                return;

            case Bytecodes.Code._ireturn:
            case Bytecodes.Code._lreturn:
            case Bytecodes.Code._freturn:
            case Bytecodes.Code._dreturn:
            case Bytecodes.Code._areturn:
            case Bytecodes.Code._return:
                // If the monitor stack height is not zero when we leave the method,
                // then we are either exiting with a non-empty stack or we have
                // found monitor trouble earlier in our analysis.  In either case,
                // assume an exception could be taken here.
                if (_monitor_top == 0) {
                    return;
                }
                break;

            case Bytecodes.Code._monitorexit:
                // If the monitor stack height is bad_monitors, then we have detected a
                // monitor matching problem earlier in the analysis.  If the
                // monitor stack height is 0, we are about to pop a monitor
                // off of an empty stack.  In either case, the bytecode
                // could throw an exception.
                if (_monitor_top != bad_monitors && _monitor_top != 0) {
                    return;
                }
                break;

            default:
                break;
        }

        if (_has_exceptions!=0){
            int bci = itr.bci();
            ExceptionTable exct=new ExceptionTable(method());
            for(int i = 0; i< exct.length(); i++) {
                int start_pc   = exct.start_pc(i);
                int end_pc     = exct.end_pc(i);
                int handler_pc = exct.handler_pc(i);
                int catch_type = exct.catch_type_index(i);

                if (start_pc <= bci && bci < end_pc) {
                    BasicBlock excBB = get_basic_block_at(handler_pc);
                    if (excBB == null){
                        throw new RuntimeException("no basic block for exception");
                    }
                    CellTypeStateList excStk = excBB.stack();
                    CellTypeStateList cOpStck = stack();
                    CellTypeState cOpStck_0 = cOpStck.get(0);
                    int cOpStackTop = _stack_top;

                    // Exception stacks are always the same.
                    if (!(method().max_stack() > 0)){
                        throw new RuntimeException("sanity check");
                    }

                    // We remembered the size and first element of "cOpStck"
                    // above; now we temporarily set them to the appropriate
                    // values for an exception handler. */
                    cOpStck.set(0,CellTypeState.make_slot_ref(_max_locals));
                    _stack_top = 1;

                    merge_state_into_bb(excBB);

                    // Now undo the temporary change.
                    cOpStck.set(0,cOpStck_0);
                    _stack_top = cOpStackTop;

                    // If this is a "catch all" handler, then we do not need to
                    // consider any additional handlers.
                    if (catch_type == 0) {
                        return;
                    }
                }
            }
        }

        // It is possible that none of the exception handlers would have caught
        // the exception.  In this case, we will exit the method.  We must
        // ensure that the monitor stack is empty in this case.
        if (_monitor_top == 0) {
            return;
        }

        // We pessimistically assume that this exception can escape the
        // method. (It is possible that it will always be caught, but
        // we don't care to analyse the types of the catch clauses.)

        // We don't set _monitor_top to bad_monitors because there are no successors
        // to this exceptional exit.

        if (_monitor_safe) {
            // We check _monitor_safe so that we only report the first mismatched
            // exceptional exit.
            report_monitor_mismatch("non-empty monitor stack at exceptional exit");
        }
        _monitor_safe = false;
    }
    protected void  check_type(CellTypeState expected, CellTypeState actual){
        if (!expected.equal_kind(actual)) {
            throw new RuntimeException("wrong type on stack (found: "+actual.to_char()+" expected: "+expected.to_char()+")");
        }
    }
    protected void  ppstore(CellTypeState[] in,  int loc_no){
        int i=0;
        while(!(in[i]).is_bottom()) {
            CellTypeState expected =in[i++];
            CellTypeState actual   = pop();
            check_type(expected, actual);
            if (!(loc_no >= 0)){
                throw new RuntimeException("sanity check");
            }
            set_var(loc_no++, actual);
        }
    }
    protected  void  ppload(CellTypeState[] out, int loc_no){
        int i=0;
        while(!(out[i]).is_bottom()) {
            CellTypeState out1 = out[i++];
            CellTypeState vcts = get_var(loc_no);
            if (!(out1.can_be_reference() || out1.can_be_value())){
                throw new RuntimeException("can only load refs. and values.");
            }
            if (out1.is_reference()){
                if (!(loc_no>=0)){
                    throw new RuntimeException("sanity check");
                }
                if (!vcts.is_reference()) {
                    // We were asked to push a reference, but the type of the
                    // variable can be something else
                    _conflict = true;
                    if (vcts.can_be_uninit()) {
                        // It is a ref-uninit conflict (at least). If there are other
                        // problems, we'll get them in the next round
                        add_to_ref_init_set(loc_no);
                        vcts = out1;
                    } else {
                        // It wasn't a ref-uninit conflict. So must be a
                        // ref-val or ref-pc conflict. Split the variable.
                        record_refval_conflict(loc_no);
                        vcts = out1;
                    }
                    push(out1); // recover...
                } else {
                    push(vcts); // preserve reference.
                }
                // Otherwise it is a conflict, but one that verification would
                // have caught if illegal. In particular, it can't be a topCTS
                // resulting from mergeing two difference pcCTS's since the verifier
                // would have rejected any use of such a merge.
            } else {
                push(out1); // handle val/init conflict
            }
            loc_no++;
        }
    }
    protected void  ppush1(CellTypeState in){
        if (!(in.is_reference() || in.is_value())){
            throw new RuntimeException("sanity check");
        }
        push(in);
    }
    protected void  ppush(CellTypeState[] in){
        int i=0;
        while (!(in[i]).is_bottom()) {
            ppush1(in[i++]);
        }
    }
    protected void ppush(CellTypeStateList in){
        int i=0;
        while (!(in.get(i)).is_bottom()) {
            ppush1(in.get(i++));
        }
    }
    protected  void  ppop1(CellTypeState out){
        CellTypeState actual = pop();
        check_type(out, actual);
    }
    protected  void  ppop(CellTypeState[] out){
        int i=0;
        while (!(out[i]).is_bottom()) {
            ppop1(out[i++]);
        }
    }
    protected  void  ppop_any(int poplen){
        if (_stack_top >= poplen) {
            _stack_top -= poplen;
        } else {
            throw new RuntimeException("stack underflow");
        }
    }
    protected void  pp(CellTypeState[] in, CellTypeState[] out){
        ppop(in);
        ppush(out);
    }
    protected void  pp_new_ref(CellTypeState[] in, int bci){
        ppop(in);
        ppush1(CellTypeState.make_line_ref(bci));
    }
    protected void  ppdupswap(int poplen,String out){
        CellTypeState[] actual=new CellTypeState[5];
        if (!(poplen < 5)){
            throw new RuntimeException("this must be less than length of actual vector");
        }

        // Pop all arguments.
        for (int i = 0; i < poplen; i++) {
            actual[i] = pop();
        }
        // Field _state is uninitialized when calling push.
        for (int i = poplen; i < 5; i++) {
            actual[i] = CellTypeState.uninit;
        }
        // put them back
        for (int i = 0; i < out.length(); i++) {
            char push_ch = out.charAt(i);
            int idx = push_ch - '1';
            if (!(idx >= 0 && idx < poplen)){
                throw new RuntimeException("wrong arguments");
            }
            push(actual[idx]);
        }
    }
    protected void  do_ldc(int bci){
        Bytecode_loadconstant ldc=new Bytecode_loadconstant(method(), bci);
        ConstantPool cp  = method().constants();
        @RawCType("constantTag")int tag = cp.tag_at(ldc.pool_index()); // idx is index in resolved_references
        @RawCType("BasicType")int       bt  = ldc.result_type();
        CellTypeState   cts;
        if (BasicType.is_reference_type(bt)) {  // could be T_ARRAY with condy
            //assert(!tag.is_string_index() && !tag.is_klass_index(), "Unexpected index tag");
            cts = CellTypeState.make_line_ref(bci);
        } else {
            cts = valCTS;
        }
        ppush1(cts);
    }
    protected void  do_astore(int idx){
        CellTypeState r_or_p = pop();
        if (!r_or_p.is_address() && !r_or_p.is_reference()) {
            // We actually expected ref or pc, but we only report that we expected a ref. It does not
            // really matter (at least for now)
            throw new RuntimeException("wrong type on stack (found: "+r_or_p.to_char()+", expected: {pr})");
        }
        set_var(idx, r_or_p);
    }
    protected void  do_jsr(int delta){
        push(CellTypeState.make_addr(delta));
    }
    protected void  do_field(boolean is_get, boolean is_static, int idx, int bci){
        // Dig up signature for field in constant pool
        ConstantPool cp     = method().constants();
        int nameAndTypeIdx     = cp.name_and_type_ref_index_at(idx);
        int signatureIdx       = cp.signature_ref_index_at(nameAndTypeIdx);
        Symbol signature      = cp.symbol_at(signatureIdx);

        CellTypeState[] temp=new CellTypeState[4];
        CellTypeState[] eff  = signature_to_effect(signature, bci, temp);

        CellTypeState[] in=new CellTypeState[4];
        CellTypeState[] out;
        int i =  0;

        if (is_get) {
            out = eff;
        } else {
            out = epsilonCTS;
            i   = copy_cts(in, eff);
        }
        if (!is_static) {
            in[i++] = CellTypeState.ref;
        }
        in[i] = CellTypeState.bottom;
        pp(in, out);
    }

    private static class ComputeCallStack extends SignatureIterator {
        private CellTypeStateList _effect;
        private int _idx;

        private void set(CellTypeState state){
            _effect.set(_idx++,state);
        }
        private int  length()                         { return _idx; };
        protected void do_type(@RawCType("BasicType")int type){
            this.do_type(type,false);
        }
        protected void do_type(@RawCType("BasicType")int type, boolean for_return) {
            if (for_return && type == BasicType.T_VOID) {
                set(CellTypeState.bottom);
            } else if (BasicType.is_reference_type(type)) {
                set(CellTypeState.ref);
            } else {
                if (!BasicType.is_java_primitive(type)){
                    throw new RuntimeException();
                }
                set(CellTypeState.value);
                if (BasicType.is_double_word_type(type)) {
                    set(CellTypeState.value);
                }
            }
        }

        public ComputeCallStack(Symbol signature){
            super(signature);
        };

        // Compute methods
        public int compute_for_parameters(boolean is_static, CellTypeStateList effect) {
            _idx    = 0;
            _effect = effect;

            if (!is_static)
                effect.set(_idx++,CellTypeState.ref);

            do_parameters_on(this);

            return length();
        };

        public int compute_for_returntype(CellTypeStateList effect) {
            _idx    = 0;
            _effect = effect;
            do_type(return_type(), true);
            set(CellTypeState.bottom);  // Always terminate with a bottom state, so ppush works

            return length();
        }
    };
    protected void  do_method(boolean is_static, boolean is_interface, int idx, int bci){
        // Dig up signature for field in constant pool
        ConstantPool cp  = _method.constants();
        Symbol signature   = cp.signature_ref_at(idx);

        // Parse method signature
        CellTypeStateList out=new CellTypeStateList(4);
        CellTypeStateList in=new CellTypeStateList(256+1);   // Includes result
        ComputeCallStack cse=new ComputeCallStack(signature);

        // Compute return type
        int res_length=  cse.compute_for_returntype(out);

        // Temporary hack.
        if (out.get(0).equal(CellTypeState.ref) && out.get(1).equal(CellTypeState.bottom)) {
            out.set(0,CellTypeState.make_line_ref(bci));
        }

        if (!(res_length<=4)){
            throw new RuntimeException("max value should be vv");
        }

        // Compute arguments
        int arg_length = cse.compute_for_parameters(is_static, in);
        if (!(arg_length<=256)){
            throw new RuntimeException("too many locals");
        }

        // Pop arguments
        for (int i = arg_length - 1; i >= 0; i--) ppop1(in.get(i));// Do args in reverse order.

        // Report results
        if (_report_result_for_send) {
            fill_stackmap_for_opcodes(_itr_send, vars(), stack(), _stack_top);
            _report_result_for_send = false;
        }

        // Push return address
        ppush(out);
    }
    protected void  do_multianewarray(int dims, int bci){
        if (!(dims >= 1)){
            throw new RuntimeException("sanity check");
        }
        for(int i = dims -1; i >=0; i--) {
            ppop1(valCTS);
        }
        ppush1(CellTypeState.make_line_ref(bci));
    }
    protected void  do_monitorenter(int bci){
        CellTypeState actual = pop();
        if (_monitor_top == bad_monitors) {
            return;
        }

        // Bail out when we get repeated locks on an identical monitor.  This case
        // isn't too hard to handle and can be made to work if supporting nested
        // redundant synchronized statements becomes a priority.
        //
        // See also "Note" in do_monitorexit(), below.
        if (actual.is_lock_reference()) {
            _monitor_top = bad_monitors;
            _monitor_safe = false;
            report_monitor_mismatch("nested redundant lock -- bailout...");
            return;
        }

        CellTypeState lock = CellTypeState.make_lock_ref(bci);
        check_type(refCTS, actual);
        if (!actual.is_info_top()) {
            replace_all_CTS_matches(actual, lock);
            monitor_push(lock);
        }
    }
    protected void  do_monitorexit(int bci){
        CellTypeState actual = pop();
        if (_monitor_top == bad_monitors) {
            return;
        }
        check_type(refCTS, actual);
        CellTypeState expected = monitor_pop();
        if (!actual.is_lock_reference() || !expected.equal(actual)) {
            // The monitor we are exiting is not verifiably the one
            // on the top of our monitor stack.  This causes a monitor
            // mismatch.
            _monitor_top = bad_monitors;
            _monitor_safe = false;

            // We need to mark this basic block as changed so that
            // this monitorexit will be visited again.  We need to
            // do this to ensure that we have accounted for the
            // possibility that this bytecode will throw an
            // exception.
            BasicBlock bb = get_basic_block_containing(bci);
            if (bb == null){
                throw new RuntimeException("no basic block for bci");
            }
            bb.set_changed(true);
            bb._monitor_top = bad_monitors;
            report_monitor_mismatch("improper monitor pair");
        } else {
            // This code is a fix for the case where we have repeated
            // locking of the same object in straightline code.  We clear
            // out the lock when it is popped from the monitor stack
            // and replace it with an unobtrusive reference value that can
            // be locked again.
            //
            // Note: when generateOopMap is fixed to properly handle repeated,
            //       nested, redundant locks on the same object, then this
            //       fix will need to be removed at that time.
            replace_all_CTS_matches(actual, CellTypeState.make_line_ref(bci));
        }
    }
    protected void  do_return_monitor_check(){
        if (_monitor_top > 0) {
            // The monitor stack must be empty when we leave the method
            // for the monitors to be properly matched.
            _monitor_safe = false;

            // Since there are no successors to the *return bytecode, it
            // isn't necessary to set _monitor_top to bad_monitors.
            report_monitor_mismatch("non-empty monitor stack at return");
        }
    }
    protected void  do_checkcast(){
        CellTypeState actual = pop();
        check_type(refCTS, actual);
        push(actual);
    }
    protected  CellTypeState[] signature_to_effect( Symbol sig, int bci, CellTypeState[] out){
        // Object and array
        @RawCType("BasicType")int bt = Signature.basic_type(sig);
        if (BasicType.is_reference_type(bt)){
            out[0] = CellTypeState.make_line_ref(bci);
            out[1] = CellTypeState.bottom;
            return out;
        }
        if (BasicType.is_double_word_type(bt)) return vvCTS; // Long and Double
        if (bt == BasicType.T_VOID) return epsilonCTS;       // Void
        return vCTS;                               // Otherwise
    }
    protected  int copy_cts(CellTypeState[] dst, CellTypeState[] src){
        int idx = 0;
        while (!src[idx].is_bottom()) {
            dst[idx] = src[idx];
            idx++;
        }
        return idx;
    }

    // Error handling
//    protected  void  error_work                          ( char *format, va_list ap) ATTRIBUTE_PRINTF(2, 0);
//    protected void  report_error                        ( char *format, ...) ATTRIBUTE_PRINTF(2, 3);
//    protected void  verify_error                        ( char *format, ...) ATTRIBUTE_PRINTF(2, 3);
    protected boolean  got_error()                         { return _got_error; }

    // Create result set
    protected  boolean  _report_result;
    protected  boolean  _report_result_for_send;            // Unfortunatly, stackmaps for sends are special, so we need some extra
    protected BytecodeStream _itr_send;                // variables to handle them properly.

    protected void  report_result                       (){
        // We now want to report the result of the parse
        _report_result = true;
        // Prolog code
        fill_stackmap_prolog(_gc_points);
        // Mark everything changed, then do one interpretation pass.
        for (int i = 0; i<_bb_count; i++) {
            if (_basic_blocks[i].is_reachable()) {
                _basic_blocks[i].set_changed(true);
                interp_bb(_basic_blocks[i]);
            }
        }
        // Note: Since we are skipping dead-code when we are reporting results, then
        // the no. of encountered gc-points might be fewer than the previously number
        // we have counted. (dead-code is a pain - it should be removed before we get here)
        fill_stackmap_epilog();
        // Report initvars
        fill_init_vars(_init_vars);
        _report_result = false;
    }

    // Initvars
    protected List<Integer> _init_vars;

    protected void  initialize_vars(){
        for (int k = 0; k < _init_vars.size(); k++)
            _state.set(_init_vars.get(k),CellTypeState.make_slot_ref(k));
    }
    protected void  add_to_ref_init_set                 (int localNo){
        // Is it already in the set?
        if (_init_vars.contains(localNo) )
            return;
        _init_vars.add(localNo);
    }

    // Conflicts rewrite logic
    protected boolean      _conflict;                      // True, if a conflict occurred during interpretation
    protected int       _nof_refval_conflicts;          // No. of conflicts that require rewrites
    protected int[]     _new_var_map;

    protected void record_refval_conflict(int varNo){
        if (!(varNo>=0 && varNo< _max_locals)){
            throw new RuntimeException("index out of range");
        }

        if (_new_var_map==null) {
            _new_var_map =new int[_max_locals];// NEW_RESOURCE_ARRAY(int, );
            for (int k = 0; k < _max_locals; k++) {
                _new_var_map[k] = k;
            }
        }

        if ( _new_var_map[varNo] == varNo) {
            // Check if max. number of locals has been reached
            if (_max_locals + _nof_refval_conflicts >= 65536){
                throw new RuntimeException("Rewriting exceeded local variable limit");
            }
            _new_var_map[varNo] = _max_locals + _nof_refval_conflicts;
            _nof_refval_conflicts++;
        }
    }
    protected void rewrite_refval_conflicts(){
        if (_nof_refval_conflicts > 0) {
            throw new UnsupportedOperationException("TODO");
        }
//        // We can get here two ways: Either a rewrite conflict was detected, or
//        // an uninitialize reference was detected. In the second case, we do not
//        // do any rewriting, we just want to recompute the reference set with the
//        // new information
//
//        int nof_conflicts = 0;              // Used for debugging only
//
//        if ( _nof_refval_conflicts == 0 )
//            return;
//
//        // Check if rewrites are allowed in this parse.
//        if (!allow_rewrites()) {
//            throw new RuntimeException("Rewriting method not allowed at this stage");
//        }
//
//
//        // Tracing flag
//        _did_rewriting = true;
//        if (_new_var_map == null){
//            throw new RuntimeException("nothing to rewrite");
//        }
//        if (!_conflict){
//            throw new RuntimeException("We should not be here");
//        }
//
//        compute_ret_adr_at_TOS();
//        if (!_got_error) {
//            for (int k = 0; k < _max_locals && !_got_error; k++) {
//                if (_new_var_map[k] != k) {
//                    rewrite_refval_conflict(k, _new_var_map[k]);
//                    if (_got_error) {
//                        return;
//                    }
//                    nof_conflicts++;
//                }
//            }
//        }
//
//        if (!(nof_conflicts == _nof_refval_conflicts)){
//            throw new RuntimeException("sanity check");
//        }
//
//        // Adjust the number of locals
//        method().set_max_locals(_max_locals+_nof_refval_conflicts);
//        _max_locals += _nof_refval_conflicts;
//
//        // That was that...
//        _new_var_map = null;
//        _nof_refval_conflicts = 0;
    }
//    protected void rewrite_refval_conflict(int from, int to){
//        boolean startOver;
//        do {
//            // Make sure that the BytecodeStream is constructed in the loop, since
//            // during rewriting a new method is going to be used, and the next time
//            // around we want to use that.
//            BytecodeStream bcs=new BytecodeStream(_method);
//            startOver = false;
//
//            while( !startOver && !_got_error &&
//                    // test bcs in case method changed and it became invalid
//                    bcs.next() >=0) {
//                startOver = rewrite_refval_conflict_inst(bcs, from, to);
//            }
//        } while (startOver && !_got_error);
//    }
//    protected boolean rewrite_refval_conflict_inst(BytecodeStream itr, int from, int to){
//        @RawCType("Bytecodes::Code")int bc = itr.code();
//        final int[] index=new int[1];
//        int bci = itr.bci();
//
//        if (is_aload(itr, index) && index[0] == from) {
//            return rewrite_load_or_store(itr, Bytecodes.Code._aload, Bytecodes.Code._aload_0, to);
//        }
//
//        if (is_astore(itr, index) && index[0] == from) {
//            if (!stack_top_holds_ret_addr(bci)) {
//                return rewrite_load_or_store(itr, Bytecodes.Code._astore, Bytecodes.Code._astore_0, to);
//            } else {
//            }
//        }
//
//        return false;
//    }

    // The argument to this method is:
    // bc : Current bytecode
    // bcN : either _aload or _astore
    // bc0 : either _aload_0 or _astore_0
//    protected boolean rewrite_load_or_store(BytecodeStream bcs, @RawCType("Bytecodes::Code")int bcN, @RawCType("Bytecodes::Code")int bc0, @RawCType("unsigned int")int varNo){
//        assert(bcN == Bytecodes.Code._astore   || bcN == Bytecodes.Code._aload,   "wrong argument (bcN)");
//        assert(bc0 == Bytecodes.Code._astore_0 || bc0 == Bytecodes.Code._aload_0, "wrong argument (bc0)");
//        int ilen = Bytecodes.length_at(_method, bcs.bcp());
//        int newIlen;
//
//        if (ilen == 4) {
//            // Original instruction was wide; keep it wide for simplicity
//            newIlen = 4;
//        } else if (varNo < 4)
//            newIlen = 1;
//        else if (varNo >= 256)
//            newIlen = 4;
//        else
//            newIlen = 2;
//
//        // If we need to relocate in order to patch the byte, we
//        // do the patching in a temp. buffer, that is passed to the reloc.
//        // The patching of the bytecode stream is then done by the Relocator.
//        // This is neccesary, since relocating the instruction at a certain bci, might
//        // also relocate that instruction, e.g., if a _goto before it gets widen to a _goto_w.
//        // Hence, we do not know which bci to patch after relocation.
//
//        assert(newIlen <= 4, "sanity check");
//        @RawCType("u_char")int inst_buffer[4]; // Max. instruction size is 4.
//        address bcp;
//
//        if (newIlen != ilen) {
//            // Relocation needed do patching in temp. buffer
//            bcp = (address)inst_buffer;
//        } else {
//            bcp = _method.bcp_from(bcs.bci());
//        }
//
//        // Patch either directly in Method* or in temp. buffer
//        if (newIlen == 1) {
//            assert(varNo < 4, "varNo too large");
//            *bcp = bc0 + varNo;
//        } else if (newIlen == 2) {
//            assert(varNo < 256, "2-byte index needed!");
//            *(bcp + 0) = bcN;
//            *(bcp + 1) = varNo;
//        } else {
//            assert(newIlen == 4, "Wrong instruction length");
//            *(bcp + 0) = Bytecodes::_wide;
//            *(bcp + 1) = bcN;
//            Bytes::put_Java_u2(bcp+2, varNo);
//        }
//
//        if (newIlen != ilen) {
//            expand_current_instr(bcs.bci(), ilen, newIlen, inst_buffer);
//        }
//
//
//        return (newIlen != ilen);
//    }

//    protected  void expand_current_instr(int bci, int ilen, int newIlen, u_char inst_buffer[]){
//        JavaThread THREAD = JavaThread.current(); // For exception macros.
//        RelocCallback rcb(this);
//        Relocator rc(_method, &rcb);
//        methodHandle m= rc.insert_space_at(bci, newIlen, inst_buffer, THREAD);
//        if (m.is_null() || HAS_PENDING_EXCEPTION) {
//            report_error("could not rewrite method - exception occurred or bytecode buffer overflow");
//            return;
//        }
//
//        // Relocator returns a new method.
//        _did_relocation = true;
//        _method = m;
//    }
    protected boolean is_astore(BytecodeStream itr, int[] index){
        @RawCType("Bytecodes::Code")int bc = itr.code();
        switch(bc) {
            case Bytecodes.Code._astore_0:
            case Bytecodes.Code._astore_1:
            case Bytecodes.Code._astore_2:
            case Bytecodes.Code._astore_3:
                index[0] = bc - Bytecodes.Code._astore_0;
                return true;
            case Bytecodes.Code._astore:
                index[0] = itr.get_index();
                return true;
            default:
                return false;
        }
    }
    protected boolean is_aload(BytecodeStream itr, int[] index){
        @RawCType("Bytecodes::Code")int bc = itr.code();
        switch(bc) {
            case Bytecodes.Code._aload_0:
            case Bytecodes.Code._aload_1:
            case Bytecodes.Code._aload_2:
            case Bytecodes.Code._aload_3:
                index[0] = bc - Bytecodes.Code._aload_0;
                return true;

            case Bytecodes.Code._aload:
                index[0] = itr.get_index();
                return true;

            default:
                return false;
        }
    }

    protected interface JumpClosure{
        void process(GenerateOopMap c, int bcpDelta, int[] data);
    }
    // Helper method. Can be used in subclasses to fx. calculate gc_points. If the current instuction
    // is a control transfer, then calls the jmpFct all possible destinations.
    protected void  ret_jump_targets_do(BytecodeStream bcs, JumpClosure jmpFct, int varNo,int[] data){
        CellTypeState ra = vars().get(varNo);
        if (!ra.is_good_address()){
            throw new RuntimeException("ret returns from two jsr subroutines?");
        }
        int target = ra.get_info();

        RetTableEntry rtEnt = _rt.find_jsrs_for_target(target);
        int bci = bcs.bci();
        for (int i = 0; i < rtEnt.nof_jsrs(); i++) {
            int target_bci = rtEnt.jsrs(i);
            // Make sure a jrtRet does not set the changed bit for dead basicblock.
            BasicBlock jsr_bb    = get_basic_block_containing(target_bci - 1);
            boolean alive = jsr_bb.is_alive();
            if (alive) {
                jmpFct.process(this, target_bci, data);
            }
        }
    }
    protected boolean  jump_targets_do                     (BytecodeStream bcs, JumpClosure jmpFct, int[] data){
        int bci = bcs.bci();

        switch (bcs.code()) {
            case Bytecodes.Code._ifeq:
            case Bytecodes.Code._ifne:
            case Bytecodes.Code._iflt:
            case Bytecodes.Code._ifge:
            case Bytecodes.Code._ifgt:
            case Bytecodes.Code._ifle:
            case Bytecodes.Code._if_icmpeq:
            case Bytecodes.Code._if_icmpne:
            case Bytecodes.Code._if_icmplt:
            case Bytecodes.Code._if_icmpge:
            case Bytecodes.Code._if_icmpgt:
            case Bytecodes.Code._if_icmple:
            case Bytecodes.Code._if_acmpeq:
            case Bytecodes.Code._if_acmpne:
            case Bytecodes.Code._ifnull:
            case Bytecodes.Code._ifnonnull:
                jmpFct.process(this,bcs.dest(),data);
                // Class files verified by the old verifier can have a conditional branch
                // as their last bytecode, provided the conditional branch is unreachable
                // during execution.  Check if this instruction is the method's last bytecode
                // and, if so, don't call the jmpFct.
                if (bci + 3 < method().code_size()) {
                    jmpFct.process(this, bci + 3, data);
                }
                break;

            case Bytecodes.Code._goto:
                jmpFct.process(this, bcs.dest(), data);
                break;
            case Bytecodes.Code._goto_w:
                jmpFct.process(this, bcs.dest_w(), data);
                break;
            case Bytecodes.Code._tableswitch:
            {
                Bytecode_tableswitch tableswitch=new Bytecode_tableswitch(method(), bcs.bcp());
                int len = tableswitch.length();

                jmpFct.process(this, bci + tableswitch.default_offset(), data); /* Default. jump address */
                while (--len >= 0) {
                    jmpFct.process(this, bci + tableswitch.dest_offset_at(len), data);
                }
                break;
            }

            case Bytecodes.Code._lookupswitch:
            {
                Bytecode_lookupswitch lookupswitch=new Bytecode_lookupswitch(method(), bcs.bcp());
                int npairs = lookupswitch.number_of_pairs();
                jmpFct.process(this, bci + lookupswitch.default_offset(), data); /* Default. */
                while(--npairs >= 0) {
                    LookupswitchPair pair = lookupswitch.pair_at(npairs);
                    jmpFct.process(this, bci + pair.offset(), data);
                }
                break;
            }
            case Bytecodes.Code._jsr:
                if (bcs.is_wide()){
                    throw new RuntimeException("sanity check");
                }
                jmpFct.process(this, bcs.dest(), data);



                break;
            case Bytecodes.Code._jsr_w:
                jmpFct.process(this, bcs.dest_w(), data);
                break;
            case Bytecodes.Code._wide:
                throw new RuntimeException("ShouldNotReachHere()");
            case Bytecodes.Code._athrow:
            case Bytecodes.Code._ireturn:
            case Bytecodes.Code._lreturn:
            case Bytecodes.Code._freturn:
            case Bytecodes.Code._dreturn:
            case Bytecodes.Code._areturn:
            case Bytecodes.Code._return:
            case Bytecodes.Code._ret:
                break;
            default:
                return true;
        }
        return false;
    }

    protected String state_vec_to_string(CellTypeStateList vec, int len) {
        for (int i = 0; i < len; i++) _state_vec_buf[i] = vec.get(i).to_char();
        _state_vec_buf[len] = 0;
        return String.copyValueOf(_state_vec_buf,0,len-1);
    }

    public GenerateOopMap(Method method){
        // We have to initialize all variables here, that can be queried directly
        _method = method;
        _max_locals=0;
        _init_vars = null;
        _rt = new RetTable();
    }

    // Compute the map - returns true on success and false on error.
    public boolean compute_map(){
        // Initialize values
        _got_error      = false;
        _conflict       = false;
        _max_locals     = method().max_locals();
        _max_stack      = method().max_stack();
        _has_exceptions = (method().has_exception_handler())?1:0;
        _nof_refval_conflicts = 0;
        _init_vars      = new ArrayList<>(5);  // There are seldom more than 5 init_vars
        _report_result  = false;
        _report_result_for_send = false;
        _new_var_map    = null;
        //_ret_adr_tos    = new GrowableArray<intptr_t>(5);  // 5 seems like a good number;
        _did_rewriting  = false;
        _did_relocation = false;

//        if (TraceNewOopMapGeneration) {
//            tty.print("Method name: %s\n", method().name().toString());
//            if (Verbose) {
//                _method->print_codes();
//                tty->print_cr("Exception table:");
//                ExceptionTable excps(method());
//                for(int i = 0; i < excps.length(); i ++) {
//                    tty->print_cr("[%d - %d] -> %d",
//                            excps.start_pc(i), excps.end_pc(i), excps.handler_pc(i));
//                }
//            }
//        }

        // if no code - do nothing
        // compiler needs info
        if (method().code_size() == 0 || _max_locals + method().max_stack() == 0) {
            fill_stackmap_prolog(0);
            fill_stackmap_epilog();
            return true;
        }
        // Step 1: Compute all jump targets and their return value
        if (!_got_error)
            _rt.compute_ret_table(_method);

        // Step 2: Find all basic blocks and count GC points
        if (!_got_error)
            mark_bbheaders_and_count_gc_points();

        // Step 3: Calculate stack maps
        if (!_got_error)
            do_interpretation();

        // Step 4:Return results
        if (!_got_error && report_results())
            report_result();

        return !_got_error;
    }
    // Returns the exception related to any error, if the map was computed by a suitable JavaThread.
    public String exception() { return _exception; }

    public void result_for_basicblock(int bci){ // Do a callback on fill_stackmap_for_opcodes for basicblock containing bci
        // We now want to report the result of the parse
        _report_result = true;

        // Find basicblock and report results
        BasicBlock bb = get_basic_block_containing(bci);
        if (bb==null){
            throw new RuntimeException("no basic block for bci");
        }
        if (!bb.is_reachable()){
            throw new RuntimeException("getting result from unreachable basicblock");
        }
        bb.set_changed(true);
        interp_bb(bb);
    }

    // Query
    public int max_locals(){ return _max_locals; }
    public Method method(){ return _method; }

    public boolean did_rewriting(){ return _did_rewriting; }
    public boolean did_relocation(){ return _did_relocation; }

    // Monitor query
    public boolean monitor_safe()                              { return _monitor_safe; }

    // Specialization methods. Intended use:
    // - possible_gc_point must return true for every bci for which the stackmaps must be returned
    // - fill_stackmap_prolog is called just before the result is reported. The arguments tells the estimated
    //   number of gc points
    // - fill_stackmap_for_opcodes is called once for each bytecode index in order (0...code_length-1)
    // - fill_stackmap_epilog is called after all results has been reported. Note: Since the algorithm does not report
    //   stackmaps for deadcode, fewer gc_points might have been encounted than assumed during the epilog. It is the
    //   responsibility of the subclass to count the correct number.
    // - fill_init_vars are called once with the result of the init_vars computation
    //
    // All these methods are used during a call to: compute_map. Note: Non of the return results are valid
    // after compute_map returns, since all values are allocated as resource objects.
    //
    // All virtual method must be implemented in subclasses
    public boolean allow_rewrites             (){ return false; }
    public boolean report_results             (){ return true;  }
    public boolean report_init_vars           (){ return true;  }
    public boolean possible_gc_point          (BytecodeStream bcs)           { throw new RuntimeException("ShouldNotReachHere");}
    public void fill_stackmap_prolog       (int nof_gc_points)             { throw new RuntimeException("ShouldNotReachHere"); }
    public void fill_stackmap_epilog       ()                              { throw new RuntimeException("ShouldNotReachHere"); }
    public void fill_stackmap_for_opcodes  (BytecodeStream bcs,
                                             CellTypeStateList vars,
                                            CellTypeStateList stack,
                                             int stackTop){ throw new RuntimeException("ShouldNotReachHere"); }
    public void fill_init_vars             (List<Integer> init_vars) { throw new RuntimeException("ShouldNotReachHere"); }
}
