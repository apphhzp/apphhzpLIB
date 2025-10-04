package apphhzp.lib.hotspot.code.x86;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.CodeCache;
import apphhzp.lib.hotspot.code.CompiledDirectStaticCall;
import apphhzp.lib.hotspot.code.NativeCall;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

public class X86CompiledDirectStaticCall extends CompiledDirectStaticCall {

    public X86CompiledDirectStaticCall(NativeCall call) {
        super(call);
    }

    @Override
    public void verify() {
        // Verify call.
        _call.verify();
        _call.verify_alignment();
        if(JVM.ENABLE_EXTRA_CHECK){
            CodeBlob cb = CodeCache.findBlobUnsafe(_call.getAddress());
            if (cb==null){
                throw new RuntimeException("sanity");
            }
        }

        // Verify stub.
        @RawCType("address")long stub = find_stub();
        if (stub==0L){
            throw new RuntimeException("no stub found for static call");
        }
        // Creation also verifies the object.
        X86NativeMovConstReg method_holder = X86NativeMovConstReg.nativeMovConstReg_at(stub);
        X86NativeJump jump= X86NativeJump.nativeJump_at(method_holder.next_instruction_address());
        // Verify state.
        if (!(is_clean() || is_call_to_compiled() || is_call_to_interpreted())){
            throw new RuntimeException("sanity check");
        }
    }

    @Override
    public boolean is_call_to_interpreted() {
        return false;
    }

    @Override
    public long instruction_address() {
        return _call.instruction_address();
    }

    @Override
    protected long resolve_call_stub() {
        return 0;
    }

    @Override
    protected void set_destination_mt_safe(long dest) {
        throw new UnsupportedOperationException();
        //_call.set_destination_mt_safe(dest);
    }

    // Stub support
    public static @RawCType("address")long find_stub_for(@RawCType("address")long instruction){
        //TODO
        throw new UnsupportedOperationException("TODO");
//        // Find reloc. information containing this call-site
//        RelocIterator iter(null, instruction);
//        while (iter.next()) {
//            if (iter.addr() == instruction) {
//                switch(iter.type()) {
//                    case relocInfo::static_call_type:
//                        return iter.static_call_reloc()->static_stub();
//                    // We check here for opt_virtual_call_type, since we reuse the code
//                    // from the CompiledIC implementation
//                    case relocInfo::opt_virtual_call_type:
//                        return iter.opt_virtual_call_reloc()->static_stub();
//                    case relocInfo::poll_type:
//                    case relocInfo::poll_return_type: // A safepoint can't overlap a call.
//                    default:
//                        throw new RuntimeException("ShouldNotReachHere()");
//                }
//            }
//        }
//        return 0L;
    }
    public @RawCType("address")long find_stub(){
        return find_stub_for(instruction_address());
    }
//    public static void set_stub_to_clean(static_stub_Relocation* static_stub);

    @Override
    protected void set_to_interpreted(Method callee, long entry) {
        @RawCType("address")long stub = find_stub();
        if (stub==0L){
            throw new RuntimeException("stub not found");
        }

        // Creation also verifies the object.
        X86NativeMovConstReg method_holder = X86NativeMovConstReg.nativeMovConstReg_at(stub);
        X86NativeJump        jump          = X86NativeJump.nativeJump_at(method_holder.next_instruction_address());
        //verify_mt_safe(callee, entry, method_holder, jump);

        // Update stub.
        method_holder.set_data(callee.address);
        jump.set_jump_destination(entry);

        // Update jump to call.
        set_destination_mt_safe(stub);
    }
}
