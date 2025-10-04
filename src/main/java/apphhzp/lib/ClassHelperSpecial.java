package apphhzp.lib;


import apphhzp.lib.api.ApphhzpRunnable;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.NativeLibrary;
import apphhzp.lib.hotspot.oops.AccessFlags;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.utilities.Dictionary;
import apphhzp.lib.natives.Kernel32;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.management.ManagementFactory;
import java.lang.ref.Cleaner;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static apphhzp.lib.ClassOption.*;
import static apphhzp.lib.natives.Kernel32.PAGE_EXECUTE_READWRITE;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public final class ClassHelperSpecial {
    private static final Logger LOGGER;
    public static final MethodHandles.Lookup lookup;
    public static final Unsafe unsafe;
    public static final InternalUnsafe internalUnsafe;
    public static final boolean hasDebugger;
    private static final com.sun.jna.NativeLibrary currentNativeLibrary;
    private static final ClassesGetter classesGetter;
    private static final Class<?> nlImplClass;
    private static final MethodHandle defineClass0Method;
    private static final MethodHandle lookupConstructor;
    private static final MethodHandle findLoadedClassMethod;
    private static final MethodHandle findBuiltinLibMethod;
    private static final MethodHandle NL_IMPL_openMethod;
    private static final MethodHandle NL_IMPL_findMethod;
    private static final MethodHandle NL_IMLP_unloaderMethod;
    private static final MethodHandle nlImplConstructor;
    private static final MethodHandle findEntry0Method;
    private static final MethodHandle isSystemDomainLoaderMethod;
    private static final MethodHandle groupAddMethod;
    private static final MethodHandle start0Method;
    private static final MethodHandle addExportMethod;
    private static final MethodHandle addOpensMethod;
    private static final MethodHandle myRunnableConstructor;
    private static final VarHandle eetopVar;
    private static final VarHandle CL_librariesVar;
    private static final VarHandle loadLibraryOnlyIfPresentVar;
    private static final VarHandle NL_loadedLibraryNamesVar;
    private static final VarHandle NL_loaderVar;
    private static final VarHandle NL_librariesVar;
    private static final VarHandle NL_nativeLibraryContextVar;
    private static final VarHandle NL_IMPL_nameVar;
    private static final VarHandle NL_IMPL_fromClassVar;
    private static final VarHandle NL_isJNIVar;
    private static final VarHandle APP_LOADERVar;
    private static final VarHandle commonCleanerVar;
    private static final VarHandle groupVar;
    private static final VarHandle classDataVar;
    private static final VarHandle packagesVar;
    private static final VarHandle NPmoduleVar;
    private static final VarHandle reflectionDataVar;
    private static final VarHandle parentVar;
    public static final boolean isWindows;
    public static final boolean isLinux;
    public static final boolean is64BitJVM;
    public static final boolean isHotspotJVM;
    private static final IntByReference oldProtect;
    private static final Map<CodeSource, ProtectionDomain> pdCache;
    public static final Object JLA_INSTANCE;
    public static final Object NATIVE_LIBS;
    static {
        if (Runtime.version().version().get(0)!=17){
            throw new UnsupportedOperationException("Please use Java17!");
        }
        try {
            LOGGER = LogManager.getLogger(ClassHelperSpecial.class);
            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            isWindows = osName.contains("win");
            isLinux =osName.contains("nux") || osName.contains("nix");//||osName.contains("unix")
            {
                Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
                c.setAccessible(true);
                unsafe = c.newInstance();
                lookup = (MethodHandles.Lookup) unsafe.getObjectVolatile(unsafe.staticFieldBase(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")), unsafe.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")));
                internalUnsafe=new InternalUnsafe(lookup.findStaticVarHandle(Unsafe.class, "theInternalUnsafe", Class.forName("jdk.internal.misc.Unsafe")).get());
            }
            is64BitJVM = unsafe.addressSize() == 8;
            currentNativeLibrary=com.sun.jna.NativeLibrary.getProcess();
            boolean tmp2;
            try {
                currentNativeLibrary.getGlobalVariableAddress("gHotSpotVMTypes");
                tmp2=true;
            }catch (UnsatisfiedLinkError t){
                tmp2 = false;
            }
            isHotspotJVM=tmp2;
            hasDebugger= checkDebugger();

            classesGetter = (ClassesGetter) unsafe.allocateInstance(ClassesGetter.class);
            lookup.findVarHandle(SecurityManager.class,"initialized",boolean.class).set(classesGetter,true);
            
            Class<?> nl=Class.forName("jdk.internal.loader.NativeLibraries"),
                    abstractTraverser=Class.forName("java.lang.StackStreamFactory$AbstractStackWalker"),
                    frameBuffer=Class.forName("java.lang.StackStreamFactory$FrameBuffer");
            nlImplClass=Class.forName("jdk.internal.loader.NativeLibraries$NativeLibraryImpl");

            defineClass0Method = lookup.findStatic(ClassLoader.class, "defineClass0", MethodType.methodType(Class.class, ClassLoader.class, Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class, boolean.class, int.class, Object.class));
            lookupConstructor = lookup.findConstructor(MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class, Class.class, int.class));
            findLoadedClassMethod=lookup.findVirtual(ClassLoader.class,"findLoadedClass", MethodType.methodType(Class.class, String.class));
            findBuiltinLibMethod=lookup.findStatic(nl,"findBuiltinLib", MethodType.methodType(String.class,String.class));
            NL_IMPL_openMethod=lookup.findVirtual(nlImplClass,"open",MethodType.methodType(boolean.class));
            NL_IMPL_findMethod=lookup.findVirtual(nlImplClass,"find",MethodType.methodType(long.class,String.class));
            NL_IMLP_unloaderMethod=lookup.findVirtual(nlImplClass,"unloader",MethodType.methodType(Runnable.class));
            nlImplConstructor=lookup.findConstructor(nlImplClass,MethodType.methodType(void.class,Class.class,String.class,boolean.class,boolean.class));
            findEntry0Method=lookup.findStatic(nl,"findEntry0", MethodType.methodType(long.class,nlImplClass,String.class));
            isSystemDomainLoaderMethod=lookup.findStatic(Class.forName("jdk.internal.misc.VM"),"isSystemDomainLoader", MethodType.methodType(boolean.class,ClassLoader.class));
            groupAddMethod=lookup.findVirtual(ThreadGroup.class,"add", MethodType.methodType(void.class, Thread.class));
            start0Method=lookup.findVirtual(Thread.class,"start0",MethodType.methodType(void.class));
            addExportMethod=lookup.findVirtual(Module.class,"implAddExports", MethodType.methodType(void.class, String.class));
            addOpensMethod=lookup.findVirtual(Module.class,"implAddOpens", MethodType.methodType(void.class, String.class));

            eetopVar= lookup.findVarHandle(Thread.class,"eetop",long.class);
            CL_librariesVar =lookup.findVarHandle(ClassLoader.class,"libraries",nl);
            loadLibraryOnlyIfPresentVar=lookup.findStaticVarHandle(nl,"loadLibraryOnlyIfPresent",boolean.class);
            NL_loadedLibraryNamesVar =lookup.findStaticVarHandle(nl,"loadedLibraryNames",Set.class);
            NL_loaderVar=lookup.findVarHandle(nl,"loader", ClassLoader.class);
            NL_librariesVar=lookup.findVarHandle(nl,"libraries",Map.class);
            NL_nativeLibraryContextVar=lookup.findStaticVarHandle(nl,"nativeLibraryContext", Deque.class);
            NL_isJNIVar=lookup.findVarHandle(nl,"isJNI",boolean.class);
            NL_IMPL_nameVar=lookup.findVarHandle(nlImplClass,"name",String.class);
            NL_IMPL_fromClassVar=lookup.findVarHandle(nlImplClass,"fromClass",Class.class);
            APP_LOADERVar=lookup.findStaticVarHandle(Class.forName("jdk.internal.loader.ClassLoaders"),"APP_LOADER", Class.forName("jdk.internal.loader.ClassLoaders$AppClassLoader"));
            commonCleanerVar=lookup.findStaticVarHandle(Class.forName("jdk.internal.ref.CleanerFactory"),"commonCleaner", Cleaner.class);
            groupVar=lookup.findVarHandle(Thread.class,"group", ThreadGroup.class);
            classDataVar=lookup.findVarHandle(Class.class,"classData", Object.class);
            packagesVar=lookup.findVarHandle(ClassLoader.class,"packages", ConcurrentHashMap.class);
            NPmoduleVar=lookup.findVarHandle(Class.forName("java.lang.NamedPackage"),"module", Module.class);
            reflectionDataVar=lookup.findVarHandle(Class.class,"reflectionData", SoftReference.class);
            parentVar=lookup.findVarHandle(ClassLoader.class,"parent",ClassLoader.class);
            exportJavaBaseModule();
            openJavaBaseModule();
            fuckReflection();

            myRunnableConstructor=lookup.findConstructor(defineHiddenClass("apphhzp.lib.api.ApphhzpRunnableImpl", ClassHelperSpecial.class,true,null,STRONG,NESTMATE).lookupClass(),MethodType.methodType(void.class, ApphhzpRunnable.class));

            oldProtect = new IntByReference(1);
            pdCache = new HashMap<>();
            JLA_INSTANCE = lookup.findStaticVarHandle(Class.forName("jdk.internal.access.SharedSecrets"),"javaLangAccess",Class.forName("jdk.internal.access.JavaLangAccess")).get();
            NATIVE_LIBS=lookup.findStaticVarHandle(Class.forName("jdk.internal.loader.BootLoader"),"NATIVE_LIBS",Class.forName("jdk.internal.loader.NativeLibraries")).get();
            //defineLibClass();
        }catch (Throwable e){
            throwOriginalException(e);
            throw new ExceptionInInitializerError(e);
        }
    }
    private ClassHelperSpecial(){throw new UnsupportedOperationException();}

    private static boolean checkDebugger(){
        List<String> lis;
        try {
            Class<?> cls=Class.forName("sun.management.VMManagementImpl");
            Object obj=unsafe.allocateInstance(cls);
            lis= List.of((String[]) lookup.findVirtual(cls, "getVmArguments0", MethodType.methodType(String[].class)).invoke(obj));
        }catch (Throwable t){
            lis=ManagementFactory.getRuntimeMXBean().getInputArguments();
        }
        for (String s:lis){
            if (s.contains("-agentlib:jdwp=")){
                return true;
            }
        }
        return false;
    }

//    private static void fuckJava23()throws Throwable{
//        try {
//            Class.forName("sun.misc.Unsafe$MemoryAccessOption");
//
//            MethodHandles.Lookup lookup= (MethodHandles.Lookup) ReflectionFactory.getReflectionFactory()
//                    .newConstructorForSerialization(MethodHandles.Lookup.class, MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,Class.class,int.class))
//                    .newInstance(Object.class,null,-1);
//            Class<?> internalUnsafeClass=Class.forName("jdk.internal.misc.Unsafe"),
//                    accessOptions=Class.forName("sun.misc.Unsafe$MemoryAccessOption");
//            MethodHandle fieldOffset=lookup.findVirtual(internalUnsafeClass,"staticFieldOffset0",MethodType.methodType(long.class, Field.class))
//                    ,fieldBase=lookup.findVirtual(internalUnsafeClass,"staticFieldBase0",MethodType.methodType(Object.class, Field.class))
//                    ,putReference=lookup.findVirtual(internalUnsafeClass,"putReference",MethodType.methodType(void.class,Object.class,long.class,Object.class))
//                    ,ensure=lookup.findVirtual(internalUnsafeClass,"ensureClassInitialized0",MethodType.methodType(void.class,Class.class));
//            Object internalUnsafe=lookup.findConstructor(internalUnsafeClass,MethodType.methodType(void.class)).invoke();
//            ensure.invoke(internalUnsafe,Class.forName("sun.misc.Unsafe",false, ClassHelperSpecial.class.getClassLoader()));
//            Field field=Unsafe.class.getDeclaredField("MEMORY_ACCESS_OPTION");
//            putReference.invoke(internalUnsafe,fieldBase.invoke(internalUnsafe,field),(long)fieldOffset.invoke(internalUnsafe,field),
//                    lookup.findStaticVarHandle(accessOptions,"ALLOW",accessOptions).get());
//        }catch (ClassNotFoundException ignored){}
//    }

    public static void exportJavaBaseModule() {
        Module module = String.class.getModule();
        for (String name : module.getPackages()) {
            addExportImpl(module,name);
        }
    }

    public static void openJavaBaseModule(){
        Module module = String.class.getModule();
        for (String name : module.getPackages()) {
            addOpensImpl(module,name);
        }
    }

    public static Set<Module> getAllModulesOfCL(ClassLoader cl) {
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String,Object> map= (ConcurrentHashMap<String, Object>) packagesVar.get(cl);
        Set<Module> re= new HashSet<>();
        for (Map.Entry<String,Object> entry : map.entrySet()) {
            re.add((Module) NPmoduleVar.get(entry.getValue()));
        }
        return re;
    }

    public static void exportAllModules(ClassLoader cl) {
        try {
            for (Module module:getAllModulesOfCL(cl)){
                for (String name:module.getPackages()) {
                    addExportImpl(module,name);
                }
            }
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static void openAllModules(ClassLoader cl) {
        try {
            for (Module module:getAllModulesOfCL(cl)){
                for (String name:module.getPackages()) {
                    addOpensImpl(module,name);
                }
            }
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static void clearReflectionData(Class<?> klass){
        reflectionDataVar.set(klass,null);
    }

    private static void fuckReflection()throws Throwable{
        Class<?> cls=Class.forName("jdk.internal.reflect.Reflection");
        VarHandle varHandle=lookup.findStaticVarHandle(cls,"fieldFilterMap", Map.class);
        for (Class<?> klass:((Map<Class<?>, Set<String>>)varHandle.get()).keySet()){
            clearReflectionData(klass);
        }
        Map<Class<?>, Set<String>> emptyMap= new HashMap<>() {
            @Override
            public Set<String> get(Object key) {
                return null;
            }

            @Override
            public Set<String> put(Class<?> key, Set<String> value) {
                return null;
            }
        };
        varHandle.set(emptyMap);
        varHandle = lookup.findStaticVarHandle(cls,"methodFilterMap", Map.class);
        for (Class<?> klass:((Map<Class<?>, Set<String>>)varHandle.get()).keySet()){
            clearReflectionData(klass);
        }
        varHandle.set(emptyMap);
    }

    public static void addExportImpl(Module current, String pkg) {
        try{
            addExportMethod.invoke(current, pkg);
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static void addOpensImpl(Module current, String pkg) {
        try{
            addOpensMethod.invoke(current,pkg);
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static Class<?> defineClass(String name, byte[] bytecodes, ClassLoader loader) {
        return defineClass(name,bytecodes,loader,null);
    }

    public static Class<?> defineClass(String name, byte[] bytecodes, ClassLoader loader,ProtectionDomain pd) {
        return internalUnsafe.defineClass(name,bytecodes,0,bytecodes.length,loader,pd);
    }

    public static <T> T allocateInstance(Class<T> type){
        try {
            //noinspection unchecked
            return (T) unsafe.allocateInstance(type);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static ProtectionDomain createProtectionDomain(CodeSource codeSource, ClassLoader cl) {
        return pdCache.computeIfAbsent(codeSource, cs -> {
            Permissions perms = new Permissions();
            perms.add(new AllPermission());
            return new ProtectionDomain(codeSource, perms, cl, null);
        });
    }

    public static MethodHandles.Lookup defineHiddenClass(String name, Class<?> lookupClass, boolean initialize, ProtectionDomain pd, ClassOption... options) {
        try {
            InputStream is = lookupClass.getResourceAsStream("/" + name.replace('.', '/') + ".class");
            byte[] dat = new byte[is.available()];
            is.read(dat);
            is.close();
            dat=OnlyInDefineClassHelper.handle(dat,name);
            return defineHiddenClass(dat, name, initialize, lookupClass, lookupClass.getClassLoader(), pd, options);
        } catch (NullPointerException e) {
            throw new RuntimeException("Could not find class in jar: " + name, e);
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static MethodHandles.Lookup defineHiddenClassWithClassData(String name, Object data, Class<?> lookupClass, boolean initialize, ProtectionDomain pd, ClassOption... options) {
        try {
            InputStream is = lookupClass.getResourceAsStream("/" + name.replace('.', '/') + ".class");
            byte[] dat = new byte[is.available()];
            is.read(dat);
            is.close();
            return defineHiddenClassWithClassData(dat, name, data, initialize, lookupClass, lookupClass.getClassLoader(), pd, options);
        } catch (NullPointerException e) {
            throw new RuntimeException("Could not find class in jar: " + name, e);
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static MethodHandles.Lookup defineHiddenClass(byte[] bytes, String name, boolean initialize, Class<?> lookupClass, ClassLoader loader, ProtectionDomain pd, ClassOption... options) {
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(options);
        int flags = 2 | optionsToFlag(Set.of(options));
        if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
            flags |= 8;
        }
        try {
            return (MethodHandles.Lookup) lookupConstructor.invoke(defineClass0Method.invoke( loader, lookupClass, name, bytes,0,bytes.length, pd, initialize, flags, null), null, -1);
        } catch (Throwable t) {
            throw new RuntimeException("Could not define a hidden class:" + name, t);
        }
    }

    public static MethodHandles.Lookup defineHiddenClassWithClassData(byte[] bytes, String name, Object classData, boolean initialize, Class<?> lookupClass, ClassLoader loader, ProtectionDomain pd, ClassOption... options) {
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(classData);
        Objects.requireNonNull(options);
        int flags = 2 | optionsToFlag(Set.of(options));
        if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
            flags |= 8;
        }
        try {
            return (MethodHandles.Lookup) lookupConstructor.invoke(defineClass0Method.invoke(loader, lookupClass, name, bytes,0,bytes.length, pd, initialize, flags, classData), null, -1);
        } catch (Throwable t) {
            throw new RuntimeException("Could not define a hidden class:" + name, t);
        }
    }

    public static boolean defineClassBypassAgent(String name, Class<?> lookupClass, boolean initialize, ProtectionDomain pd){
        if (!isHotspotJVM){
            return false;
        }
        if(findLoadedClass(lookupClass.getClassLoader(), name)!=null) {
            return false;
        }
        Class<?> clazz;
        try {
            clazz=defineHiddenClass(name,lookupClass,initialize,pd,ClassOption.STRONG, ClassOption.NESTMATE).lookupClass();
        }catch (VerifyError error){
            return false;
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
        ClassLoaderData cld=ClassLoaderData.as(lookupClass.getClassLoader());
        Dictionary dict=cld.getDictionary();
        if (dict != null) {
            InstanceKlass klass= Klass.asKlass(clazz).asInstanceKlass();
            Symbol symbol=Symbol.lookupOrCreate(name.replace('.','/'));
            symbol.incrementRefCount();
            klass.setName(symbol);
            klass.setAccessFlags(klass.getAccessFlags().flags&~AccessFlags.JVM_ACC_IS_HIDDEN_CLASS);
            dict.addKlass(dict.computeHash(klass.name()),klass.name(),klass);
            return true;
        }else {
            return false;
        }
    }

    public static void createHiddenThread(ApphhzpRunnable task,String name){
        try {
            Thread thread= new Thread(null, (Runnable) myRunnableConstructor.invoke(task),name);
            groupAddMethod.invoke(groupVar.get(thread),thread);
            start0Method.invoke(thread);
            if (!hasDebugger){
                eetopVar.set(thread,0L);
            }
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static NativeLibrary load(Class<?> caller, String filename){
        File file = new File(filename);
        if (!file.isAbsolute()) {
            throw new UnsatisfiedLinkError("Expecting an absolute path of the library: " + filename);
        }
        ClassLoader loader = caller == null ? null : caller.getClassLoader();
        Object libs = loader != null ? CL_librariesVar.get(loader): NATIVE_LIBS;
        NativeLibrary nl = loadLibrary(libs,caller, file);
        if (nl != null) {
            return nl;
        }
        throw new UnsatisfiedLinkError("Can't load library: " + file);
    }

    private static NativeLibrary loadLibrary(Object nl,Class<?> fromClass, File file) {
        try {
            String name = (String) findBuiltinLibMethod.invoke(file.getName());
            boolean isBuiltin = name != null;
            if (!isBuiltin) {
                name = AccessController.doPrivileged((PrivilegedAction<String>) () -> {
                    try {
                        if ((boolean)loadLibraryOnlyIfPresentVar.get() && !file.exists()) {
                            return null;
                        }
                        return file.getCanonicalPath();
                    } catch (IOException e) {
                        return null;
                    }
                });
                if (name == null) {
                    return null;
                }
            }
            return loadLibrary(nl,fromClass, name, isBuiltin);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    private static NativeLibrary loadLibrary(Object nl,Class<?> fromClass, String name, boolean isBuiltin) {
        ClassLoader loader = fromClass == null ? null : fromClass.getClassLoader();
        if (NL_loaderVar.get(nl) != loader) {
            throw new InternalError(fromClass.getName() + " not allowed to load library");
        }
        final Set<String> loadedLibraryNames=(Set<String>) NL_loadedLibraryNamesVar.get();
        try {
            synchronized (loadedLibraryNames) {
                Object cached=((Map) NL_librariesVar.get(nl)).get(name);

                if (cached != null) {
                    return createFromNLIMPL(cached);
                }

                //Bypass this check!
                /*if (loadedLibraryNames.contains(name)) {
                    throw new UnsatisfiedLinkError("Native Library " + name +
                            " already loaded in another classloader");
                }*/

                for (Object lib :(Deque)NL_nativeLibraryContextVar.get()) {
                    if (name.equals(NL_IMPL_nameVar.get(lib))) {
                        if (loader == ((Class<?>)NL_IMPL_fromClassVar.get(lib)).getClassLoader()) {
                            return createFromNLIMPL(lib);
                        } else {
                            throw new UnsatisfiedLinkError("Native Library " +
                                    name + " is being loaded in another classloader");
                        }
                    }
                }
                Object lib = nlImplConstructor.invoke(fromClass, name, isBuiltin, NL_isJNIVar.get(nl));
                ((Deque)NL_nativeLibraryContextVar.get()).push(lib);
                try {
                    if (!(boolean)NL_IMPL_openMethod.invoke(lib) ) {
                        return null;
                    }
                    boolean autoUnload = (boolean)NL_isJNIVar.get(nl) && !((boolean)isSystemDomainLoaderMethod.invoke(loader))
                            && loader != APP_LOADERVar.get();
                    if (autoUnload) {
                        ((Cleaner)commonCleanerVar.get()).register(loader,(Runnable)NL_IMLP_unloaderMethod.invoke(lib));
                    }
                } finally {
                    ((Deque)NL_nativeLibraryContextVar.get()).pop();
                }
                // register the loaded native library
                loadedLibraryNames.add(name);
                ((Map)NL_librariesVar.get(nl)).put(name, lib);
                return createFromNLIMPL(lib);
            }
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    private static NativeLibrary createFromNLIMPL(Object obj) {
        try {
            if (obj.getClass()!=nlImplClass){
                throw new IllegalArgumentException(obj.toString());
            }
            return new NativeLibrary() {
                @Override
                public String name() {
                    return (String) NL_IMPL_nameVar.get(obj);
                }

                @Override
                public long lookup(String entry) {
                    try {
                        return (long) findEntry0Method.invoke(obj,entry);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public boolean open() {
                    try {
                        return (boolean) NL_IMPL_openMethod.invoke(obj);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    public static long lookupFromLoadedLibrary(String entry) {
        try {
            return Pointer.nativeValue(currentNativeLibrary.getGlobalVariableAddress(entry));
        }catch (Throwable t){
            return 0;
        }
    }
    

    public static Class<?>[] getClassContext(){
        Class<?>[] cc=classesGetter.getClassContext(),re=new Class<?>[cc.length-2];
        System.arraycopy(cc,2,re,0,cc.length-2);
        return re;
    }

    private static class ClassesGetter extends SecurityManager {
        @Override
        public Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }

    @SuppressWarnings("unchecked")
    public static  <T> T getClassData(Class<?> klass){
        return (T) classDataVar.get(klass);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void throwOriginalException(Throwable throwable) throws E {
        throw (E) throwable;
    }

    public static boolean getBoolFromResource(String name,Class<?> lookup){
        try {
            InputStream is=lookup.getResourceAsStream(name);
            if (is == null){
                LOGGER.error("File({}) not found", name);
                return false;
            }
            byte[] dat = new byte[is.available()];
            is.read(dat);
            is.close();
            String s = new String(dat);
            return !s.contains("false") && s.contains("true");
        } catch (Throwable t){
            LOGGER.catching(t);
            return false;
        }
    }

//    public static void runShellcode(byte[] payload) {
//        apphhzp.lib.hotspot.oops.method.Method executor=Klass.asKlass(ShellcodeExecutor.class).asInstanceKlass().getMethod("run","()V");
//        long addr=unsafe.allocateMemory(payload.length);
//        for (int i=0;i<payload.length;++i){
//            unsafe.putByte(ad+i,payload[i]);
//        }
//        //executor.setFromCompiledEntry(addr);
//        //executor.setFromCompiledEntry(0L);
//        //unsafe.freeMemory(addr);
////        try {
////            enqueueMethod.invoke(-1,payload,"enqueue","enqueue");
////        } catch (Throwable e) {
////            throw new RuntimeException(e);
////        }
//    }

    private static final WeakHashMap<byte[], String> classNameCache = new WeakHashMap<>();

    public static String getClassNameWithCaches(byte[] bytes) {
        return classNameCache.computeIfAbsent(bytes, ClassHelperSpecial::getClassName);
    }

    public static String getClassName(byte[] bytes) {
        int int1 = 0, offset = 10;
        int[] cpInfoOffsets;
        int int2, cpInfoSize, currentCpInfoIndex;
        byte val;
        for (int2 = (bytes[8] & 0xFF) << 8 | bytes[9] & 0xFF, cpInfoOffsets = new int[int2], currentCpInfoIndex = 1;
             currentCpInfoIndex < int2; offset += cpInfoSize) {
            cpInfoOffsets[currentCpInfoIndex++] = offset + 1;
            val = bytes[offset];
            if (val == 9 || val == 10 || val == 11 || val == 3 || val == 4 || val == 12 || val == 17 || val == 18) {
                cpInfoSize = 5;
            } else if (val == 5 || val == 6) {
                cpInfoSize = 9;
                currentCpInfoIndex++;
            } else if (val == 1) {
                cpInfoSize = 3 + ((bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 2] & 0xFF);
                if (cpInfoSize > int1) {
                    int1 = cpInfoSize;
                }
            } else if (val == 15) {
                cpInfoSize = 4;
            } else if (val == 7 || val == 8 || val == 16 || val == 20 || val == 19) {
                cpInfoSize = 3;
            } else {
                throw new IllegalArgumentException();
            }
        }
        char[] charBuffer = new char[int1];
        int1 = (bytes[offset + 2] & 0xFF) << 8 | bytes[offset + 3] & 0xFF;
        offset = cpInfoOffsets[int1];
        if (bytes[offset - 1] == 7) {
            int1 = (bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF;
            if (int1 == 0) {
                throw new ClassFormatError();
            }
            offset = cpInfoOffsets[int1];
            int2 = offset + 2 + ((bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF);
            offset += 2;
            int1 = 0;
            while (offset < int2) {
                val = bytes[offset++];
                if ((val & 0x80) == 0) {
                    charBuffer[int1++] = (char) (val & 0x7F);
                } else if ((val & 0xE0) == 0xC0) {
                    charBuffer[int1++] = (char) (((val & 0x1F) << 6) + (bytes[offset++] & 0x3F));
                } else {
                    charBuffer[int1++] = (char) (((val & 0xF) << 12) + ((bytes[offset++] & 0x3F) << 6) + (bytes[offset++] & 0x3F));
                }
            }
            return new String(charBuffer, 0, int1);
        }
        throw new ClassFormatError("this_class item: #" + int1 + " not a CONSTANT_Class_info");
    }

    public static String getSuperName(byte[] bytes) {
        int index = 1;
        int offset = 10;
        int maxStringLength = 0;
        int currentByte;
        int val = (bytes[8] & 0xFF) << 8 | bytes[9] & 0xFF;
        int[] cpInfoOffsets = new int[val];
        while (index < val) {
            cpInfoOffsets[index++] = offset + 1;
            int cpInfoSize;
            currentByte = bytes[offset];
            if (currentByte == 9 || currentByte == 10 || currentByte == 11 || currentByte == 12 || currentByte == 3 || currentByte == 4 || currentByte == 18 || currentByte == 17) {
                cpInfoSize = 5;
            } else if (currentByte == 5 || currentByte == 6) {
                cpInfoSize = 9;
                index++;
            } else if (currentByte == 1) {
                cpInfoSize = 3 + ((bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 2] & 0xFF);
                if (cpInfoSize > maxStringLength) {
                    maxStringLength = cpInfoSize;
                }
            } else if (currentByte == 15) {
                cpInfoSize = 4;
            } else if (currentByte == 7 || currentByte == 8 || currentByte == 16 || currentByte == 20 || currentByte == 19) {
                cpInfoSize = 3;
            } else {
                throw new IllegalArgumentException();
            }
            offset += cpInfoSize;
        }
        char[] charBuffer = new char[maxStringLength];
        offset=cpInfoOffsets[(bytes[offset + 4] & 0xFF) << 8 | bytes[offset + 5] & 0xFF];
        val = (bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF;
        if (offset == 0 || val == 0) {
            throw new ClassFormatError();
        }
        offset = cpInfoOffsets[val];
        val = offset + 2 + ((bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF);
        offset += 2;
        index = 0;
        while (offset < val) {
            currentByte = bytes[offset++];
            if ((currentByte & 0x80) == 0) {
                charBuffer[index++] = (char) (currentByte & 0x7F);
            } else if ((currentByte & 0xE0) == 0xC0) {
                charBuffer[index++] = (char) (((currentByte & 0x1F) << 6) + (bytes[offset++] & 0x3F));
            } else {
                charBuffer[index++] = (char) (((currentByte & 0xF) << 12) + ((bytes[offset++] & 0x3F) << 6) + (bytes[offset++] & 0x3F));
            }
        }
        return new String(charBuffer, 0, index);
    }

    public static String[] getInterfaceNames(byte[] bytes) {
        int cnt = (bytes[8] & 0xFF) << 8 | bytes[9] & 0xFF;
        int[] cpInfoOffsets;
        int index = 1;
        int offset = 10;
        int maxStringLength = 0;
        int currentByte;
        cpInfoOffsets = new int[cnt];
        while (index < cnt) {
            cpInfoOffsets[index++] = offset + 1;
            int cpInfoSize;
            currentByte = bytes[offset];
            if (currentByte == 9 || currentByte == 10 || currentByte == 11 || currentByte == 12 || currentByte == 3 || currentByte == 4 || currentByte == 18 || currentByte == 17) {
                cpInfoSize = 5;
            } else if (currentByte == 5 || currentByte == 6) {
                cpInfoSize = 9;
                index++;
            } else if (currentByte == 1) {
                cpInfoSize = 3 + ((bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 2] & 0xFF);
                if (cpInfoSize > maxStringLength) {
                    maxStringLength = cpInfoSize;
                }
            } else if (currentByte == 15) {
                cpInfoSize = 4;
            } else if (currentByte == 7 || currentByte == 8 || currentByte == 16 || currentByte == 20 || currentByte == 19) {
                cpInfoSize = 3;
            } else {
                throw new IllegalArgumentException();
            }
            offset += cpInfoSize;
        }
        index = offset + 6;
        cnt = (bytes[index] & 0xFF) << 8 | bytes[index + 1] & 0xFF;
        String[] interfaces = new String[cnt];
        if (cnt > 0) {
            char[] charBuffer = new char[maxStringLength];
            for (int i = 0; i < cnt; ++i) {
                index += 2;
                offset = cpInfoOffsets[(bytes[index] & 0xFF) << 8 | bytes[index + 1] & 0xFF];
                int constantPoolEntryIndex = (bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF;
                if (offset == 0 || constantPoolEntryIndex == 0) {
                    throw new ClassFormatError();
                }
                offset = cpInfoOffsets[constantPoolEntryIndex];
                int endOffset = offset + 2 + ((bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF);
                int strLength = 0;
                offset += 2;
                while (offset < endOffset) {
                    currentByte = bytes[offset++];
                    if ((currentByte & 0x80) == 0) {
                        charBuffer[strLength++] = (char) (currentByte & 0x7F);
                    } else if ((currentByte & 0xE0) == 0xC0) {
                        charBuffer[strLength++] = (char) (((currentByte & 0x1F) << 6) + (bytes[offset++] & 0x3F));
                    } else {
                        charBuffer[strLength++]= (char)
                                        (((currentByte & 0xF) << 12)
                                                + ((bytes[offset++] & 0x3F) << 6)
                                                + (bytes[offset++] & 0x3F));
                    }
                }
                interfaces[i] = new String(charBuffer, 0, strLength);
            }
        }
        return interfaces;
    }

    public static int addReturn0ToCFunction(long... func_addr){
        int cnt=0;
        if (PlatformInfo.isX86()){
            for (long func:func_addr){
                if (func!=0L){
                    if (Kernel32.INSTANCE.VirtualProtect(func,3,PAGE_EXECUTE_READWRITE,oldProtect.getPointer())) {
                        unsafe.putByte(func, (byte) 0x33);
                        unsafe.putByte(func + 1L, (byte) 0xc0);
                        unsafe.putByte(func + 2L, (byte) 0xc3);
                        ++cnt;
                    }
                }
            }
        }
        return cnt;
    }

    public static int addReturnToCFunction(long... func_addr){
        int cnt=0;
        if (PlatformInfo.isX86()){
            for (long func:func_addr){
                if (func!=0L){
                    Kernel32.INSTANCE.VirtualProtect(func,1,PAGE_EXECUTE_READWRITE,oldProtect.getPointer());
                    unsafe.putByte(func,(byte) 0xc3);
                }
            }
        }
        return cnt;
    }

    public static VarHandle findVarHandle(Class<?> recv, String mcpName,String srgName, Class<?> type){
        try {
            try {
                return lookup.findVarHandle(recv,mcpName,type);
            }catch (NoSuchFieldException e){
                return lookup.findVarHandle(recv,srgName,type);
            }
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static VarHandle findStaticVarHandle(Class<?> recv, String mcpName,String srgName, Class<?> type){
        try {
            try {
                return lookup.findStaticVarHandle(recv,mcpName,type);
            }catch (NoSuchFieldException e){
                return lookup.findStaticVarHandle(recv,srgName,type);
            }
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static MethodHandle findVirtual(Class<?> refc, String mcpName,String srgName, MethodType type){
        try {
            try {
                return lookup.findVirtual(refc,mcpName,type);
            }catch (NoSuchMethodException e){
                return lookup.findVirtual(refc,srgName,type);
            }
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static MethodHandle findStatic(Class<?> refc, String mcpName,String srgName, MethodType type){
        try {
            try {
                return lookup.findStatic(refc,mcpName,type);
            }catch (NoSuchMethodException e){
                return lookup.findStatic(refc,srgName,type);
            }
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T forceGetField(Field field, Object obj) {
        if (Modifier.isStatic(field.getModifiers())) {
            if(field.getType()==int.class){
                return (T)Integer.valueOf(unsafe.getIntVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            }else if (field.getType()==long.class){
                return (T)Long.valueOf(unsafe.getLongVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            }else if (field.getType()==boolean.class){
                return (T)Boolean.valueOf(unsafe.getBooleanVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            }else if (field.getType()==char.class){
                return (T)Character.valueOf(unsafe.getCharVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            }else if (field.getType()==short.class){
                return (T)Short.valueOf(unsafe.getShortVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            } else if (field.getType()==byte.class){
                return (T)Byte.valueOf(unsafe.getByteVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            }else if (field.getType()==float.class){
                return (T)Float.valueOf(unsafe.getFloatVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            }else if (field.getType()==double.class){
                return (T)Double.valueOf(unsafe.getDoubleVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field)));
            }
            return (T) unsafe.getObjectVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field));
        }
        if(field.getType()==int.class){
            return (T)Integer.valueOf(unsafe.getIntVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        }else if (field.getType()==long.class){
            return (T)Long.valueOf(unsafe.getLongVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        }else if (field.getType()==boolean.class){
            return (T)Boolean.valueOf(unsafe.getBooleanVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        }else if (field.getType()==char.class){
            return (T)Character.valueOf(unsafe.getCharVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        }else if (field.getType()==short.class){
            return (T)Short.valueOf(unsafe.getShortVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        } else if (field.getType()==byte.class){
            return (T)Byte.valueOf(unsafe.getByteVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        }else if (field.getType()==float.class){
            return (T)Float.valueOf(unsafe.getFloatVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        }else if (field.getType()==double.class){
            return (T)Double.valueOf(unsafe.getDoubleVolatile(obj, internalUnsafe.objectFieldOffset(field)));
        }
        return (T) unsafe.getObjectVolatile(obj, internalUnsafe.objectFieldOffset(field));
    }

    public static void forceSetField(Object o, Field field, Object x) {
        if (Modifier.isStatic(field.getModifiers())){
            if(field.getType()==int.class){
                unsafe.putIntVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Integer) x);
            }else if (field.getType()==long.class){
                unsafe.putLongVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Long) x);
            }else if (field.getType()==short.class){
                unsafe.putShortVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Short) x);
            }else if (field.getType()==byte.class){
                unsafe.putByteVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Byte) x);
            }else if (field.getType()==float.class){
                unsafe.putFloatVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Float) x);
            }else if (field.getType()==double.class){
                unsafe.putDoubleVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Double) x);
            }else if (field.getType()==char.class){
                unsafe.putCharVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Character) x);
            }else if (field.getType()==boolean.class){
                unsafe.putBoolean(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field), (Boolean) x);
            } else {
                unsafe.putObjectVolatile(internalUnsafe.staticFieldBase(field), internalUnsafe.staticFieldOffset(field),x);
            }
        } else {
            if (field.getType()==int.class){
                unsafe.putIntVolatile(o, internalUnsafe.objectFieldOffset(field), (Integer) x);
            }else if (field.getType()==long.class){
                unsafe.putLongVolatile(o, internalUnsafe.objectFieldOffset(field), (Long) x);
            }else if (field.getType()==short.class){
                unsafe.putShortVolatile(o, internalUnsafe.objectFieldOffset(field), (Short) x);
            }else if (field.getType()==byte.class){
                unsafe.putByteVolatile(o, internalUnsafe.objectFieldOffset(field), (Byte) x);
            }else if (field.getType()==float.class){
                unsafe.putFloatVolatile(o, internalUnsafe.objectFieldOffset(field), (Float) x);
            }else if (field.getType()==double.class){
                unsafe.putDoubleVolatile(o, internalUnsafe.objectFieldOffset(field), (Double) x);
            }else if (field.getType()==char.class){
                unsafe.putCharVolatile(o, internalUnsafe.objectFieldOffset(field), (Character) x);
            }else if (field.getType()==boolean.class){
                unsafe.putBooleanVolatile(o, internalUnsafe.objectFieldOffset(field), (Boolean) x);
            } else {
                unsafe.putObjectVolatile(o, internalUnsafe.objectFieldOffset(field), x);
            }
        }
    }

    public static void setClassPointer(Object object, Class<?> targetClass) {
        if (object == null)
            throw new NullPointerException("object==null");
        if (targetClass == null)
            throw new NullPointerException("targetClass==null");
        try {
            lookup.ensureInitialized(targetClass);
            if (JVM.usingCompressedClassPointers) {
                int klass_ptr = unsafe.getIntVolatile(unsafe.allocateInstance(targetClass), unsafe.addressSize());
                unsafe.putIntVolatile(object, unsafe.addressSize(), klass_ptr);
            } else {
                long klass_ptr = unsafe.getLongVolatile(unsafe.allocateInstance(targetClass), unsafe.addressSize());
                unsafe.putLongVolatile(object, unsafe.addressSize(), klass_ptr);
            }
        } catch (InstantiationException | IllegalAccessException ex) {
            throwOriginalException(ex);
            throw new RuntimeException(ex);
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T getOuterInstance(Object obj, Class<T> fa) {
        try {
            return (T) lookup.findVarHandle(obj.getClass(), "this$0", fa).get(obj);
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not get OuterInstance:", throwable);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOuterInstance(Object obj, Class<T> fa,String srgName) {
        try {
            return (T) lookup.findVarHandle(obj.getClass(), srgName, fa).get(obj);
        }catch (Throwable t){
            try {
                return getOuterInstance(obj,fa);
            }catch (Throwable t2){
                throw new RuntimeException("Could not get OuterInstance:",t2);
            }
        }
    }
    public static String getJarPath(Class<?> clazz) {
        return getJarPath(clazz,false);
    }

    public static String getJarPath(Class<?> clazz,boolean needFix) {
        try {
            String file ;
            if (needFix&&JarPathPatch.isLoadedByMCL(clazz)){
                file=JarPathPatch.getJarPath(clazz);
            }else {
                file=clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
            }
            if (!file.isEmpty()) {
                if (file.startsWith("union:"))
                    file = file.substring(6);
                if (isWindows){
                    if (file.startsWith("/"))
                        file = file.substring(1);
                }
                file = file.substring(0, file.lastIndexOf(".jar")+4);
                if (isWindows){
                    file = file.replace('/', File.separatorChar);
                }
            }
            return URLDecoder.decode(file, StandardCharsets.UTF_8);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static Class<?> findLoadedClass(ClassLoader loader,String name){
        try {
            return (Class<?>)findLoadedClassMethod.invoke(loader,name);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
    public static void defineClassesFromJar(Class<?> caller, Predicate<String> predicate){
        defineClassesFromJar(caller,caller.getClassLoader(),predicate);
    }

    public static void defineClassesFromJar(Class<?> caller,ClassLoader loader, Predicate<String> predicate){
        try {
            //LOGGER.error("DO NOT USE THIS METHOD! Do not support character '!' or '+'");
            //StringBuilder builder=new StringBuilder();
            JarFile jarFile = new JarFile(getJarPath(caller,true));
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")&&predicate.test(name)) {
                    //builder.append(",\"/").append(name).append("\"");
                    name = name.replace('/', '.').substring(0, name.length() - 6);
                    if (findLoadedClass(loader, name) == null) {
                        InputStream in = jarFile.getInputStream(entry);
                        byte[] dat = new byte[in.available()];
                        in.read(dat);
                        in.close();
                        defineClass(name,dat,loader);
                    }
                }
            }
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static int getPid() {
        return (int) ProcessHandle.current().pid();//Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }
    public static int version(){
        return 10;
    }
    public static int minorVersion(){
        return 0;
    }
}
