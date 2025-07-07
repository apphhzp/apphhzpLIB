package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

public class INT extends AMD64Opcode {
    public int IDT_index;
    public INT(){
        this.IDT_index = -1;
    }
    public INT(int idt){
        this.IDT_index = idt;
    }

    public boolean hasIDT(){
        return IDT_index != -1;
    }
    @Override
    public int size() {
        return hasIDT() ? 2 : 1;
    }

    @Override
    public byte[] toOpcodes() {
        return hasIDT()?new byte[]{(byte) 0xcd,(byte)(IDT_index&0xff)}:new byte[]{(byte) 0xcc};
    }

    @Override
    public byte getOpcode() {
        return (byte) (hasIDT()?0xcd:0xcc);
    }
}
