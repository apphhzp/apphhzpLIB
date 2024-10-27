package apphhzp.lib.hotspot.code;

public class CompressedLineNumberReadStream extends CompressedReadStream {
    private int bci;
    private int line;

    public CompressedLineNumberReadStream(long addr) {
        this(addr, 0);
    }

    public CompressedLineNumberReadStream(long addr, int st_pos) {
        super(addr, st_pos);
    }

    /**
     * Read (bci, line number) pair from stream. Returns false at end-of-stream.
     */
    public boolean readPair() {
        int next = readByte() & 0xFF;
        if (next == 0) {
            return false;
        }
        if (next == 0xFF) {
            bci += readSignedInt();
            line += readSignedInt();
        } else {
            bci += next >> 3;
            line += next & 0x7;
        }
        return true;
    }

    public int bci() {
        return bci;
    }

    public int line() {
        return line;
    }
}
