package apphhzp.lib.hotspot.oop;

import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oop.constant.Utf8Constant;
import org.objectweb.asm.Opcodes;

import static apphhzp.lib.ClassHelper.unsafe;

public class FieldInfo extends JVMObject {
    public final InstanceKlass owner;

    public FieldInfo(InstanceKlass klass, long fake) {
        super(fake);
        this.owner = klass;
    }

    public AccessFlags getAccessFlags() {
        return AccessFlags.getOrCreate(unsafe.getShort(this.address) & 0xffff);
    }

    public void setAccessFlags(int flags) {
        unsafe.putShort(this.address, (short) (flags & 0xffff));
    }

    public Symbol getName() {
        return ((Utf8Constant) this.owner.getConstantPool().getConstant(this.getNameIndex())).str;
    }

    public int getNameIndex() {
        return unsafe.getShort(this.address + 2) & 0xffff;
    }

    public void setNameIndex(int index) {
        unsafe.putShort(this.address + 2, (short) (index & 0xffff));
    }

    public Symbol getSignature() {
        return ((Utf8Constant) this.owner.getConstantPool().getConstant(this.getSignatureIndex())).str;
    }

    public int getSignatureIndex() {
        return unsafe.getShort(this.address + 4) & 0xffff;
    }

    public void setSignatureIndex(int index) {
        unsafe.putShort(this.address + 4, (short) (index & 0xffff));
    }

    public int getInitialValueIndex() {
        return unsafe.getShort(this.address + 6) & 0xffff;
    }

    public void setInitialValueIndex(int index) {
        unsafe.putShort(this.address + 6, (short) (index & 0xffff));
    }

    public int getOffset() {
        return unsafe.getInt(this.address + 8);
    }

    public void setOffset(int offset) {
        unsafe.putInt(this.address + 8, offset);
    }

    public void setAccessible() {
        int flags = unsafe.getShort(this.address)&0xffff;
        flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        flags |= Opcodes.ACC_PUBLIC;
        this.setAccessFlags(flags);
    }

    // field flags
    // Note: these flags must be defined in the low order 16 bits because
    // InstanceKlass only stores a ushort worth of information from the
    // AccessFlags value.
    // field access is watched by JVMTI
    public static final long JVM_ACC_FIELD_ACCESS_WATCHED = 0x00002000;
    // field modification is watched by JVMTI
    public static final long JVM_ACC_FIELD_MODIFICATION_WATCHED = 0x00008000;
    // field has generic signature
    public static final long JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE = 0x00000800;

    public boolean isAccessWatched() {
        return (unsafe.getShort(this.address)&0xffff & JVM_ACC_FIELD_ACCESS_WATCHED) != 0;
    }

    public boolean isModificationWatched() {
        return (unsafe.getShort(this.address)&0xffff & JVM_ACC_FIELD_MODIFICATION_WATCHED) != 0;
    }

    public boolean hasGenericSignature() {
        return (unsafe.getShort(this.address)&0xffff & JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE) != 0;
    }
}
