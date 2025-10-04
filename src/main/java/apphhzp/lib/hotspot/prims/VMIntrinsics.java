package apphhzp.lib.hotspot.prims;

import apphhzp.lib.helfy.JVM;

public final class VMIntrinsics {
    public static final int none=0;
    public static final int invokeBasic = JVM.intConstant("vmIntrinsics::_invokeBasic");
    public static final int linkToVirtual = JVM.intConstant("vmIntrinsics::_linkToVirtual");
    public static final int linkToStatic = JVM.intConstant("vmIntrinsics::_linkToStatic");
    public static final int linkToSpecial = JVM.intConstant("vmIntrinsics::_linkToSpecial");
    public static final int linkToInterface = JVM.intConstant("vmIntrinsics::_linkToInterface");
    public static final int linkToNative = JVM.intConstant("vmIntrinsics::_linkToNative");
    public static final int FIRST_MH_SIG_POLY = JVM.includeJVMCI?JVM.intConstant("vmIntrinsics::FIRST_MH_SIG_POLY"):invokeBasic-1;
    public static final int LAST_MH_SIG_POLY = JVM.includeJVMCI?JVM.intConstant("vmIntrinsics::LAST_MH_SIG_POLY"):linkToNative;
    public static final int invokeGeneric = JVM.includeJVMCI?JVM.intConstant("vmIntrinsics::_invokeGeneric"):invokeBasic-1;
    public static final int compiledLambdaForm =JVM.includeJVMCI?JVM.intConstant("vmIntrinsics::_compiledLambdaForm"):linkToNative+1;
    public static final int invoke=invokeGeneric-1;
    public static final int Object_init=invoke-1;
    public static final int FIRST_MH_STATIC=linkToVirtual;
}
