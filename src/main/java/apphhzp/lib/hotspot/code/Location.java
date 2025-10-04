package apphhzp.lib.hotspot.code;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.stream.DebugInfoReadStream;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.helfy.JVM.BytesPerInt;
import static apphhzp.lib.helfy.JVM.LogBytesPerInt;
import static apphhzp.lib.hotspot.code.Location.Type.*;
import static apphhzp.lib.hotspot.code.Location.Where.in_register;
import static apphhzp.lib.hotspot.code.Location.Where.on_stack;

public class Location {
    public static final class Where {
        public static final int on_stack = JVM.intConstant("Location::on_stack"),
                in_register = JVM.intConstant("Location::in_register");
    }

    public static final class Type {
        public static final int invalid = JVM.intConstant("Location::invalid"),                    // Invalid location
                normal = JVM.intConstant("Location::normal"),                     // Ints, floats, double halves
                oop = JVM.intConstant("Location::oop"),                        // Oop (please GC me!)
                int_in_long = JVM.intConstant("Location::int_in_long"),                // Integer held in long register
                lng = JVM.intConstant("Location::lng"),                        // Long held in one register
                float_in_dbl = JVM.intConstant("Location::float_in_dbl"),               // Float held in double register
                dbl = JVM.intConstant("Location::dbl"),                        // Double held in one register
                vector = dbl + 1,                     // Vector in one register
                addr = JVM.intConstant("Location::addr"),                       // JSR return address
                narrowoop = JVM.intConstant("Location::narrowoop");                   // Narrow Oop (please GC me!)
    }

    ;

    private static final int TYPE_MASK = JVM.intConstant("Location::TYPE_MASK"),
            TYPE_SHIFT = JVM.intConstant("Location::TYPE_SHIFT"),
            WHERE_MASK = JVM.intConstant("Location::WHERE_MASK"),
            WHERE_SHIFT = JVM.intConstant("Location::WHERE_SHIFT"),
            OFFSET_MASK = JVM.intConstant("Location::OFFSET_MASK"),
            OFFSET_SHIFT = JVM.intConstant("Location::OFFSET_SHIFT");

    private @RawCType("juint") int _value;

    // Create a bit-packed Location
    private Location(@RawCType("Where") int where_, @RawCType("Type") int type_, @RawCType("unsigned") int offset_) {
        set(where_, type_, offset_);
    }

    private void set(@RawCType("Where") int where_, @RawCType("Type") int type_, @RawCType("unsigned") int offset_) {
        _value = ((where_ << WHERE_SHIFT) |
                (type_ << TYPE_SHIFT) |
                ((offset_ << OFFSET_SHIFT) & OFFSET_MASK));
    }

    // Stack location Factory.  Offset is 4-byte aligned; remove low bits
    public static Location new_stk_loc(@RawCType("Type") int t, int offset) {
        return new Location(on_stack, t, offset >> LogBytesPerInt);
    }

    // Register location Factory
    public static Location new_reg_loc(@RawCType("Type") int t, VMReg reg) {
        return new Location(in_register, t, reg.value());
    }

    // Default constructor
    public Location() {
        set(on_stack, invalid, 0);
    }

    // Bit field accessors
    public @RawCType("Where") int where() {
        return ((_value & WHERE_MASK) >>> WHERE_SHIFT);
    }

    public @RawCType("Type") int type() {
        return ((_value & TYPE_MASK) >>> TYPE_SHIFT);
    }

    public @RawCType("unsigned") int offset() {
        return ((_value & OFFSET_MASK) >>> OFFSET_SHIFT);
    }

    // Accessors
    public boolean is_register() {
        return where() == in_register;
    }

    public boolean is_stack() {
        return where() == on_stack;
    }

    public int stack_offset() {
        if (where() != on_stack){
            throw new IllegalStateException("wrong Where");
        }
        return offset() << LogBytesPerInt;
    }

    public int register_number() {
        if (where() != in_register){
            throw new IllegalStateException("wrong Where");
        }
        return offset();
    }

    public VMReg reg() {
        if (where() != in_register){
            throw new IllegalStateException("wrong Where");
        }
        return VMRegImpl.as_VMReg(offset());
    }

    // Valid argument to Location::new_stk_loc()?
    public boolean legal_offset_in_bytes(int offset_in_bytes) {
        if ((offset_in_bytes % BytesPerInt) != 0) {
            return false;
        }
        return (offset_in_bytes / BytesPerInt) < (OFFSET_MASK >>> OFFSET_SHIFT);
    }

    public Location(DebugInfoReadStream stream) {
        _value = stream.read_int();
    }

    public void print_on(PrintStream st){
        if(type() == invalid) {
            // product of Location::invalid_loc() or Location::Location().
            int where = where();
            if (where == on_stack) {
                st.print("empty");
            } else if (where == in_register) {
                st.print("invalid");
            }
            return;
        }
        int where = where();
        if (where == on_stack) {
            st.print("stack["+stack_offset()+"]");
        } else if (where == in_register) {
            st.print("reg "+reg().name()+" ["+register_number()+"]");
        } else {
            st.print("Wrong location where "+where());
        }
        int type = type();
        if (type == normal) {
        } else if (type == oop) {
            st.print(",oop");
        } else if (type == narrowoop) {
            st.print(",narrowoop");
        } else if (type == int_in_long) {
            st.print(",int");
        } else if (type == lng) {
            st.print(",long");
        } else if (type == float_in_dbl) {
            st.print(",float");
        } else if (type == dbl) {
            st.print(",double");
        } else if (type == addr) {
            st.print(",address");
        } else if (type == vector) {
            st.print(",vector");
        } else {
            st.print("Wrong location type "+type());
        }
    }
}
