package apphhzp.lib.hotspot.oop.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelper.unsafe;

public class LocalVariableTableElement extends JVMObject {
    public static final Type TYPE = JVM.type("LocalVariableTableElement");
    public static final int SIZE = TYPE.size;
    public static final long START_BCI_OFFSET = TYPE.offset("start_bci");
    public static final long LENGTH_OFFSET = TYPE.offset("length");
    public static final long NAME_CP_INDEX_OFFSET = TYPE.offset("name_cp_index");
    public static final long DESC_CP_INDEX_OFFSET = TYPE.offset("descriptor_cp_index");
    public static final long SIGNATURE_CP_INDEX_OFFSET = TYPE.offset("signature_cp_index");
    public static final long SLOT_OFFSET = TYPE.offset("slot");

    public LocalVariableTableElement(long addr) {
        super(addr);
    }

    public int getStartBci() {
        return unsafe.getShort(this.address + START_BCI_OFFSET) & 0xffff;
    }

    public void setStartBci(int bci) {
        unsafe.putShort(this.address + START_BCI_OFFSET, (short) (bci & 0xffff));
    }

    public int getLength() {
        return unsafe.getShort(this.address + LENGTH_OFFSET) & 0xffff;
    }

    public void setLength(int length) {
        unsafe.putShort(this.address + LENGTH_OFFSET, (short) (length & 0xffff));
    }

    public int getNameCPIndex() {
        return unsafe.getShort(this.address + NAME_CP_INDEX_OFFSET) & 0xffff;
    }

    public void setNameCPIndex(int index) {
        unsafe.putShort(this.address + NAME_CP_INDEX_OFFSET, (short) (index & 0xffff));
    }

    public int getDescCPIndex() {
        return unsafe.getShort(this.address + DESC_CP_INDEX_OFFSET) & 0xffff;
    }

    public void setDescCPIndex(int index) {
        unsafe.putShort(this.address + DESC_CP_INDEX_OFFSET, (short) (index & 0xffff));
    }

    //Generic signature index
    public int getSignatureCPIndex() {
        return unsafe.getShort(this.address + SIGNATURE_CP_INDEX_OFFSET) & 0xffff;
    }

    public void setSignatureCPIndex(int index) {
        unsafe.putShort(this.address + SIGNATURE_CP_INDEX_OFFSET, (short) (index & 0xffff));
    }

    //Index in LocalVariableTable
    public int getSlot() {
        return unsafe.getShort(this.address + SLOT_OFFSET) & 0xffff;
    }

    public void setSlot(int slot) {
        unsafe.putShort(this.address + SLOT_OFFSET, (short) (slot & 0xffff));
    }
}
