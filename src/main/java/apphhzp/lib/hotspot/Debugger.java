package apphhzp.lib.hotspot;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oop.FieldInfo;
import apphhzp.lib.hotspot.oop.InstanceKlass;
import apphhzp.lib.hotspot.oop.Klass;
import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.constant.ConstantTag;
import apphhzp.lib.hotspot.oop.constant.MethodRefConstant;
import apphhzp.lib.hotspot.oop.constant.Utf8Constant;

import java.io.InputStream;

public final class Debugger {
    public static boolean isDebug=false;
    private static int x1;

    public static void main(String[] args) {//-XX:-UseCompressedOops  -XX:-UseCompressedClassPointers -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining
//        {
//            int i = 200;
//            while (i-- > 0){
//                test();
//            }
//        }
        //BytecodeRunner.inject(Debugger.class, "main", "([Ljava/lang/String;)V", new byte[]{});
//        JVM.INSTANCE.printAllTypes();
        //isDebug=true;
//        ClassHelper.objectInstImpl.setHeapSamplingInterval(0);
//        ClassHelper.objectInstImpl.addMonitor(new ObjectMemoryMonitor() {
//            @Override
//            public void onVMObjectAlloc(Thread thread, Object obj, Class<?> objClass, long size) {
//                ObjectMemoryMonitor.super.onVMObjectAlloc(thread, obj, objClass, size);
//            }
//            @Override
//            public void onObjectFree(long tag) {
//                ObjectMemoryMonitor.super.onObjectFree(tag);
//            }
//            @Override
//            public void onSampledObjectAlloc(Thread thread, Object obj, Class<?> objClass, long size) {
//                ObjectMemoryMonitor.super.onSampledObjectAlloc(thread, obj, objClass, size);
//            }
//        });

//        ClassHelper.instImpl.addTransformer(new ClassFileTransformer() {
//            @Override
//            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer){
//                System.err.println((loader==null?"null classloader":loader.getName())+":"+className);
//                return null;
//            }
//        }, true);
        try {
            InputStream is = Debugger.class.getResourceAsStream("/JVM.class");
            byte[] dat = new byte[is.available()];
            is.read(dat);
            is.close();
//            Class<?> c= ClassHelper.defineHiddenClass(dat,"看你妈",true, Debugger.class,null,null, ClassHelper.ClassOption.NESTMATE).lookupClass();
//            InstanceKlass klass=Klass.asKlass(c).asInstanceKlass();
//
//            TestSuper s= (TestSuper) c.newInstance();
//            s.print();
//            System.err.println(c.getName());

        }catch (Throwable t){
            throw new RuntimeException(t);
        }

        JVM.printAllTypes();
        JVM.printAllConstants();
        JVM.printAllVTBL();
        InstanceKlass klass=Klass.getOrCreate(ClassHelper.unsafe.getAddress(JVM.type("vmClasses").global("_klasses[static_cast<int>(vmClassID::ClassLoader_klass_knum)]"))).asInstanceKlass();
        System.err.println(klass.getName());
        for (FieldInfo info:klass.getFieldInfos()){
            System.err.println(info.getName(klass.getConstantPool()));
        }

//        InstanceKlass klass=Klass.asKlass(TestSuper.class).asInstanceKlass();
//        Method method=klass.getMethod("call1","()V");
//        for (int i=0;i<291993;i++){
//            test();
//        }
//        CodeCache.markAllNMethodsForDeoptimization();
//        CodeCache.makeMarkedNMethodsNotEntrant();
//        modifyConstant();
//        //CodeCache.markAllNMethodsForEvolDeoptimization();
//
//        test();
//        System.err.println(x1);
        //
        //klass.setMiscFlags(klass.getMiscFlags()|InstanceKlass.MiscFlags.REWRITTEN);
        //test();
        //System.err.println("aadsa:"+ClassHelper.objectImpl.canHookVMObjectAllocEvents()+","+ClassHelper.objectImpl.canHookObjectFreeEvents());
//        ConstantPool pool=Klass.asKlass(Debugger.class).getConstantPool();
//        for (int i=1,len=pool.length();i<len;i++){
//            System.err.println(pool.getConstant(i));
//        }
/*JNIHandles @ 1
  static OopStorage* _global_handles @ 0x7ff8bcf0b728
  static OopStorage* _weak_global_handles @ 0x7ff8bcf0b730*/
        /*
  ciEnv @ 232
  void* _compiler_data @ 128<----Compile*  */
//        List<Klass> klasses = cld.getKlasses();
//        for (int i = 0, klassesSize = klasses.size(); i < klassesSize; i++) {
//            Klass tmp = klasses.get(i);
//            if (tmp.equals(klass)) {
//                Klass pre=klasses.get(i-1);
//                pre.setNextKlass(tmp.getNextKlass());
//            }
//        }
//        for (Klass klass:Klass.getAllKlasses()){
//            if (klass.isInstanceKlass()){
//                InstanceKlass instanceKlass=(InstanceKlass) klass;
//                for (Method method:instanceKlass.getMethods()){
//                    method.setFlags(method.getFlags()|Method.DONT_INLINE);
//                    method.setFlags(method.getFlags()&~Method.FORCE_INLINE);
//                }
//            }
//        }
        //InstanceKlass klass = Klass.asKlass(Debugger.class).asInstanceKlass();
        //Method method = klass.getMethod("call1", "()V"), target = klass.getMethod("call33", "()V"), method1 = klass.getMethod("ddd", "()V");
        for(;;);
        //System.err.println(Klass.asKlass(A.class).getName().getString());
//        for (int i=0;i<100;i++) {
//            //testConstantModify();
//        }
    }

    private static class A {
        public int val;

        public static void say() {
            System.err.println("AAA");
        }
    }

    private static class B {
        public static void say() {
            System.err.println("BBB");
        }
    }

    public static void function_() {
        System.err.println("A method...");
    }


    public static void modifyConstant() {
        InstanceKlass klass = Klass.asKlass(Debugger.class).asInstanceKlass();
        ConstantPool pool = klass.getConstantPool();
        ConstantPool newCP = pool.copy(1);
        int len = newCP.getLength();
        MethodRefConstant call1 = newCP.findConstant((MethodRefConstant c) -> c.nameAndType.name.str.toString().equals("call1"), ConstantTag.Methodref);
        Utf8Constant call33 = newCP.findConstant((Utf8Constant c) -> c.str.toString().equals("call33"), ConstantTag.Utf8);
        Utf8Constant desc_V = newCP.findConstant((Utf8Constant c) -> c.str.toString().equals("()V"), ConstantTag.Utf8);
        newCP.nameAndType_at_put(len - 1, call33.which, desc_V.which);
        newCP.methodRef_at_put(call1.which, call1.klass.which, len - 1);
        klass.setConstantPool(newCP);
        if (newCP.getCache() != null) {
            newCP.getCache().setConstantPool(newCP);
            newCP.getCache().clearResolvedCacheEntry();
        }
    }

    public static void test() {
        call1();
    }

    public static void call1() {
        x1++;
        //System.err.println("Hello World!");
    }

    public static void call33() {
        x1--;
    }
}
