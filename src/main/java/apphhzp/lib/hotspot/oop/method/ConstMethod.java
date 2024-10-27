package apphhzp.lib.hotspot.oop.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.CompressedLineNumberReadStream;
import apphhzp.lib.hotspot.oop.Symbol;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.constant.Utf8Constant;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelper.unsafe;
import static apphhzp.lib.helfy.JVM.oopSize;

public class ConstMethod extends JVMObject {
    public static final Type TYPE = JVM.type("ConstMethod");
    public static final int SIZE = TYPE.size;
    public static final long CONSTANT_POOL_OFFSET = TYPE.offset("_constants");
    public static final long SIZE_OFFSET = TYPE.offset("_constMethod_size");
    public static final long FLAGS_OFFSET = TYPE.offset("_flags");
    public static final long CODE_SIZE_OFFSET = TYPE.offset("_code_size");
    public static final long NAME_INDEX_OFFSET = TYPE.offset("_name_index");
    public static final long SIGNATURE_INDEX_OFFSET = TYPE.offset("_signature_index");
    public static final long ID_NUM_OFFSET = TYPE.offset("_method_idnum");
    public static final long MAX_STACK_OFFSET = TYPE.offset("_max_stack");
    public static final long MAX_LOCALS_OFFSET = TYPE.offset("_max_locals");
    //[FLAGS]
    public static final int HAS_LINENUMBER_TABLE = JVM.intConstant("ConstMethod::_has_linenumber_table");
    public static final int HAS_CHECKED_EXCEPTIONS = JVM.intConstant("ConstMethod::_has_checked_exceptions");
    public static final int HAS_LOCALVARIABLE_TABLE = JVM.intConstant("ConstMethod::_has_localvariable_table");
    public static final int HAS_EXCPETION_TABLE = JVM.intConstant("ConstMethod::_has_exception_table");
    public static final int HAS_GENERIC_SIGNATURE = JVM.intConstant("ConstMethod::_has_generic_signature");
    public static final int HAS_METHOD_PARAMETERS = JVM.intConstant("ConstMethod::_has_method_parameters");
    public static final int HAS_METHOD_ANNOTATIONS = JVM.intConstant("ConstMethod::_has_method_annotations");
    public static final int HAS_PARAMETER_ANNOTATIONS = JVM.intConstant("ConstMethod::_has_parameter_annotations");
    public static final int HAS_DEFAULT_ANNOTATIONS = JVM.intConstant("ConstMethod::_has_default_annotations");
    public static final int HAS_TYPE_ANNOTATIONS = JVM.intConstant("ConstMethod::_has_type_annotations");
    //END
    private ConstantPool constantPoolCache;
    public ConstMethod(long addr) {
        super(addr);
    }

    public Symbol getName() {
        return ((Utf8Constant) this.getConstantPool().getConstant(this.getNameIndex())).str;
    }

    public Symbol getSignature() {
        return ((Utf8Constant) this.getConstantPool().getConstant(this.getSignatureIndex())).str;
    }

    public ConstantPool getConstantPool() {
        long addr = unsafe.getAddress(this.address + CONSTANT_POOL_OFFSET);
        if (!isEqual(this.constantPoolCache, addr)) {
            this.constantPoolCache = ConstantPool.getOrCreate(addr);
        }
        return this.constantPoolCache;
    }

    public void setConstantPool(ConstantPool pool) {
        unsafe.putAddress(this.address + CONSTANT_POOL_OFFSET, pool.address);
    }

    //WordSize
    public int getConstMethodSize() {
        return unsafe.getInt(this.address + SIZE_OFFSET);
    }

    public void setConstMethodSize(int size) {
        unsafe.putInt(this.address + SIZE_OFFSET, size);
    }

    public int getFlags() {
        return unsafe.getShort(this.address + FLAGS_OFFSET) & 0xffff;
    }

    public void setFlags(int flags) {
        unsafe.putShort(this.address + FLAGS_OFFSET, (short) (flags & 0xffff));
    }

    public int getCodeSize() {
        return unsafe.getShort(this.address + CODE_SIZE_OFFSET) & 0xffff;
    }

    public void setCodeSize(int size) {
        unsafe.putShort(this.address + CODE_SIZE_OFFSET, (short) (size & 0xffff));
    }

    public int getNameIndex() {
        return unsafe.getShort(this.address + NAME_INDEX_OFFSET) & 0xffff;
    }

    public void setNameIndex(int nameIndex) {
        unsafe.putShort(this.address + NAME_INDEX_OFFSET, (short) (nameIndex & 0xffff));
    }

    public int getSignatureIndex() {
        return unsafe.getShort(this.address + SIGNATURE_INDEX_OFFSET) & 0xffff;
    }

    public void setSignatureIndex(int signatureIndex) {
        unsafe.putShort(this.address + SIGNATURE_INDEX_OFFSET, (short) (signatureIndex & 0xffff));
    }

    public int getMethodID() {
        return unsafe.getShort(this.address + ID_NUM_OFFSET) & 0xffff;
    }

    public Method getMethod() {
        return this.getConstantPool().getHolder().getMethods().get(this.getMethodID());
    }

    public int getMaxStack() {
        return unsafe.getShort(this.address + MAX_STACK_OFFSET) & 0xffff;
    }

    public void setMaxStack(int maxStack) {
        unsafe.putShort(this.address + MAX_STACK_OFFSET, (short) (maxStack & 0xffff));
    }

    public int getMaxLocals() {
        return unsafe.getShort(this.address + MAX_LOCALS_OFFSET) & 0xffff;
    }

    public void setMaxLocals(int maxLocals) {
        unsafe.putShort(this.address + MAX_LOCALS_OFFSET, (short) (maxLocals & 0xffff));
    }

    public byte getCode(int index) {
        if (index<0||index>=this.getCodeSize()){
            throw new NoSuchElementException();
        }
        return unsafe.getByte(this.address + SIZE + index);
    }

    public void setCode(int index, byte bytecode) {
        if (index<0||index>=this.getCodeSize()){
            throw new NoSuchElementException();
        }
        unsafe.putByte(this.address + SIZE + index, bytecode);
    }

    /**Get the size in bytes.*/
    public long getSize(){
        return (long) this.getConstMethodSize() * oopSize;
    }

//    public int getExactSize(){
//        int re=this.getSize(),rr=this.lastU2ElementOffset();
//
//    }

    public byte[] getCodes() {
        int len = this.getCodeSize();
        long base = this.address + SIZE;
        byte[] re = new byte[len];
        for (int i = 0; i < len; i++) {
            re[i] = unsafe.getByte(base + i);
        }
        return re;
    }

//    public ConstMethod copy(int expand,){
//        int oldSize = this.getSize();
//        long addr = unsafe.allocateMemory(oldSize + expand), base = addr + SIZE;
//        unsafe.copyMemory(this.address, addr, SIZE);
//
//        return new ConstMethod(addr);
//    }

    public ConstMethod copy(int expand, byte[] newCodes) {
        int oldLen = this.getCodeSize(), len = oldLen + expand;
        if (len < 0) {
            throw new IllegalArgumentException("The new _code_size is less than 0.");
        }
        if (newCodes.length > len) {
            throw new IllegalArgumentException("The new bytecode array is too large: " + newCodes.length + " > " + (len));
        }
        int flags = this.getFlags();
        int offset = 0;
        if (hasMethodAnnotations(flags)) {
            offset++;
        }
        if (hasParameterAnnotations(flags)) {
            offset++;
        }
        if (hasTypeAnnotations(flags)) {
            offset++;
        }
        if (hasDefaultAnnotations(flags)) {
            offset++;
        }
        offset *= oopSize;
        long oldSize = this.getSize(),newSize=(oldSize+expand+oopSize-1L)/oopSize*oopSize;//(oldSize+expand+oopSize-1L)/oopSize*oopSize
        long addr = unsafe.allocateMemory(newSize), base = addr + SIZE;
        //Copy
        unsafe.copyMemory(this.address, addr, SIZE);
        unsafe.copyMemory(this.address + SIZE + oldLen, addr + SIZE + len, oldSize - SIZE - oldLen - offset);
        unsafe.copyMemory(this.address+oldSize-offset,addr+newSize-offset,offset);
        //End
        //Fill bytecodes
        ConstMethod re = new ConstMethod(addr);
        for (int i = 0, maxi = Math.min(len, newCodes.length); i < maxi; i++) {
            unsafe.putByte(base + i, newCodes[i]);
        }
        re.setCodeSize(len);
        //End
        re.setConstMethodSize(Math.toIntExact(newSize / oopSize));
        return re;
    }

    public ConstMethod copy(int expand, byte[] newCodes, int maxLocals, int maxStacks) {
        ConstMethod re = this.copy(expand, newCodes);
        re.setMaxLocals(maxLocals);
        re.setMaxStack(maxStacks);
//        if (true){
//            throw new IllegalStateException("未完成...");
//        }
        return re;
    }

    //!=========WARNING=========!
    //   获取偏移量部分没有防呆设计
    //!=========WARNING=========!

    //[LineNumberTable]
    public long compressedLineNumberTableOffset() {
        return SIZE + this.getCodeSize()+(this.getMethod().getAccessFlags().isNative()?2L*oopSize:0);
    }

    public CompressedLineNumberReadStream getCompressedLineNumberReadStream() {
        return new CompressedLineNumberReadStream(this.address + this.compressedLineNumberTableOffset(), 0);
    }

    public int getLineNumberFromBCI(int bci) {
        if (this.getMethod().getAccessFlags().isNative()) {
            return -1;
        }
        if (bci < 0 || bci >= this.getCodeSize()) {
            throw new IllegalArgumentException("Illegal bci:" + bci);
        }
        int bestBCI = 0, bestLine = -1;
        if (hasLineNumberTable(this.getFlags())) {
            CompressedLineNumberReadStream stream = this.getCompressedLineNumberReadStream();
            while (stream.readPair()) {
                if (stream.bci() == bci) {
                    return stream.line();
                } else {
                    if (stream.bci() < bci && stream.bci() >= bestBCI) {
                        bestBCI = stream.bci();
                        bestLine = stream.line();
                    }
                }
            }
        }
        return bestLine;
    }

    public int getLineNumberTableLength() {
        int len = 0;
        if (hasLineNumberTable(this.getFlags())) {
            CompressedLineNumberReadStream stream = this.getCompressedLineNumberReadStream();
            while (stream.readPair()) {
                len += 1;
            }
        }
        return len;
    }
    //END

    //[LocalVariableTable]
    private long localVariableTableLengthOffset() {
        int flags = this.getFlags();
        return hasExceptionTable(flags) ? this.exceptionTableOffset() - 2 : hasCheckedExceptions(flags) ? this.checkedExceptionsOffset() - 2 : hasMethodParameters(flags) ? this.methodParametersOffset() - 2 : hasGenericSignature(flags) ? this.lastU2ElementOffset() - 2 : this.lastU2ElementOffset();
    }

    public int getLocalVariableTableLength() {
        return hasLocalVariableTable(this.getFlags()) ? unsafe.getShort(this.address + this.localVariableTableLengthOffset()) & 0xffff : 0;
    }

    public long localVariableTableOffset() {
        return this.localVariableTableLengthOffset() - (long) this.getLocalVariableTableLength() * LocalVariableTableElement.SIZE;
    }

    @Nullable
    public LocalVariableTableElement[] getLocalVariableTable(){
        if (!hasLocalVariableTable(this.getFlags())){
            return null;
        }
        int len=this.getLocalVariableTableLength();
        LocalVariableTableElement[] re=new LocalVariableTableElement[len];
        long base=this.address+this.localVariableTableOffset();
        for (int i=0;i<len;i++){
            re[i]=new LocalVariableTableElement(base+ (long) i *LocalVariableTableElement.SIZE);
        }
        return re;
    }
    //END

    //[ExceptionTable]
    public long exceptionTableLengthOffset() {
        int flags = this.getFlags();
        return hasCheckedExceptions(flags) ? this.checkedExceptionsOffset() - 2 : hasMethodParameters(flags) ? this.methodParametersOffset() - 2 : hasGenericSignature(flags) ? this.lastU2ElementOffset() - 2 : this.lastU2ElementOffset();
    }

    public int getExceptionTableLength() {
        return hasExceptionTable(this.getFlags()) ? unsafe.getShort(this.address + this.exceptionTableLengthOffset()) & 0xffff : 0;
    }

    public long exceptionTableOffset() {
        return this.exceptionTableLengthOffset() - (long) this.getExceptionTableLength() * ExceptionTableElement.SIZE;
    }

    @Nullable
    public ExceptionTableElement[] getExceptionTable(){
        if (!hasExceptionTable(this.getFlags())){
            return null;
        }
        int len=this.getExceptionTableLength();
        long base=this.exceptionTableOffset()+this.address;
        ExceptionTableElement[] re=new ExceptionTableElement[len];
        for (int i=0;i<len;i++){
            re[i]=new ExceptionTableElement(base+ (long) i *ExceptionTableElement.SIZE);
        }
        return re;
    }
    //END

    //[CheckedExceptionTable]
    public long checkedExceptionsLengthOffset() {
        int flags = this.getFlags();
        return hasMethodParameters(flags) ? this.methodParametersOffset() - 2 : hasGenericSignature(flags) ? this.lastU2ElementOffset() - 2 : this.lastU2ElementOffset();
    }

    public int getCheckedExceptionsLength() {
        return hasCheckedExceptions(this.getFlags()) ? unsafe.getShort(this.address + this.checkedExceptionsLengthOffset()) & 0xffff : 0;
    }

    public long checkedExceptionsOffset() {
        return this.checkedExceptionsLengthOffset() - (long) this.getCheckedExceptionsLength() * CheckedExceptionElement.SIZE;
    }
    //END

    //[MethodParameters]
    public long methodParametersLengthOffset() {
        return hasGenericSignature(this.getFlags()) ? this.lastU2ElementOffset() - 2 : this.lastU2ElementOffset();
    }

    public int getMethodParametersLength() {
        return hasMethodParameters(this.getFlags()) ? unsafe.getShort(this.address + this.methodParametersLengthOffset()) & 0xffff : 0;
    }

    public long methodParametersOffset() {
        return this.methodParametersLengthOffset() - (long) this.getMethodParametersLength() * MethodParametersElement.SIZE;
    }
    //END

    // Offset of last short in Method* before annotations, if present
    public long lastU2ElementOffset() {
        int flags = this.getFlags(), offset = 0;
        if (hasMethodAnnotations(flags)) {
            offset++;
        }
        if (hasParameterAnnotations(flags)) {
            offset++;
        }
        if (hasTypeAnnotations(flags)) {
            offset++;
        }
        if (hasDefaultAnnotations(flags)) {
            offset++;
        }
        return (long) this.getConstMethodSize() * oopSize - (long) offset * oopSize - 2;
    }

    public static boolean hasLineNumberTable(int flags) {
        return (flags & HAS_LINENUMBER_TABLE) != 0;
    }

    public static boolean hasCheckedExceptions(int flags) {
        return (flags & HAS_CHECKED_EXCEPTIONS) != 0;
    }

    public static boolean hasLocalVariableTable(int flags) {
        return (flags & HAS_LOCALVARIABLE_TABLE) != 0;
    }

    public static boolean hasExceptionTable(int flags) {
        return (flags & HAS_EXCPETION_TABLE) != 0;
    }

    public static boolean hasGenericSignature(int flags) {
        return (flags & HAS_GENERIC_SIGNATURE) != 0;
    }

    public static boolean hasMethodParameters(int flags) {
        return (flags & HAS_METHOD_PARAMETERS) != 0;
    }

    public static boolean hasMethodAnnotations(int flags) {
        return (flags & HAS_METHOD_ANNOTATIONS) != 0;
    }

    public static boolean hasParameterAnnotations(int flags) {
        return (flags & HAS_PARAMETER_ANNOTATIONS) != 0;
    }

    public static boolean hasDefaultAnnotations(int flags) {
        return (flags & HAS_DEFAULT_ANNOTATIONS) != 0;
    }

    public static boolean hasTypeAnnotations(int flags) {
        return (flags & HAS_TYPE_ANNOTATIONS) != 0;
    }
}
