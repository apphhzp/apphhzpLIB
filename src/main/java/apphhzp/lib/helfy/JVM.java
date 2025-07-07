package apphhzp.lib.helfy;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.hotspot.JVMUtil;
import apphhzp.lib.hotspot.NativeLibrary;
import apphhzp.lib.hotspot.cds.FileMapHeader;
import apphhzp.lib.hotspot.cds.FileMapInfo;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.*;
import apphhzp.lib.hotspot.runtime.Thread;
import com.sun.jna.*;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.util.*;

import static apphhzp.lib.ClassHelperSpecial.*;

public final class JVM {
    public static final boolean ENABLE_EXTRA_CHECK =true;
    private static final NativeLibrary JVM;
    public static final String cpu=PlatformInfo.getCPU();
    private static final Map<String, Type> types = new LinkedHashMap<>();
    private static final Map<String, Number> constants = new LinkedHashMap<>();
    private static final Object2LongMap<String> functions = new Object2LongOpenHashMap<>();
    private static final Set<String> jvmciOnlyConstants=new LinkedHashSet<>();
    public static final int oopSize;
    public static final int intSize;
    public static final int longSize;
    public static final int size_tSize;
    public static final int floatSize;
    public static final int doubleSize;
    public static final int unsignedSize;
    public static final boolean includeJVMTI;
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
    public static final int LogBytesPerWord;
    public static final int LogBytesPerLong    = 3;
    public static final int BytesPerShort      = 1 << LogBytesPerShort;
    public static final int BytesPerInt        = 1 << LogBytesPerInt;
    public static final int BytesPerWord;
    public static final int BytesPerLong       = 1 << LogBytesPerLong;
    public static final int LogBitsPerByte     = 3;
    public static final int LogBitsPerShort    = LogBitsPerByte + LogBytesPerShort;
    public static final int LogBitsPerInt      = LogBitsPerByte + LogBytesPerInt;
    public static final int LogBitsPerWord;
    public static final int LogBitsPerLong     = LogBitsPerByte + LogBytesPerLong;
    public static final int BitsPerByte        = 1 << LogBitsPerByte;
    public static final int BitsPerShort       = 1 << LogBitsPerShort;
    public static final int BitsPerInt         = 1 << LogBitsPerInt;
    public static final int BitsPerWord;
    public static final int BitsPerLong        = 1 << LogBitsPerLong;
    public static final int WordAlignmentMask;
    public static final int LongAlignmentMask  = (1 << LogBytesPerLong) - 1;
    public static final int LogHeapWordSize;
    public static final int HeapWordsPerLong;
    public static final int LogHeapWordsPerLong;
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
    public static final boolean includeAssert;
    public static final boolean usePerfData;
    public static final boolean specialAlignment;
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

    private static void readVmCIAddresses(){
        long entry = getSymbol("jvmciHotSpotVMAddresses");
        long nameOffset = 0;
        long valueOffset = oopSize;
        long arrayStride = 2L *oopSize;
        for (; ; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;
            if (!functions.containsKey(name)) {
                long value = unsafe.getAddress(entry + valueOffset);
                functions.put(name, value);
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
        long address = JVM.lookup(name);
        if (address == 0) {
            throw new NoSuchElementException("No such symbol: " + name);
        }
        return unsafe.getLong(address);
    }

    public static long lookupSymbol(String name) {
        return JVM.lookup(name);
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

    public static long function(String name){
        return includeJVMCI?functions.getLong(name):0;
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

    public static void printAllFunctions(){
        if (!isHotspotJVM){
            System.err.println("NO FUNCTIONS");
        }
        for (Object2LongMap.Entry<String> entry : functions.object2LongEntrySet()) {
            System.err.println("&"+entry.getKey()+" = 0x"+Long.toHexString(entry.getLongValue()));
        }
        System.err.println("over");
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

    private static final String vt;

    public static String vtblSymbolForType(Type type) {
        if (!isWindows&&vt==null) {
            throw new IllegalStateException("Unsupported OS");
        }
        return isWindows?"??_7" + type.name + "@@6B@":vt+type.name.length()+type.name;
    }

    public static long getVtblForType(Type type) {
        if (type == null || (!isWindows&&vt==null)) {
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
                    long addr = JVM.lookup(vtblSymbol);
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
        if (!isWindows&&vt==null) {
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

    public static long align_metadata_size(long size){
        return alignUp(size,1);
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

    public static void assertOffset(long expectation, long actuality){
        if (expectation!=actuality){
            throw new AssertionError("Unexpected offset! expectation: "+expectation+" actuality: "+actuality);
        }
    }

    public static int extract_low_short_from_int(int x) {
        return x & 0xffff;
    }

    public static int extract_high_short_from_int(int x) {
        return (x >> 16) & 0xffff;
    }

    public static int build_int_from_shorts( int low, int high ) {
        return (((high&0xffff) << 16) | (low&0xffff));
    }
    private static final HashMap<String,JVMFlag> flagsCache=new HashMap<>();
    public static JVMFlag getFlag(String str){
        return flagsCache.computeIfAbsent(str,(name)->{
            for (JVMFlag flag:JVMFlag.getAllFlags()){
                if (flag.getName().equals(name)){
                    return flag;
                }
            }
            return null;
        });
    }

    public static final class Functions{
        private static final Object unused=new Object();
        private static final long unused_space=OopDesc.getAddress(unused);
        public static final Function identity_hash_code_function;
        public static final Function log_object_function;
        public static final Function validate_object_function;
        public static final Function object_notify_function;
        public static final Function object_notifyAll_function;
        public static final Function invoke_static_method_one_arg_function;
        public static final Function test_deoptimize_call_int_function;
        //public static final Function dynamic_new_instance_function=Function.getFunction(new Pointer(function("JVMCIRuntime::dynamic_new_instance")));
        //public static final Function dynamic_new_array_function=Function.getFunction(new Pointer(function("JVMCIRuntime::dynamic_new_array")));
        public static final Function uncommon_trap_function;
        public static final Function dll_lookup_function;
        public static final Function dll_load_function;
        public static final Function javaTimeNanos_function;
        public static final Function javaTimeMillis_function;
        public static final Function throw_class_cast_exception_function;
        public static final Function throw_and_post_jvmti_exception_function;
        public static final Function throw_klass_external_name_exception_function;
        public static final Function vm_error_function;
        public static final Function log_primitive_function;

        static {
            if (includeJVMCI){
                identity_hash_code_function = Function.getFunction(new Pointer(function("JVMCIRuntime::identity_hash_code")));
                log_object_function = Function.getFunction(new Pointer(function("JVMCIRuntime::log_object")));
                validate_object_function = Function.getFunction(new Pointer(function("JVMCIRuntime::validate_object")));
                object_notify_function=Function.getFunction(new Pointer(function("JVMCIRuntime::object_notify")));
                object_notifyAll_function=Function.getFunction(new Pointer(function("JVMCIRuntime::object_notifyAll")));
                invoke_static_method_one_arg_function=Function.getFunction(new Pointer(function("JVMCIRuntime::invoke_static_method_one_arg")));
                test_deoptimize_call_int_function=Function.getFunction(new Pointer(function("JVMCIRuntime::test_deoptimize_call_int")));
                uncommon_trap_function=Function.getFunction(new Pointer(function("Deoptimization::uncommon_trap")));
                dll_lookup_function=Function.getFunction(new Pointer(function("os::dll_lookup")));
                dll_load_function=Function.getFunction(new Pointer(function("os::dll_load")));
                javaTimeNanos_function=Function.getFunction(new Pointer(function("os::javaTimeNanos")));
                javaTimeMillis_function=Function.getFunction(new Pointer(function("os::javaTimeMillis")));
                throw_class_cast_exception_function=Function.getFunction(new Pointer(function("JVMCIRuntime::throw_class_cast_exception")));
                throw_and_post_jvmti_exception_function=Function.getFunction(new Pointer(function("JVMCIRuntime::throw_and_post_jvmti_exception")));
                throw_klass_external_name_exception_function=Function.getFunction(new Pointer(function("JVMCIRuntime::throw_klass_external_name_exception")));
                vm_error_function=Function.getFunction(new Pointer(function("JVMCIRuntime::vm_error")));
                log_primitive_function=Function.getFunction(new Pointer(function("JVMCIRuntime::log_primitive")));
            }else {
                identity_hash_code_function=log_object_function=validate_object_function=object_notify_function
                        = object_notifyAll_function=invoke_static_method_one_arg_function=test_deoptimize_call_int_function
                        =uncommon_trap_function=dll_lookup_function=dll_load_function=javaTimeNanos_function
                        =javaTimeMillis_function=throw_class_cast_exception_function=throw_and_post_jvmti_exception_function
                        =throw_klass_external_name_exception_function=vm_error_function=log_primitive_function=null;
            }
        }

        private Functions(){throw new UnsupportedOperationException();}

        public static long identity_hash_code(Object oop){
            if (identity_hash_code_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }

            return identity_hash_code_function.invokeLong(new Object[]{unused_space,OopDesc.getAddress(oop)});
        }

        public static void log_object(Object oop,boolean as_string,boolean newline){
            if (log_object_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            log_object_function.invokeVoid(new Object[]{unused_space, OopDesc.getAddress(oop),as_string,newline});
        }
        public static boolean validate_object(Object parent,Object child){
            if (validate_object_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return (boolean) validate_object_function.invoke(boolean.class,new Object[]{unused_space,OopDesc.getAddress(parent),OopDesc.getAddress(child)});
        }

        //Object.notify() fast path, caller does slow path
        public static boolean object_notify(JavaThread javaThread,Object oop){
            if (object_notify_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return (boolean) object_notify_function.invoke(boolean.class,new Object[]{javaThread.address,OopDesc.getAddress(oop)});
        }

        public static boolean object_notifyAll(JavaThread javaThread,Object oop){
            if (object_notifyAll_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return (boolean) object_notifyAll_function.invoke(boolean.class,new Object[]{javaThread.address,OopDesc.getAddress(oop)});
        }

        //Object返回值存储在javaThread->_vm_result里
        public static long invoke_static_method_one_arg(JavaThread javaThread,Method method,long arg){
            if (invoke_static_method_one_arg_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return invoke_static_method_one_arg_function.invokeLong(new Object[]{javaThread.address,method.address,arg});
        }

        public static int test_deoptimize_call_int(int value){
            if (test_deoptimize_call_int_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return test_deoptimize_call_int_function.invokeInt(new Object[]{JavaThread.first().address,value});
        }

//        public static Object dynamic_new_instance(JavaThread javaThread,Object type_mirror){
//            dynamic_new_instance_function.invokeVoid(new Object[]{javaThread.address,OopDesc.getAddress(type_mirror)});
//            return javaThread.getVMResult().getObject();
//        }
//
//        public static Object dynamic_new_array(JavaThread javaThread,Object type_mirror,int length){
//            dynamic_new_array_function.invokeVoid(new Object[]{javaThread.address,OopDesc.getAddress(type_mirror),length});
//            return javaThread.getVMResult().getObject();
//        }

        public static Deoptimization.UnrollBlock uncommon_trap(JavaThread current, int trap_request, int exec_mode){
            if (uncommon_trap_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return new Deoptimization.UnrollBlock(Pointer.nativeValue(uncommon_trap_function.invokePointer(new Object[]{current.address,trap_request,exec_mode})));
        }


        public static long dll_lookup(long handle, String name){
            if (dll_lookup_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return Pointer.nativeValue(dll_lookup_function.invokePointer(new Object[]{handle,name}));
        }

        public static long dll_load(String name,long ebuf, int ebuflen){
            if (dll_load_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return Pointer.nativeValue(dll_load_function.invokePointer(new Object[]{name,ebuf,ebuflen}));
        }

        public static long javaTimeNanos(){
            if (javaTimeNanos_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return javaTimeNanos_function.invokeLong(new Object[]{});
        }

        public static long javaTimeMillis(){
            if (javaTimeMillis_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return javaTimeMillis_function.invokeLong(new Object[]{});
        }

        public static int throw_class_cast_exception(JavaThread current, String exception, Klass caster_klass, Klass target_klass){
            if (throw_class_cast_exception_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return throw_class_cast_exception_function.invokeInt(new Object[]{current.address,exception,caster_klass.address,target_klass.address});
        }

        public static int throw_and_post_jvmti_exception(JavaThread current,String exception,String message){
            if (throw_and_post_jvmti_exception_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            return throw_and_post_jvmti_exception_function.invokeInt(new Object[]{current.address,exception,message});
        }

        public static void vm_error(JavaThread current, long where, long format, long value){
            if (vm_error_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            vm_error_function.invokeVoid(new Object[]{current.address,where,format,value});
        }

        public static void log_primitive(char typeChar, long value, boolean newline){
            if (log_primitive_function==null){
                throw new UnsupportedOperationException("includeJVMCI==false");
            }
            log_primitive_function.invokeVoid(new Object[]{unused_space,typeChar,value,newline});
        }
    }

    static {
        try {

            if (isHotspotJVM) {
                JVM = JVMUtil.findJvm();
                readVmTypes(readVmStructs());
                readVmIntConstants();
                readVmLongConstants();
                includeJVMCI = intConstant("INCLUDE_JVMCI") == 1&&lookupSymbol("jvmciHotSpotVMTypes")!=0L;
                oopSize = intConstant("oopSize");
                if (includeJVMCI){
                    readVmCITypes(readVmCIStructs());
                    readVmCIIntConstants();
                    readVmCILongConstants();
                    readVmCIAddresses();
                }

                intSize = type("int").size;
                longSize=type("long").size;
                size_tSize = type("size_t").size;
                floatSize=type("jfloat").size;
                doubleSize=type("jdouble").size;
                unsignedSize=type("unsigned").size;
                includeJVMTI = type("InstanceKlass").contains("_breakpoints");
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

                if (!isWindows){
                    if (lookupSymbol("__vt_10JavaThread")!=0L){
                        vt="__vt_";
                    }else if (lookupSymbol("_vt_10JavaThread")!=0L){
                        vt="_vt_";
                    }else if (lookupSymbol("_ZTV10JavaThread")!=0L){
                        vt="_ZTV";
                    }else {
                        vt=null;
                    }
                }else {
                    vt=null;
                }

                JVMFlag[] flags = JVMFlag.getAllFlags();
//                boolean sharedSpaces = false, compressedOops = false, compressedClassPointers = false, TLAB = false,
//                        RestrictContended=false, RestrictReservedStack=false,EnableContended =false,
//                        DumpSharedSpaces=false,BytecodeVerificationRemote=false,
//                        BytecodeVerificationLocal=false,INCLUDE_JFR=false,ClassUnloading=false,ExtensiveErrorReports=false,
//                        UsePerfData=false;
//                long PMRC=0,CodeEntryAlignment=0;
//                int DiagnoseSyncOnValueBasedClasses=0,OAIB=8;
                for (JVMFlag flag : flags) {
                    String name = flag.getName();
//                    if ("UseSharedSpaces".equals(name)) {
//                        sharedSpaces = flag.getBool();
//                    } else if ("UseCompressedOops".equals(name)) {
//                        compressedOops=flag.getBool();
//                    } else if ("UseCompressedClassPointers".equals(name)) {
//                        compressedClassPointers=flag.getBool();
//                    } else if ("UseTLAB".equals(name)) {
//                        TLAB= flag.getBool();
//                    } else if ("PerMethodRecompilationCutoff".equals(name)) {
//                        PMRC=flag.getIntx();
//                    }else if ("ObjectAlignmentInBytes".equals(name)){
//                        OAIB=(int) flag.getIntx();
//                    }else if ("RestrictContended".equals(name)){
//                        RestrictContended=flag.getBool();
//                    }else if ("RestrictReservedStack".equals(name)){
//                        RestrictReservedStack=flag.getBool();
//                    }else if ("EnableContended".equals(name)){
//                        EnableContended=flag.getBool();
//                    }else if ("DiagnoseSyncOnValueBasedClasses".equals(name)){
//                        DiagnoseSyncOnValueBasedClasses=(int) flag.getIntx();
//                    }else if ("DumpSharedSpaces".equals(name)){
//                        DumpSharedSpaces=flag.getBool();
//                    }else if ("BytecodeVerificationRemote".equals(name)){
//                        BytecodeVerificationRemote=flag.getBool();
//                    }else if ("BytecodeVerificationLocal".equals(name)){
//                        BytecodeVerificationLocal=flag.getBool();
//                    }else if ("FlightRecorder".equals(name)){
//                        INCLUDE_JFR=true;
//                    }else if ("ClassUnloading".equals(name)){
//                        ClassUnloading=flag.getBool();
//                    }else if("ExtensiveErrorReports".equals(name)){
//                        ExtensiveErrorReports=flag.getBool();
//                    }else if ("CodeEntryAlignment".equals(name)){
//                        CodeEntryAlignment=flag.getIntx();
//                    }else if ("UsePerfData".equals(name)){
//                        UsePerfData=flag.getBool();
//                    }
                    System.err.println(name);
                }
                usingSharedSpaces = getFlag("UseSharedSpaces").getBool();
                usingCompressedOops = getFlag("UseCompressedOops").getBool();
                usingCompressedClassPointers = getFlag("UseCompressedClassPointers").getBool();
                usingTLAB = getFlag("UseTLAB").getBool();
                invocationEntryBci=intConstant("InvocationEntryBci");
                PerMethodRecompilationCutoff=getFlag("PerMethodRecompilationCutoff").getIntx();
                objectAlignmentInBytes= (int) getFlag("ObjectAlignmentInBytes").getIntx();
                logMinObjAlignmentInBytes= Integer.numberOfTrailingZeros(objectAlignmentInBytes);
                if (usingCompressedOops){
                    heapOopSize=intSize;
                }else {
                    heapOopSize=oopSize;
                }
                LogBytesPerWord=intConstant("LogBytesPerWord");
                isLP64= LogBytesPerWord==3;
                BytesPerWord       = intConstant("BytesPerWord");
                LogBitsPerWord     = LogBitsPerByte + LogBytesPerWord;
                BitsPerWord        = 1 << LogBitsPerWord;
                WordAlignmentMask  = (1 << LogBytesPerWord) - 1;
                LogHeapWordSize=intConstant("LogHeapWordSize");
                LogHeapWordsPerLong = LogBytesPerLong - LogHeapWordSize;
                HeapWordsPerLong=BytesPerLong / oopSize;
                restrictContended=getFlag("RestrictContended").getBool();
                restrictReservedStack=getFlag("RestrictReservedStack").getBool();
                enableContended=getFlag("EnableContended").getBool();
                diagnoseSyncOnValueBasedClasses= (int) getFlag("DiagnoseSyncOnValueBasedClasses").getIntx();
                dumpSharedSpaces=getFlag("DumpSharedSpaces").getBool();
                bytecodeVerificationRemote=getFlag("BytecodeVerificationRemote").getBool();
                bytecodeVerificationLocal=getFlag("BytecodeVerificationLocal").getBool();
                wordSize=type("char*").size;
                includeCDS=types.containsKey("FileMapInfo");
                includeG1GC=types.containsKey("G1CollectedHeap");
                includeCDSJavaHeap=includeCDS&&includeG1GC&&isLP64&&!PlatformInfo.getOS().equals("win32");
                includeJFR=getFlag("FlightRecorder")!=null;
                classUnloading=getFlag("ClassUnloading").getBool();
                {
                    product=computeOffset(oopSize,includeJFR?type.offset("_flags")+4:type.offset("_flags")+2)==type.offset("_i2i_entry");
                }
                codeEntryAlignment=getFlag("CodeEntryAlignment").getIntx();
                includeAssert=intConstant("ConstantPool::CPCACHE_INDEX_TAG")!=0;
                usePerfData=getFlag("UsePerfData").getBool();
                specialAlignment=type("Arguments").global("_num_jvm_flags")<type("Arguments").global("_jvm_flags_array");
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
                vt=null;
                isLP64= "aarch64".equals(cpu) || "amd64".equals(cpu) || "x86_64".equals(cpu) || "ppc64".equals(cpu);
                JVM = null;
                codeEntryAlignment=0;
                LogHeapWordsPerLong=LogHeapWordSize=WordAlignmentMask=BitsPerWord=LogBitsPerWord=BytesPerWord=LogBytesPerWord=unsignedSize=floatSize=doubleSize=diagnoseSyncOnValueBasedClasses=logMinObjAlignmentInBytes=objectAlignmentInBytes = intSize = size_tSize = oopSize =longSize= 0;
                specialAlignment=usePerfData=includeAssert=product=classUnloading=includeJFR=includeCDSJavaHeap=includeCDS=bytecodeVerificationRemote=
                        bytecodeVerificationLocal=dumpSharedSpaces=includeG1GC= enableContended=restrictReservedStack=
                        restrictContended= includeJVMTI = usingClientCompiler = usingServerCompiler =
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
