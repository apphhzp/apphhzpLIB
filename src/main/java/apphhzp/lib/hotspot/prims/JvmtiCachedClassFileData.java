package apphhzp.lib.hotspot.prims;

import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class JvmtiCachedClassFileData extends JVMObject {
    public static final long LENGTH_OFFSET=0;
    public static final long DATA_OFFSET=LENGTH_OFFSET+4;
    public static JvmtiCachedClassFileData create(int length,long data){
        long addr= unsafe.allocateMemory(8+length);
        JvmtiCachedClassFileData re=new JvmtiCachedClassFileData(addr);
        unsafe.copyMemory(addr+DATA_OFFSET, data, length);
        unsafe.putInt(addr+LENGTH_OFFSET,length);
        return re;
    }
    public JvmtiCachedClassFileData(long addr) {
        super(addr);
    }

    public int length(){
        return unsafe.getInt(this.address+LENGTH_OFFSET);
    }

    public void setLength(int length){
        unsafe.putInt(this.address+LENGTH_OFFSET,length);
    }

    public byte[] data(){
        byte[] re=new byte[length()];
        for(int i=0;i<re.length;i++){
            re[i]=unsafe.getByte(this.address+DATA_OFFSET+i);
        }
        return re;
    }

    public byte data(int index){
        if (index<0 || index>=length()){
            throw new IndexOutOfBoundsException();
        }
        return unsafe.getByte(this.address+DATA_OFFSET+index);
    }

    public void setData(byte[] data,int len){
        if (len>data.length){
            throw new IllegalArgumentException();
        }
        if (len>this.length()){
            len=this.length();
        }
        for (int i=0;i<len;i++){
            unsafe.putByte(this.address+DATA_OFFSET+i,data[i]);
        }
    }

    public void setData(byte val,int index){
        if (index<0 || index>=length()){
            throw new IndexOutOfBoundsException();
        }
        unsafe.putByte(this.address+DATA_OFFSET+index,val);
    }
}
