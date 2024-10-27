package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

import static apphhzp.lib.ClassHelper.unsafe;

public final class VMVersion{
    public static final Type TYPE= JVM.type("Abstract_VM_Version");
    public static final long FEATURES_STRING_ADDRESS=TYPE.global("_features_string");
    public static final long VM_MAJOR_VERSION_ADDRESS=TYPE.global("_vm_major_version");
    public static final long VM_SECURITY_VERSION_ADDRESS=TYPE.global("_vm_security_version");
    public static final long VM_BUILD_NUMBER_ADDRESS=TYPE.global("_vm_build_number");
    public static final long S_VM_RELEASE_ADDRESS=TYPE.global("_s_vm_release");
    public static final long S_INTERNAL_VM_INFO_STRING_ADDRESS=TYPE.global("_s_internal_vm_info_string");
    public static final long FEATURES_ADDRESS=TYPE.global("_features");
    public static final long VM_MINOR_VERSION_ADDRESS=TYPE.global("_vm_minor_version");
//    public static final long CPU_CX8=JVM.longConstant("VM_Version::CPU_CX8");
//    public static final long CPU_CMOV=JVM.longConstant("VM_Version::CPU_CMOV");
//    public static final long CPU_FXSR=JVM.longConstant("VM_Version::CPU_FXSR");
//    public static final long CPU_HT=JVM.longConstant("VM_Version::CPU_HT");
//    public static final long CPU_MMX=JVM.longConstant("VM_Version::CPU_MMX");
//    public static final long CPU_3DNOW_PREFETCH=JVM.longConstant("VM_Version::CPU_3DNOW_PREFETCH");
//    public static final long CPU_SSE=JVM.longConstant("VM_Version::CPU_SSE");
//    public static final long CPU_SSE2=JVM.longConstant("VM_Version::CPU_SSE2");
//    public static final long CPU_SSE3=JVM.longConstant("VM_Version::CPU_SSE3");
//    public static final long CPU_SSSE3=JVM.longConstant("VM_Version::CPU_SSSE3");
//    public static final long CPU_SSE4A=JVM.longConstant("VM_Version::CPU_SSE4A");
//    public static final long CPU_SSE4_1=JVM.longConstant("VM_Version::CPU_SSE4_1");
//    public static final long CPU_SSE4_2=JVM.longConstant("VM_Version::CPU_SSE4_2");
//    public static final long CPU_POPCNT=JVM.longConstant("VM_Version::CPU_POPCNT");
//    public static final long CPU_LZCNT=JVM.longConstant("VM_Version::CPU_LZCNT");
//    public static final long CPU_TSC=JVM.longConstant("VM_Version::CPU_TSC");
//    public static final long CPU_TSCINV_BIT=JVM.longConstant("VM_Version::CPU_TSCINV_BIT");
//    public static final long CPU_TSCINV=JVM.longConstant("VM_Version::CPU_TSCINV");
//    public static final long CPU_AVX=JVM.longConstant("VM_Version::CPU_AVX");
//    public static final long CPU_AVX2=JVM.longConstant("VM_Version::CPU_AVX2");
//    public static final long CPU_AES=JVM.longConstant("VM_Version::CPU_AES");
//    public static final long CPU_ERMS=JVM.longConstant("VM_Version::CPU_ERMS");
//    public static final long CPU_CLMUL=JVM.longConstant("VM_Version::CPU_CLMUL");
//    public static final long CPU_BMI1=JVM.longConstant("VM_Version::CPU_BMI1");
//    public static final long CPU_BMI2=JVM.longConstant("VM_Version::CPU_BMI2");
//    public static final long CPU_RTM=JVM.longConstant("VM_Version::CPU_RTM");
//    public static final long CPU_ADX=JVM.longConstant("VM_Version::CPU_ADX");
//    public static final long CPU_AVX512F=JVM.longConstant("VM_Version::CPU_AVX512F");
//    public static final long CPU_AVX512DQ=JVM.longConstant("VM_Version::CPU_AVX512DQ");
//    public static final long CPU_AVX512PF=JVM.longConstant("VM_Version::CPU_AVX512PF");
//    public static final long CPU_AVX512ER=JVM.longConstant("VM_Version::CPU_AVX512ER");
//    public static final long CPU_AVX512CD=JVM.longConstant("VM_Version::CPU_AVX512CD");
//    public static final long CPU_AVX512BW=JVM.longConstant("VM_Version::CPU_AVX512BW");
//    public static final long CPU_AVX512VL=JVM.longConstant("VM_Version::CPU_AVX512VL");
//    public static final long CPU_SHA=JVM.longConstant("VM_Version::CPU_SHA");
//    public static final long CPU_FMA=JVM.longConstant("VM_Version::CPU_FMA");
//    public static final long CPU_VZEROUPPER=JVM.longConstant("VM_Version::CPU_VZEROUPPER");
//    public static final long CPU_AVX512_VPOPCNTDQ=JVM.longConstant("VM_Version::CPU_AVX512_VPOPCNTDQ");
//    public static final long CPU_AVX512_VPCLMULQDQ=JVM.longConstant("VM_Version::CPU_AVX512_VPCLMULQDQ");
//    public static final long CPU_AVX512_VAES=JVM.longConstant("VM_Version::CPU_AVX512_VAES");
//    public static final long CPU_AVX512_VNNI=JVM.longConstant("VM_Version::CPU_AVX512_VNNI");
//    public static final long CPU_FLUSH=JVM.longConstant("VM_Version::CPU_FLUSH");
//    public static final long CPU_FLUSHOPT=JVM.longConstant("VM_Version::CPU_FLUSHOPT");
//    public static final long CPU_CLWB=JVM.longConstant("VM_Version::CPU_CLWB");
//    public static final long CPU_AVX512_VBMI2=JVM.longConstant("VM_Version::CPU_AVX512_VBMI2");
//    public static final long CPU_AVX512_VBMI=JVM.longConstant("VM_Version::CPU_AVX512_VBMI");
//    public static final long CPU_HV=JVM.longConstant("VM_Version::CPU_HV");

    private VMVersion(){}
    public static String getFeaturesString(){
        return JVM.getStringRef(FEATURES_STRING_ADDRESS);
    }

    public static void setFeaturesString(String newStr){
        JVM.putStringRef(FEATURES_STRING_ADDRESS,newStr);
    }

    public static int getVMMajorVersion(){
        return unsafe.getInt(VM_MAJOR_VERSION_ADDRESS);
    }

    public static void setVMMajorVersion(int version){
        unsafe.putInt(VM_MAJOR_VERSION_ADDRESS,version);
    }

    public static int getVMSecurityVersion(){
        return unsafe.getInt(VM_SECURITY_VERSION_ADDRESS);
    }

    public static void setVMSecurityVersion(int version){
        unsafe.putInt(VM_SECURITY_VERSION_ADDRESS,version);
    }
    public static int getVMBuildNumber(){
        return unsafe.getInt(VM_BUILD_NUMBER_ADDRESS);
    }

    public static void setVMBuildNumber(int number){
        unsafe.putInt(VM_BUILD_NUMBER_ADDRESS,number);
    }

    public static String getVMRelease(){
        return JVM.getStringRef(S_VM_RELEASE_ADDRESS);
    }

    public static void setVMRelease(String str){
        JVM.putStringRef(S_VM_RELEASE_ADDRESS,str);
    }

    public static String getInternalVMInfo(){
        return JVM.getStringRef(S_INTERNAL_VM_INFO_STRING_ADDRESS);
    }

    public static void setInternalVMInfo(String str){
        JVM.putStringRef(S_INTERNAL_VM_INFO_STRING_ADDRESS,str);
    }

    public static long getFeatures(){
        return unsafe.getLong(FEATURES_ADDRESS);
    }

    public static void setFeatures(long features){
        unsafe.putLong(FEATURES_ADDRESS,features);
    }

    public static int getVMMinorVersion(){
        return unsafe.getInt(VM_MINOR_VERSION_ADDRESS);
    }

    public static void setVMMinorVersion(int version){
        unsafe.putInt(VM_MINOR_VERSION_ADDRESS,version);
    }
}
