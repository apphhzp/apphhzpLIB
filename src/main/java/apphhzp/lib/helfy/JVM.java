package apphhzp.lib.helfy;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.JVMUtil;
import apphhzp.lib.hotspot.NativeLibrary;
import apphhzp.lib.hotspot.cds.FileMapHeader;
import apphhzp.lib.hotspot.cds.FileMapInfo;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.JVMFlag;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.runtime.ObjectMonitor;
import apphhzp.lib.hotspot.runtime.Thread;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.util.*;

import static apphhzp.lib.ClassHelper.*;

public final class JVM {
    public static final boolean ASSERTS_ENABLED=true;
    private static final NativeLibrary JVM;
    public static final String cpu=PlatformInfo.getCPU();
    private static final Map<String, Type> types = new LinkedHashMap<>();
    private static final Map<String, Number> constants = new LinkedHashMap<>();
    private static final Set<String> jvmciOnlyConstants=new LinkedHashSet<>();
    public static final int oopSize;
    public static final int intSize;
    public static final int longSize;
    public static final int size_tSize;
    public static final int floatSize;
    public static final int doubleSize;
    public static final int unsignedSize;
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
    public static final int objectAlignmentInBytes;
    public static final int logMinObjAlignmentInBytes;
    public static final int heapOopSize;
    public static final boolean isLP64;
    public static final int LogBytesPerShort   = 1;
    public static final int LogBytesPerInt     = 2;
    public static final int LogBytesPerWord="aarch64".equals(cpu) || "amd64".equals(cpu) || "x86_64".equals(cpu) || "ppc64".equals(cpu)?3:2;
    public static final int LogBytesPerLong    = 3;
    public static final int BytesPerShort      = 1 << LogBytesPerShort;
    public static final int BytesPerInt        = 1 << LogBytesPerInt;
    public static final int BytesPerWord       = 1 << LogBytesPerWord;
    public static final int BytesPerLong       = 1 << LogBytesPerLong;
    public static final int LogBitsPerByte     = 3;
    public static final int LogBitsPerShort    = LogBitsPerByte + LogBytesPerShort;
    public static final int LogBitsPerInt      = LogBitsPerByte + LogBytesPerInt;
    public static final int LogBitsPerWord     = LogBitsPerByte + LogBytesPerWord;
    public static final int LogBitsPerLong     = LogBitsPerByte + LogBytesPerLong;
    public static final int BitsPerByte        = 1 << LogBitsPerByte;
    public static final int BitsPerShort       = 1 << LogBitsPerShort;
    public static final int BitsPerInt         = 1 << LogBitsPerInt;
    public static final int BitsPerWord        = 1 << LogBitsPerWord;
    public static final int BitsPerLong        = 1 << LogBitsPerLong;
    public static final int WordAlignmentMask  = (1 << LogBytesPerWord) - 1;
    public static final int LongAlignmentMask  = (1 << LogBytesPerLong) - 1;
    public static final int LogHeapWordSize     = "aarch64".equals(cpu) || "amd64".equals(cpu) || "x86_64".equals(cpu) || "ppc64".equals(cpu)?3:2;
    public static final int HeapWordsPerLong;
    public static final int LogHeapWordsPerLong = LogBytesPerLong - LogHeapWordSize;
    public static final boolean restrictContended;
    public static final boolean enableContended;
    public static final boolean restrictReservedStack;
    public static final int diagnoseSyncOnValueBasedClasses;
    public static final boolean dumpSharedSpaces;
    public static final boolean bytecodeVerificationLocal;
    public static final boolean bytecodeVerificationRemote;
    public static final int wordSize;
    public static final boolean includeCDS;
    public static final boolean includeG1GC;
    public static final boolean includeCDSJavaHeap;
    public static final boolean includeJFR;
    public static final boolean classUnloading;
    public static final boolean product;
    public static final long codeEntryAlignment;
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
            fields.add(new Field(fieldName, typeString, offset, isStatic,true));
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
                types.put(entry1.getKey(),new Type.UnknownType(entry1.getKey(),entry1.getValue()));
            }else{
                //noinspection DataFlowIssue
                types.compute(entry1.getKey(), (k, old) -> new Type(old.name, old.superName, old.size, old.isOop, old.isInt, old.isUnsigned, updateOrCreateFields(entry1.getValue(), old)));
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

    private static Set<Field> updateOrCreateFields(@Nullable Set<Field> newFields, @Nullable Type oldType){
        if (oldType!=null){
            TreeSet<Field> re = new TreeSet<>(List.of(oldType.fields));
            if (newFields!=null){
                re.addAll(newFields);
            }
            return re;
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
            if (!constants.containsKey(name)) {
                constants.put(name, value);
                jvmciOnlyConstants.add(name);
            }
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
            if (!constants.containsKey(name)) {
                constants.put(name, value);
                jvmciOnlyConstants.add(name);
            }
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
            System.err.println((jvmciOnlyConstants.contains(type.getKey())?"(JVMCI)":"")+ type.getKey()+(type.getValue() instanceof Long?"(L)":"(I)") + "=" + type.getValue());
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

    public static long alignUp(long size, long alignment) {
        return size + alignment - 1 & -alignment;
    }

    public static long alignObjectSize(long size){
        return alignUp(size, objectAlignmentInBytes);
    }

    public static long alignDown(long size, long alignment) {
        return size & -alignment;
    }

    public static int nthBit(int n){
        return n >= BitsPerWord ? 0 : 1 << n;
    }

    public static int right_n_bits(int n){
        return nthBit(n) - 1;
    }

    public static char charAt(String s,int i){
        if (i==s.length()){
            return '\0';
        }
        return s.charAt(i);
    }
//    public boolean isCore() {
//        return !(usingClientCompiler || usingServerCompiler);
//    }
    /**
     * @param alignment 对于{@code struct}类型：其成员的对齐要求的最大值。
     *             对于基本类型：其占据空间大小。
     * @param originalOffset 不进行内存对齐时的原始偏移量（按{@code byte}计算）
     */
    public static long computeOffset(long alignment,long originalOffset){
        return (originalOffset+alignment-1)/alignment*alignment;
    }

    public static long[] computeOffsets(boolean has_vtbl_pointer,long[] alignments,long[] sizes){
        if (alignments.length!=sizes.length) {
            throw new IllegalArgumentException("alignments.length!=sizes.length");
        }
        long[] re=new long[sizes.length+(has_vtbl_pointer?1:0)];
        int offset=has_vtbl_pointer?1:0;
        if (has_vtbl_pointer){
            re[0]=oopSize;
        }
        for (int i=0;i<sizes.length;i++) {
            re[i+offset]=computeOffset(alignments[i],(i+offset>0?re[i+offset-1]:0)+(i>0?sizes[i-1]:0));
        }
        return has_vtbl_pointer?Arrays.copyOfRange(re,1,re.length):re;
    }

    static {
        try {
            isLP64= "aarch64".equals(cpu) || "amd64".equals(cpu) || "x86_64".equals(cpu) || "ppc64".equals(cpu);
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
                floatSize=type("jfloat").size;
                doubleSize=type("jdouble").size;
                unsignedSize=type("unsigned").size;
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
                boolean sharedSpaces = false, compressedOops = false, compressedClassPointers = false, TLAB = false,
                        RestrictContended=false, RestrictReservedStack=false,EnableContended =false,
                        DumpSharedSpaces=false,BytecodeVerificationRemote=false,
                        BytecodeVerificationLocal=false,INCLUDE_JFR=false,ClassUnloading=false,ExtensiveErrorReports=false;
                long PMRC=0,CodeEntryAlignment=0;
                int DiagnoseSyncOnValueBasedClasses=0,OAIB=8;
                for (JVMFlag flag : flags) {
                    String name = flag.getName();
                    if ("UseSharedSpaces".equals(name)) {
                        sharedSpaces = flag.getBool();
                    } else if ("UseCompressedOops".equals(name)) {
                        compressedOops=flag.getBool();
                    } else if ("UseCompressedClassPointers".equals(name)) {
                        compressedClassPointers=flag.getBool();
                    } else if ("UseTLAB".equals(name)) {
                        TLAB= flag.getBool();
                    } else if ("PerMethodRecompilationCutoff".equals(name)) {
                        PMRC=flag.getIntx();
                    }else if ("ObjectAlignmentInBytes".equals(name)){
                        OAIB=(int) flag.getIntx();
                    }else if ("RestrictContended".equals(name)){
                        RestrictContended=flag.getBool();
                    }else if ("RestrictReservedStack".equals(name)){
                        RestrictReservedStack=flag.getBool();
                    }else if ("EnableContended".equals(name)){
                        EnableContended=flag.getBool();
                    }else if ("DiagnoseSyncOnValueBasedClasses".equals(name)){
                        DiagnoseSyncOnValueBasedClasses=(int) flag.getIntx();
                    }else if ("DumpSharedSpaces".equals(name)){
                        DumpSharedSpaces=flag.getBool();
                    }else if ("BytecodeVerificationRemote".equals(name)){
                        BytecodeVerificationRemote=flag.getBool();
                    }else if ("BytecodeVerificationLocal".equals(name)){
                        BytecodeVerificationLocal=flag.getBool();
                    }else if ("FlightRecorder".equals(name)){
                        INCLUDE_JFR=true;
                    }else if ("ClassUnloading".equals(name)){
                        ClassUnloading=flag.getBool();
                    }else if("ExtensiveErrorReports".equals(name)){
                        ExtensiveErrorReports=flag.getBool();
                    }else if ("CodeEntryAlignment".equals(name)){
                        CodeEntryAlignment=flag.getIntx();
                    }
                    System.err.println(name);
                }
                usingSharedSpaces = sharedSpaces;
                usingCompressedOops = compressedOops;
                usingCompressedClassPointers = compressedClassPointers;
                usingTLAB = TLAB;
                invocationEntryBci=intConstant("InvocationEntryBci");
                PerMethodRecompilationCutoff=PMRC;
                objectAlignmentInBytes=OAIB;
                logMinObjAlignmentInBytes= Integer.numberOfTrailingZeros(OAIB);
                if (usingCompressedOops){
                    heapOopSize=intSize;
                }else {
                    heapOopSize=oopSize;
                }
                HeapWordsPerLong=BytesPerLong / oopSize;
                restrictContended=RestrictContended;
                restrictReservedStack=RestrictReservedStack;
                enableContended=EnableContended;
                diagnoseSyncOnValueBasedClasses=DiagnoseSyncOnValueBasedClasses;
                dumpSharedSpaces=DumpSharedSpaces;
                bytecodeVerificationRemote=BytecodeVerificationRemote;
                bytecodeVerificationLocal=BytecodeVerificationLocal;
                wordSize=type("char*").size;
                includeCDS=types.containsKey("FileMapInfo");
                includeG1GC=types.containsKey("G1CollectedHeap");
                includeCDSJavaHeap=includeCDS&&includeG1GC&&isLP64&&!PlatformInfo.getOS().equals("win32");
                includeJFR=INCLUDE_JFR;
                classUnloading=ClassUnloading;
                product=!ExtensiveErrorReports;
                codeEntryAlignment=CodeEntryAlignment;
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
                codeEntryAlignment=unsignedSize=floatSize=doubleSize=diagnoseSyncOnValueBasedClasses=logMinObjAlignmentInBytes=objectAlignmentInBytes = intSize = size_tSize = oopSize =longSize= 0;
                product=classUnloading=includeJFR=includeCDSJavaHeap=includeCDS=bytecodeVerificationRemote=
                        bytecodeVerificationLocal=dumpSharedSpaces=includeG1GC= enableContended=restrictReservedStack=
                        restrictContended=isJVMTISupported = usingClientCompiler = usingServerCompiler =
                        usingSharedSpaces = usingTLAB = includeJVMCI = false;
                usingCompressedOops = Unsafe.ARRAY_OBJECT_INDEX_SCALE == 4;
                HeapWordsPerLong=BytesPerLong / unsafe.addressSize();
                if (usingCompressedOops) {
                    heapOopSize=4;
                }else {
                    heapOopSize=unsafe.addressSize();
                }
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
                wordSize=oopSize;

            }

        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public static long pointerDelta(long left,
                                     long right,
                                     long element_size) {
        if (left<right){
            throw new IllegalArgumentException("avoid underflow - left: 0x"+Long.toHexString(left)+" right: 0x"+Long.toHexString(right));
        }
        return ((left) - (right)) / element_size;
    }

    public static long pointerDeltaHeapWord(long left, long right) {
        return pointerDelta(left,right,oopSize);
    }

    public static long pointerDeltaMetaWord(long left,long right) {
        return pointerDelta(left, right, oopSize);
    }
}
