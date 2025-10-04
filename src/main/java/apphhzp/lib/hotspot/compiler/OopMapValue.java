package apphhzp.lib.hotspot.compiler;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.VMReg;
import apphhzp.lib.hotspot.code.VMRegImpl;
import apphhzp.lib.hotspot.stream.CompressedReadStream;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.hotspot.compiler.OopMapValue.OopTypes.*;

public class OopMapValue {
    private short _value;

    private int value() {
        return _value;
    }

    private void set_value(int value) {
        _value = (short) value;
    }

    private short _content_reg;

    // Constants
    public static final int type_bits = JVM.intConstant("OopMapValue::type_bits"),
            register_bits = JVM.intConstant("OopMapValue::register_bits");

    public static final int type_shift = JVM.intConstant("OopMapValue::type_shift"),
            register_shift = JVM.intConstant("OopMapValue::register_shift");

    public static final int type_mask = JVM.intConstant("OopMapValue::type_mask"),
            type_mask_in_place = JVM.intConstant("OopMapValue::type_mask_in_place"),
            register_mask = JVM.intConstant("OopMapValue::register_mask"),
            register_mask_in_place = JVM.intConstant("OopMapValue::register_mask_in_place");

    public static final class OopTypes{
        public static final int oop_value = JVM.intConstant("OopMapValue::oop_value"),
                narrowoop_value = JVM.intConstant("OopMapValue::narrowoop_value"),
                callee_saved_value = JVM.intConstant("OopMapValue::callee_saved_value"),
                derived_oop_value = JVM.intConstant("OopMapValue::derived_oop_value"),
                unused_value = JVM.intConstant("OopMapValue::unused_value");//Only used as a sentinel value
    }
    public OopMapValue () {
        set_value(0);
        set_content_reg(VMRegImpl.Bad());
    }
    public OopMapValue (VMReg reg, @RawCType("oop_types") int t, VMReg reg2) {
        set_reg_type(reg, t);
        set_content_reg(reg2);
    }
    private void set_reg_type(VMReg p,@RawCType("oop_types")int t) {
        set_value((p.value() << register_shift) | t);
        if (reg() != p){
            throw new RuntimeException("sanity check");
        }
        if (type() != t){
            throw new RuntimeException("sanity check");
        }
    }

    private void set_content_reg(VMReg r) {
        if (is_callee_saved()) {
            // This can never be a stack location, so we don't need to transform it.
            if (!r.is_reg()){
                throw new RuntimeException("Trying to callee save a stack location");
            }
        } else if (is_derived_oop()) {
            if (!r.is_valid()){
                throw new RuntimeException("must have a valid VMReg");
            }
        } else {
            if (r.is_valid()){
                throw new RuntimeException("valid VMReg not allowed");
            }
        }
        _content_reg = (short) r.value();
    }

    // Archiving
//    public void write_on(CompressedWriteStream stream) {
//        stream->write_int(value());
//        if(is_callee_saved() || is_derived_oop()) {
//            stream->write_int(content_reg()->value());
//        }
//    }

    public void read_from(CompressedReadStream stream) {
        set_value(stream.read_int());
        if (is_callee_saved() || is_derived_oop()) {
            set_content_reg(VMRegImpl.as_VMReg(stream.read_int(), true));
        }
    }

    // Querying
    public boolean is_oop()               { return (value()&type_mask_in_place) == oop_value; }
    public boolean is_narrowoop()         { return (value()&type_mask_in_place) == OopTypes.narrowoop_value; }
    public boolean is_callee_saved()      { return (value()&type_mask_in_place) == callee_saved_value; }
    public boolean is_derived_oop()       { return (value()&type_mask_in_place) == derived_oop_value; }

    public VMReg reg()  { return VMRegImpl.as_VMReg((value()&register_mask_in_place) >> register_shift); }
    public @RawCType("oop_types")int type(){
        return (value()& type_mask_in_place);
    }

    public static boolean legal_vm_reg_name(VMReg p) {
        return (p.value()  == (p.value() & register_mask));
    }

    public VMReg content_reg(){
        return VMRegImpl.as_VMReg(_content_reg, true);
    }

    // Returns offset from sp.
    public int stack_offset() {
        if (!reg().is_stack()){
            throw new RuntimeException("must be stack location");
        }
        return reg().reg2stack();
    }

    // Printing code is present in product build for -XX:+PrintAssembly.

    private static void print_register_type(@RawCType("oop_types")int x, VMReg optional,
                             PrintStream st) {
        if (x == oop_value) {
            st.print("Oop");
        } else if (x == OopTypes.narrowoop_value) {
            st.print("NarrowOop");
        } else if (x == callee_saved_value) {
            st.print("Callers_");
            optional.print_on(st);
        } else if (x == derived_oop_value) {
            st.print("Derived_oop_");
            optional.print_on(st);
        } else {
            throw new RuntimeException("ShouldNotReachHere");
        }
    }

    public void print_on(PrintStream st){
        reg().print_on(st);
        st.print("=");
        print_register_type(type(),content_reg(),st);
        st.print(" ");
    }
}
