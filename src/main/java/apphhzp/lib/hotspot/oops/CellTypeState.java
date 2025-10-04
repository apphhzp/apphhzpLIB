package apphhzp.lib.hotspot.oops;

import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.helfy.JVM.nthBit;
import static apphhzp.lib.helfy.JVM.right_n_bits;

public class CellTypeState {


    private @RawCType("unsigned int")final int _state;

    // Masks for separating the BITS and INFO portions of a CellTypeState
    private static final int info_mask            = right_n_bits(28),
            bits_mask            = ~info_mask;

    // These constant are used for manipulating the BITS portion of a
    // CellTypeState
    private static final int uninit_bit           = nthBit(31),
            ref_bit              = nthBit(30),
            val_bit              = nthBit(29),
            addr_bit             = nthBit(28),
            live_bits_mask       = bits_mask & ~uninit_bit;

    // These constants are used for manipulating the INFO portion of a
    // CellTypeState
    private static final int top_info_bit         = nthBit(27),
            not_bottom_info_bit  = nthBit(26),
            info_data_mask       = right_n_bits(26),
            info_conflict        = info_mask ;

    // Within the INFO data, these values are used to distinguish different
    // kinds of references.
    private static final int ref_not_lock_bit     = nthBit(25),  // 0 if this reference is locked as a monitor
            ref_slot_bit         = nthBit(24),  // 1 if this reference is a "slot" reference,
            // 0 if it is a "line" reference.
            ref_data_mask        = right_n_bits(24);


    // These values are used to initialize commonly used CellTypeState
    // constants.
    private static final int bottom_value         = 0,
            uninit_value         = uninit_bit | info_conflict,
            ref_value            = ref_bit,
            ref_conflict         = ref_bit | info_conflict,
            val_value            = val_bit | info_conflict,
            addr_value           = addr_bit,
            addr_conflict        = addr_bit | info_conflict ;


    private CellTypeState(int val) {
        this._state=val;
    }

    // Since some C++ constructors generate poor code for declarations of the
    // form...
    //
    //   CellTypeState vector[length];
    //
    // ...we avoid making a constructor for this class.  CellTypeState values
    // should be constructed using one of the make_* methods:

    public static CellTypeState make_any(int state) {
        CellTypeState s=new CellTypeState(state);
        // Causes SS10 warning.
        // assert(s.is_valid_state(), "check to see if CellTypeState is valid");
        return s;
    }

    public static CellTypeState make_bottom() {
        return make_any(0);
    }

    public static CellTypeState make_top() {
        return make_any(-1);
    }

    public static CellTypeState make_addr(int bci){
        if (!((bci >= 0) && (bci < info_data_mask))){
            throw new RuntimeException("check to see if ret addr is valid");
        }
        return make_any(addr_bit | not_bottom_info_bit | (bci & info_data_mask));
    }

    public static CellTypeState make_slot_ref(int slot_num){
        if (!(slot_num >= 0 && slot_num < ref_data_mask)){
            throw new RuntimeException("slot out of range");
        }
        return make_any(ref_bit | not_bottom_info_bit | ref_not_lock_bit | ref_slot_bit |
                (slot_num & ref_data_mask));
    }

    public static CellTypeState make_line_ref(int bci){
        if (!(bci >= 0 && bci < ref_data_mask)){
            throw new RuntimeException("line out of range");
        }
        return make_any(ref_bit | not_bottom_info_bit | ref_not_lock_bit |
                (bci & ref_data_mask));
    }

    public static CellTypeState make_lock_ref(int bci){
        if (!(bci >= 0 && bci < ref_data_mask)){
            throw new RuntimeException("line out of range");
        }
        return make_any(ref_bit | not_bottom_info_bit | (bci & ref_data_mask));
    }

    // Query methods:
    public boolean is_bottom()                { return _state == 0; }
    public boolean is_live()                  { return ((_state & live_bits_mask) != 0); }
    public boolean is_valid_state() {
        // Uninitialized and value cells must contain no data in their info field:
        if ((can_be_uninit() || can_be_value()) && !is_info_top()) {
            return false;
        }
        // The top bit is only set when all info bits are set:
        if (is_info_top() && ((_state & info_mask) != info_mask)) {
            return false;
        }
        // The not_bottom_bit must be set when any other info bit is set:
        if (is_info_bottom() && ((_state & info_mask) != 0)) {
            return false;
        }
        return true;
    }

    public boolean is_address()               { return ((_state & bits_mask) == addr_bit); }
    public boolean is_reference()             { return ((_state & bits_mask) == ref_bit); }
    public boolean is_value()                 { return ((_state & bits_mask) == val_bit); }
    public boolean is_uninit()                { return ((_state & bits_mask) == uninit_bit); }

    public boolean can_be_address()           { return ((_state & addr_bit) != 0); }
    public boolean can_be_reference()         { return ((_state & ref_bit) != 0); }
    public boolean can_be_value()             { return ((_state & val_bit) != 0); }
    public boolean can_be_uninit()            { return ((_state & uninit_bit) != 0); }

    public boolean is_info_bottom()            { return ((_state & not_bottom_info_bit) == 0); }
    public boolean is_info_top()              { return ((_state & top_info_bit) != 0); }
    public int  get_info(){
        if (!((!is_info_top() && !is_info_bottom()))){
            throw new RuntimeException("check to make sure top/bottom info is not used");
        }
        return (_state & info_data_mask);
    }

    public boolean is_good_address(){ return is_address() && !is_info_top(); }
    public boolean is_lock_reference()  {
        return ((_state & (bits_mask | top_info_bit | ref_not_lock_bit)) == ref_bit);
    }
    public boolean is_nonlock_reference()  {
        return ((_state & (bits_mask | top_info_bit | ref_not_lock_bit)) == (ref_bit | ref_not_lock_bit));
    }

    public boolean equal(CellTypeState a)  { return _state == a._state; }
    public boolean equal_kind(CellTypeState a)  {
        return (_state & bits_mask) == (a._state & bits_mask);
    }

    public char to_char(){
        if (can_be_reference()) {
            if (can_be_value() || can_be_address())
                return '#';    // Conflict that needs to be rewritten
            else
                return 'r';
        } else if (can_be_value())
            return 'v';
        else if (can_be_address())
            return 'p';
        else if (can_be_uninit())
            return ' ';
        else
            return '@';
    }

    // Merge
    public CellTypeState merge (CellTypeState cts, int slot){
        CellTypeState result;

        if (!(!is_bottom() && !cts.is_bottom())){
            throw new RuntimeException("merge of bottom values is handled elsewhere");
        }

        result=new CellTypeState(_state | cts._state);

        // If the top bit is set, we don't need to do any more work.
        if (!result.is_info_top()){
            if (!((result.can_be_address() || result.can_be_reference()))){
                throw new RuntimeException("only addresses and references have non-top info");
            }

            if (!equal(cts)) {
                // The two values being merged are different.  Raise to top.
                if (result.is_reference()) {
                    result = CellTypeState.make_slot_ref(slot);
                } else {
                    result=new CellTypeState(result._state| info_conflict);
                }
            }
        }
        if (!result.is_valid_state()){
            throw new RuntimeException("checking that CTS merge maintains legal state");
        }

        return result;
    }

    // Print a detailed CellTypeState.  Indicate all bits that are set.  If
    // the CellTypeState represents an address or a reference, print the
    // value of the additional information.
    public void print(PrintStream os) {
        if (can_be_address()) {
            os.print("(p");
        } else {
            os.print("( ");
        }
        if (can_be_reference()) {
            os.print("r");
        } else {
            os.print(" ");
        }
        if (can_be_value()) {
            os.print("v");
        } else {
            os.print(" ");
        }
        if (can_be_uninit()) {
            os.print("u|");
        } else {
            os.print(" |");
        }
        if (is_info_top()) {
            os.print("Top)");
        } else if (is_info_bottom()) {
            os.print("Bot)");
        } else {
            if (is_reference()) {
                int info = get_info();
                int data = info & ~(ref_not_lock_bit | ref_slot_bit);
                if ((info & ref_not_lock_bit)!=0) {
                    // Not a monitor lock reference.
                    if ((info & ref_slot_bit)!=0){
                        // slot
                        os.print("slot"+data+")");
                    } else {
                        // line
                        os.print("line"+data+")");
                    }
                } else {
                    // lock
                    os.print("lock"+data+")");
                }
            } else {
                os.print(get_info()+")");
            }
        }
    }

    // Default values of common values
    public static final CellTypeState bottom = CellTypeState.make_bottom();;
    public static final CellTypeState uninit=CellTypeState.make_any(uninit_value);
    public static final CellTypeState ref=CellTypeState.make_any(ref_conflict);
    public static final CellTypeState value=CellTypeState.make_any(val_value);
    public static final CellTypeState refUninit=CellTypeState.make_any(ref_conflict | uninit_value);
    public static final CellTypeState top=CellTypeState.make_top();
    public static final CellTypeState addr=CellTypeState.make_any(addr_conflict);
}
