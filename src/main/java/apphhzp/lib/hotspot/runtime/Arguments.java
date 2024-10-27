package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

import static apphhzp.lib.ClassHelper.unsafe;

public class Arguments {
    public static final Type TYPE = JVM.type("Arguments");
    public static final long NUM_JVM_FLAGS_ADDRESS = TYPE.global("_num_jvm_flags");
    public static final long JVM_FLAGS_ARRAY_ADDRESS = TYPE.global("_jvm_flags_array");
    public static final long JVM_ARGS_ARRAY_ADDRESS = TYPE.global("_jvm_args_array");
    public static final long NUM_JVM_ARGS_ADDRESS = TYPE.global("_num_jvm_args");
    public static final long JAVA_COMMAND_ADDRESS = TYPE.global("_java_command");
    private Arguments(){}

    public static int getJVMFlagsLength() {
        return unsafe.getInt(NUM_JVM_FLAGS_ADDRESS);
    }

    public static void setJVMFlagsLength(int length) {
        unsafe.putInt(NUM_JVM_FLAGS_ADDRESS, length);
    }

    public static int getJVMArgsLength() {
        return unsafe.getInt(NUM_JVM_ARGS_ADDRESS);
    }

    public static void setJVMArgsLength(int length) {
        unsafe.putInt(NUM_JVM_ARGS_ADDRESS,length);
    }

    public static String[] getJVMFlags(){
        int len=getJVMFlagsLength();
        String[] re=new String[len];
        long base=unsafe.getAddress(JVM_FLAGS_ARRAY_ADDRESS);
        for (int i=0;i<len;i++){
            re[i]=JVM.getStringRef(base+ (long) i *JVM.oopSize);
        }
        return re;
    }

    public static String[] getJVMArgs(){
        int len=getJVMArgsLength();
        String[] re=new String[len];
        long base=unsafe.getAddress(JVM_ARGS_ARRAY_ADDRESS);
        for (int i=0;i<len;i++){
            re[i]=JVM.getStringRef(base+ (long) i *JVM.oopSize);
        }
        return re;
    }

    public static String getJavaCommand(){
        return JVM.getStringRef(JAVA_COMMAND_ADDRESS);
    }
}
