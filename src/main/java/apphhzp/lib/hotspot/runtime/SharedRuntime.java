package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.code.blob.RuntimeStub;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class SharedRuntime {
    public static final Type TYPE= JVM.type("SharedRuntime");
    public static final long WRONG_METHOD_BLOB_ADDRESS=TYPE.global("_wrong_method_blob");
    public static final long WRONG_METHOD_ABSTRACT_BLOB_ADDRESS=WRONG_METHOD_BLOB_ADDRESS+JVM.oopSize;
    public static final long IC_MISS_BLOB_ADDRESS=TYPE.global("_ic_miss_blob");
    public static final long RESOLVE_OPT_VIRTUAL_CALL_BLOB_ADDRESS=IC_MISS_BLOB_ADDRESS+JVM.oopSize;
    public static final long RESOVLE_VIRTUAL_CALL_BLOB_ADDRESS=RESOLVE_OPT_VIRTUAL_CALL_BLOB_ADDRESS+JVM.oopSize;
    public static final long DEOPT_BLOB_ADDRESS=TYPE.global("_deopt_blob");

    public static @RawCType("address")long get_resolve_opt_virtual_call_stub() {
        return new RuntimeStub(unsafe.getAddress(RESOLVE_OPT_VIRTUAL_CALL_BLOB_ADDRESS)).entry_point();
    }
    public static @RawCType("address")long get_resolve_virtual_call_stub() {
        return new RuntimeStub(unsafe.getAddress(RESOVLE_VIRTUAL_CALL_BLOB_ADDRESS)).entry_point();
    }
}
