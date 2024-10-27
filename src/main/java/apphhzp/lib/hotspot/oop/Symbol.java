package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Random;

import static apphhzp.lib.ClassHelper.unsafe;

public class Symbol extends JVMObject {
    public static final Type TYPE=JVM.type("Symbol");
    public static final int SIZE=TYPE.size;
    public static final long VM_SYMBOLS_ADDRESS =TYPE.global("_vm_symbols[0]");
    public static final long HASH_REF_OFFSET=TYPE.offset("_hash_and_refcount");
    public static final long LENGTH_OFFSET=TYPE.offset("_length");
    public static final long BODY_OFFSET=TYPE.offset("_body");
    public static final int FIRST_SID=JVM.intConstant("vmSymbols::FIRST_SID");
    public static final int SID_LIMIT=JVM.intConstant("vmSymbols::SID_LIMIT");
    public static final int MAX_LENGTH=JVM.intConstant("Symbol::max_symbol_length");
    public static Symbol create(String s){
        return create(s.getBytes(StandardCharsets.UTF_8),1);
    }

    public static Symbol create(byte[] bytes){
        return create(bytes,1);
    }

    public static Symbol create(byte[] bytes, int ref_count){
        if (bytes.length>MAX_LENGTH){
            throw new IllegalArgumentException("String too large:"+bytes.length);
        }
        long addr=unsafe.allocateMemory(SIZE+bytes.length);
        unsafe.putShort(addr + LENGTH_OFFSET, (short) bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            unsafe.putByte(addr + BODY_OFFSET + i, bytes[i]);
        }
        unsafe.putInt(addr,((new Random().nextInt())<<16)|ref_count);
        return new Symbol(addr);
    }

    public static Symbol getVMSymbol(int index){
        if (index<FIRST_SID||index>SID_LIMIT){
            throw new ArrayIndexOutOfBoundsException(index);
        }
        long addr=unsafe.getAddress(VM_SYMBOLS_ADDRESS + (long) index *JVM.oopSize);
        if (addr==0L){
            return null;
        }
        return new Symbol(addr);
    }

    public Symbol(long addr){
        super(addr);
    }

    public int getHash(){
        return unsafe.getInt(this.address+HASH_REF_OFFSET)>>16;
    }

    public int getRefCount(){
        return unsafe.getInt(this.address+HASH_REF_OFFSET)&0xffff;
    }

    public int getLength(){
        return unsafe.getShort(this.address+LENGTH_OFFSET)&0xffff;
    }

    public char getCChar(int index){
        if (index<0||index>=this.getLength()){
            throw new NoSuchElementException();
        }
        return (char)unsafe.getByte(this.address+BODY_OFFSET+index);
    }

    @Override
    public String toString() {
        long body = this.address + BODY_OFFSET;
        int length=this.getLength();
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = unsafe.getByte(body + i);
        }
        return new String(b, StandardCharsets.UTF_8);
    }
}
