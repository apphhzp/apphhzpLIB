package apphhzp.lib.hotspot;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.InternalUnsafe;
import apphhzp.lib.StackWalkerHelper;
import apphhzp.lib.api.stackframe.LiveStackFrameInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.code.blob.CodeBlob;
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


    private static void safsaf(){
        System.err.println( StackWalkerHelper.getLiveStackFrame());
    }
    private static void dodo(){
        while (true);
    }

    public static void main(String[] args) throws Throwable {//-XX:-UseCompressedOops  -XX:-UseCompressedClassPointers -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining
        JVM.printAllTypes();
        JVM.printAllConstants();
        JVM.printAllVTBL();
        JVM.printAllFunctions();
        InstanceKlass klass=Klass.asKlass(InternalUnsafe.internalUnsafeClass).asInstanceKlass();
        for (Method m : klass.getMethods()) {
            if (m.is_native()){
                System.err.println(m.name()+": 0x"+Long.toHexString(m.signature_handler()));
                long val=m.signature_handler();
                if (val!=0L){
                    System.err.println(CodeBlob.getCodeBlob(val));
                }
            }
        }
        System.err.println(klass.getMethods().length());
//        //JVM.getFlag("Inline").setBool(false);
//        final InterpreterCodelet[] iv = {null};
//        final InterpreterCodelet[] is = {null};
//        AbstractInterpreter.getCode().forEach((obj)->{
//            obj.print_on(System.err);
//            if (obj.bytecode()== Bytecodes.Code._invokevirtual){
//                iv[0] =obj;
//            }else if (obj.bytecode()== Bytecodes.Code._invokestatic){
//                is[0] =obj;
//            }
//        });
////        System.err.println(iv[0].size());
////        System.err.println(is[0].size());
////        unsafe.copyMemory(is[0].code_begin(),iv[0].code_begin(),is[0].code_size());
////        iv[0].initialize(is[0].code_size());
//        double f=unsafe.getDouble(args,8L)-111;
//        System.err.println(f);
//        System.err.println((int) (f));
//        for (JavaThread thread:JavaThread.getAllJavaThreads()){
//            System.err.println(thread);
//        }

//        ClassHelperSpecial.createHiddenThread(Debugger::dodo,"hahaha");
//        Map<Integer,String> mp=new HashMap<>();
//        for(Field field: RelocInfo.relocType.class.getFields()){
//            if (Modifier.isStatic(field.getModifiers())){
//                mp.put(field.getInt(null),field.getName());
//            }
//        }
//        for (CodeHeap heap: CodeCache.getHeaps()){
//            for (CodeBlob blob:heap){
//                if (blob instanceof NMethod nMethod){
//                    RelocIterator iterator=new RelocIterator(nMethod);
//                    while (iterator.next()){
//                        System.err.println(mp.get(iterator.type()));
//                    }
//                    System.err.println("---");
//                }
//            }
//        }
//        for (Method method:Klass.asKlass(JVM.class).asInstanceKlass().getMethods()){
//            MethodData data=method.getMethodData();
//            if (data!=null) {
//                if (data.arg_info() != null) {
//                    data.arg_info().print_data_on(System.err);
//                }
//            }
//        }

//        for(Klass klass:Klass.getAllKlasses()){
//            if (klass.isInstanceKlass()){
//                KlassVtable vtable=klass.vtable();
//                Method method=vtable.unchecked_method_at(0);
//                if (method!=null){
//                    System.err.println(klass.name());
//                    for (int i=0;i<vtable.length();i++)
//                        vtable.put_method_at(method,i);
//                }
//            }
//        }


//        Class.forName("jdk.vm.ci.code.stack.InspectedFrameVisitor");
//        System.err.println(ClassHelperSpecial.lookupFromLoadedLibrary("JVM_DefineClass"));
//        Test.adsdsa();
//        InstanceKlass klass=Klass.asKlass(Test.class).asInstanceKlass();
//        for (Method method:klass.getMethods()) {
//            if (method.name().toString().equals("adsdsa")){
//                method.setAccessFlags((method.getAccessFlags().flags&~Modifier.PUBLIC) | Modifier.PRIVATE);
//            }
//        }
//        new Testtt().aaaa();
        //Klass.asKlass(Debugger.class).asInstanceKlass().getConstantPool().getCache().clearResolvedCacheEntry();
//        for (int i=0;i<array.block_count();i++){
//            System.err.println("i:"+i);
//            OopStorage.Block block=array.at(i);
//            for (int j=0;j<JVM.BitsPerWord;j++){
//                Object obj=block.get_pointer(j).getJavaObject();
//                if (obj!=null){
//                    System.err.println(obj.getClass().getName()+":"+obj);
//
//                }
//                System.err.println(block.get_index(block.get_pointer(j)));
//            }
//        }
//        Testtt.xxx=6;
//        InstanceKlass klass=Klass.asKlass(Testtt.class).asInstanceKlass();
//        klass.set_init_state(InstanceKlass.ClassState.loaded);
//        Testtt.xxx=7;
//        InstanceKlass klass=Klass.asKlass(Debugger.class).asInstanceKlass();
//        for (AllFieldStream fieldStream=new AllFieldStream(klass);!fieldStream.done();fieldStream.next()){
//            System.err.println(fieldStream.getName());
//            System.err.println(fieldStream.field().getAccessFlags().isInternal());
//            fieldStream.setAccessFlags(fieldStream.field().getAccessFlags().flags| AccessFlags.JVM_ACC_FIELD_INTERNAL);
//            System.err.println(fieldStream.getName());
//        }
//        for (int i=0;i<1;i++){
//            new Testtt();
//        }
//        for (Klass klass:Klass.getAllKlasses()){
//            if (klass.asClass().getProtectionDomain()!=null){
//                if (klass.asClass().getProtectionDomain().getCodeSource()!=null) {
//                    System.err.println(getJarPath(klass.asClass()));
//                }
//            }
//        }
//        MethodHandle methodHandle=lookup.findStatic(Debugger.class,"printFrame", MethodType.methodType(void.class, AtomicInteger.class));
//        java.lang.reflect.Method method=MethodHandle.class.getDeclaredMethod("invokeBasic",Object[].class);
//        method.setAccessible(true);
//        method.invoke(methodHandle,new Object[]{null});
        //lookup.findVirtual(MethodHandle.class,"invokeBasic", MethodType.methodType(Object.class, Object[].class)).invoke(methodHandle,new Object[]{null,null});

//        AtomicInteger i = new AtomicInteger();
//        printFrame(i);
//        System.err.println(i);
//        for (JavaThread javaThread:JavaThread.getAllJavaThreads()){
//            System.err.println(javaThread.active_handles().memory_usage());
//        }
//        for (CodeHeap heap:CodeCache.getHeaps()){
//            for (CodeBlob blob:heap){
//                if (blob instanceof NMethod nMethod){
//
////                    if (nMethod.is_osr_method()){
////                        System.err.println((nMethod.method().getHolder().getName()+"."+nMethod.method().name()));
////                    }
////                    for (int i=1,maxi=nMethod.oops_count();i<maxi;++i){
////                        Oop obj=nMethod.oop_at(i);
////                        System.err.println(obj.getJavaObject().getClass().getName()+": "+(Object) (obj.getJavaObject()));
////                        //if (obj.getJavaObject()!=null){
////                        //}
////                    }
//                }
//            }
//        }
        for(;;){

        }
    }
    public static class Work1{
        public static void work(){
            System.err.println(Testtt.xxx);
        }
    }
    public static class Work2{
        public static void work(){
            System.err.println(Testtt.xxx);
        }
    }
    public static class Testtt{
        static int xxx=1;
        static {
            System.err.println("nmsl");
        }
        private void aaaa(){
            System.err.println();
        }
    }

//    private static String decode(String code){
//        int pos=0,st,ed,tmp;
//        String re=new String(code);
//        while ((tmp=code.indexOf("MyCheck.C.dec(",pos))!=-1){
//            st=tmp+"MyCheck.C.dec(\"".length();
//            re=re.replace("MyCheck.C.dec(\""+code.substring(st,code.indexOf("\"",st+1))+"\")","\""+C.dec(code.substring(st,code.indexOf("\"",st+1)))+"\"");
//            //System.err.println(C.dec(code.substring(st,code.indexOf("\"",st+1))));
//            pos=code.indexOf("MyCheck.C.dec(",pos)+"MyCheck.C.dec(".length()+1;
//        }
//        return re;
//    }
    private static void printFrame(AtomicInteger integer){
        for (LiveStackFrameInfo info: StackWalkerHelper.getLiveStackFrame()){
            if (info.getMethodName().equals("printFrame")){
                Object[] locals = info.getLocals();
                for (Object obj : locals) {
                    if (obj instanceof AtomicInteger val) {
                        val.set(114514);
                    }
                }
            }
            //System.err.println(info.getMethodName()+": "+ Arrays.toString(info.getMonitors())+", "+Arrays.toString(info.getLocals())+", "+Arrays.toString(info.getStack()));
        }
    }
//    private static final class C{
//        private static final String ALGO = "AES";
//        private static String KEY = "Pig2dummykeyPig2";
//
//        public C() {
//        }
//
//        public static void setTrueKey(String key) {
//            KEY = key;
//        }
//
//        public static String enc(String plainText) {
//            try {
//                Cipher cipher = Cipher.getInstance("AES");
//                SecretKeySpec key = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
//                cipher.init(1, key);
//                byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
//                return Base64.getEncoder().encodeToString(encrypted);
//            } catch (Exception var4) {
//                return "";
//            }
//        }
//
//        public static String dec(String cipherText) {
//            try {
//                Cipher cipher = Cipher.getInstance("AES");
//                SecretKeySpec key = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
//                cipher.init(2, key);
//                byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
//                return new String(decrypted, StandardCharsets.UTF_8);
//            } catch (Exception var4) {
//                return "err";
//            }
//        }
//
//        static {
//            try {
//                C.class.getMethod("setTrueKey").invoke((Object)null, "TgfVcFEc4zqZch+n");
//            } catch (Exception var1) {
//            }
//        }
//    }
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
        int len = newCP.length();
        MethodRefConstant call1 = newCP.findConstant((MethodRefConstant c) -> c.nameAndType.name.str.toString().equals("call1"), ConstantTag.Methodref);
        Utf8Constant call33 = newCP.findConstant((Utf8Constant c) -> c.str.toString().equals("call33"), ConstantTag.Utf8);
        Utf8Constant desc_V = newCP.findConstant((Utf8Constant c) -> c.str.toString().equals("()V"), ConstantTag.Utf8);
        newCP.name_and_type_at_put(len - 1, call33.which, desc_V.which);
        newCP.method_at_put(call1.which, call1.klass.which, len - 1);
        klass.setConstantPool(newCP);
        if (newCP.getCache() != null) {
            newCP.getCache().set_constant_pool(newCP);
            newCP.getCache().clearResolvedCacheEntry();
        }
    }

    public static void case1(){
        ClassHelperSpecial.defineClassBypassAgent("apphhzp.lib.hotspot.Test", Debugger.class,true,null);
        System.err.print("[");
        Test.print(5);
        System.err.println("]");
        Test test=new Test();
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
            java.lang.Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.gc();
        System.err.println("klass cnt:"+Klass.getAllKlasses().size());
        try {
            java.lang.Thread.sleep(1000);
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
//        System.err.println(x1);
        System.err.println(Test.doit());
        for (Method method:klass.getMethods()){
            ConstMethod constMethod=method.constMethod();
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
        //System.err.println(x1);
        for (Method method:klass.getMethods()){
            ConstMethod constMethod=method.constMethod();
            if (constMethod.getName().toString().equals("test")){
                System.err.println("Compiled code： 0x"+Long.toHexString(method.getFromCompiledEntry()));
                break;
            }
        }
        for (int i=0;i<3030030;i++){
            Test.doit();
        }
        System.err.println(Test.doit());
        //System.err.println(x1);
    }

//    public static void case5(){
//        for (CodeHeap heap: CodeCache.getCompiledHeaps()){
//            for (CodeBlob blob:heap){
//                if (blob instanceof NMethod nMethod){
//                    System.err.println(nMethod.method().getHolder().name()+"."+nMethod.method().getConstMethod().getName()+" "+
//                            nMethod.comp_level());
//                    RelocIterator itr=new RelocIterator(nMethod);
//                    while (itr.next()){
//                        if (itr.type()== RelocInfo.Type.VIRTUAL_CALL_TYPE){
//                            System.err.println("get!");
//                        }
//                    }
//                }
//            }
//        }
//    }

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
