package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

public class INTO extends AMD64Opcode {
    @Override
    public byte[] toOpcodes() {
        return new byte[]{(byte) 0xce};
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public byte getOpcode() {
        return (byte) 0xce;
    }
}
