package apphhzp.lib.hotspot;

import apphhzp.lib.hotspot.code.blob.CompiledMethod;
import apphhzp.lib.hotspot.code.blob.NMethod;
import apphhzp.lib.hotspot.oop.InstanceKlass;
import apphhzp.lib.hotspot.oop.Klass;
import apphhzp.lib.hotspot.oop.MethodCounters;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.constant.ConstantPoolCacheEntry;
import apphhzp.lib.hotspot.oop.method.ConstMethod;
import apphhzp.lib.hotspot.oop.method.Method;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static apphhzp.lib.ClassHelper.unsafe;

public final class ClassModifier {
    private ClassModifier() {
    }

    public static void modifyResolvedCall(InstanceKlass klass, Method src, Method target) {
        ConstantPool pool = klass.getConstantPool();
        if (pool.getCache() != null) {
            for (ConstantPoolCacheEntry entry : pool.getCache()) {
                if (entry.isResolved() && ConstantPoolCacheEntry.is_method_entry(entry.getFlags())) {
                    Method method = entry.f1AsMethod();
                    if (src.equals(method)) {
                        CompiledMethod compiledMethod = method.getCode();
//                        if (counters!=null){
//                            method.setCounters(null);
//                        }
                        if (compiledMethod != null) {
                            System.err.println("name:"+compiledMethod.getName());
                            if (compiledMethod instanceof NMethod nmethod){
                                System.err.println(nmethod.address);
                                System.err.println(nmethod.getCompLevel());
                                System.err.println("bci:"+nmethod.getEntryBci());
                                System.err.println("state:"+(int) nmethod.getState());
                            }
                            compiledMethod.setMethod(target);
                            method.setFromCompiledEntry(unsafe.getAddress(unsafe.getAddress(target.address+32L)+56L-24L));
                            method.setFromInterpretedEntry(target.getI2IEntry());
                            //method.setCode(null);
                            MethodCounters counters=method.getCounters();
                            if (counters!=null){
                                counters.invocationCounter.setCount(0);
                                counters.backedgeCounter.setCount(0);
                            }

                        }
                        entry.setF1(target.address);
                    } else {
                        method = entry.f2AsMethod();
                        if (src.equals(method)) {
                            entry.setF2(target.address);
                            CompiledMethod compiledMethod = method.getCode();
                            if (compiledMethod != null) {
                                System.err.println("f2:"+compiledMethod.getMethod().getConstMethod().getName());
                                compiledMethod.setMethod(target);
                            }
                        } else {
                            int f2 = entry.f2AsIndex();
                            if (f2 != -1) {
                                method = entry.vtableIndex2Method(f2);
                                if (src.equals(method)) {
                                    System.err.println("addsdsas");
//                                    entry.setF2(target.address);
                                    CompiledMethod compiledMethod = method.getCode();
                                    if (compiledMethod != null) {
                                        compiledMethod.setMethod(target);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void addReturn(Class<?> clazz,String name,String desc){
        if (!desc.endsWith(")V")){
            return;
        }
        InstanceKlass klass= Klass.asKlass(clazz).asInstanceKlass();
        Method method=klass.getMethod(name,desc);
        if (method!=null){
            ConstMethod constMethod=method.getConstMethod(),newConstMethod;
            ConstantPool pool=klass.getConstantPool(),newPool;
//            constMethod.setCode(0,(byte) Opcodes.RETURN);
//            ///pool.getCache().clearResolvedCacheEntry();
            byte[] oldCode,newCode;
            oldCode=constMethod.getCodes();
            newCode=new byte[1];
            newCode[0]= (byte) Opcodes.RETURN;
            //System.arraycopy(oldCode, 0, newCode, 1, oldCode.length);
            printUnsignedByteArray(oldCode);
            System.err.println(constMethod.getSize());
            System.err.println(constMethod.lastU2ElementOffset());
            if (ConstMethod.hasLineNumberTable(constMethod.getFlags())) {
               System.err.println(constMethod.getLineNumberFromBCI(3));
            }
            System.err.println(constMethod.getMethodParametersLength());
            newConstMethod=constMethod.copy(1-constMethod.getCodeSize(),newCode);
            printUnsignedByteArray(newCode);
            System.err.println(newConstMethod.getSize());
            System.err.println(newConstMethod.lastU2ElementOffset());
            if (ConstMethod.hasLineNumberTable(constMethod.getFlags())) {
                System.err.println(constMethod.getLineNumberFromBCI(3));
            }
            System.err.println(constMethod.getMethodParametersLength());
            method.setConstMethod(newConstMethod);
            method.setCode(null);
        }
    }

    private static void printUnsignedByteArray(byte[] array){
        short[] out=new short[array.length];
        for (int i=0,len=out.length;i<len;i++){
            out[i]= (short) (array[i]&0xff);
        }
        System.err.println(Arrays.toString(out));
    }
}
