package apphhzp.lib.hotspot.oops.method;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/**Utility class describing elements in local variable table inlined in Method*.*/
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

    public int start_bci() {
        return unsafe.getShort(this.address + START_BCI_OFFSET) & 0xffff;
    }

    public void set_start_bci(int bci) {
        unsafe.putShort(this.address + START_BCI_OFFSET, (short) (bci & 0xffff));
    }

    public int length() {
        return unsafe.getShort(this.address + LENGTH_OFFSET) & 0xffff;
    }

    public void set_length(int length) {
        unsafe.putShort(this.address + LENGTH_OFFSET, (short) (length & 0xffff));
    }

    public int name_cp_index() {
        return unsafe.getShort(this.address + NAME_CP_INDEX_OFFSET) & 0xffff;
    }

    public void set_name_cp_index(int index) {
        unsafe.putShort(this.address + NAME_CP_INDEX_OFFSET, (short) (index & 0xffff));
    }

    public int descriptor_cp_index() {
        return unsafe.getShort(this.address + DESC_CP_INDEX_OFFSET) & 0xffff;
    }

    public void set_descriptor_cp_index(int index) {
        unsafe.putShort(this.address + DESC_CP_INDEX_OFFSET, (short) (index & 0xffff));
    }

    //Generic signature index
    public int signature_cp_index() {
        return unsafe.getShort(this.address + SIGNATURE_CP_INDEX_OFFSET) & 0xffff;
    }

    public void set_signature_cp_index(int index) {
        unsafe.putShort(this.address + SIGNATURE_CP_INDEX_OFFSET, (short) (index & 0xffff));
    }

    //Index in LocalVariableTable
    public int slot() {
        return unsafe.getShort(this.address + SLOT_OFFSET) & 0xffff;
    }

    public void slot(int slot) {
        unsafe.putShort(this.address + SLOT_OFFSET, (short) (slot & 0xffff));
    }

    @Override
    public String toString() {
        return "LocalVariableTableElement@0x"+Long.toHexString(this.address);
    }
}
