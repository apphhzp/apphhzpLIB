package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantTag;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.oop.Oop;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.signature.Signature;
import apphhzp.lib.hotspot.stream.AllFieldStream;
import apphhzp.lib.hotspot.util.CString;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/** A fieldDescriptor describes the attributes of a single field (instance or class variable).
 * It needs the class constant pool to work (because it only holds indices into the pool
 * rather than the actual info).*/

public class fieldDescriptor {

    private AccessFlags _access_flags;
    private int _index; // the field index
    private ConstantPool _cp;

    // update the access_flags for the field in the klass
    private void update_klass_field_access_flag(){
        InstanceKlass ik = field_holder();
        ik.field(index()).setAccessFlags(_access_flags.flags);
    }

    private FieldInfo field(){
        InstanceKlass ik = field_holder();
        return ik.field(_index);
    }

    public fieldDescriptor() {
        //DEBUG_ONLY(_index = badInt);
    }

    public fieldDescriptor(InstanceKlass ik, int index) {
//        DEBUG_ONLY(_index = badInt);
        reinitialize(ik, index);
    }

    public Symbol name(){
        return field().name(_cp);
    }

    public Symbol signature(){
        return field().signature(_cp);
    }

    public InstanceKlass field_holder(){
        return  _cp.pool_holder();
    }

    public ConstantPool constants(){
        return _cp;
    }

    public AccessFlags access_flags() {
        return _access_flags;
    }

    public Oop loader(){
        return _cp.pool_holder().getClassLoaderData().getClassLoaderOop();
    }

    // Offset (in bytes) of field from start of instanceOop / Klass*
    public int offset(){
        return field().offset();
    }

    public Symbol generic_signature(){
        if (!has_generic_signature()) {
            return null;
        }
        int idx = 0;
        InstanceKlass ik = field_holder();
        for (AllFieldStream fs=new AllFieldStream(ik); !fs.done(); fs.next()) {
            if (idx == _index) {
                return fs.generic_signature();
            } else {
                idx ++;
            }
        }
        throw new RuntimeException("should never happen");
    }

    public int index() {
        return _index;
    }

    public @RawCType("AnnotationArray*") U1Array annotations(){
        InstanceKlass ik = field_holder();
        @RawCType("Array<AnnotationArray*>*")VMTypeArray<U1Array> md = ik.fields_annotations();
        if (md == null)
            return null;
        return md.get(index());
    }

    public @RawCType("AnnotationArray*") U1Array type_annotations(){
        InstanceKlass ik = field_holder();
        @RawCType("Array<AnnotationArray*>*")VMTypeArray<U1Array> type_annos = ik.fields_type_annotations();
        if (type_annos == null)
            return null;
        return type_annos.get(index());
    }

    // Initial field value
    public boolean has_initial_value(){
        return field().initval_index() != 0;
    }

    public int initial_value_index(){
        return field().initval_index();
    }

    // The tag will return true on one of is_int(), is_long(), is_single(), is_double()
    public @RawCType("constantTag") int initial_value_tag(){
        return constants().tag_at(initial_value_index());
    }

    public int int_initial_value(){
        return constants().int_at(initial_value_index());
    }

    public long long_initial_value(){
        return constants().long_at(initial_value_index());
    }

    public float float_initial_value(){
        return constants().float_at(initial_value_index());
    }

    public double double_initial_value(){
        return constants().double_at(initial_value_index());
    }

    public String string_initial_value(){
        return constants().uncached_string_at(initial_value_index());
    }

    // Field signature type
    public @RawCType("BasicType") int field_type(){
        return Signature.basic_type(signature());
    }

    // Access flags
    public boolean is_public() {
        return access_flags().isPublic();
    }

    public boolean is_private() {
        return access_flags().isPrivate();
    }

    public boolean is_protected() {
        return access_flags().isProtected();
    }

    public boolean is_package_private() {
        return !is_public() && !is_private() && !is_protected();
    }

    public boolean is_static() {
        return access_flags().isStatic();
    }

    public boolean is_final() {
        return access_flags().isFinal();
    }

    public boolean is_stable() {
        return access_flags().isStable();
    }

    public boolean is_volatile() {
        return access_flags().isVolatile();
    }

    public boolean is_transient() {
        return access_flags().isTransient();
    }

    public boolean is_synthetic() {
        return access_flags().isSynthetic();
    }

    public boolean is_field_access_watched() {
        return access_flags().isFieldAccessWatched();
    }

    public boolean is_field_modification_watched() {
        return access_flags().isFieldModificationWatched();
    }

    public boolean has_initialized_final_update() {
        return access_flags().hasFieldInitializedFinalUpdate();
    }

    public boolean has_generic_signature() {
        return access_flags().fieldHasGenericSignature();
    }

    public boolean is_trusted_final(){
        InstanceKlass ik = field_holder();
        return is_final() && (is_static() || ik.isHidden() || ik.is_record());
    }

    public void set_is_field_access_watched(boolean value){
        _access_flags=AccessFlags.getOrCreate(value?_access_flags.flags|AccessFlags.JVM_ACC_FIELD_ACCESS_WATCHED:_access_flags.flags&~AccessFlags.JVM_ACC_FIELD_ACCESS_WATCHED);
        update_klass_field_access_flag();
    }

    public void set_is_field_modification_watched(boolean value){
        _access_flags=AccessFlags.getOrCreate(value?_access_flags.flags|AccessFlags.JVM_ACC_FIELD_MODIFICATION_WATCHED:_access_flags.flags&~AccessFlags.JVM_ACC_FIELD_MODIFICATION_WATCHED);
        update_klass_field_access_flag();
    }

    public void set_has_initialized_final_update(boolean value){
        _access_flags=AccessFlags.getOrCreate(value?_access_flags.flags|AccessFlags.JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE:_access_flags.flags&~AccessFlags.JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE);
        update_klass_field_access_flag();
    }

    // Initialization
    public void reinitialize(InstanceKlass ik, int index){
        if (_cp==null || !field_holder().equals(ik)) {
            _cp = (ik.getConstantPool());
            // _cp should now reference ik's constant pool; i.e., ik is now field_holder.
            if (!(field_holder().equals(ik))){
                throw new RuntimeException("must be already initialized to this class");
            }
        }
        FieldInfo f = ik.field(index);
        if (f.isInternal()){
            throw new RuntimeException("regular Java fields only");
        }

        _access_flags = f.getAccessFlags();
        if (!(f.name_index() != 0 && f.signature_index() != 0)){
            throw new RuntimeException("bad constant pool index for fieldDescriptor");
        }
        _index = index;
        verify();
    }

    // Print
    public void print_on(PrintStream st) {
        access_flags().print_on(st);
        name().print_value_on(st);
        st.print(" ");
        signature().print_value_on(st);
        st.printf(" @%d ", offset());
        if (JVM.getFlag("WizardMode").getBool() && has_initial_value()) {
            st.print("(initval ");
            @RawCType("constantTag")int t = initial_value_tag();
            if (t== ConstantTag.Integer) {
                st.printf("int %d)", int_initial_value());
            } else if (t==ConstantTag.Long){
                st.printf("long %d)",long_initial_value());
            } else if (t==ConstantTag.Float){
                st.printf("float %f)", float_initial_value());
            } else if (t==ConstantTag.Double){
                st.printf("double %f)", double_initial_value());
            }
        }
    }

    public void print_on_for(PrintStream st, Object obj) {
        print_on(st);
        @RawCType("BasicType")int ft = field_type();
        int as_int = 0;
        if (ft == BasicType.T_BYTE) {
            as_int = unsafe.getByte(obj, offset());
            st.printf(" %d", unsafe.getByte(obj, offset()));
        } else if (ft == BasicType.T_CHAR) {
            as_int = unsafe.getChar(obj, offset());
            char c = unsafe.getChar(obj, offset());
            as_int = c;
            st.printf(" %c %d", CString.isprint(c) ? c : ' ', (int) c);
        } else if (ft == BasicType.T_DOUBLE) {
            st.printf(" %f", unsafe.getDouble(obj, offset()));
        } else if (ft == BasicType.T_FLOAT) {
            as_int = unsafe.getInt(obj, offset());
            st.printf(" %f", unsafe.getFloat(obj, offset()));
        } else if (ft == BasicType.T_INT) {
            as_int = unsafe.getInt(obj, offset());
            st.printf(" %d", unsafe.getInt(obj, offset()));
        } else if (ft == BasicType.T_LONG) {
            st.print(" ");
            st.print(unsafe.getLong(obj, offset()));
        } else if (ft == BasicType.T_SHORT) {
            as_int = unsafe.getShort(obj, offset());
            st.printf(" %d", unsafe.getShort(obj, offset()));
        } else if (ft == BasicType.T_BOOLEAN) {
            as_int = unsafe.getBoolean(obj, offset()) ? 1 : 0;
            st.printf(" %s", unsafe.getBoolean(obj, offset()) ? "true" : "false");
        } else if (ft == BasicType.T_ARRAY || ft == BasicType.T_OBJECT) {
            st.print(" ");
            if (!JVM.isLP64) {
                as_int = unsafe.getInt(obj, offset());
            }
            if (unsafe.getObject(obj, offset()) != null) {
                OopDesc.of(unsafe.getObject(obj, offset())).print_value_on(st);
            } else {
                st.print("NULL");
            }
        } else {
            throw new RuntimeException("ShouldNotReachHere()");
        }
        // Print a hint as to the underlying integer representation. This can be wrong for
        // pointers on an LP64 machine
        if (JVM.isLP64){
            if (BasicType.is_reference_type(ft) && JVM.getFlag("UseCompressedOops").getBool()) {
                st.printf(" (%x)",unsafe.getInt(obj,offset()));
            }
            else // <- intended
            if (ft == BasicType.T_LONG || ft == BasicType.T_DOUBLE || !BasicType.is_java_primitive(ft) ) {
                st.printf(" (%x %x)",unsafe.getInt(obj,offset()),unsafe.getInt(obj,(offset()+4)));
            } else if (as_int < 0 || as_int > 9) {
                st.printf(" (%x)", as_int);
            }
        }else {
            if (ft == BasicType.T_LONG || ft == BasicType.T_DOUBLE) {
                st.printf(" (%x %x)",unsafe.getInt(obj,offset()),unsafe.getInt(obj,(offset()+4)));
            } else if (as_int < 0 || as_int > 9) {
                st.printf(" (%x)", as_int);
            }
        }
    }

    public void verify() {
        if (JVM.ENABLE_EXTRA_CHECK) {
            if (_cp==null) {

            } else {
                if (!(_index >= 0)){
                    throw new RuntimeException("good index");
                }
                if (!(_index < field_holder().java_fields_count())){
                    throw new IndexOutOfBoundsException("oob");
                }
            }
        }
    }
}
