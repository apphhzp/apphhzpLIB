package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.method.ConstMethod;
import apphhzp.lib.hotspot.oops.method.Method;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.helfy.JVM.includeJVMTI;

public class BreakpointInfo extends JVMObject {
    public static final Type TYPE = includeJVMTI ? JVM.type("BreakpointInfo") : null;
    public static final int SIZE = TYPE == null ? 0 : TYPE.size;
    public static final long ORG_BYTECODE_OFFSET = TYPE == null ? -1 : TYPE.offset("_orig_bytecode");
    public static final long BCI_OFFSET = TYPE == null ? -1 : TYPE.offset("_bci");
    public static final long NAME_INDEX_OFFSET = TYPE == null ? -1 : TYPE.offset("_name_index");
    public static final long SIGNATURE_INDEX_OFFSET = TYPE == null ? -1 : TYPE.offset("_signature_index");
    public static final long NEXT_OFFSET = TYPE == null ? -1 : TYPE.offset("_next");
    private BreakpointInfo nextCache;
    public static BreakpointInfo of(long addr) {
        if (!includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        return new BreakpointInfo(addr);
    }

    public static BreakpointInfo create(Method m, int bci) {
        if (!includeJVMTI) {
            throw new UnsupportedOperationException();
        }
        long addr=unsafe.allocateMemory(SIZE);
        BreakpointInfo re=new BreakpointInfo(addr);
        re.setBytecodeIndex(bci);
        re.setNameIndex(m.name_index());
        re.setSignatureIndex(m.signature_index());
        int val=unsafe.getByte(m.bcp_from(bci))&0xff;
        if (val== Bytecodes.Code._breakpoint){
            re.setOrigBytecode(m.orig_bytecode_at(bci));
        }else {
            re.setOrigBytecode(val);
        }
        re.setNext(null);
        return re;
    }

    private BreakpointInfo(long addr) {
        super(addr);
    }

    public int getOrigBytecode(){
        return unsafe.getInt(this.address+ORG_BYTECODE_OFFSET);
    }

    public void setOrigBytecode(int code){
        unsafe.putInt(this.address+ORG_BYTECODE_OFFSET,code);
    }

    public int getBytecodeIndex(){
        return unsafe.getInt(this.address+BCI_OFFSET);
    }

    public void setBytecodeIndex(int index){
        unsafe.putInt(this.address+BCI_OFFSET,index);
    }


    public int getNameIndex(){
        return unsafe.getShort(this.address+NAME_INDEX_OFFSET)&0xffff;
    }

    public void setNameIndex(int index){
        unsafe.putShort(this.address+NAME_INDEX_OFFSET,(short) (index&0xffff));
    }

    public int getSignatureIndex(){
        return unsafe.getShort(this.address+SIGNATURE_INDEX_OFFSET)&0xffff;
    }

    public void setSignatureIndex(int index){
        unsafe.putShort(this.address+SIGNATURE_INDEX_OFFSET,(short) (index&0xffff));
    }

    public boolean match(Method method){
        ConstMethod constMethod=method.constMethod();
        return constMethod.getNameIndex()==this.getNameIndex()&&constMethod.getSignatureIndex()==this.getSignatureIndex();
    }

    public boolean match(Method method,int bci){
        return bci==this.getBytecodeIndex()&&this.match(method);
    }

    @Nullable
    public BreakpointInfo getNext(){
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextCache,addr)){
            this.nextCache=new BreakpointInfo(addr);
        }
        return this.nextCache;
    }

    public void setNext(@Nullable BreakpointInfo info){
        unsafe.putAddress(this.address+NEXT_OFFSET,info==null?0L:info.address);
    }

    public void clear(Method method) {
        unsafe.putByte(method.bcp_from(this.getBytecodeIndex()), (byte) (this.getOrigBytecode()&0xff));
        if (method.number_of_breakpoints()<=0){
            throw new IllegalStateException("must not go negative");
        }
        method.decr_number_of_breakpoints();
    }
}
