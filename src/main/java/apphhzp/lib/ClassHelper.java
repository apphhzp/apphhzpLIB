package apphhzp.lib;


import apphhzp.lib.api.ObjectInstrumentation;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.Debugger;
import apphhzp.lib.natives.NativeUtil;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.*;

import static apphhzp.lib.ClassHelper.ClassOption.optionsToFlag;

@SuppressWarnings("unused")
public final class ClassHelper {
    private static final Logger LOGGER;
    public static final MethodHandles.Lookup lookup;
    public static final Unsafe unsafe;
    public static final Object internalUnsafe;
    private static final Class<?> internalClass;
    public static final MethodHandle staticFieldBaseMethod;
    public static final MethodHandle staticFieldOffsetMethod;
    public static final MethodHandle objectFieldOffsetMethod;
    public static final MethodHandle defineClassMethod;
    public static final MethodHandle JLA_defineClassMethod;
    public static final MethodHandle lookupConstructor;
    public static final MethodHandle compareAndSetByteMethod;
    @Nullable
    public static final Instrumentation instImpl;
    @Nullable
    public static final ObjectInstrumentation objectInstImpl;
    public static final boolean isWindows;
    public static final boolean isLinux;
    public static final boolean is64BitJVM;
    public static final boolean isHotspotJVM;
    private static final IntByReference oldProtect;
    private static final Map<CodeSource,ProtectionDomain> pdCache;
    public static final Object JLA_INSTANCE;

    static {
        try {
            LOGGER = LogManager.getLogger(ClassHelper.class);
            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            isWindows = osName.contains("win");
            isLinux = osName.contains("nux")||osName.contains("nix");//||osName.contains("unix")
            Unsafe tmp;
            Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
            c.setAccessible(true);
            tmp = c.newInstance();
            lookup = (MethodHandles.Lookup) tmp.getObjectVolatile(tmp.staticFieldBase(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")), tmp.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")));
            internalClass = Class.forName("jdk.internal.misc.Unsafe");
            unsafe = createUnsafe();
            is64BitJVM = unsafe.addressSize() == 8;
            isHotspotJVM = System.getProperty("java.vm.name").toLowerCase().contains("hotspot");
            internalUnsafe = lookup.findStaticVarHandle(Unsafe.class, "theInternalUnsafe", internalClass).get();
            staticFieldBaseMethod = lookup.findVirtual(internalClass, "staticFieldBase", MethodType.methodType(Object.class, Field.class));
            staticFieldOffsetMethod = lookup.findVirtual(internalClass, "staticFieldOffset", MethodType.methodType(long.class, Field.class));
            objectFieldOffsetMethod = lookup.findVirtual(internalClass, "objectFieldOffset", MethodType.methodType(long.class, Field.class));
            defineClassMethod = lookup.findVirtual(internalClass,"defineClass",MethodType.methodType(Class.class,String.class,byte[].class,int.class,int.class,ClassLoader.class, ProtectionDomain.class));
            compareAndSetByteMethod=lookup.findVirtual(internalClass,"compareAndSetByte",MethodType.methodType(boolean.class,Object.class,long.class,byte.class,byte.class));
            JLA_defineClassMethod=lookup.findVirtual(Class.forName("jdk.internal.access.JavaLangAccess"),"defineClass",MethodType.methodType(Class.class,ClassLoader.class,Class.class,String.class,byte[].class,ProtectionDomain.class,boolean.class,int.class,Object.class));
            lookupConstructor=lookup.findConstructor(MethodHandles.Lookup.class,MethodType.methodType(void.class,Class.class,Class.class,int.class));
            exportJDKInternalModule();
            if (isWindows && !Debugger.isDebug) {
                instImpl = NativeUtil.createInstrumentationImpl();
                objectInstImpl =NativeUtil.createObjectInstrumentationImpl();
            } else {
                instImpl = null;
                objectInstImpl =null;
            }
            oldProtect=new IntByReference(1);
            pdCache=new HashMap<>();
            JLA_INSTANCE = lookup.unreflectVarHandle(Class.forName("jdk.internal.access.SharedSecrets").getDeclaredField("javaLangAccess")).get();
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
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
            MethodHandle addExport=lookup.findVirtual(Class.forName("jdk.internal.access.JavaLangAccess"),"addExports",MethodType.methodType(void.class, Module.class, String.class));
            for(String name:module.getPackages()){
                //System.err.println("name:"+name);
                addExport.invoke(o, module, name);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void addExportImpl(Module current,String pkg){
        try {
            Field accField = Class.forName("jdk.internal.access.SharedSecrets").getDeclaredField("javaLangAccess");
            Object o = lookup.unreflectVarHandle(accField).get();
            Method addExport = Class.forName("jdk.internal.access.JavaLangAccess").getDeclaredMethod("addExports", Module.class, String.class);
            lookup.unreflect(addExport).invoke(o, current, pkg);
        } catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static Class<?> defineClass(String name,byte[] bytecodes,ClassLoader loader){
        try {
            return (Class<?>) defineClassMethod.invoke(internalUnsafe,name,bytecodes,0,bytecodes.length,loader,null);
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static ProtectionDomain createProtectionDomain(CodeSource codeSource, ClassLoader cl) {
        return pdCache.computeIfAbsent(codeSource, cs->{
            Permissions perms = new Permissions();
            perms.add(new AllPermission());
            return new ProtectionDomain(codeSource, perms, cl, null);
        });
    }


    public static MethodHandles.Lookup defineHiddenClass(byte[] bytes,String name, boolean initialize,Class<?> lookupClass, ClassLoader loader, ProtectionDomain pd, ClassOption... options) {
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(options);
        int flags = 2|optionsToFlag(Set.of(options));
        if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
            flags |= 8;
        }
        try {
            return (MethodHandles.Lookup) lookupConstructor.invoke(JLA_defineClassMethod.invoke(JLA_INSTANCE,loader, lookupClass, name, bytes, pd, initialize, flags, null), null, 95);
        }catch (Throwable t){
            throw new RuntimeException("Could not define a hidden class:"+name,t);
        }
    }

    public MethodHandles.Lookup defineHiddenClassWithClassData(byte[] bytes,String name, Object classData, boolean initialize,Class<?> lookupClass,ClassLoader loader, ProtectionDomain pd, ClassOption... options) {
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(classData);
        Objects.requireNonNull(options);
        int flags = 2|optionsToFlag(Set.of(options));
        if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
            flags |= 8;
        }
        try {
            return (MethodHandles.Lookup) lookupConstructor.invoke(JLA_defineClassMethod.invoke(JLA_INSTANCE,loader, lookupClass, name, bytes, pd, initialize, flags, classData), null, 95);
        }catch (Throwable t){
            throw new RuntimeException("Could not define a hidden class:"+name,t);
        }
    }
    public enum ClassOption {
        NESTMATE(1),
        STRONG(4);
        private final int flag;
        ClassOption(int flag) {
            this.flag = flag;
        }
        static int optionsToFlag(Set<ClassOption> options) {
            int flags = 0;
            for (ClassOption cp : options) {
                flags |= cp.flag;
            }
            return flags;
        }
    }

    public static String getClassName(byte[] bytes) {
        if (4 > bytes.length) {
            throw new ClassFormatError("Invalid ClassFile structure");
        }
        int magic = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        if (magic != 0xCAFEBABE) {
            throw new ClassFormatError("Incompatible magic value: " + magic);
        }
        try {
            ClassReader reader = new ClassReader(bytes);
            int thisClass = reader.readUnsignedShort(reader.header + 2);
            Object constant = reader.readConst(thisClass, new char[reader.getMaxStringLength()]);
            if (!(constant instanceof Type type)) {
                throw new ClassFormatError("this_class item: #" + thisClass + " not a CONSTANT_Class_info");
            }
            if (!type.getDescriptor().startsWith("L")) {
                throw new ClassFormatError("this_class item: #" + thisClass + " not a CONSTANT_Class_info");
            }
            return type.getClassName();
        } catch (RuntimeException e) {
            ClassFormatError cfe = new ClassFormatError();
            cfe.initCause(e);
            throw cfe;
        }
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

    @SuppressWarnings("unchecked")
    public static <T> T getOuterInstance(Object obj, Class<T> fa) {
        try {
            return (T) lookup.findVarHandle(obj.getClass(), "this$0", fa).get(obj);
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not get OuterInstance:", throwable);
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

    public static boolean compareAndSwapByte(Object o,long offset,byte expected,byte x){
        try {
            return (boolean) compareAndSetByteMethod.invoke(internalUnsafe,o,offset,expected,x);
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static int getPid() {
        return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }
}
