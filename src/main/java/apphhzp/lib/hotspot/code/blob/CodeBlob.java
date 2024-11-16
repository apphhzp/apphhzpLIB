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
    public static final long DATA_OFFSET_OFFSET=TYPE.offset("_data_offset");
    public static final long CODE_BEGIN_OFFSET=TYPE.offset("_code_begin");
    public static final long CODE_END_OFFSET=TYPE.offset("_code_end");
    public static final long CONTENT_BEGIN_OFFSET=TYPE.offset("_content_begin");
    public static final long DATA_END_OFFSET=TYPE.offset("_data_end");
    public static final long RELOCATION_BEGIN_OFFSET=DATA_END_OFFSET+JVM.oopSize;
    public static final long RELOCATION_END_OFFSET=RELOCATION_BEGIN_OFFSET+JVM.oopSize;
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

    public CompilerType getCompilerType() {
        return CompilerType.of(unsafe.getInt(this.address + TYPE_OFFSET));
    }

    public void setCompilerType(CompilerType type) {
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

    public int getDataOffset() {
        return unsafe.getInt(this.address + DATA_OFFSET_OFFSET);
    }

    public void setDataOffset(int offset) {
        unsafe.putInt(this.address + DATA_OFFSET_OFFSET, offset);
    }

    public long codeBegin(){
        return unsafe.getAddress(this.address + CODE_BEGIN_OFFSET);
    }
    public void setCodeBegin(long begin) {
        unsafe.putAddress(this.address + CODE_BEGIN_OFFSET, begin);
    }

    public long codeEnd(){
        return unsafe.getAddress(this.address + CODE_END_OFFSET);
    }

    public void setCodeEnd(long end) {
        unsafe.putAddress(this.address + CODE_END_OFFSET, end);
    }

    public long contentBegin(){
        return unsafe.getAddress(this.address + CONTENT_BEGIN_OFFSET);
    }
    public void setContentBegin(long begin) {
        unsafe.putAddress(this.address + CONTENT_BEGIN_OFFSET, begin);
    }

    //_code_end == _content_end is true for all types of blobs for now
    public long contentEnd(){
        return this.codeEnd();
    }

    public long dataEnd(){
        return unsafe.getAddress(this.address + DATA_END_OFFSET);
    }

    public void setDataEnd(long end) {
        unsafe.putAddress(this.address + DATA_END_OFFSET, end);
    }

    public long relocationBegin() {
        return unsafe.getAddress(this.address + RELOCATION_BEGIN_OFFSET);
    }

    public void setRelocationBegin(long begin) {
        unsafe.putAddress(this.address + RELOCATION_BEGIN_OFFSET, begin);
    }

    public long relocationEnd() {
        return unsafe.getAddress(this.address + RELOCATION_END_OFFSET);
    }

    public void setRelocationEnd(long end) {
        unsafe.putAddress(this.address + RELOCATION_END_OFFSET, end);
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
