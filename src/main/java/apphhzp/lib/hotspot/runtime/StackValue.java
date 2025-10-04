package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.Location;
import apphhzp.lib.hotspot.code.VMRegImpl;
import apphhzp.lib.hotspot.code.scope.*;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.hotspot.utilities.BasicType.T_INT;
import static apphhzp.lib.hotspot.utilities.BasicType.T_OBJECT;

public class StackValue{
    private @RawCType("BasicType")int _type;
    private @RawCType("intptr_t")long  _integer_value; // Blank java stack slot value
    private @RawCType("Handle") Oop _handle_value;  // Java stack slot value interpreted as a Handle


    public StackValue(@RawCType("intptr_t")long value) {
        _type              = T_INT;
        _integer_value     = value;
    }
    public StackValue(Oop value) {
        this(value,0);
    }

    public StackValue(Oop value, @RawCType("intptr_t")long scalar_replaced) {
        _type                = T_OBJECT;
        _integer_value       = scalar_replaced;
        _handle_value        = value;
        if (!(_integer_value == 0 ||  _handle_value==null)){
            throw new IllegalArgumentException("not null object should not be marked as scalar replaced");
        }
    }

    public StackValue() {
        _type           = BasicType.T_CONFLICT;
        _integer_value  = 0;
    }

    // Only used during deopt- preserve object type.
    public StackValue(@RawCType("intptr_t")long o, @RawCType("BasicType")int t) {
        if (!(t == T_OBJECT)){
            throw new RuntimeException("should not be used");
        }
        _type          = t;
        _integer_value = o;
    }

    @Nullable
    public Oop get_obj(){
        if (!(type() == T_OBJECT)){
            throw new RuntimeException("type check");
        }
        return _handle_value;
    }

    public boolean obj_is_scalar_replaced(){
        if (!(type() == T_OBJECT)){
            throw new RuntimeException("type check");
        }
        return _integer_value != 0;
    }

    public void set_obj(Oop value){
        if (!(type() == T_OBJECT)){
            throw new RuntimeException("type check");
        }
        _handle_value = value;
    }

    public @RawCType("intptr_t")long get_int(){
        if (!(type() == T_INT)){
            throw new RuntimeException("type check");
        }
        return _integer_value;
    }

    // For special case in deopt.
    public @RawCType("intptr_t")long get_int(@RawCType("BasicType")int t){
        if (!(t == T_OBJECT && type() == T_OBJECT)){
            throw new RuntimeException("type check");
        }
        return _integer_value;
    }

    public void set_int(@RawCType("intptr_t")long value){
        if (!(type() == T_INT)){
            throw new RuntimeException("type check");
        }
        _integer_value = value;
    }

    public @RawCType("BasicType")int type() { return  _type; }

    public boolean equal(StackValue value) {
        if (_type != value._type) {
            return false;
        }
        if (_type == T_OBJECT)
            return (_handle_value.equals(value._handle_value));
        else {
            if (!(_type == T_INT)){
                throw new RuntimeException("sanity check");
            }
            long addr=unsafe.allocateMemory(JVM.oopSize*2L);
            unsafe.putAddress(addr,_integer_value);
            unsafe.putAddress(addr+JVM.oopSize,value._integer_value);
            // [phh] compare only low addressed portions of intptr_t slots
            boolean re=(unsafe.getInt(addr) == unsafe.getInt(addr+JVM.oopSize));
            unsafe.freeMemory(addr);
            return re;
        }
    }
    public static StackValue create_stack_value(Frame fr,RegisterMap reg_map, ScopeValue sv){
        if (sv.is_location()) {
            // Stack or register value
            Location loc = ((LocationValue)sv).location();

            // First find address of value

            @RawCType("address")long value_addr = loc.is_register()
                    // Value was in a callee-save register
                    ? reg_map.location(VMRegImpl.as_VMReg(loc.register_number()))
                    // Else value was directly saved on the stack. The frame's original stack pointer,
                    // before any extension by its callee (due to Compiler1 linkage on SPARC), must be used.
                    : (fr.unextended_sp()) + loc.stack_offset();

            // Then package it right depending on type
            // Note: the transfer of the data is thru a union that contains
            // an intptr_t. This is because an interpreter stack slot is
            // really an intptr_t. The use of a union containing an intptr_t
            // ensures that on a 64 bit platform we have proper alignment
            // and that we store the value where the interpreter will expect
            // to find it (i.e. proper endian). Similarly on a 32bit platform
            // using the intptr_t ensures that when a value is larger than
            // a stack slot (jlong/jdouble) that we capture the proper part
            // of the value for the stack slot in question.
            //
            int type = loc.type();
            if (type == Location.Type.float_in_dbl) {// Holds a float in a double register?
                // The callee has no clue whether the register holds a float,
                // double or is unused.  He always saves a double.  Here we know
                // a double was saved, but we only want a float back.  Narrow the
                // saved double to the float that the JVM wants.
                if (!loc.is_register()) {
                    throw new RuntimeException("floats always saved to stack in 1 word");
                }

                long addr = unsafe.allocateMemory(8);
                unsafe.putAddress(addr, 0xDEADDEAFDEADDEAFL);
                unsafe.putFloat(addr, (float) unsafe.getDouble(value_addr));
                StackValue re = new StackValue(unsafe.getAddress(addr));
                unsafe.freeMemory(addr);

                return re; // 64-bit high half is stack junk
            } else if (type == Location.Type.int_in_long) {// Holds an int in a long register?
                // The callee has no clue whether the register holds an int,
                // long or is unused.  He always saves a long.  Here we know
                // a long was saved, but we only want an int back.  Narrow the
                // saved long to the int that the JVM wants.
                if (!loc.is_register()) {
                    throw new RuntimeException("ints always saved to stack in 1 word");
                }
                long addr = unsafe.allocateMemory(8);
                unsafe.putAddress(addr, 0xDEADDEAFDEADDEAFL);
                unsafe.putInt(addr, (int) unsafe.getLong(value_addr));
                StackValue re = new StackValue(unsafe.getAddress(addr));
                unsafe.freeMemory(addr);
                return re; // 64-bit high half is stack junk
//#ifdef _LP64
            } else if (type == Location.Type.dbl) {// Double value in an aligned adjacent pair
                return new StackValue(unsafe.getAddress(value_addr));
            } else if (type == Location.Type.lng) {// Long   value in an aligned adjacent pair
                return new StackValue(unsafe.getAddress(value_addr));
            } else if (type == Location.Type.narrowoop) {//                    union { intptr_t p; narrowOop noop;} value;
//                    value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
                long noop;
                if (loc.is_register()) {
                    // The callee has no clue whether the register holds an int,
                    // long or is unused.  He always saves a long.  Here we know
                    // a long was saved, but we only want an int back.  Narrow the
                    // saved long to the int that the JVM wants.  We can't just
                    // use narrow_oop_cast directly, because we don't know what
                    // the high bits of the value might be.
                    @RawCType("juint") int narrow_value = (int) (unsafe.getLong(value_addr) & 0xffffffffL);
                    noop = narrow_value & 0xffffffffL;
                } else {
                    noop = unsafe.getInt(value_addr) & 0xffffffffL;
                }
                // Decode narrowoop
                OopDesc val = OopDesc.of(OopDesc.decodeOop(noop));
                Oop h = new Oop(val); // Wrap a handle around the oop
                return new StackValue(h);
//#endif
            } else if (type == Location.Type.oop) {
                OopDesc val = OopDesc.of(unsafe.getAddress(value_addr));
                if (JVM.isLP64 && OopDesc.narrow_oop_base == (val.address)) {
                    // Compiled code may produce decoded oop = narrow_oop_base
                    // when a narrow oop implicit null check is used.
                    // The narrow_oop_base could be NULL or be the address
                    // of the page below heap. Use NULL value for both cases.
                    val = null;
                }
                Oop h = new Oop(val); // Wrap a handle around the oop
                return new StackValue(h);
            } else if (type == Location.Type.addr) {
                loc.print_on(System.err);
                throw new RuntimeException("ShouldNotReachHere()");// both C1 and C2 now inline jsrs
            } else if (type == Location.Type.normal) {// Just copy all other bits straight through
                long addr = unsafe.allocateMemory(8);
                unsafe.putAddress(addr, 0xDEADDEAFDEADDEAFL);
                unsafe.putInt(addr, unsafe.getInt(value_addr));
                StackValue re = new StackValue(unsafe.getAddress(addr));
                unsafe.freeMemory(addr);
                return re;
            } else if (type == Location.Type.invalid) {
                return new StackValue();
            } else if (type == Location.Type.vector) {
                loc.print_on(System.err);
                throw new RuntimeException("ShouldNotReachHere()"); // should be handled by VectorSupport::allocate_vector()
            }
            loc.print_on(System.err);
            throw new RuntimeException("ShouldNotReachHere()");

        } else if (sv.is_constant_int()) {
            // Constant int: treat same as register int.
            long addr=unsafe.allocateMemory(8);
            unsafe.putAddress(addr,0xDEADDEAFDEADDEAFL);
            unsafe.putInt(addr,((ConstantIntValue)sv).value());
            StackValue re=new StackValue(unsafe.getAddress(addr));
            unsafe.freeMemory(addr);
            return re;
        } else if (sv.is_constant_oop()) {
            // constant oop
            return new StackValue(sv.as_ConstantOopReadValue().value());
//#ifdef _LP64
        } else if (JVM.isLP64&&sv.is_constant_double()) {
            // Constant double in a single stack slot
//            union { intptr_t p; double d; } value;
//            value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
//            value.d = ((ConstantDoubleValue)sv).value();
            long addr=unsafe.allocateMemory(8);
            unsafe.putAddress(addr,0xDEADDEAFDEADDEAFL);
            unsafe.putDouble(addr,((ConstantDoubleValue)sv).value());
            StackValue re=new StackValue(unsafe.getAddress(addr));
            unsafe.freeMemory(addr);
            return re;
        } else if (JVM.isLP64&&sv.is_constant_long()) {
            // Constant long in a single stack slot
            return new StackValue(((ConstantLongValue)sv).value());
//#endif
        } else if (sv.is_object()) { // Scalar replaced object in compiled frame
            Oop ov = ((ObjectValue)sv).value();
            return new StackValue(ov, (ov==null||ov.getJavaObject()==null) ? 1 : 0);
        } else if (sv.is_marker()) {
            // Should never need to directly construct a marker.
            throw new RuntimeException("ShouldNotReachHere()");
        }
        // Unknown ScopeValue type
        throw new RuntimeException("ShouldNotReachHere()");
    }
    public static BasicLock resolve_monitor_lock(Frame fr,Location location){
        if (!location.is_stack()){
            throw new RuntimeException("for now we only look at the stack");
        }
        int word_offset = location.stack_offset() / JVM.wordSize;
        // (stack picture)
        // high: [     ]  word_offset + 1
        // low   [     ]  word_offset
        //
        // sp->  [     ]  0
        // the word_offset is the distance from the stack pointer to the lowest address
        // The frame's original stack pointer, before any extension by its callee
        // (due to Compiler1 linkage on SPARC), must be used.
        return new BasicLock(fr.unextended_sp() + (long) word_offset *JVM.oopSize);
    }
}
