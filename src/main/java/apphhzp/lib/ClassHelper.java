package apphhzp.lib;


import apphhzp.lib.api.ObjectInstrumentation;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.JVMUtil;
import apphhzp.lib.hotspot.NativeLibrary;
import apphhzp.lib.hotspot.oops.AccessFlags;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.utilities.Dictionary;
import apphhzp.lib.natives.NativeUtil;
import com.sun.jna.ptr.IntByReference;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.management.ManagementFactory;
import java.lang.ref.Cleaner;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static apphhzp.lib.ClassOption.optionsToFlag;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public final class ClassHelper {
    //private static final Logger LOGGER;
    public static final MethodHandles.Lookup lookup;
    public static final Unsafe unsafe;
    public static final Object internalUnsafe;
    private static final Class<?> internalClass;
    private static final Class<?> nlImplClass;
    private static final MethodHandle staticFieldBaseMethod;
    private static final MethodHandle staticFieldOffsetMethod;
    private static final MethodHandle objectFieldOffsetMethod;
    private static final MethodHandle defineClassMethod;
    private static final MethodHandle JLA_defineClassMethod;
    private static final MethodHandle lookupConstructor;
    private static final MethodHandle compareAndSetByteMethod;
    private static final MethodHandle findLoadedClassMethod;
    private static final MethodHandle findBuiltinLibMethod;
    private static final MethodHandle NL_IMPL_openMethod;
    private static final MethodHandle NL_IMPL_findMethod;
    private static final MethodHandle NL_IMLP_unloaderMethod;
    private static final MethodHandle nlImplConstructor;
    private static final MethodHandle findEntry0Method;
    private static final MethodHandle isSystemDomainLoaderMethod;
    private static final MethodHandle enqueueMethod;
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
    @Nullable
    public static final Instrumentation instImpl;
    @Nullable
    public static final ObjectInstrumentation objectInstImpl;
    public static final boolean isWindows;
    public static final boolean isLinux;
    public static final boolean is64BitJVM;
    public static final boolean isHotspotJVM;
    private static final IntByReference oldProtect;
    private static final Map<CodeSource, ProtectionDomain> pdCache;
    public static final Object JLA_INSTANCE;
    public static final Object NATIVE_LIBS;
    static {
        try {
            //LOGGER = LogManager.getLogger(ClassHelper.class);

            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            isWindows = osName.contains("win");
            isLinux = osName.contains("nux") || osName.contains("nix");//||osName.contains("unix")
            fuckJava23();
            Unsafe tmp;
            Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
            c.setAccessible(true);
            tmp = c.newInstance();
            lookup = (MethodHandles.Lookup) tmp.getObjectVolatile(tmp.staticFieldBase(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")), tmp.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")));
            internalClass = Class.forName("jdk.internal.misc.Unsafe");
            unsafe = createUnsafe();
            is64BitJVM = unsafe.addressSize() == 8;
            boolean tmp2=false;
            try {
                tmp2 = JVMUtil.findJvm().find("gHotSpotVMTypes")!=0;
            }catch (Throwable t){
                tmp2 = false;
            }
            isHotspotJVM=tmp2;
            Class<?> nl=Class.forName("jdk.internal.loader.NativeLibraries");
            nlImplClass=Class.forName("jdk.internal.loader.NativeLibraries$NativeLibraryImpl");
            internalUnsafe = lookup.findStaticVarHandle(Unsafe.class, "theInternalUnsafe", internalClass).get();
            staticFieldBaseMethod = lookup.findVirtual(internalClass, "staticFieldBase", MethodType.methodType(Object.class, Field.class));
            staticFieldOffsetMethod = lookup.findVirtual(internalClass, "staticFieldOffset", MethodType.methodType(long.class, Field.class));
            objectFieldOffsetMethod = lookup.findVirtual(internalClass, "objectFieldOffset", MethodType.methodType(long.class, Field.class));
            defineClassMethod = lookup.findVirtual(internalClass, "defineClass", MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class));
            compareAndSetByteMethod = lookup.findVirtual(internalClass, "compareAndSetByte", MethodType.methodType(boolean.class, Object.class, long.class, byte.class, byte.class));
            JLA_defineClassMethod = lookup.findVirtual(Class.forName("jdk.internal.access.JavaLangAccess"), "defineClass", MethodType.methodType(Class.class, ClassLoader.class, Class.class, String.class, byte[].class, ProtectionDomain.class, boolean.class, int.class, Object.class));
            lookupConstructor = lookup.findConstructor(MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class, Class.class, int.class));
            findLoadedClassMethod=lookup.findVirtual(ClassLoader.class,"findLoadedClass", MethodType.methodType(Class.class, String.class));
            findBuiltinLibMethod=lookup.findStatic(nl,"findBuiltinLib", MethodType.methodType(String.class,String.class));
            NL_IMPL_openMethod=lookup.findVirtual(nlImplClass,"open",MethodType.methodType(boolean.class));
            NL_IMPL_findMethod=lookup.findVirtual(nlImplClass,"find",MethodType.methodType(long.class,String.class));
            NL_IMLP_unloaderMethod=lookup.findVirtual(nlImplClass,"unloader",MethodType.methodType(Runnable.class));
            nlImplConstructor=lookup.findConstructor(nlImplClass,MethodType.methodType(void.class,Class.class,String.class,boolean.class,boolean.class));
            findEntry0Method=lookup.findStatic(nl,"findEntry0", MethodType.methodType(long.class,nlImplClass,String.class));
            isSystemDomainLoaderMethod=lookup.findStatic(Class.forName("jdk.internal.misc.VM"),"isSystemDomainLoader", MethodType.methodType(boolean.class,ClassLoader.class));
            enqueueMethod=lookup.findStatic(Class.forName("sun.tools.attach.VirtualMachineImpl"),"enqueue",MethodType.methodType(void.class, long.class, byte[].class, String.class, String.class, Object[].class));

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
            exportJDKInternalModule();
            if (isWindows) {
                instImpl = NativeUtil.createInstrumentationImpl();
                objectInstImpl = NativeUtil.createObjectInstrumentationImpl();
            } else {
                instImpl = null;
                objectInstImpl = null;
            }
            oldProtect = new IntByReference(1);
            pdCache = new HashMap<>();
            JLA_INSTANCE = lookup.unreflectVarHandle(Class.forName("jdk.internal.access.SharedSecrets").getDeclaredField("javaLangAccess")).get();
            NATIVE_LIBS=lookup.unreflectVarHandle(Class.forName("jdk.internal.loader.BootLoader").getDeclaredField("NATIVE_LIBS")).get();
            //defineLibClass();
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

//    private static void defineLibClass(){
//        try {
//            InputStream is = ClassHelper.class.getResourceAsStream("/apphhzp/lib/ClassOption.class");
//            byte[] dat;
//            if (is==null){
//                dat=Base64.getDecoder().decode("yv66vgAAAD0AUgcAAgEAF2FwcGhoenAvbGliL0NsYXNzT3B0aW9uCQABAAQMAAUABgEACE5FU1RNQVRFAQAZTGFwcGhoenAvbGliL0NsYXNzT3B0aW9uOwkAAQAIDAAJAAYBAAZTVFJPTkcJAAEACwwADAANAQAHJFZBTFVFUwEAGltMYXBwaGh6cC9saWIvQ2xhc3NPcHRpb247CgAPABAHAA0MABEAEgEABWNsb25lAQAUKClMamF2YS9sYW5nL09iamVjdDsKABQAFQcAFgwAFwAYAQAOamF2YS9sYW5nL0VudW0BAAd2YWx1ZU9mAQA1KExqYXZhL2xhbmcvQ2xhc3M7TGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvRW51bTsKABQAGgwAGwAcAQAGPGluaXQ+AQAWKExqYXZhL2xhbmcvU3RyaW5nO0kpVgkAAQAeDAAfACABAARmbGFnAQABSQsAIgAjBwAkDAAlACYBAA1qYXZhL3V0aWwvU2V0AQAIaXRlcmF0b3IBABYoKUxqYXZhL3V0aWwvSXRlcmF0b3I7CwAoACkHACoMACsALAEAEmphdmEvdXRpbC9JdGVyYXRvcgEAB2hhc05leHQBAAMoKVoLACgALgwALwASAQAEbmV4dAgABQoAAQAyDAAbADMBABcoTGphdmEvbGFuZy9TdHJpbmc7SUkpVggACQoAAQA2DAA3ADgBAAckdmFsdWVzAQAcKClbTGFwcGhoenAvbGliL0NsYXNzT3B0aW9uOwEABnZhbHVlcwEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAC0oTGphdmEvbGFuZy9TdHJpbmc7KUxhcHBoaHpwL2xpYi9DbGFzc09wdGlvbjsBABJMb2NhbFZhcmlhYmxlVGFibGUBAARuYW1lAQASTGphdmEvbGFuZy9TdHJpbmc7AQAEdGhpcwEACVNpZ25hdHVyZQEABChJKVYBAA1vcHRpb25zVG9GbGFnAQASKExqYXZhL3V0aWwvU2V0OylJAQACY3ABAAdvcHRpb25zAQAPTGphdmEvdXRpbC9TZXQ7AQAFZmxhZ3MBABZMb2NhbFZhcmlhYmxlVHlwZVRhYmxlAQAqTGphdmEvdXRpbC9TZXQ8TGFwcGhoenAvbGliL0NsYXNzT3B0aW9uOz47AQANU3RhY2tNYXBUYWJsZQEALShMamF2YS91dGlsL1NldDxMYXBwaGh6cC9saWIvQ2xhc3NPcHRpb247PjspSQEACDxjbGluaXQ+AQADKClWAQArTGphdmEvbGFuZy9FbnVtPExhcHBoaHpwL2xpYi9DbGFzc09wdGlvbjs+OwEAClNvdXJjZUZpbGUBABBDbGFzc09wdGlvbi5qYXZhQDEAAQAUAAAABEAZAAUABgAAQBkACQAGAAAAEgAfACAAABAaAAwADQAAAAYACQA5ADgAAQA6AAAAIgABAAAAAAAKsgAKtgAOwAAPsAAAAAEAOwAAAAYAAQAAAAUACQAXADwAAQA6AAAANAACAAEAAAAKEgEquAATwAABsAAAAAIAOwAAAAYAAQAAAAUAPQAAAAwAAQAAAAoAPgA/AAAAAgAbADMAAgA6AAAASAADAAQAAAAMKisctwAZKh21AB2xAAAAAgA7AAAADgADAAAACgAGAAsACwAMAD0AAAAWAAIAAAAMAEAABgAAAAAADAAfACAAAwBBAAAAAgBCAAgAQwBEAAIAOgAAAJoAAgAEAAAAKAM8KrkAIQEATSy5ACcBAJkAFyy5AC0BAMAAAU4bLbQAHYA8p//mG6wAAAAEADsAAAAWAAUAAAAPAAIAEAAcABEAIwASACYAEwA9AAAAIAADABwABwBFAAYAAwAAACgARgBHAAAAAgAmAEgAIAABAEkAAAAMAAEAAAAoAEYASgAAAEsAAAAMAAL9AAkBBwAo+gAcAEEAAAACAEwQCgA3ADgAAQA6AAAAKQAEAAAAAAARBb0AAVkDsgADU1kEsgAHU7AAAAABADsAAAAGAAEAAAAFAAgATQBOAAEAOgAAAEMABQAAAAAAI7sAAVkSMAMEtwAxswADuwABWRI0BAe3ADGzAAe4ADWzAAqxAAAAAQA7AAAADgADAAAABgAOAAcAHAAFAAIAQQAAAAIATwBQAAAAAgBR");
//            }else{
//                dat = new byte[is.available()];
//                is.read(dat);
//                is.close();
//            }
//            ClassHelper.defineClass("apphhzp.lib.ClassOption",dat,ClassHelper.class.getClassLoader());
//        }catch (Throwable t){
//            throw new RuntimeException(t);
//        }
//    }

    private static void fuckJava23()throws Throwable{
        try {

            Class.forName("sun.misc.Unsafe$MemoryAccessOption");

            MethodHandles.Lookup lookup= (MethodHandles.Lookup) ReflectionFactory.getReflectionFactory()
                    .newConstructorForSerialization(MethodHandles.Lookup.class, MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,Class.class,int.class))
                    .newInstance(Object.class,null,-1);
            Class<?> internalUnsafeClass=Class.forName("jdk.internal.misc.Unsafe"),
                    accessOptions=Class.forName("sun.misc.Unsafe$MemoryAccessOption");
            MethodHandle fieldOffset=lookup.findVirtual(internalUnsafeClass,"staticFieldOffset0",MethodType.methodType(long.class, Field.class))
                    ,fieldBase=lookup.findVirtual(internalUnsafeClass,"staticFieldBase0",MethodType.methodType(Object.class, Field.class))
                    ,putReference=lookup.findVirtual(internalUnsafeClass,"putReference",MethodType.methodType(void.class,Object.class,long.class,Object.class))
                    ,ensure=lookup.findVirtual(internalUnsafeClass,"ensureClassInitialized0",MethodType.methodType(void.class,Class.class));
            Object internalUnsafe=lookup.findConstructor(internalUnsafeClass,MethodType.methodType(void.class)).invoke();
            ensure.invoke(internalUnsafe,Class.forName("sun.misc.Unsafe",false,ClassHelper.class.getClassLoader()));
            Field field=Unsafe.class.getDeclaredField("MEMORY_ACCESS_OPTION");
            putReference.invoke(internalUnsafe,fieldBase.invoke(internalUnsafe,field),(long)fieldOffset.invoke(internalUnsafe,field),
                    lookup.findStaticVarHandle(accessOptions,"ALLOW",accessOptions).get());
        }catch (ClassNotFoundException ignored){}
    }

    public static Unsafe createUnsafe() {
        try {
            VarHandle varHandle = lookup.findStaticVarHandle(Unsafe.class, "theInternalUnsafe", internalClass);
            MethodHandle methodHandle = lookup.findSpecial(internalClass, "allocateInstance", MethodType.methodType(Object.class, Class.class), internalClass);
            Object internalUnsafe = varHandle.get();
            return (Unsafe) methodHandle.bindTo(internalUnsafe).invoke(Unsafe.class);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void exportJDKInternalModule() {
        try {
            Class<?> klass = Class.forName("jdk.internal.misc.TerminatingThreadLocal");
            Module module = klass.getModule();
            Field accField = Class.forName("jdk.internal.access.SharedSecrets").getDeclaredField("javaLangAccess");
            Object o = lookup.unreflectVarHandle(accField).get();
            MethodHandle addExport = lookup.findVirtual(Class.forName("jdk.internal.access.JavaLangAccess"), "addExports", MethodType.methodType(void.class, Module.class, String.class));
            for (String name : module.getPackages()) {
                addExport.invoke(o, module, name);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void addExportImpl(Module current, String pkg) {
        try {
            Field accField = Class.forName("jdk.internal.access.SharedSecrets").getDeclaredField("javaLangAccess");
            Object o = lookup.unreflectVarHandle(accField).get();
            Method addExport = Class.forName("jdk.internal.access.JavaLangAccess").getDeclaredMethod("addExports", Module.class, String.class);
            lookup.unreflect(addExport).invoke(o, current, pkg);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Class<?> defineClass(String name, byte[] bytecodes, ClassLoader loader) {
        try {
            return (Class<?>) defineClassMethod.invoke(internalUnsafe, name, bytecodes, 0, bytecodes.length, loader, null);
        } catch (Throwable t) {
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
            return (MethodHandles.Lookup) lookupConstructor.invoke(JLA_defineClassMethod.invoke(JLA_INSTANCE, loader, lookupClass, name, bytes, pd, initialize, flags, null), null, 95);
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
            return (MethodHandles.Lookup) lookupConstructor.invoke(JLA_defineClassMethod.invoke(JLA_INSTANCE, loader, lookupClass, name, bytes, pd, initialize, flags, classData), null, 95);
        } catch (Throwable t) {
            throw new RuntimeException("Could not define a hidden class:" + name, t);
        }
    }

    public static boolean defineClassBypassAgent(String name, Class<?> lookupClass, boolean initialize, ProtectionDomain pd){
        if (!isHotspotJVM){
            throw new UnsupportedOperationException("Only in Hotspot JVM");
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
            throw new RuntimeException(t);
        }
        Dictionary dict=ClassLoaderData.as(lookupClass.getClassLoader()).getDictionary();
        if (dict != null) {
            InstanceKlass klass= Klass.asKlass(clazz).asInstanceKlass();
            Symbol symbol=Symbol.lookupOrCreate(name.replace('.','/'));
            symbol.incrementRefCount();
            klass.setName(symbol);
            klass.setAccessFlags(klass.getAccessFlags().flags&~AccessFlags.JVM_ACC_IS_HIDDEN_CLASS);
            dict.addKlass(dict.computeHash(klass.getName()),klass.getName(),klass);
            return true;
        }else {
            return false;
        }
    }

    public static void createHiddenThread(Runnable task,String name){
        Thread thread= new Thread(null, task,name);
        thread.start();
        eetopVar.set(thread,0L);
    }

    public static NativeLibrary load(Class<?> call, String filename){
        File file = new File(filename);
        if (!file.isAbsolute()) {
            throw new UnsatisfiedLinkError("Expecting an absolute path of the library: " + filename);
        }
        ClassLoader loader = call == null ? null : call.getClassLoader();
        Object libs = loader != null ? CL_librariesVar.get(loader): NATIVE_LIBS;
        NativeLibrary nl = loadLibrary(libs,call, file);
        if (nl != null) {
            return nl;
        }
        throw new UnsatisfiedLinkError("Can't load library: " + file);
    }

    public static NativeLibrary loadLibrary(Object nl,Class<?> fromClass, File file) {
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
            throw new RuntimeException(t);
        }
    }

    public static NativeLibrary loadLibrary(Object nl,Class<?> fromClass, String name, boolean isBuiltin) {
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
                public long find(String entry) {
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
            throw new RuntimeException(e);
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
        return classNameCache.computeIfAbsent(bytes, ClassHelper::getClassName);
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
                        charBuffer[strLength++] =
                                (char) (((currentByte & 0x1F) << 6) + (bytes[offset++] & 0x3F));
                    } else {
                        charBuffer[strLength++] =
                                (char)
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

//    public static int addReturn0ToCFunction(long[] func_addr){
//        int cnt=0;
//        if (isWindows){
//            for (long func:func_addr){
//                if (func!=0L){
//                    if (Kernel32.INSTANCE.VirtualProtect(func,3,PAGE_EXECUTE_READWRITE,oldProtect.getPointer())) {
//                        unsafe.putByte(func, (byte) 0x33);
//                        unsafe.putByte(func + 1L, (byte) 0xc0);
//                        unsafe.putByte(func + 2L, (byte) 0xc3);
//                        ++cnt;
//                    }
//                }
//            }
//        }else if (isLinux){
//
//        }
//        return cnt;
//    }
//
//    public static int addReturnToCFunction(long[] func_addr){
//        int cnt=0;
//        if (isWindows){
//            for (long func:func_addr){
//                if (func!=0L){
//                    Kernel32.INSTANCE.VirtualProtect(func,1,PAGE_EXECUTE_READWRITE,oldProtect.getPointer());
//                    unsafe.putByte(func,(byte) 0xc3);
//                }
//            }
//        }else if (isLinux){
//
//        }
//        return cnt;
//    }

    public static Object forceGetField(Field field, Object obj) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                return unsafe.getObjectVolatile(staticFieldBaseMethod.invoke(internalUnsafe, field), (Long) staticFieldOffsetMethod.invoke(internalUnsafe, field));
            } else {
                return unsafe.getObjectVolatile(obj, (Long) objectFieldOffsetMethod.invoke(internalUnsafe, field));
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not get.", throwable);
        }
    }

    public static void forceSetField(Object o, Field field, Object x) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                unsafe.putObjectVolatile(staticFieldBaseMethod.invoke(internalUnsafe, field), (Long) staticFieldOffsetMethod.invoke(internalUnsafe, field), x);
            } else {
                unsafe.putObjectVolatile(o, (Long) objectFieldOffsetMethod.invoke(internalUnsafe, field), x);
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not set:", throwable);
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
            throw new RuntimeException(ex);
        }
    }

//    public static Object getUncompressedObject(long address){
//        try {
//            return getUncompressedObjectMethod.invoke(internalUnsafe, address);
//        }catch (Throwable t){
//            throw new RuntimeException(t);
//        }
//    }


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
        String file = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (!file.isEmpty()) {
            if (file.startsWith("union:"))
                file = file.substring(6);
            if (file.startsWith("/"))
                file = file.substring(1);
            file = file.substring(0, file.lastIndexOf(".jar") + 4);
            file = file.replaceAll("/", "\\\\");
        }
        return URLDecoder.decode(file, StandardCharsets.UTF_8);
    }

    public static boolean compareAndSwapByte(Object o, long offset, byte expected, byte x) {
        try {
            return (boolean) compareAndSetByteMethod.invoke(internalUnsafe, o, offset, expected, x);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Class<?> findLoadedClass(ClassLoader loader,String name){
        try {
            return (Class<?>)findLoadedClassMethod.invoke(loader,name);
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static void defineClassesFromJar(Class<?> caller, Predicate<String> predicate){
        try {
            //LOGGER.error("DO NOT USE THIS METHOD! Do not support character '!' or '+'");
            StringBuilder builder=new StringBuilder();
            JarFile jarFile = new JarFile(getJarPath(caller));
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")&&predicate.test(name)) {
                    builder.append(",\"/").append(name).append("\"");
                    name = name.replace('/', '.').substring(0, name.length() - 6);
                    if (findLoadedClass(caller.getClassLoader(), name) == null) {
                        InputStream in = jarFile.getInputStream(entry);
                        byte[] dat = new byte[in.available()];
                        in.read(dat);
                        in.close();
                        defineClass(name,dat,caller.getClassLoader());
                    }
                }
            }
            System.err.println(builder);
        }catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int getPid() {
        return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }

    public static int version(){
        return 7;
    }
}
