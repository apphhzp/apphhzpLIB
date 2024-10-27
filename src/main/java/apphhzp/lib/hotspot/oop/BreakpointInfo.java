package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oop.method.ConstMethod;
import apphhzp.lib.hotspot.oop.method.Method;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;
import static apphhzp.lib.helfy.JVM.isJVMTISupported;

public class BreakpointInfo extends JVMObject {
    public static final Type TYPE = isJVMTISupported ? JVM.type("BreakpointInfo") : null;
    public static final int SIZE = TYPE == null ? 0 : TYPE.size;
    public static final long ORG_BYTECODE_OFFSET = TYPE == null ? -1 : TYPE.offset("_orig_bytecode");
    public static final long BCI_OFFSET = TYPE == null ? -1 : TYPE.offset("_bci");
    public static final long NAME_INDEX_OFFSET = TYPE == null ? -1 : TYPE.offset("_name_index");
    public static final long SIGNATURE_INDEX_OFFSET = TYPE == null ? -1 : TYPE.offset("_signature_index");
    public static final long NEXT_OFFSET = TYPE == null ? -1 : TYPE.offset("_next");
    private BreakpointInfo nextCache;

    public static BreakpointInfo of(long addr) {
        if (!isJVMTISupported) {
            throw new IllegalStateException("Need JVMTI supported!");
        }
        return new BreakpointInfo(addr);
    }

    private BreakpointInfo(long addr) {
        super(addr);
    }

    public int getOriginBytecode(){
        return unsafe.getInt(this.address+ORG_BYTECODE_OFFSET);
    }

    public void setOriginBytecode(int code){
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
        ConstMethod constMethod=method.getConstMethod();
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
}
