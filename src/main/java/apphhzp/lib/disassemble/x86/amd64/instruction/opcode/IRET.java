package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

public class IRET extends AMD64Opcode {
    @Override
    public byte[] toOpcodes() {
        return new byte[]{(byte) 0xcf};
    }

    @Override
    public byte getOpcode() {
        return (byte) 0xcf;
    }

    @Override
    public int size() {
        return 1;
    }
}
