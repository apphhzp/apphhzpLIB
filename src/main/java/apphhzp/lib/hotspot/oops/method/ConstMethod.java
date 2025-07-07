package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.U1Array;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.Utf8Constant;
import apphhzp.lib.hotspot.stream.CompressedLineNumberReadStream;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.helfy.JVM.oopSize;
import static apphhzp.lib.hotspot.oops.method.ConstMethod.Flags.*;

public class ConstMethod extends JVMObject {
    public static final Type TYPE = JVM.type("ConstMethod");
    public static final int SIZE = TYPE.size;
    public static final long FINGERPRINT_OFFSET=TYPE.offset("_fingerprint");
    public static final long CONSTANT_POOL_OFFSET = TYPE.offset("_constants");
    public static final long STACKMAP_DATA_OFFSET=TYPE.offset("_stackmap_data");
    public static final long SIZE_OFFSET = TYPE.offset("_constMethod_size");
    public static final long FLAGS_OFFSET = TYPE.offset("_flags");
    public static final long RESULT_TYPE_OFFSET=JVM.computeOffset(1,FLAGS_OFFSET+2);
    public static final long CODE_SIZE_OFFSET = TYPE.offset("_code_size");
    public static final long NAME_INDEX_OFFSET = TYPE.offset("_name_index");
    public static final long SIGNATURE_INDEX_OFFSET = TYPE.offset("_signature_index");
    public static final long METHOD_IDNUM_OFFSET = TYPE.offset("_method_idnum");
    public static final long MAX_STACK_OFFSET = TYPE.offset("_max_stack");
    public static final long MAX_LOCALS_OFFSET = TYPE.offset("_max_locals");
    public static final long SIZE_OF_PARAMETERS_OFFSET=TYPE.offset("_size_of_parameters");
    public static final long ORIG_METHOD_IDNUM_OFFSET=JVM.computeOffset(2,SIZE_OF_PARAMETERS_OFFSET+2);
    private ConstantPool constantPoolCache;
    private U1Array stackmapDataCache;
    static {
        JVM.assertOffset(CODE_SIZE_OFFSET, JVM.computeOffset(2,RESULT_TYPE_OFFSET+1));
        JVM.assertOffset(SIZE,JVM.computeOffset(8,ORIG_METHOD_IDNUM_OFFSET+2));
    }
    public ConstMethod(long addr) {
        super(addr);
    }

    public U1Array stackmap_data(){
        long addr=unsafe.getAddress(this.address+STACKMAP_DATA_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.stackmapDataCache,addr)){
            this.stackmapDataCache=new U1Array(addr);
        }
        return this.stackmapDataCache;
    }
    public void set_stackmap_data(@Nullable U1Array sd){
        unsafe.putAddress(this.address+STACKMAP_DATA_OFFSET,sd==null?0L:sd.address);
    }

    public boolean has_stackmap_table(){
        return unsafe.getAddress(this.address+STACKMAP_DATA_OFFSET)!=0L;
    }

    public void init_fingerprint(){
        unsafe.putLong(this.address+FINGERPRINT_OFFSET,0x8000000000000000L);
    }

    public @RawCType("uint64_t") long fingerprint(){
        // Since reads aren't atomic for 64 bits, if any of the high or low order
        // word is the initial value, return 0.  See init_fingerprint for initval.
        long _fingerprint=unsafe.getLong(this.address+FINGERPRINT_OFFSET);
        int high_fp = (int) ((_fingerprint >>> 32)&0xffffffffL);
        if ((int) _fingerprint == 0 || high_fp == 0x80000000) {
            return 0L;
        } else {
            return _fingerprint;
        }
    }

    public @RawCType("uint64_t") long set_fingerprint(@RawCType("uint64_t") long new_fingerprint) {
        unsafe.putLong(this.address+FINGERPRINT_OFFSET,new_fingerprint);
        if (!(((new_fingerprint >>> 32) != 0x80000000L) && (int)new_fingerprint !=0)){
            throw new IllegalArgumentException("fingerprint should call init to set initial value");
        }
        return new_fingerprint;
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

    /**constMethod size in <b>words<b/>*/
    public int size() {
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

    public int  size_of_parameters(){
        return unsafe.getShort(this.address+SIZE_OF_PARAMETERS_OFFSET) & 0xffff;
    }
    public void set_size_of_parameters(int size){
        unsafe.putShort(this.address+SIZE_OF_PARAMETERS_OFFSET, (short) (size&0xffff));
    }

    public @RawCType("BasicType") int result_type(){
        int re=unsafe.getByte(this.address+RESULT_TYPE_OFFSET)&0xff;
        if (re< BasicType.T_BOOLEAN){
            throw new IllegalStateException("Must be set");
        }
        return re;
    }

    public void set_result_type(@RawCType("BasicType") int rt) {
        if (rt>=16){
            throw new IllegalArgumentException("result type too large");
        }
        unsafe.putByte(this.address+RESULT_TYPE_OFFSET, (byte) (rt&0xff));
    }

    public int getGenericSignatureIndex(){
        if (hasGenericSignature(this.getFlags())){
            return unsafe.getShort(this.address+lastU2ElementOffset());
        }
        return 0;
    }

    public void setGenericSignatureIndex(int val){
        if (!hasGenericSignature(this.getFlags())){
            throw new IllegalStateException();
        }
        unsafe.putShort(this.address+lastU2ElementOffset(), (short) (val & 0xffff));
    }

    @Nullable
    public @RawCType("AnnotationArray*") U1Array method_annotations(){
        return hasMethodAnnotations(this.getFlags())?new U1Array(unsafe.getAddress(this.method_annotations_addr())):null;
    }

    public void  set_method_annotations(@RawCType("AnnotationArray*")U1Array anno) {
        unsafe.putAddress(this.method_annotations_addr(),anno.address);
    }

    @Nullable
    public @RawCType("AnnotationArray*") U1Array parameter_annotations(){
        return hasParameterAnnotations(this.getFlags())?new U1Array(unsafe.getAddress(this.parameter_annotations_addr())):null;
    }

    public void  set_parameter_annotations(@RawCType("AnnotationArray*")U1Array anno) {
        unsafe.putAddress(this.parameter_annotations_addr(),anno.address);
    }

    @Nullable
    public @RawCType("AnnotationArray*") U1Array type_annotations() {
        return hasTypeAnnotations(this.getFlags()) ? new U1Array(unsafe.getAddress(type_annotations_addr())) : null;
    }
    public void set_type_annotations(@RawCType("AnnotationArray*")U1Array anno) {
        unsafe.putAddress(type_annotations_addr(),anno.address);
    }

    @Nullable
    public @RawCType("AnnotationArray*") U1Array default_annotations() {
        return hasDefaultAnnotations(this.getFlags()) ? new U1Array(unsafe.getAddress(default_annotations_addr())):null;
    }
    public void set_default_annotations(@RawCType("AnnotationArray*")U1Array anno) {
        unsafe.putAddress(default_annotations_addr(),anno.address);
    }

    public int method_annotations_length() {
        return hasMethodAnnotations(this.getFlags()) ? method_annotations().length() : 0;
    }
    public int parameter_annotations_length() {
        return hasParameterAnnotations(this.getFlags()) ? parameter_annotations().length() : 0;
    }
    public int type_annotations_length() {
        return hasTypeAnnotations(this.getFlags()) ? type_annotations().length() : 0;
    }
    public int default_annotations_length() {
        return hasDefaultAnnotations(this.getFlags()) ? default_annotations().length() : 0;
    }



    public @RawCType("u2") int getMethodIdnum() {
        return unsafe.getShort(this.address + METHOD_IDNUM_OFFSET) & 0xffff;
    }

    public void setMethodIdnum(@RawCType("u2") int val){
        unsafe.putShort(this.address+ METHOD_IDNUM_OFFSET,(short) (val & 0xffff));
    }

    public @RawCType("u2") int getOrigMethodIdnum() {
        return unsafe.getShort(this.address + ORIG_METHOD_IDNUM_OFFSET) & 0xffff;
    }

    public void setOrigMethodIdnum(@RawCType("u2") int val){
        unsafe.putShort(this.address+ORIG_METHOD_IDNUM_OFFSET,(short) (val & 0xffff));
    }

    public Method getMethod() {
        return this.getConstantPool().getHolder().getMethods().get(this.getMethodIdnum());
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

    public void set_code(long code) {
        if (this.getCodeSize() > 0) {
            unsafe.copyMemory(this.code_base(),code,this.getCodeSize());
        }
    }
    public long code_base(){ return (this.address+SIZE); }
    public long code_end(){ return code_base() + this.getCodeSize(); }
    public boolean contains(long bcp){ return code_base() <= bcp
            && bcp < code_end(); }


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

//    public ConstMethod copy(int expand, byte[] newCodes) {
//        if (true){
//            throw new UnsupportedOperationException();
//        }
//        int oldLen = this.getCodeSize(), len = oldLen + expand;
//        if (len < 0) {
//            throw new IllegalArgumentException("The new _code_size is less than 0.");
//        }
//        if (newCodes.length > len) {
//            throw new IllegalArgumentException("The new bytecode array is too large: " + newCodes.length + " > " + (len));
//        }
//        int flags = this.getFlags();
//        int offset = 0;
//        if (hasMethodAnnotations(flags)) {
//            offset++;
//        }
//        if (hasParameterAnnotations(flags)) {
//            offset++;
//        }
//        if (hasTypeAnnotations(flags)) {
//            offset++;
//        }
//        if (hasDefaultAnnotations(flags)) {
//            offset++;
//        }
//        offset *= oopSize;
//        long oldSize = this.getSize(),newSize=(oldSize+expand+oopSize-1L)/oopSize*oopSize;//(oldSize+expand+oopSize-1L)/oopSize*oopSize
//        long addr = unsafe.allocateMemory(newSize), base = addr + SIZE;
//        //Copy
//        unsafe.copyMemory(this.address, addr, SIZE);
//        unsafe.copyMemory(this.address + SIZE + oldLen, addr + SIZE + len, oldSize - SIZE - oldLen - offset);
//        unsafe.copyMemory(this.address+oldSize-offset,addr+newSize-offset,offset);
//        //End
//        //Fill bytecodes
//        ConstMethod re = new ConstMethod(addr);
//        for (int i = 0, maxi = Math.min(len, newCodes.length); i < maxi; i++) {
//            unsafe.putByte(base + i, newCodes[i]);
//        }
//        re.setCodeSize(len);
//        //End
//        re.setConstMethodSize(Math.toIntExact(newSize / oopSize));
//        return re;
//    }
//
//    public ConstMethod copy(int expand, byte[] newCodes, int maxLocals, int maxStacks) {
//        if (true){
//            throw new UnsupportedOperationException();
//        }
//        ConstMethod re = this.copy(expand, newCodes);
//        re.setMaxLocals(maxLocals);
//        re.setMaxStack(maxStacks);
//        return re;
//    }

    // Since the size of the compressed line number table is unknown, the
    // offsets of the other variable sized sections are computed backwards
    // from the end of the ConstMethod*.
    // First byte after ConstMethod*
    public long constMethodEnd()
    { return (this.address + (long) this.size() * oopSize); }


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
        return (long) this.size() * oopSize - (long) offset * oopSize - 2;
    }

    public @RawCType("AnnotationArray**") long method_annotations_addr() {
        if (!hasMethodAnnotations(this.getFlags())){
            throw new IllegalStateException("should only be called if method annotations are present");
        }
        return this.constMethodEnd() - oopSize;
    }

    public @RawCType("AnnotationArray**") long parameter_annotations_addr() {
        int flags = this.getFlags();
        if (!hasParameterAnnotations(flags)){
            throw new IllegalStateException("should only be called if method parameter annotations are present");
        }
        int offset = 1;
        if (hasMethodAnnotations(flags)){
            offset++;
        }
        return this.constMethodEnd() - (long) offset *oopSize;
    }

    public @RawCType("AnnotationArray**") long type_annotations_addr() {
        int flags = this.getFlags();
        if (!hasTypeAnnotations(flags)){
            throw new IllegalStateException("should only be called if method type annotations are present");
        }
        int offset = 1;
        if (hasMethodAnnotations(flags)){
            offset++;
        }
        if (hasParameterAnnotations(flags)){
            offset++;
        }
        return this.constMethodEnd() - (long) offset *oopSize;
    }

    public @RawCType("AnnotationArray**") long default_annotations_addr() {
        int flags = this.getFlags();
        if (!hasDefaultAnnotations(flags)){
            throw new IllegalStateException("should only be called if method default annotations are present");
        }
        int offset = 1;
        if (hasMethodAnnotations(flags)) {
            offset++;
        }
        if (hasParameterAnnotations(flags)) {
            offset++;
        }
        if (hasTypeAnnotations(flags)) {
            offset++;
        }
        return this.constMethodEnd() - (long) offset *oopSize;
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

    public static final class Flags{
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
    }
}
