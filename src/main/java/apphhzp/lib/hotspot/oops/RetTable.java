package apphhzp.lib.hotspot.oops;

import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.BytecodeStream;
import apphhzp.lib.hotspot.util.RawCType;

public class RetTable {
    private RetTableEntry _first;
    private static final int _init_nof_entries=10;;
    // Adds entry to list
    private void add_jsr(int return_bci, int target_bci) {
        RetTableEntry entry = _first;

        // Scan table for entry
        for (;entry!=null && entry.target_bci() != target_bci; entry = entry.next());

        if (entry==null){
            // Allocate new entry and put in list
            entry = new RetTableEntry(target_bci, _first);
            _first = entry;
        }

        // Now "entry" is set.  Make sure that the entry is initialized
        // and has room for the new jsr.
        entry.add_jsr(return_bci);
    }
    public RetTable(){ _first = null; }
    public void compute_ret_table(Method method){
        BytecodeStream i=new BytecodeStream(method);
        @RawCType("Bytecodes::Code")int bytecode;

        while( (bytecode = i.next()) >= 0) {
            switch (bytecode) {
                case Bytecodes.Code._jsr:
                    add_jsr(i.next_bci(), i.dest());
                    break;
                case Bytecodes.Code._jsr_w:
                    add_jsr(i.next_bci(), i.dest_w());
                    break;
                default:
                    break;
            }
        }
    }
    public void update_ret_table(int bci, int delta){
        RetTableEntry cur = _first;
        while(cur!=null) {
            cur.add_delta(bci, delta);
            cur = cur.next();
        }
    }
    public RetTableEntry find_jsrs_for_target(int targBci){
        RetTableEntry cur = _first;

        while(cur!=null){
            if (cur.target_bci() == -1){
                throw new RuntimeException("sanity check");
            }
            if (cur.target_bci() == targBci)  return cur;
            cur = cur.next();
        }
        throw new RuntimeException("ShouldNotReachHere()");
    }
}
