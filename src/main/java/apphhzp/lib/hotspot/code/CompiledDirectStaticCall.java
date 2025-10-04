package apphhzp.lib.hotspot.code;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.code.x86.X86CompiledDirectStaticCall;
import apphhzp.lib.hotspot.util.RawCType;

public abstract class CompiledDirectStaticCall extends CompiledStaticCall{
    protected static CompiledDirectStaticCall create(NativeCall val){
        if (PlatformInfo.isX86()){
            return new X86CompiledDirectStaticCall(val);
        }else {
            throw new RuntimeException("Unsupported platform");
        }
    }
    protected NativeCall _call;

    protected CompiledDirectStaticCall(NativeCall call){
        _call = call;
    }
    @Override
    public long destination() {
        return _call.destination();
    }

    public abstract void verify();

    public static  CompiledDirectStaticCall before(@RawCType("address")long return_addr) {
        CompiledDirectStaticCall st = create(NativeCall.nativeCall_before(return_addr));
        st.verify();
        return st;
    }

    public static CompiledDirectStaticCall at(@RawCType("address")long native_call) {
        CompiledDirectStaticCall st = create(NativeCall.nativeCall_at(native_call));
        st.verify();
        return st;
    }

//    public static CompiledDirectStaticCall* at(Relocation* call_site) {
//        return at(call_site.addr());
//    }
    @Override
    protected String name() {
        return "CompiledDirectStaticCall";
    }
}
