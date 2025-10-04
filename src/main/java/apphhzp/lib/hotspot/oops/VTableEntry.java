package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.CString;

import javax.annotation.Nullable;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class VTableEntry extends JVMObject {
    public static final Type TYPE= JVM.type("vtableEntry");
    public static final int SIZE=TYPE.size;
    public static final long METHOD_OFFSET=TYPE.offset("_method");
    public VTableEntry(long addr) {
        super(addr);
    }

    @Nullable
    public Method method(){
        long addr=unsafe.getAddress(this.address+METHOD_OFFSET);
        if (addr==0L){
            return null;
        }
        return Method.getOrCreate(addr);
    }

    public void set(Method method){
        if (method==null){
            throw new IllegalArgumentException("use clear");
        }
        unsafe.putAddress(this.address+METHOD_OFFSET,method.address);
    }

    public void clear(){
        unsafe.putAddress(this.address+METHOD_OFFSET,0);
    }

    public void print(PrintStream tty) {
        tty.print("vtableEntry "+ CString.toString(method().name().as_C_string())+": ");
        if (JVM.getFlag("Verbose").getBool()) {
            tty.print("m 0x"+Long.toHexString(method().address)+ " ");
        }
    }

    // size in words
    public static int size()          { return SIZE / JVM.wordSize; }
    public static int size_in_bytes() { return SIZE; }


    @Override
    public String toString() {
        return "vtableEntry@0x"+Long.toHexString(this.address);
    }
}
