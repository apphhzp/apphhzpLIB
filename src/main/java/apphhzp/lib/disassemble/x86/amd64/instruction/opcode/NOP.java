package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

public class NOP extends AMD64Opcode {

    @Override
    public int size() {
        return 1;
    }

    @Override
    public byte[] toOpcodes() {
        return new byte[]{(byte) 0x90};
    }

    @Override
    public byte getOpcode() {
        return (byte) 0x90;
    }
}
