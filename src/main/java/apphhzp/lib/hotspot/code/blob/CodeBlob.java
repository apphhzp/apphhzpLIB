package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.compiler.CompilerType;

import static apphhzp.lib.ClassHelper.unsafe;

public class CodeBlob extends JVMObject {
    public static final Type TYPE = JVM.type("CodeBlob");
    public static final int SIZE = TYPE.size;
    public static final long SIZE_OFFSET = TYPE.offset("_size");
    public static final long TYPE_OFFSET = SIZE_OFFSET - 4;
    public static final long NAME_OFFSET = TYPE.offset("_name");
    public final Type actualType;

    @SuppressWarnings("unchecked")
    public static <T extends CodeBlob> T getCodeBlob(long addr) {
        if (addr == 0L) {
            return null;
        }
        Type type = JVM.findDynamicTypeForAddress(addr, CodeBlob.TYPE);
        if (type == null) {
            return null;
        }
        if (type == CompiledMethod.TYPE) {
            return (T) new CompiledMethod(addr);
        } else if (type == NMethod.TYPE) {
            return (T) new NMethod(addr);
        } else if (type == RuntimeStub.TYPE) {
            return (T) new RuntimeStub(addr);
        } else if (type == DeoptimizationBlob.TYPE) {
            return (T) new DeoptimizationBlob(addr);
        }
        return (T) new CodeBlob(addr, type);
    }

    public CodeBlob(long addr, Type type) {
        super(addr);
        this.actualType = type;
    }

    public int getSize() {
        return unsafe.getInt(this.address + SIZE_OFFSET);
    }

    public void setSize(int size) {
        unsafe.putInt(this.address + SIZE_OFFSET, size);
    }

    public CompilerType getType() {
        return CompilerType.of(unsafe.getInt(this.address + TYPE_OFFSET));
    }

    public void setType(CompilerType type) {
        unsafe.putInt(this.address + TYPE_OFFSET, type.id);
    }

    boolean isCompiledByC1() {
        return unsafe.getInt(this.address+TYPE_OFFSET) == CompilerType.C1.id;
    }


    boolean isCompiledByC2() {
        return unsafe.getInt(this.address+TYPE_OFFSET) == CompilerType.C2.id;
    }


    boolean isCompiledByJVMCI() {
        return unsafe.getInt(this.address+TYPE_OFFSET) == CompilerType.JVMCI.id;
    }


    public String getName() {
        return JVM.getStringRef(this.address + NAME_OFFSET);
    }

    public boolean isZombie() {
        return false;
    }

    public boolean isLockedByVM() {
        return false;
    }

    @Override
    public String toString() {
        return actualType.name + "(" + this.getName() + ")@0x" + Long.toHexString(this.address);
    }
}
