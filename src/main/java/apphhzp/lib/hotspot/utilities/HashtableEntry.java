package apphhzp.lib.hotspot.utilities;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import java.util.function.LongFunction;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class HashtableEntry extends BasicHashtableEntry{
    public static final Type TYPE= JVM.type("IntptrHashtableEntry");
    public static final int SIZE=TYPE.size;
    public static final long LITERAL_OFFSET=TYPE.offset("_literal");
    public HashtableEntry(long addr) {
        super(addr);
    }

    public static HashtableEntry create(long hash, JVMObject obj, BasicHashtableEntry next, long size, LongFunction<? extends HashtableEntry> constructor) {
        long addr=unsafe.allocateMemory(size);
        HashtableEntry entry=constructor.apply(addr);
        entry.setNext(null);
        entry.setHash(hash);
        entry.setLiteralPointer(obj==null?0L:obj.address);
        return entry;
    }

    public long getLiteralPointer(){
        return unsafe.getAddress(this.address+LITERAL_OFFSET);
    }

    public void setLiteralPointer(long literal){
        unsafe.putAddress(this.address+LITERAL_OFFSET, literal);
    }


    @Override
    public String toString() {
        return "HashtableEntry@0x"+Long.toHexString(this.address);
    }
}
