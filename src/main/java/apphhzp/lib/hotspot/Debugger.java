package apphhzp.lib.hotspot;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.api.stackframe.LiveStackFrameInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.CodeCache;
import apphhzp.lib.hotspot.code.CodeHeap;
import apphhzp.lib.hotspot.code.RelocInfo;
import apphhzp.lib.hotspot.code.RelocIterator;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
import apphhzp.lib.hotspot.code.blob.NMethod;
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
import apphhzp.lib.hotspot.oops.method.ConstMethod;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.SignatureStream;
import apphhzp.lib.natives.NativeUtil;
import org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;

public final class Debugger {
    public static boolean isDebug=true;
    private static int x1;

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InterruptedException {//-XX:-UseCompressedOops  -XX:-UseCompressedClassPointers -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining
//        {
//            int i = 200;
//            while (i-- > 0){
//                test();
//            }
//        }
        //BytecodeRunner.inject(Debugger.class, "main", "([Ljava/lang/String;)V", new byte[]{});
//        JVM.INSTANCE.printAllTypes();
        //isDebug=true;
//        ClassHelperSpecial.objectInstImpl.setHeapSamplingInterval(0);
//        ClassHelperSpecial.objectInstImpl.addMonitor(new ObjectMemoryMonitor() {
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
        JVM.printAllTypes();
        JVM.printAllConstants();
        JVM.printAllVTBL();
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

//        if (!NativeUtil.setJNIFunction(JNIFunctions.CallObjectMethodV, cb)){
//            throw new RuntimeException();
//        }
//
//        ApphhzpInst inst=NativeUtil.createApphhzpInstImpl();
//        inst.getAllLoadedClasses();
//        inst.addTransformer(new ClassFileTransformer() {
//            @Override
//            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//                System.err.println("found: "+className);
//                return ClassFileTransformer.super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
//            }
//        },true);
        InstanceKlass klass=Klass.asKlass(Debugger.class).asInstanceKlass();
        for (Method method:klass.getMethods()){
            System.err.print(method.name().toString()+method.signature()+":  ");
            //if (method.name().toString().equals("printFrame")){
                SignatureStream signatureStream=new SignatureStream(method.signature());
                for (;!signatureStream.is_done();signatureStream.next()){
                    System.err.print(signatureStream.as_symbol()+",");
                    System.gc();
                }
                System.err.println();
            //}
        }
//        for (Klass klass:Klass.getAllKlasses()){
//            if (klass.isInstanceKlass()){
//                for (Method method:klass.asInstanceKlass().getMethods()){
//                    LocalVariableTableElement[] array=method.getConstMethod().getLocalVariableTable();
//                    if (array!=null){
//                        System.err.println(klass.getName()+"."+method.name()+method.signature() +"===" + Arrays.toString(array));
//                    }
//                }
//            }
//        }
        AtomicInteger val=new AtomicInteger();
        val.set(114);
        System.err.println(val.get());
        printFrame(val);
        System.err.println(val.get());
        //case3();
//        case4();
//        case5();
//        System.err.println(cb);
        //CallA.call();

//        for (Thread obj: Thread.getAllStackTraces().keySet()){
//            System.err.println(obj);
//            if (obj.getName().equals("azzz")){
//                obj.suspend();
//            }
//        }

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
//        boolean a= ClassHelperSpecial.isWindows;
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

//        InstanceKlass klass=Klass.getOrCreate(ClassHelperSpecial.unsafe.getAddress(JVM.type("vmClasses").global("_klasses[static_cast<int>(vmClassID::ClassLoader_klass_knum)]"))).asInstanceKlass();
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
        //System.err.println("aadsa:"+ClassHelperSpecial.objectImpl.canHookVMObjectAllocEvents()+","+ClassHelperSpecial.objectImpl.canHookObjectFreeEvents());
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
        for(;;){

        }
        //System.err.println(Klass.asKlass(A.class).getName().getString());
//        for (int i=0;i<100;i++) {
//            //testConstantModify();
//        }
    }


    private static void printFrame(AtomicInteger integer){
        for (LiveStackFrameInfo info:ClassHelperSpecial.getLiveStackFrame()){
            if (info.getMethodName().equals("printFrame")){
                Object[] locals = info.getLocals();
                for (int i = 0, localsLength = locals.length; i < localsLength; i++) {
                    Object obj = locals[i];
                    if (obj instanceof AtomicInteger val) {
                        val.set(114514);
                    }
                }
            }
            //System.err.println(info.getMethodName()+": "+ Arrays.toString(info.getMonitors())+", "+Arrays.toString(info.getLocals())+", "+Arrays.toString(info.getStack()));
        }
    }
    private static void throwEx()throws ClassNotFoundException{
        throw new ClassNotFoundException("FuckYouClass not found");
    }

    private static String toBinaryString(byte val) {
        StringBuilder s= new StringBuilder(Long.toBinaryString(val & 0xffL));
        while (s.length()<8) {
            s.insert(0, "0");
        }
        return s.toString();
    }

    private static class CallA{
        public static int fx;
        public static void call(){
            System.err.println("111");
        }

        public void print(){
            System.err.println("print");
        }
    }

    private static class HookClassLoader{
        private static final MethodHandle mh;
        static {
            Unsafe unsafe;
            try {
                MethodHandles.Lookup lookup= (MethodHandles.Lookup) ReflectionFactory.getReflectionFactory()
                        .newConstructorForSerialization(MethodHandles.Lookup.class, MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,Class.class,int.class))
                        .newInstance(Object.class,null,-1);
                mh=lookup.findStatic(ClassLoader.class,"defineClass0", MethodType.methodType(Class.class, ClassLoader.class, Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class, boolean.class, int.class, Object.class));
                unsafe= (Unsafe) lookup.findStaticVarHandle(Unsafe.class,"theUnsafe",Unsafe.class).get();
            }catch (Throwable t){
                throw new RuntimeException(t);
            }
            unsafe.putLong(HookClassLoader.class,16,unsafe.getLong(ClassLoader.class,16));
        }

        public static Class<?> defineClass0(ClassLoader loader,
                                            Class<?> lookup,
                                            String name,
                                            byte[] b, int off, int len,
                                            ProtectionDomain pd,
                                            boolean initialize,
                                            int flags,
                                            Object classData) throws Throwable {
            //NativeUtil.createMsgBox(name,"",0);
            return (Class<?>) mh.invoke(loader, lookup, name, b, off, len, pd, initialize, flags, classData);
        }
    }

    private static class A {
        public int val;
        public String name= "adsed";

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
        newCP.name_and_type_at_put(len - 1, call33.which, desc_V.which);
        newCP.method_at_put(call1.which, call1.klass.which, len - 1);
        klass.setConstantPool(newCP);
        if (newCP.getCache() != null) {
            newCP.getCache().setConstantPool(newCP);
            newCP.getCache().clearResolvedCacheEntry();
        }
    }

    public static void case1(){
        ClassHelperSpecial.defineClassBypassAgent("apphhzp.lib.hotspot.Test", Debugger.class,true,null);
        System.err.print("[");
        Test.print(5);
        System.err.println("]");
        Test test=new Test(-114);
        test.add(514);
        try {
            Class.forName("apphhzp.lib.hotspot.Test");
            System.err.println("found!!!");
        }catch (ClassNotFoundException t){
            System.err.println("not found");
        }
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

    public static void case4(){
        for (int i=0;i<3030030;i++){
            Test.doit();
        }
        InstanceKlass klass=Klass.asKlass(Debugger.class).asInstanceKlass();
        System.err.println(x1);
        System.err.println(Test.doit());
        for (Method method:klass.getMethods()){
            ConstMethod constMethod=method.getConstMethod();
            if (constMethod.getName().toString().equals("test")){
                System.err.println("Compiled code： 0x"+Long.toHexString(method.getFromCompiledEntry()));
                constMethod.setCode(0, (byte) Opcodes.ACONST_NULL);
                constMethod.setCode(1, (byte) Opcodes.ARETURN);

                break;
            }
        }
        for (int i=0;i<3030030;i++){
            Test.doit();
        }
        System.err.println(Test.doit());
        System.err.println(x1);
        for (Method method:klass.getMethods()){
            ConstMethod constMethod=method.getConstMethod();
            if (constMethod.getName().toString().equals("test")){
                System.err.println("Compiled code： 0x"+Long.toHexString(method.getFromCompiledEntry()));
                break;
            }
        }
        for (int i=0;i<3030030;i++){
            Test.doit();
        }
        System.err.println(Test.doit());
        System.err.println(x1);
    }

    public static void case5(){
        for (CodeHeap heap: CodeCache.getCompiledHeaps()){
            for (CodeBlob blob:heap){
                if (blob instanceof NMethod nMethod){
                    System.err.println(nMethod.getMethod().getHolder().getName()+"."+nMethod.getMethod().getConstMethod().getName()+" "+
                            nMethod.getCompLevel());
                    RelocIterator itr=new RelocIterator(nMethod);
                    while (itr.next()){
                        if (itr.type()== RelocInfo.Type.VIRTUAL_CALL_TYPE){
                            System.err.println("get!");
                        }
                    }
                }
            }
        }
    }

    public static Object test(Integer integer) {
        call1();
        return null;
    }

    public static void call1() {
        x1++;
        //System.err.println("Hello World!");
    }

    public static void call33() {
        x1=0;
    }
}
