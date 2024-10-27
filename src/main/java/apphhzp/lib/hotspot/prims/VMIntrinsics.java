package apphhzp.lib.hotspot.prims;

import apphhzp.lib.helfy.JVM;

public final class VMIntrinsics {
    public static final int invokeBasic = JVM.intConstant("vmIntrinsics::_invokeBasic");
    public static final int linkToVirtual = JVM.intConstant("vmIntrinsics::_linkToVirtual");
    public static final int linkToStatic = JVM.intConstant("vmIntrinsics::_linkToStatic");
    public static final int linkToSpecial = JVM.intConstant("vmIntrinsics::_linkToSpecial");
    public static final int linkToInterface = JVM.intConstant("vmIntrinsics::_linkToInterface");
    public static final int linkToNative = JVM.intConstant("vmIntrinsics::_linkToNative");
    public static final int FIRST_MH_SIG_POLY = JVM.intConstant("vmIntrinsics::FIRST_MH_SIG_POLY");
    public static final int LAST_MH_SIG_POLY = JVM.intConstant("vmIntrinsics::LAST_MH_SIG_POLY");
    public static final int invokeGeneric = JVM.intConstant("vmIntrinsics::_invokeGeneric");
    public static final int compiledLambdaForm = JVM.intConstant("vmIntrinsics::_compiledLambdaForm");

    public static boolean isSignaturePolymorphic(int iid){
        return iid >=FIRST_MH_SIG_POLY && iid <= LAST_MH_SIG_POLY;
    }
    public static boolean isSignaturePolymorphicIntrinsic(int iid) {
        return iid != invokeGeneric;
    }
}
