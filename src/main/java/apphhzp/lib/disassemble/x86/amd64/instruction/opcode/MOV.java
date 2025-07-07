package apphhzp.lib.disassemble.x86.amd64.instruction.opcode;

import apphhzp.lib.disassemble.x86.amd64.instruction.ModRM;
import apphhzp.lib.disassemble.x86.amd64.instruction.SIB;
import apphhzp.lib.disassemble.x86.amd64.instruction.prefix.RexPrefix;

import javax.annotation.Nullable;

public class MOV extends AMD64Opcode {
    private byte opcode;
    @Nullable
    private RexPrefix rex;
    @Nullable
    private ModRM modRM;
    @Nullable
    private SIB sib;
    public MOV(byte opcode, RexPrefix rex, ModRM modRM, SIB sib) {
        this.opcode = opcode;
        this.rex = rex;
        this.modRM = modRM;
        this.sib = sib;
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public byte[] toOpcodes() {
        return new byte[0];
    }

    @Override
    public byte getOpcode() {
        return opcode;
    }

    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }

    @Override
    public int size() {
        return 0;
    }
}
