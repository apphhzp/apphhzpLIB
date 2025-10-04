package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.VMRegImpl;
import apphhzp.lib.hotspot.runtime.x86.X86RegisterMap;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** A companion structure used for stack traversal. The RegisterMap contains
 misc. information needed in order to do correct stack traversal of stack
 frames.  Hence, it must always be passed in as an argument to
 frame::sender(RegisterMap*).
<br>
 In particular,<br>
   1) It provides access to the thread for which the stack belongs.  The
      thread object is needed in order to get sender of a deoptimized frame.
 <br>
   2) It is used to pass information from a callee frame to its caller
      frame about how the frame should be traversed.  This is used to let
      the caller frame take care of calling oops-do of out-going
      arguments, when the callee frame is not instantiated yet.  This
      happens, e.g., when a compiled frame calls into
      resolve_virtual_call.  (Hence, it is critical that the same
      RegisterMap object is used for the entire stack walk.  Normally,
      this is hidden by using the StackFrameStream.)  This is used when
      doing follow_oops and oops_do.
 <br>
   3) The RegisterMap keeps track of the values of callee-saved registers
      from frame to frame (hence, the name).  For some stack traversal the
      values of the callee-saved registers does not matter, e.g., if you
      only need the static properties such as frame type, pc, and such.
      Updating of the RegisterMap can be turned off by instantiating the
      register map as: RegisterMap map(thread, false);*/
public abstract class RegisterMap {
    //typedef julong LocationValidType;
    public static final int
        reg_count = JVM.intConstant("ConcreteRegisterImpl::number_of_registers"),
                location_valid_type_size = JVM.type("julong").size*8,
                location_valid_size = (reg_count+location_valid_type_size-1)/location_valid_type_size;
    protected @RawCType("intptr_t*") long[] _location;    // Location of registers (intptr_t* looks better than address in the debugger)
    protected @RawCType("LocationValidType") long[] _location_valid;
    protected boolean        _include_argument_oops;   // Should include argument_oop marked locations for compiler
    protected JavaThread _thread;                  // Reference to current thread
    protected boolean        _update_map;              // Tells if the register map need to be
    // updated when traversing the stack
    protected boolean        _process_frames;          // Should frames be processed by stack watermark barriers?

    public static RegisterMap create(JavaThread thread){
        return create(thread,true,true);
    }
    public static RegisterMap create(JavaThread thread, boolean update_map){
        return create(thread,update_map,true);
    }
    public static RegisterMap create(JavaThread thread, boolean update_map, boolean process_frames){
        if (PlatformInfo.isX86()){
            return new X86RegisterMap(thread,update_map,process_frames);
        }else {
            throw new RuntimeException("Unsupported platform");
        }
    }
    public static RegisterMap create(RegisterMap map){
        if (PlatformInfo.isX86()){
            return new X86RegisterMap(map);
        }else {
            throw new RuntimeException("Unsupported platform");
        }
    }

    protected RegisterMap(JavaThread thread, boolean update_map, boolean process_frames){
        _location=new long[reg_count];
        _location_valid=new long[location_valid_size];

        _thread         = thread;
        _update_map     = update_map;
        _process_frames = process_frames;
        clear();
        //debug_only(_update_for_id = NULL;)
        if (!JVM.product){
            for (int i = 0; i < reg_count ; i++ ){
                _location[i] = 0L;
            }
        }
    }

    protected RegisterMap(RegisterMap map){
        _location=new long[reg_count];
        _location_valid=new long[location_valid_size];
        if (map == this){
            throw new IllegalArgumentException("bad initialization parameter");
        }
        if (map==null){
            throw new IllegalArgumentException("RegisterMap must be present");
        }
        _thread                = map.thread();
        _update_map            = map.update_map();
        _process_frames        = map.process_frames();
        _include_argument_oops = map.include_argument_oops();
        //debug_only(_update_for_id = map._update_for_id;)
        pd_initialize_from(map);
        if (update_map()) {
            for(int i = 0; i < location_valid_size; i++) {
                @RawCType("julong") long bits = !update_map() ? 0 : map._location_valid[i];
                _location_valid[i] = bits;
                // for whichever bits are set, pull in the corresponding map->_location
                int j = i*location_valid_type_size;
                while (bits != 0) {
                    if ((bits & 1)!=0){
                        if (!(0 <= j && j < reg_count)){
                            throw new RuntimeException("range check");
                        }
                        _location[j] = map._location[j];
                    }
                    bits >>>= 1;
                    j += 1;
                }
            }
        }
    }

    public @RawCType("address")long location(VMReg reg)  {
        int index = reg.value() / location_valid_type_size;
        if (!(0 <= reg.value() && reg.value() < reg_count)){
            throw new RuntimeException("range check");
        }
        if (!(0 <= index && index < location_valid_size)){
            throw new RuntimeException("range check");
        }
        if ((_location_valid[index] & (1L << (reg.value() % location_valid_type_size)))!=0) {
            return _location[reg.value()];
        } else {
            return pd_location(reg);
        }
    }

    public @RawCType("address") long location(VMReg base_reg, int slot_idx) {
        if (slot_idx > 0) {
            return pd_location(base_reg, slot_idx);
        } else {
            return location(base_reg);
        }
    }

    public abstract @RawCType("address") long pd_location(VMReg reg) ;
    public abstract @RawCType("address") long pd_location(VMReg base_reg, int slot_idx);

    public void set_location(VMReg reg,@RawCType("address")long loc) {
        int index = reg.value() / location_valid_type_size;
        if (!(0 <= reg.value() && reg.value() < reg_count)){
            throw new RuntimeException("range check");
        }
        if (!(0 <= index && index < location_valid_size)){
            throw new RuntimeException("range check");
        }
        if (!_update_map){
            throw new RuntimeException("updating map that does not need updating");
        }
        _location[reg.value()] =  loc;
        _location_valid[index] |= (1L << (reg.value() % location_valid_type_size));
        if (JVM.ENABLE_EXTRA_CHECK) {
            check_location_valid();
        }
    }

    public abstract void check_location_valid();

    // Called by an entry frame.
    public void clear(){
        set_include_argument_oops(true);
        if (_update_map) {
            for(int i = 0; i < location_valid_size; i++) {
                _location_valid[i] = 0;
            }
            pd_clear();
        } else {
            pd_initialize();
        }
    }

    public boolean include_argument_oops()      { return _include_argument_oops; }
    public void set_include_argument_oops(boolean f)  { _include_argument_oops = f; }

    public JavaThread thread()   { return _thread; }
    public boolean update_map()      { return _update_map; }
    public boolean process_frames()  { return _process_frames; }
    public abstract void pd_clear();
    public abstract void pd_initialize();
    public abstract void pd_initialize_from(RegisterMap map);

    public void print_on(PrintStream st){
        st.println("Register map");
        for(int i = 0; i < reg_count; i++) {
            VMReg r = VMRegImpl.as_VMReg(i);
            @RawCType("intptr_t*")long src = location(r);
            if (src != 0L){
                r.print_on(st);
                st.print(" [" +"0x"+Long.toHexString(src)+"] = ");
                if ((src & (JVM.type("intptr_t").size-1)) != 0) {
                    st.println("<misaligned>");
                } else {
                    st.println("0x"+Long.toHexString(unsafe.getAddress(src)));
                }
            }
        }
    }
    public abstract RegisterMap clone();
}
