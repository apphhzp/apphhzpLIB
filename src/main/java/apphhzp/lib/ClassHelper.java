package apphhzp.lib;


import apphhzp.lib.api.ObjectInstrumentation;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.Debugger;
import apphhzp.lib.hotspot.oop.*;
import apphhzp.lib.hotspot.utilities.Dictionary;
import apphhzp.lib.natives.NativeUtil;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.io.InputStream;
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

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
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
    //public static final MethodHandle getUncompressedObjectMethod;
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

    static {
        try {
            LOGGER = LogManager.getLogger(ClassHelper.class);
            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            isWindows = osName.contains("win");
            isLinux = osName.contains("nux") || osName.contains("nix");//||osName.contains("unix")
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
            defineClassMethod = lookup.findVirtual(internalClass, "defineClass", MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class));
            compareAndSetByteMethod = lookup.findVirtual(internalClass, "compareAndSetByte", MethodType.methodType(boolean.class, Object.class, long.class, byte.class, byte.class));
            JLA_defineClassMethod = lookup.findVirtual(Class.forName("jdk.internal.access.JavaLangAccess"), "defineClass", MethodType.methodType(Class.class, ClassLoader.class, Class.class, String.class, byte[].class, ProtectionDomain.class, boolean.class, int.class, Object.class));
            lookupConstructor = lookup.findConstructor(MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class, Class.class, int.class));
            //getUncompressedObjectMethod=lookup.findVirtual(internalClass, "getUncompressedObject", MethodType.methodType(Object.class, long.class));
            exportJDKInternalModule();
            if (isWindows && !Debugger.isDebug) {
                instImpl = NativeUtil.createInstrumentationImpl();
                objectInstImpl = NativeUtil.createObjectInstrumentationImpl();
            } else {
                instImpl = null;
                objectInstImpl = null;
            }
            oldProtect = new IntByReference(1);
            pdCache = new HashMap<>();
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
        Class<?> clazz;
        try {
            clazz=defineHiddenClass(name,lookupClass,initialize,pd).lookupClass();
        }catch (VerifyError error){
            return false;
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
        Dictionary dict=ClassLoaderData.as(lookupClass.getClassLoader()).getDictionary();
        if (dict != null) {
            InstanceKlass klass=Klass.asKlass(clazz).asInstanceKlass();
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

    private static final WeakHashMap<byte[], String> classNameCache = new WeakHashMap<>();

    public static String getClassNameWithCaches(byte[] bytes) {
        return classNameCache.computeIfAbsent(bytes, ClassHelper::getClassName);
    }

    public static String getClassName(byte[] bytes) {
        int int1 = 0, offset = 10;
        int[] cpInfoOffsets;
        int int2, cpInfoSize, currentCpInfoIndex;
        byte val;
        for (int2 = ((bytes[8] & 0xFF) << 8) | (bytes[9] & 0xFF), cpInfoOffsets = new int[int2], currentCpInfoIndex = 1;
             currentCpInfoIndex < int2; offset += cpInfoSize) {
            cpInfoOffsets[currentCpInfoIndex++] = offset + 1;
            val = bytes[offset];
            if (val == 9 || val == 10 || val == 11 || val == 3 || val == 4 || val == 12 || val == 17 || val == 18) {
                cpInfoSize = 5;
            } else if (val == 5 || val == 6) {
                cpInfoSize = 9;
                currentCpInfoIndex++;
            } else if (val == 1) {
                cpInfoSize = 3 + (((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset + 2] & 0xFF));
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
        int1 = (((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF));
        offset = cpInfoOffsets[int1];
        if (bytes[offset - 1] == 7) {
            int1 = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
            if (int1 == 0) {
                throw new ClassFormatError();
            }
            offset = cpInfoOffsets[int1];
            int2 = offset + 2 + (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));
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
        int val = ((bytes[8] & 0xFF) << 8) | (bytes[9] & 0xFF);
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
                cpInfoSize = 3 + (((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset + 2] & 0xFF));
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
        offset=cpInfoOffsets[((bytes[offset + 4] & 0xFF) << 8) | (bytes[offset + 5] & 0xFF)];
        val = (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));
        if (offset == 0 || val == 0) {
            throw new ClassFormatError();
        }
        offset = cpInfoOffsets[val];
        val = offset + 2 + (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));
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
        int cnt = ((bytes[8] & 0xFF) << 8) | (bytes[9] & 0xFF);
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
                cpInfoSize = 3 + (((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset + 2] & 0xFF));
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
        cnt = ((bytes[index] & 0xFF) << 8) | (bytes[index + 1] & 0xFF);
        String[] interfaces = new String[cnt];
        if (cnt > 0) {
            char[] charBuffer = new char[maxStringLength];
            for (int i = 0; i < cnt; ++i) {
                index += 2;
                offset = cpInfoOffsets[((bytes[index] & 0xFF) << 8) | (bytes[index + 1] & 0xFF)];
                int constantPoolEntryIndex = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
                if (offset == 0 || constantPoolEntryIndex == 0) {
                    throw new ClassFormatError();
                }
                offset = cpInfoOffsets[constantPoolEntryIndex];
                int endOffset = offset + 2 + (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));
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

    public static int getPid() {
        return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }
}
