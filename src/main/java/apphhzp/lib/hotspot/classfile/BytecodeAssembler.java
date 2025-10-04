package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;
import it.unimi.dsi.fastutil.bytes.ByteList;

import static apphhzp.lib.hotspot.utilities.BasicType.*;

/** Partial bytecode assembler - only what we need for creating
 * overpass methods for default methods is implemented*/
public class BytecodeAssembler {
    private final ByteList _code;
    private final BytecodeConstantPool _cp;

    private void append(@RawCType("u1")byte imm_u1){
        _code.add(imm_u1);
    }
    private void append(@RawCType("u2")short imm_u2){
        append((byte)((imm_u2&0xffff)>>>8));
        append((byte)(imm_u2&0xff));
    }
    private void append(@RawCType("u4")int imm_u4){
        append((byte) (imm_u4>>>24));
        append((byte) ((imm_u4>>>16)&0xff));
        append((byte) ((imm_u4>>>8)&0xff));
        append((byte) (imm_u4&0xff));
    }

    private void xload(@RawCType("u4")int index, @RawCType("u1")byte onebyteop, @RawCType("u1")byte twobyteop){
        if (Integer.compareUnsigned(index,4)<0) {
            _code.add((byte) ((onebyteop&0xff) + index));
        } else {
            _code.add(twobyteop);
            _code.add((byte) (index&0xffff));
        }
    }

    public BytecodeAssembler(ByteList buffer, BytecodeConstantPool cp) {
        _code=(buffer);
        _cp=(cp);
    }

    public void aload(@RawCType("u4")int index){
        xload(index, (byte) Bytecodes.Code._aload_0, (byte) Bytecodes.Code._aload);
    }
    public void areturn(){
        _code.add((byte) Bytecodes.Code._areturn);
    }
    public void athrow(){
        _code.add((byte) Bytecodes.Code._athrow);
    }
    public void checkcast(Symbol sym){
        @RawCType("u2")int cpool_index = _cp.klass(sym);
        _code.add((byte) Bytecodes.Code._checkcast);
        append(cpool_index);
    }
    public void dload(@RawCType("u4")int index){
        xload(index, (byte) Bytecodes.Code._dload_0, (byte) Bytecodes.Code._dload);
    }
    public void dreturn(){
        _code.add((byte) Bytecodes.Code._dreturn);
    }
    public void dup(){
        _code.add((byte) Bytecodes.Code._dup);
    }
    public void fload(@RawCType("u4")int index){
        xload(index, (byte) Bytecodes.Code._fload_0, (byte) Bytecodes.Code._fload);
    }
    public void freturn(){
        _code.add((byte) Bytecodes.Code._freturn);
    }
    public void iload(@RawCType("u4")int index){
        xload(index, (byte) Bytecodes.Code._iload_0, (byte) Bytecodes.Code._iload);
    }
    public void invokespecial(Method method){
        invokespecial(method.klass_name(), method.name(), method.signature());
    }
    public void invokespecial(Symbol cls, Symbol name, Symbol sig){
        @RawCType("u2")int methodref_index = _cp.methodref(cls, name, sig);
        _code.add((byte) Bytecodes.Code._invokespecial);
        append(methodref_index);
    }
    public void invokevirtual(Method method){
        invokevirtual(method.klass_name(), method.name(), method.signature());
    }
    public void invokevirtual(Symbol cls, Symbol name, Symbol sig){
        @RawCType("u2")int methodref_index = _cp.methodref(cls, name, sig);
        _code.add((byte) Bytecodes.Code._invokevirtual);
        append(methodref_index);
    }
    public void ireturn(){
        _code.add((byte) Bytecodes.Code._ireturn);
    }
    public void ldc(@RawCType("u1")byte index){
        _code.add((byte) Bytecodes.Code._ldc);
        append(index);
    }
    public void ldc_w(@RawCType("u2")short index){
        _code.add((byte) Bytecodes.Code._ldc_w);
        append(index);
    }
    public void lload(@RawCType("u4")int index){
        xload(index, (byte) Bytecodes.Code._lload_0, (byte) Bytecodes.Code._lload);
    }
    public void lreturn(){
        _code.add((byte) Bytecodes.Code._lreturn);
    }
    public void _new(Symbol sym){
        @RawCType("u2")int cpool_index = _cp.klass(sym);
        _code.add((byte) Bytecodes.Code._new);
        append(cpool_index);
    }
    public void _return(){
        _code.add((byte) Bytecodes.Code._return);
    }

    public void load_string(Symbol sym){
        @RawCType("u2")int cpool_index = _cp.string(sym);
        if (cpool_index < 0x100) {
            ldc((byte) cpool_index);
        } else {
            ldc_w((short) cpool_index);
        }
    }
    public void load(@RawCType("BasicType")int bt, @RawCType("u4")int index){
        if (bt == T_BOOLEAN || bt == T_CHAR || bt == T_BYTE || bt == T_SHORT || bt == T_INT) {
            iload(index);
        } else if (bt == T_FLOAT) {
            fload(index);
        } else if (bt == T_DOUBLE) {
            dload(index);
        } else if (bt == T_LONG) {
            lload(index);
        } else {
            if (is_reference_type(bt)) {
                aload(index);
                return;
            }
            throw new RuntimeException("ShouldNotReachHere()");
        }
    }
    public void _return(@RawCType("BasicType")int bt){
        if (bt == T_BOOLEAN || bt == T_CHAR || bt == T_BYTE || bt == T_SHORT || bt == T_INT) {
            ireturn();
        } else if (bt == T_FLOAT) {
            freturn();
        } else if (bt == T_DOUBLE) {
            dreturn();
        } else if (bt == T_LONG) {
            lreturn();
        } else if (bt == T_VOID) {
            _return();
        } else {
            if (is_reference_type(bt)) {
                areturn();
                return;
            }
            throw new RuntimeException("ShouldNotReachHere()");
        }
    }
}
