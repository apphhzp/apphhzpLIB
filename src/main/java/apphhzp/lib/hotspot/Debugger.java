package apphhzp.lib.hotspot;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.HeapVisitor;
import apphhzp.lib.hotspot.oops.ObjectHeap;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantTag;
import apphhzp.lib.hotspot.oops.constant.MethodRefConstant;
import apphhzp.lib.hotspot.oops.constant.Utf8Constant;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.natives.NativeUtil;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public final class Debugger {
    public static boolean isDebug=true;
    private static int x1;

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {//-XX:-UseCompressedOops  -XX:-UseCompressedClassPointers -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining
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
//        JVM.printAllVTBL();
        JVM.printAllFunctions();

//        int id=0;
//        for (VTableEntry entry:klass.getVTableEntries()){
//            if (entry.method()!=null){
//                System.err.println(id+":"+entry.method().getConstMethod().getName());
//            }
//            ++id;
//        }
//        AbstractInterpreter.getCode().iterator().forEachRemaining(x -> {
//            System.err.println(x.getDesc());
//            if (x.getDesc().equals("iload")){
//                System.err.println(Bytecodes.length_for(x.getBytecode()));
//            }
//        });
//        case3();
//        case3();

        ClassHelper.instImpl.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (className.startsWith("apphhzp/lib")){
                    System.err.println("found: "+className);
                }
                return null;
            }
        },true);

        /*
        * new byte[]{0x33, (byte) 0xC0, 0x50, (byte) 0xB8, 0x2E, 0x64, 0x6C, 0x6C,
                     0x50, (byte) 0xB8, 0x65, 0x6C, 0x33, 0x32, 0x50, (byte) 0xB8,
                     0x6B, 0x65, 0x72, 0x6E, 0x50, (byte) 0x8B, (byte) 0xC4, 0x50,
                (byte) 0xB8, 0x7B, 0x1D, (byte) 0x80, 0x7C, (byte) 0xFF, (byte) 0xD0, 0x33,
                (byte) 0xC0, 0x50, (byte) 0xB8, 0x2E, 0x65, 0x78, 0x65, 0x50,
                (byte) 0xB8, 0x63, 0x61, 0x6C, 0x63, 0x50, (byte) 0x8B, (byte) 0xC4,
                     0x6A, 0x05, 0x50, (byte) 0xB8, (byte) 0xAD, 0x23, (byte) 0x86, 0x7C,
                (byte) 0xFF, (byte) 0xD0, 0x33, (byte) 0xC0, 0x50, (byte) 0xB8, (byte) 0xFA, (byte) 0xCA,
                (byte) 0x81, 0x7C, (byte) 0xFF, (byte) 0xD0}
        * */
        ClassHelper.defineClassBypassAgent("apphhzp.lib.hotspot.Test", Debugger.class,false,null);
        System.err.print("[");
        Test.print(5);
        System.err.println("]");
        Test test=new Test(-114);
        test.add(514);

        ClassHelper.createHiddenThread(()->{
            //varHandle.set(Thread.currentThread(),0);
            while (true){
                System.err.println("azzz");
                try {
                    Thread.sleep(1000);
                }catch (Throwable t){
                }
            }
        },"azzz");
        for (Thread obj: Thread.getAllStackTraces().keySet()){
            System.err.println(obj);
            if (obj.getName().equals("azzz")){
                obj.suspend();
            }
        }

//        System.err.println(test.val);
//        System.err.println(Runtime1.blobFor(32).getName());
//        long st=System.nanoTime(),ed;
//        for (int i=1000;i<=500000;i++){
//            Symbol.newSymbol(String.valueOf(i));
//        }
//        ed=System.nanoTime();
//        System.err.println(ed-st);
//        for (int i=0;;i++){
//            System.err.println(Long.toHexString(unsafe.getByte(addr+i)&0xffL));
//            if (i%8==0){
//                System.err.println("0x"+Long.toHexString(unsafe.getLong(addr+i)));
//            }
//            if ((unsafe.getByte(addr+i)&0xffL)==0xcc){
//                break;
//            }
//        }
        //Pointer pointer=new_symbol.invokePointer(new Object[]{"saddsadffdas"});
        //System.err.println(Symbol.of(Pointer.nativeValue(pointer)));
//        Function function=Function.getFunction(new Pointer(JVM.lookupSymbol("psd")));
//        for (CodeHeap heap:CodeCache.getHeaps()){
//            for (CodeBlob blob:heap){
//                System.err.println(
//                        CodeCache.findNMethod(blob.address));
//            }
//        }
//        for (Klass klass:Klass.getAllKlasses()){
//            if (klass.isInstanceKlass()){
//                if (klass.asInstanceKlass().getInnerClasses().length()!=0){
//                    System.err.println(klass);
//                }
//            }
//        }
//        for (Klass klass:Klass.getAllKlasses()){
//            if (klass instanceof InstanceKlass instanceKlass){
//                System.err.println(instanceKlass.module());
//            }
//        }
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
