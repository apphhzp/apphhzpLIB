package apphhzp.lib.helfy;

import apphhzp.lib.hotspot.oop.constant.ConstantPool;
import apphhzp.lib.hotspot.oop.method.Method;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.runtime.ObjectMonitor;
import apphhzp.lib.hotspot.runtime.Thread;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import apphhzp.lib.hotspot.JVMUtil;
import apphhzp.lib.hotspot.NativeLibrary;
import apphhzp.lib.hotspot.cds.FileMapHeader;
import apphhzp.lib.hotspot.cds.FileMapInfo;
import apphhzp.lib.hotspot.runtime.JVMFlag;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.util.*;

import static apphhzp.lib.ClassHelper.*;

public final class JVM {
    public static final boolean ASSERTS_ENABLED=true;
    private static final NativeLibrary JVM;
    public static final Map<String, Type> types = new LinkedHashMap<>();
    public static final Map<String, Number> constants = new LinkedHashMap<>();
    public static final int oopSize;
    public static final int intSize;
    public static final int longSize;
    public static final int size_tSize;
    public static final int heapWordSize;
    public static final boolean isJVMTISupported;
    public static final boolean usingClientCompiler;
    public static final boolean usingServerCompiler;
    public static final boolean usingSharedSpaces;
    public static final boolean usingCompressedClassPointers;
    public static final boolean usingCompressedOops;
    public static final boolean usingTLAB;
    public static final boolean includeJVMCI;
    private static final Map<Type, Long> type2vtblMap = new HashMap<>();
    private static final Object2LongOpenHashMap<Type> type2vtbl = new Object2LongOpenHashMap<>();
    public static final int invocationEntryBci;
    public static final long PerMethodRecompilationCutoff;

    private JVM() {
    }

    private static Map<String, Set<Field>> readVmStructs() {
        long entry = getSymbol("gHotSpotVMStructs");
        long typeNameOffset = getSymbol("gHotSpotVMStructEntryTypeNameOffset");
        long fieldNameOffset = getSymbol("gHotSpotVMStructEntryFieldNameOffset");
        long typeStringOffset = getSymbol("gHotSpotVMStructEntryTypeStringOffset");
        long isStaticOffset = getSymbol("gHotSpotVMStructEntryIsStaticOffset");
        long offsetOffset = getSymbol("gHotSpotVMStructEntryOffsetOffset");
        long addressOffset = getSymbol("gHotSpotVMStructEntryAddressOffset");
        long arrayStride = getSymbol("gHotSpotVMStructEntryArrayStride");
        Map<String, Set<Field>> structs = new HashMap<>();
        for (; ; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            String fieldName = getStringRef(entry + fieldNameOffset);
            if (fieldName == null) break;
            String typeString = getStringRef(entry + typeStringOffset);
            boolean isStatic = unsafe.getInt(entry + isStaticOffset) != 0;
            long offset = unsafe.getLong(entry + (isStatic ? addressOffset : offsetOffset));
            Set<Field> fields = structs.computeIfAbsent(typeName, k -> new TreeSet<>());
            fields.add(new Field(fieldName, typeString, offset, isStatic));
        }
        return structs;
    }

    private static Map<String, Set<Field>> readVmCIStructs() {
        long entry = getSymbol("jvmciHotSpotVMStructs");
        long typeNameOffset = getSymbol("gHotSpotVMStructEntryTypeNameOffset");
        long fieldNameOffset = getSymbol("gHotSpotVMStructEntryFieldNameOffset");
        long typeStringOffset = getSymbol("gHotSpotVMStructEntryTypeStringOffset");
        long isStaticOffset = getSymbol("gHotSpotVMStructEntryIsStaticOffset");
        long offsetOffset = getSymbol("gHotSpotVMStructEntryOffsetOffset");
        long addressOffset = getSymbol("gHotSpotVMStructEntryAddressOffset");
        long arrayStride = getSymbol("gHotSpotVMStructEntryArrayStride");
        Map<String, Set<Field>> structs = new HashMap<>();
        for (; ; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            String fieldName = getStringRef(entry + fieldNameOffset);
            if (fieldName == null) break;
            String typeString = getStringRef(entry + typeStringOffset);
            boolean isStatic = unsafe.getInt(entry + isStaticOffset) != 0;
            long offset = unsafe.getLong(entry + (isStatic ? addressOffset : offsetOffset));
            Set<Field> fields = structs.computeIfAbsent(typeName, k -> new TreeSet<>());
            fields.add(new Field(fieldName, typeString, offset, isStatic));
        }
        return structs;
    }

    private static void readVmTypes(Map<String, Set<Field>> structs) {
        long entry = getSymbol("gHotSpotVMTypes");
        long typeNameOffset = getSymbol("gHotSpotVMTypeEntryTypeNameOffset");
        long superclassNameOffset = getSymbol("gHotSpotVMTypeEntrySuperclassNameOffset");
        long isOopTypeOffset = getSymbol("gHotSpotVMTypeEntryIsOopTypeOffset");
        long isIntegerTypeOffset = getSymbol("gHotSpotVMTypeEntryIsIntegerTypeOffset");
        long isUnsignedOffset = getSymbol("gHotSpotVMTypeEntryIsUnsignedOffset");
        long sizeOffset = getSymbol("gHotSpotVMTypeEntrySizeOffset");
        long arrayStride = getSymbol("gHotSpotVMTypeEntryArrayStride");
        for (; ; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            if (typeName == null) break;
            String superclassName = getStringRef(entry + superclassNameOffset);
            boolean isOop = unsafe.getInt(entry + isOopTypeOffset) != 0;
            boolean isInt = unsafe.getInt(entry + isIntegerTypeOffset) != 0;
            boolean isUnsigned = unsafe.getInt(entry + isUnsignedOffset) != 0;
            int size = unsafe.getInt(entry + sizeOffset);
            Set<Field> fields = structs.get(typeName);
            types.put(typeName, new Type(typeName, superclassName, size, isOop, isInt, isUnsigned, fields));
        }
    }
    private static void readVmCITypes(Map<String, Set<Field>> structs) {
        long entry = getSymbol("jvmciHotSpotVMTypes");
        long typeNameOffset = getSymbol("gHotSpotVMTypeEntryTypeNameOffset");
        long superclassNameOffset = getSymbol("gHotSpotVMTypeEntrySuperclassNameOffset");
        long isOopTypeOffset = getSymbol("gHotSpotVMTypeEntryIsOopTypeOffset");
        long isIntegerTypeOffset = getSymbol("gHotSpotVMTypeEntryIsIntegerTypeOffset");
        long isUnsignedOffset = getSymbol("gHotSpotVMTypeEntryIsUnsignedOffset");
        long sizeOffset = getSymbol("gHotSpotVMTypeEntrySizeOffset");
        long arrayStride = getSymbol("gHotSpotVMTypeEntryArrayStride");
        for (Map.Entry<String,Set<Field>> entry1:structs.entrySet()){
            if (!types.containsKey(entry1.getKey())){
                types.put(entry1.getKey(),new Type(entry1.getKey(),null,1,false,false,false,entry1.getValue()));
            }else {
                Type old=types.get(entry1.getKey());
                types.put(entry1.getKey(),new Type(old.name,old.superName,old.size,old.isOop,old.isInt,old.isUnsigned,updateOrCreateFields(entry1.getValue(),old)));
            }
        }
        for (; ; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            if (typeName == null) break;
            String superclassName = getStringRef(entry + superclassNameOffset);
            boolean isOop = unsafe.getInt(entry + isOopTypeOffset) != 0;
            boolean isInt = unsafe.getInt(entry + isIntegerTypeOffset) != 0;
            boolean isUnsigned = unsafe.getInt(entry + isUnsignedOffset) != 0;
            int size = unsafe.getInt(entry + sizeOffset);
            Set<Field> fields =updateOrCreateFields(structs.get(typeName),types.get(typeName));
            types.put(typeName, new Type(typeName, superclassName, size, isOop, isInt, isUnsigned, fields));
        }
    }

    private static Set<Field> updateOrCreateFields(@Nullable Set<Field> newFields,@Nullable Type oldType){
        if (oldType!=null&&newFields!=null){
            newFields.addAll(List.of(oldType.fields));
        }else if (newFields==null){
            return oldType==null?null:new TreeSet<>(List.of(oldType.fields));
        }
        return newFields;
    }

    private static void readVmIntConstants() {
        long entry = getSymbol("gHotSpotVMIntConstants");
        long nameOffset = getSymbol("gHotSpotVMIntConstantEntryNameOffset");
        long valueOffset = getSymbol("gHotSpotVMIntConstantEntryValueOffset");
        long arrayStride = getSymbol("gHotSpotVMIntConstantEntryArrayStride");
        for (; ; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;
            int value = unsafe.getInt(entry + valueOffset);
            constants.put(name, value);
        }
    }

    private static void readVmCIIntConstants() {
        long entry = getSymbol("jvmciHotSpotVMIntConstants");
        long nameOffset = getSymbol("gHotSpotVMIntConstantEntryNameOffset");
        long valueOffset = getSymbol("gHotSpotVMIntConstantEntryValueOffset");
        long arrayStride = getSymbol("gHotSpotVMIntConstantEntryArrayStride");
        for (; ; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;
            int value = unsafe.getInt(entry + valueOffset);
            constants.put(name, value);
        }
    }

    private static void readVmLongConstants() {
        long entry = getSymbol("gHotSpotVMLongConstants");
        long nameOffset = getSymbol("gHotSpotVMLongConstantEntryNameOffset");
        long valueOffset = getSymbol("gHotSpotVMLongConstantEntryValueOffset");
        long arrayStride = getSymbol("gHotSpotVMLongConstantEntryArrayStride");
        for (; ; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;

            long value = unsafe.getLong(entry + valueOffset);
            constants.put(name, value);
        }
    }
    private static void readVmCILongConstants() {
        long entry = getSymbol("jvmciHotSpotVMLongConstants");
        long nameOffset = getSymbol("gHotSpotVMLongConstantEntryNameOffset");
        long valueOffset = getSymbol("gHotSpotVMLongConstantEntryValueOffset");
        long arrayStride = getSymbol("gHotSpotVMLongConstantEntryArrayStride");
        for (; ; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;
            long value = unsafe.getLong(entry + valueOffset);
            constants.put(name, value);
        }
    }

    public static String getString(long addr) {
        if (addr == 0) {
            return null;
        }
        char[] chars = new char[40];
        int offset = 0;
        for (byte b; (b = unsafe.getByte(addr + offset)) != 0; ) {
            if (offset >= chars.length) chars = Arrays.copyOf(chars, offset * 2);
            chars[offset++] = (char) b;
        }
        return new String(chars, 0, offset);
    }

    public static String getStringRef(long addr) {
        return getString(unsafe.getAddress(addr));
    }

    public static void putStringRef(long addr, String str) {
        byte[] bytes = str.getBytes();
        int len = bytes.length;
        long base = unsafe.allocateMemory(len + 1);
        for (int i = 0; i < len; i++) {
            unsafe.putByte(base + i, bytes[i]);
        }
        unsafe.putByte(base + len, (byte) 0);
        unsafe.putAddress(addr, base);
    }

    public static long getSymbol(String name) {
        long address = JVM.findEntry(name);
        if (address == 0) {
            throw new NoSuchElementException("No such symbol: " + name);
        }
        return unsafe.getLong(address);
    }

    public static Type type(String name) {
        if (!isHotspotJVM) {
            return Type.EMPTY;
        }
        Type type = types.get(name);
        if (type == null) {
            throw new NoSuchElementException("No such type: " + name);
        }
        return type;
    }

    public static Number constant(String name) {
        if (!isHotspotJVM) {
            return 0;
        }
        Number constant = constants.get(name);
        if (constant == null) {
            throw new NoSuchElementException("No such constant: " + name);
        }
        return constant;
    }

    public static int intConstant(String name) {
        return constant(name).intValue();
    }

    public static long longConstant(String name) {
        return constant(name).longValue();
    }

    public static void printAllTypes() {
        if (!isHotspotJVM) {
            System.err.println("NO TYPES");
        }
        for (Type type : types.values()) {
            System.err.println(type);
        }
    }

    public static void printAllConstants() {
        if (!isHotspotJVM) {
            System.err.println("NO CONSTANTS");
        }
        for (Map.Entry<String, Number> type : constants.entrySet()) {
            System.err.println(type.getKey() + "=" + type.getValue());
        }
    }

    public static void printAllVTBL() {
        if (!isHotspotJVM || !isWindows) {
            System.err.println("Unsupported");
            return;
        }
        for (Type type : types.values()) {
            long vtbl = vtblForType(type);
            if (vtbl != 0L) {
                System.err.println("vtbl(" + type.name + "):0x" + Long.toHexString(vtbl));
            }
        }
    }

    public static void putCLevelLong(long address,long val){
        if (longSize==4){
            unsafe.putInt(address,(int) val);
        }else {
            unsafe.putLong(address,val);
        }
    }
    public static long getCLevelLong(long address){
        if (longSize==4){
            return unsafe.getInt(address);
        }else {
            return unsafe.getLong(address);
        }
    }

    public static void putSizeT(long address,long val){
        if (size_tSize==4){
            unsafe.putInt(address,(int) val);
        }else {
            unsafe.putLong(address,val);
        }
    }
    public static long getSizeT(long address){
        if (size_tSize==4){
            return unsafe.getInt(address);
        }else {
            return unsafe.getLong(address);
        }
    }

    public static void clearAllCacheMaps(){
        Method.clearCacheMap();
        ConstantPool.clearCacheMap();
        JavaThread.clearCacheMap();
        ObjectMonitor.clearCacheMap();
        Thread.clearCacheMap();
    }

    public static String vtblSymbolForType(Type type) {
        if (!isWindows) {
            throw new IllegalStateException("Unsupported OS");
        }
        return "??_7" + type.name + "@@6B@";
    }

    public static long getVtblForType(Type type) {
        if (type == null || !isWindows) {
            return 0L;
        } else {
            if (type2vtblMap.containsKey(type)) {
                return type2vtblMap.get(type);
            } else {
                String vtblSymbol = vtblSymbolForType(type);
                if (vtblSymbol == null) {
                    type2vtblMap.put(type, 0L);
                    return 0L;
                }
                try {
                    long addr = JVM.findEntry(vtblSymbol);
                    if (addr != 0L) {
                        type2vtblMap.put(type, addr);
                        return addr;
                    }
                    type2vtblMap.put(type, 0L);
                    return 0;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    type2vtblMap.put(type, 0L);
                    return 0;
                }
            }
        }
    }

    public static long vtblForType(Type type) {
        if (!type2vtbl.containsKey(type)) {
            long vtblAddr = type2vtbl.getLong(type);
            if (vtblAddr == 0) {
                vtblAddr = getVtblForType(type);
                if (vtblAddr != 0) {
                    type2vtbl.put(type, vtblAddr);
                }
            }
            return vtblAddr;
        }
        return type2vtbl.getLong(type);
    }

    public static Type findDynamicTypeForAddress(long addr, Type baseType) {
        if (!isWindows) {
            return baseType;
        }
        if (vtblForType(baseType) == 0) {
            throw new InternalError(baseType + " does not appear to be polymorphic");
        } else {
            long loc1 = unsafe.getAddress(addr), loc2 = 0, loc3 = 0, offset2 = baseType.size;
            if (usingSharedSpaces) {
                @SuppressWarnings("DataFlowIssue")
                FileMapHeader header = FileMapInfo.getCurrent().getHeader();
                if (header.inCopiedVtableSpace(loc1)) {
                    return header.getTypeForVptrAddress(loc1);
                }
            }
            offset2 = offset2 - offset2 % unsafe.addressSize() - unsafe.addressSize();
            if (offset2 > 0L) {
                loc2 = unsafe.getAddress(addr + offset2);
            }
            long offset3 = offset2 - unsafe.addressSize();
            if (offset3 > 0L) {
                loc3 = unsafe.getAddress(addr + offset3);
            }
            Type loc2Match = null;
            Type loc3Match = null;
            for (Type type : types.values()) {
                Type superClass;
                for (superClass = type; !Objects.equals(superClass, baseType) && superClass != null; superClass = types.get(superClass.superName)) {
                }
                if (superClass != null) {
                    long vtblAddr = vtblForType(type);
                    if (vtblAddr != 0) {
                        if (vtblAddr == loc1) {
                            return type;
                        }
                        if (loc2 != 0 && loc2Match == null && vtblAddr == loc2) {
                            loc2Match = type;
                        }
                        if (loc3 != 0 && loc3Match == null && vtblAddr == loc3) {
                            loc3Match = type;
                        }
                    }
                }
            }
            if (loc2Match != null) {
                return loc2Match;
            } else {
                return loc3Match;
            }
        }
    }

//    public boolean isCore() {
//        return !(usingClientCompiler || usingServerCompiler);
//    }

    static {
        try {
            if (isHotspotJVM) {
                JVM = JVMUtil.findJvm();
                readVmTypes(readVmStructs());
                readVmIntConstants();
                readVmLongConstants();
                includeJVMCI = intConstant("INCLUDE_JVMCI") != 0;
                if (includeJVMCI){
                    readVmCITypes(readVmCIStructs());
                    readVmCIIntConstants();
                    readVmCILongConstants();
                }
                oopSize = intConstant("oopSize");
                intSize = type("int").size;
                longSize=type("long").size;
                size_tSize = type("size_t").size;
                heapWordSize = intConstant("HeapWordSize");
                isJVMTISupported = type("InstanceKlass").contains("_breakpoints");
                Type type = type("Method");
                if (type.contains("_from_compiled_entry")) {
                    if (types.containsKey("Matcher")) {
                        usingServerCompiler = true;
                        usingClientCompiler = false;
                    } else {
                        usingClientCompiler = true;
                        usingServerCompiler = false;
                    }
                } else {
                    usingClientCompiler = usingServerCompiler = false;
                }
                JVMFlag[] flags = JVMFlag.getAllFlags();
                boolean sharedSpaces = false, compressedOops = false, compressedClassPointers = false, TLAB = false;
                long PMRC=0;
                for (JVMFlag flag : flags) {
                    String name = flag.getName();
                    if ("UseSharedSpaces".equals(name)) {
                        if (flag.getAddress() != 0) {
                            sharedSpaces = true;
                        }
                    } else if ("UseCompressedOops".equals(name)) {
                        if (flag.getAddress() != 0) {
                            compressedOops = true;
                        }
                    } else if ("UseCompressedClassPointers".equals(name)) {
                        if (flag.getAddress() != 0) {
                            compressedClassPointers = true;
                        }
                    } else if ("UseTLAB".equals(name)) {
                        if (flag.getAddress() != 0) {
                            TLAB = true;
                        }
                    } else if ("PerMethodRecompilationCutoff".equals(name)) {
                        PMRC = unsafe.getLong(flag.getAddress());
                    }
                }
                usingSharedSpaces = sharedSpaces;
                usingCompressedOops = compressedOops;
                usingCompressedClassPointers = compressedClassPointers;
                usingTLAB = TLAB;
                invocationEntryBci=intConstant("InvocationEntryBci");
                PerMethodRecompilationCutoff=PMRC;
//                String cpu = getCPU();
//                Class<?> machDescClass = Class.forName("sun.jvm.hotspot.debugger.MachineDescription");
//                int pid=getPid();
//                Object tmp=null;
//                if (isWindows){
//                    Class<?> windbgDebuggerLocal=Class.forName("sun.jvm.hotspot.debugger.windbg.WindbgDebuggerLocal");
//                    MethodHandle debuggerConstructor = lookup.findConstructor(windbgDebuggerLocal, methodType(void.class, machDescClass, boolean.class));
//                    MethodHandle attach = lookup.findVirtual(windbgDebuggerLocal, "attach", methodType(void.class, int.class));
//                    Object machDesc = switch (cpu) {
//                        case "x86" ->
//                                Class.forName("sun.jvm.hotspot.debugger.MachineDescriptionIntelX86").getDeclaredConstructor().newInstance();
//                        case "amd64" ->
//                                Class.forName("sun.jvm.hotspot.debugger.MachineDescriptionAMD64").getDeclaredConstructor().newInstance();
//                        case "aarch64" ->
//                                Class.forName("sun.jvm.hotspot.debugger.MachineDescriptionAArch64").getDeclaredConstructor().newInstance();
//                        default -> throw new IllegalStateException("Win32 supported under x86, amd64 and aarch64 only");
//                    };
//                    Object debugger=debuggerConstructor.invoke(machDesc,false);
//                    attach.invoke(debugger,pid);
//                    tmp=debugger;
//                }
            } else {
                JVM = null;
                oopSize = intSize = size_tSize = heapWordSize =longSize= 0;
                isJVMTISupported = usingClientCompiler = usingServerCompiler = usingSharedSpaces = usingTLAB = includeJVMCI = false;
                usingCompressedOops = Unsafe.ARRAY_OBJECT_INDEX_SCALE == 4;
                boolean flag = true;
                for (String s : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                    if (s.contains("-UseCompressedClassPointers")) {
                        flag = false;
                        break;
                    }
                }
                usingCompressedClassPointers = flag;
                invocationEntryBci=-1;
                PerMethodRecompilationCutoff=-1;
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
}
