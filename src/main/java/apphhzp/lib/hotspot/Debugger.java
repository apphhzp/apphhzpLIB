package apphhzp.lib.hotspot;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.gc.g1.G1BiasedMappedArray;
import apphhzp.lib.hotspot.gc.g1.G1CollectedHeap;
import apphhzp.lib.hotspot.gc.g1.HeapRegion;
import apphhzp.lib.hotspot.oops.*;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantTag;
import apphhzp.lib.hotspot.oops.constant.MethodRefConstant;
import apphhzp.lib.hotspot.oops.constant.Utf8Constant;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.natives.NativeUtil;
import apphhzp.lib.service.ApphhzpLibService;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
//                if (className.equals("apphhzp/lib/hotspot/Test")){
//                    ClassNode classNode=CoremodHelper.bytes2ClassNote(classfileBuffer,className);
//                    for (MethodNode method: classNode.methods){
//                        if (method.name.equals("print")){
//                            method.instructions.clear();
//                            method.instructions.add(new InsnNode(Opcodes.RETURN));
//                        }
//                    }
//                    System.err.println("changed");
//                    return CoremodHelper.classNote2bytes(classNode,true);
//                }
//                return null;
//            }
//        }, true);
        JVM.printAllTypes();
        JVM.printAllConstants();
        JVM.printAllVTBL();
        try {
            String name="/"+ Opcodes.class.getName().replace('.','/')+".class";
            String file =  Opcodes.class.getResource(name).getFile();
            file=file.substring(0,file.length()-name.length()-1);
           System.err.println(file);
        }catch (Throwable t){
            throw new RuntimeException(t);
        }

//        case3();
//        case3();
        ClassHelper.defineClassBypassAgent("apphhzp.lib.hotspot.Test", Debugger.class,true,null);
        System.err.print("[");
        Test.print(5);
        System.err.println("]");
        Test test=new Test(-114);
        test.add(514);
        System.err.println(test.val);

//        boolean a= ClassHelper.isWindows;
//        if (a){
//            case3();
//        }
//        case1();
//        case2();
//        for (Klass klass2 :cld.getKlasses()){
//            if (klass2.isInstanceKlass()){
//                System.err.println(klass2.getName()+":"+dict.contains(klass2.asInstanceKlass()));
//            }
//        }
//        A a=new A();
//        a.val=2103034;
//        Oop oop=new Oop(a);
//        long addr=oop.getNarrow();
//        try {
//            System.err.println(unsafe.getInt(addr+unsafe.objectFieldOffset(A.class.getField("val"))));
//        }catch (Throwable t){
//
//        }

//        InstanceKlass klass=Klass.getOrCreate(ClassHelper.unsafe.getAddress(JVM.type("vmClasses").global("_klasses[static_cast<int>(vmClassID::ClassLoader_klass_knum)]"))).asInstanceKlass();
//        System.err.println(klass.getName());
//        for (FieldInfo info:klass.getFieldInfos()){
//            System.err.println(info.getName(klass.getConstantPool()));
//        }

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
        System.err.println();
        for(;;);
        //System.err.println(Klass.asKlass(A.class).getName().getString());
//        for (int i=0;i<100;i++) {
//            //testConstantModify();
//        }
    }

    private static class A {
        public int val;
        public String name=new String("adsed");

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

    public static void case1(){
//        for (ClassLoaderData cld:ClassLoaderData.getAllClassLoaderData()){
//            for (Klass klass:cld.getKlasses()){
//                if (klass.getAccessFlags().isHiddenClass()){
//                    System.err.println(klass.getName());
//                }
//            }
//        }
    }
    public static void case2(){
        System.err.println("st");
        for (int i=0;i<10000;i++){
            Symbol.newSymbol("-1");
        }
        System.err.println("hot");
        System.out.println(ClassLoaderData.as(Symbol.class.getClassLoader()).getKlasses().size());
        long time=System.nanoTime(),end;
//        for (int i=0;i<100;i++){
//            String[] arr=new String[1000];
//            for (int j=0;j<1000;j++){
//                arr[j]=String.valueOf(j);
//            }
//            Symbol[] symbols=Symbol.newSymbols(arr);
//            System.err.println("tot:"+symbols[new Random().nextInt(1000-1)]);
//        }
        System.err.println("klass cnt:"+Klass.getAllKlasses().size());
        for (int i=0;i<100000;i++){
            if (i==2355){
                System.err.println(i);
            }
            if (i%10000==0){
                Symbol symbol=Symbol.newSymbol(Integer.toString(i));
                System.err.println(symbol+" ref:"+symbol.getRefCount());
            }else {
                Symbol.newSymbol(Integer.toString(i));
            }
        }
        end=System.nanoTime();
        System.out.println(end-time);
        System.err.println("klass cnt:"+Klass.getAllKlasses().size());
        //System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.gc();
        System.err.println("klass cnt:"+Klass.getAllKlasses().size());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.err.println("klass cnt:"+Klass.getAllKlasses().size());
    }

    public static void case3(){

        final int[] cnt = {0};
        int cnt2;
        System.gc();
        cnt2= NativeUtil.getInstancesOfClass(String.class).length;
        ObjectHeap.iterateSubtypes(new HeapVisitor() {
            @Override
            public void prologue(long usedSize) {
            }

            @Override
            public boolean doObj(OopDesc obj) {
                System.err.println(obj.getObject().toString());
                ++cnt[0];
                return false;
            }

            @Override
            public void epilogue() {

            }
        },Klass.asKlass(String.class));
        System.err.println("------------");
        System.err.println(cnt[0]+","+cnt2);
        System.err.println("------------");
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
