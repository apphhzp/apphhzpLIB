package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.Utf8Constant;
import org.objectweb.asm.Opcodes;

import static apphhzp.lib.ClassHelper.unsafe;
import static apphhzp.lib.hotspot.oops.AccessFlags.JVM_ACC_FIELD_INTERNAL;
import static apphhzp.lib.hotspot.oops.AccessFlags.JVM_ACC_FIELD_STABLE;

public class FieldInfo extends JVMObject {
    public static final int access_flags_offset = JVM.intConstant("FieldInfo::access_flags_offset");
    public static final int name_index_offset = JVM.intConstant("FieldInfo::name_index_offset");
    public static final int signature_index_offset = JVM.intConstant("FieldInfo::signature_index_offset");
    public static final int initval_index_offset = JVM.intConstant("FieldInfo::initval_index_offset");
    public static final int low_packed_offset = JVM.intConstant("FieldInfo::low_packed_offset");
    public static final int high_packed_offset = JVM.intConstant("FieldInfo::high_packed_offset");
    public static final int field_slots = JVM.intConstant("FieldInfo::field_slots");
    public static final int FIELDINFO_TAG_SIZE = JVM.intConstant("FIELDINFO_TAG_SIZE");
    public static final int FIELDINFO_TAG_OFFSET = JVM.intConstant("FIELDINFO_TAG_OFFSET");
    public static final int FIELDINFO_TAG_CONTENDED= 2;

    public FieldInfo(long address) {
        super(address);
    }

    public static FieldInfo from_field_array(U2Array fields, int index) {
        return new FieldInfo(fields.address+U2Array.DATA_OFFSET+2L*index * field_slots);
    }
    public static FieldInfo from_field_array(long fields, int index) {
        return new FieldInfo(fields + 2L*index * field_slots);
    }

    public AccessFlags getAccessFlags() {
        return AccessFlags.getOrCreate(unsafe.getShort(this.address + access_flags_offset * 2L) & 0xffff);
    }

    public void setAccessFlags(int flags) {
        unsafe.putShort(this.address + access_flags_offset * 2L, (short) (flags & 0xffff));
    }

    public Symbol getName(ConstantPool pool) {
        if (this.isInternal()){
            return Symbol.getVMSymbol(this.getNameIndex());
        }
        return ((Utf8Constant) pool.getConstant(this.getNameIndex())).str;
    }

    public int getNameIndex() {
        return unsafe.getShort(this.address + name_index_offset * 2L) & 0xffff;
    }

    public void setNameIndex(int index) {
        unsafe.putShort(this.address + name_index_offset * 2L, (short) (index & 0xffff));
    }

    public Symbol getSignature(ConstantPool pool) {
        if (this.isInternal()){
            return Symbol.getVMSymbol(this.getSignatureIndex());
        }
        return ((Utf8Constant) pool.getConstant(this.getSignatureIndex())).str;
    }

    public int getSignatureIndex() {
        return unsafe.getShort(this.address + signature_index_offset * 2L) & 0xffff;
    }

    public void setSignatureIndex(int index) {
        unsafe.putShort(this.address + signature_index_offset * 2L, (short) (index & 0xffff));
    }

    public int getInitialValueIndex() {
        return unsafe.getShort(this.address + initval_index_offset * 2L) & 0xffff;
    }

    public void setInitialValueIndex(int index) {
        unsafe.putShort(this.address + initval_index_offset * 2L, (short) (index & 0xffff));
    }

    public boolean isContended() {
        return (unsafe.getShort(this.address+low_packed_offset*2L)&FIELDINFO_TAG_CONTENDED) != 0;
    }

    public int contendedGroup() {
        if ((unsafe.getShort(this.address+low_packed_offset*2L)&FIELDINFO_TAG_OFFSET)!=0){
            throw new IllegalStateException("Offset must not have been set");
        }
        if ((unsafe.getShort(this.address+low_packed_offset*2L)&FIELDINFO_TAG_CONTENDED)==0){
            throw new IllegalStateException("Field must be contended");
        }
        return unsafe.getShort(this.address+high_packed_offset*2L)&0xffff;
    }


    // Packed field has the tag, and can be either of:
    //    hi bits <--------------------------- lo bits
    //   |---------high---------|---------low---------|
    //    ..........................................CO
    //    ..........................................00  - non-contended field
    //    [--contention_group--]....................10  - contended field with contention group
    //    [------------------offset----------------]01  - real field offset

    // Bit O indicates if the packed field contains an offset (O=1) or not (O=0)
    // Bit C indicates if the field is contended (C=1) or not (C=0)
    //       (if it is contended, the high packed field contains the contention group)

    public int getOffset() {
        if (!isOffsetSet()) {
            throw new IllegalStateException("Offset must have been set");
        }
        return (unsafe.getShort(this.address + low_packed_offset * 2L) | unsafe.getShort(this.address + high_packed_offset * 2L) << 16) >> FIELDINFO_TAG_SIZE;
    }

    public boolean isOffsetSet() {
        return (unsafe.getShort(this.address + low_packed_offset * 2L) & FIELDINFO_TAG_OFFSET) != 0;
    }

    public void setOffset(int val) {
        val = val << FIELDINFO_TAG_SIZE;
        unsafe.putShort(this.address + low_packed_offset * 2L, (short) (val & 0xffff | FIELDINFO_TAG_OFFSET));
        unsafe.putShort(this.address + high_packed_offset * 2L, (short) (val >> 16 & 0xffff));
    }

    public void setAccessible() {
        int flags = unsafe.getShort(this.address+access_flags_offset*2L)&0xffff;
        flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        flags |= Opcodes.ACC_PUBLIC;
        this.setAccessFlags(flags);
    }

    public boolean isInternal() {
        return (unsafe.getShort(this.address + access_flags_offset * 2L) & 0xffff & JVM_ACC_FIELD_INTERNAL) != 0;
    }

    public boolean isStable()  {
        return (unsafe.getShort(this.address + access_flags_offset * 2L) & 0xffff & JVM_ACC_FIELD_STABLE) != 0;
    }
    public void setStable(boolean z) {
        if (z) unsafe.putShort(this.address+access_flags_offset*2L, (short) (unsafe.getShort(this.address+access_flags_offset*2L)|JVM_ACC_FIELD_STABLE));
        else unsafe.putShort(this.address+access_flags_offset*2L, (short) (unsafe.getShort(this.address+access_flags_offset*2L)& ~JVM_ACC_FIELD_STABLE));//_shorts[access_flags_offset] &= ~JVM_ACC_FIELD_STABLE;
    }

    public Symbol lookup_symbol(int symbol_index){
        if (!isInternal()){
            throw new IllegalStateException("only internal fields");
        }
        return Symbol.getVMSymbol(symbol_index);
    }
}
