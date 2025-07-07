package apphhzp.lib.hotspot.code;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.code.x86.NativeCallX86;

public class DirectNativeCallWrapper extends NativeCallWrapper{
    private  NativeCall _call;
    public DirectNativeCallWrapper(long addr) {
        if (true){
            throw new UnsupportedOperationException("Incomplete");
        }
        String cpu= PlatformInfo.getCPU();
        if (cpu.equals("amd64")||cpu.equals("x86")){
            _call = new NativeCallX86(addr);
        }else {
            throw new UnsupportedOperationException("Unsupported CPU");
        }
    }

    @Override
    public long destination() {
        return _call.destination();
    }

    @Override
    public long instruction_address() {
        return _call.instruction_address();
    }

    @Override
    public long next_instruction_address() {
        return _call.next_instruction_address();
    }

    @Override
    public long return_address() {
        return _call.return_address();
    }
}
