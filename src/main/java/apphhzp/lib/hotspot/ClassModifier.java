package apphhzp.lib.hotspot;

import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.method.Method;

import java.util.Arrays;

final class ClassModifier {
    private ClassModifier() {
    }

    public static void modifyResolvedCall(InstanceKlass klass, Method src, Method target) {

    }

//    public static void addReturn(Class<?> clazz,String name,String desc){
//        if (!desc.endsWith(")V")){
//            return;
//        }
//        InstanceKlass klass= Klass.asKlass(clazz).asInstanceKlass();
//        Method method=klass.getMethod(name,desc);
//        if (method!=null){
//            ConstMethod constMethod=method.getConstMethod(),newConstMethod;
//            ConstantPool pool=klass.getConstantPool(),newPool;
////            constMethod.setCode(0,(byte) Opcodes.RETURN);
////            ///pool.getCache().clearResolvedCacheEntry();
//            byte[] oldCode,newCode;
//            oldCode=constMethod.getCodes();
//            newCode=new byte[1];
//            newCode[0]= (byte) Opcodes.RETURN;
//            //System.arraycopy(oldCode, 0, newCode, 1, oldCode.length);
//            printUnsignedByteArray(oldCode);
//            System.err.println(constMethod.getSize());
//            System.err.println(constMethod.lastU2ElementOffset());
//            if (ConstMethod.hasLineNumberTable(constMethod.getFlags())) {
//               System.err.println(constMethod.getLineNumberFromBCI(3));
//            }
//            System.err.println(constMethod.getMethodParametersLength());
//            newConstMethod=constMethod.copy(1-constMethod.getCodeSize(),newCode);
//            printUnsignedByteArray(newCode);
//            System.err.println(newConstMethod.getSize());
//            System.err.println(newConstMethod.lastU2ElementOffset());
//            if (ConstMethod.hasLineNumberTable(constMethod.getFlags())) {
//                System.err.println(constMethod.getLineNumberFromBCI(3));
//            }
//            System.err.println(constMethod.getMethodParametersLength());
//            method.setConstMethod(newConstMethod);
//            method.setCompiledMethod(null);
//        }
//    }

    private static void printUnsignedByteArray(byte[] array){
        short[] out=new short[array.length];
        for (int i=0,len=out.length;i<len;i++){
            out[i]= (short) (array[i]&0xff);
        }
        System.err.println(Arrays.toString(out));
    }
}
