package apphhzp.lib.service;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.ClassOption;
import apphhzp.lib.CoremodHelper;
import apphhzp.lib.hotspot.Debugger;
import apphhzp.lib.hotspot.oops.AccessFlags;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.utilities.Dictionary;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static apphhzp.lib.ClassHelper.lookup;

public class ApphhzpLibService implements ITransformationService {
    //private static final String[] classes = new String[]{"/apphhzp/lib/api/ObjectInstrumentation.class", "/apphhzp/lib/api/ObjectMemoryMonitor.class", "/apphhzp/lib/ApphhzpLibMod.class", "/apphhzp/lib/ClassHelper.class", "/apphhzp/lib/ClassOption.class", "/apphhzp/lib/CoremodHelper.class", "/apphhzp/lib/helfy/Field$FakeField.class", "/apphhzp/lib/helfy/Field.class", "/apphhzp/lib/helfy/JVM.class", "/apphhzp/lib/helfy/Type$FakeType.class", "/apphhzp/lib/helfy/Type$UnknownType.class", "/apphhzp/lib/helfy/Type.class", "/apphhzp/lib/hotspot/cds/CDSFileMapRegion.class", "/apphhzp/lib/hotspot/cds/FileMapHeader.class", "/apphhzp/lib/hotspot/cds/FileMapInfo.class", "/apphhzp/lib/hotspot/ci/CiEnv.class", "/apphhzp/lib/hotspot/classfile/ClassDefiner.class", "/apphhzp/lib/hotspot/classfile/ClassFileParser$AnnotationCollector.class", "/apphhzp/lib/hotspot/classfile/ClassFileParser$ClassAnnotationCollector.class", "/apphhzp/lib/hotspot/classfile/ClassFileParser$FieldAllocationCount.class", "/apphhzp/lib/hotspot/classfile/ClassFileParser.class", "/apphhzp/lib/hotspot/classfile/ClassFileStream.class", "/apphhzp/lib/hotspot/classfile/ClassInstanceInfo.class", "/apphhzp/lib/hotspot/classfile/ClassLoadInfo.class", "/apphhzp/lib/hotspot/classfile/FieldLayoutInfo.class", "/apphhzp/lib/hotspot/classfile/JavaClasses$Class.class", "/apphhzp/lib/hotspot/classfile/JavaClasses$ClassLoader.class", "/apphhzp/lib/hotspot/classfile/JavaClasses$String.class", "/apphhzp/lib/hotspot/classfile/JavaClasses.class", "/apphhzp/lib/hotspot/classfile/KlassFactory.class", "/apphhzp/lib/hotspot/classfile/ModuleEntry.class", "/apphhzp/lib/hotspot/classfile/ModuleEntryTable.class", "/apphhzp/lib/hotspot/classfile/OopMapBlocksBuilder.class", "/apphhzp/lib/hotspot/classfile/PackageEntry.class", "/apphhzp/lib/hotspot/classfile/PackageEntryTable.class", "/apphhzp/lib/hotspot/classfile/ProtectionDomainCacheEntry.class", "/apphhzp/lib/hotspot/classfile/ProtectionDomainEntry.class", "/apphhzp/lib/hotspot/classfile/SystemDictionary.class", "/apphhzp/lib/hotspot/classfile/VMClasses.class", "/apphhzp/lib/hotspot/ClassModifier.class", "/apphhzp/lib/hotspot/code/blob/CodeBlob.class", "/apphhzp/lib/hotspot/code/blob/CompiledMethod$States.class", "/apphhzp/lib/hotspot/code/blob/CompiledMethod.class", "/apphhzp/lib/hotspot/code/blob/DeoptimizationBlob.class", "/apphhzp/lib/hotspot/code/blob/NMethod.class", "/apphhzp/lib/hotspot/code/blob/RuntimeStub.class", "/apphhzp/lib/hotspot/code/CodeCache.class", "/apphhzp/lib/hotspot/code/CodeHeap$BlobIterator.class", "/apphhzp/lib/hotspot/code/CodeHeap.class", "/apphhzp/lib/hotspot/code/HeapBlock$Header.class", "/apphhzp/lib/hotspot/code/HeapBlock.class", "/apphhzp/lib/hotspot/code/InterpreterCodelet.class", "/apphhzp/lib/hotspot/code/RelocInfo$Type.class", "/apphhzp/lib/hotspot/code/RelocInfo.class", "/apphhzp/lib/hotspot/code/RelocIterator.class", "/apphhzp/lib/hotspot/code/Stub.class", "/apphhzp/lib/hotspot/code/StubQueue$1.class", "/apphhzp/lib/hotspot/code/StubQueue.class", "/apphhzp/lib/hotspot/compiler/CompilerType.class", "/apphhzp/lib/hotspot/compiler/CompileTask.class", "/apphhzp/lib/hotspot/compiler/CompLevel.class", "/apphhzp/lib/hotspot/Debugger$1.class", "/apphhzp/lib/hotspot/Debugger$A.class", "/apphhzp/lib/hotspot/Debugger$B.class", "/apphhzp/lib/hotspot/Debugger.class", "/apphhzp/lib/hotspot/gc/AdaptiveWeightedAverage.class", "/apphhzp/lib/hotspot/gc/CollectedHeap.class", "/apphhzp/lib/hotspot/gc/g1/G1BiasedMappedArray.class", "/apphhzp/lib/hotspot/gc/g1/G1CMBitMap.class", "/apphhzp/lib/hotspot/gc/g1/G1CollectedHeap.class", "/apphhzp/lib/hotspot/gc/g1/G1CollectionSet.class", "/apphhzp/lib/hotspot/gc/g1/G1ConcurrentMark.class", "/apphhzp/lib/hotspot/gc/g1/G1EdenRegions.class", "/apphhzp/lib/hotspot/gc/g1/G1EvacStats.class", "/apphhzp/lib/hotspot/gc/g1/G1HeapRegionTable$HeapRegionIterator.class", "/apphhzp/lib/hotspot/gc/g1/G1HeapRegionTable.class", "/apphhzp/lib/hotspot/gc/g1/G1MonitoringSupport.class", "/apphhzp/lib/hotspot/gc/g1/G1RegionsOnNodes.class", "/apphhzp/lib/hotspot/gc/g1/G1SurvivorRegions.class", "/apphhzp/lib/hotspot/gc/g1/HeapRegion.class", "/apphhzp/lib/hotspot/gc/g1/HeapRegionClosure.class", "/apphhzp/lib/hotspot/gc/g1/HeapRegionManager.class", "/apphhzp/lib/hotspot/gc/g1/HeapRegionSetBase.class", "/apphhzp/lib/hotspot/gc/g1/HeapRegionType.class", "/apphhzp/lib/hotspot/gc/g1/HumongousReclaimCandidates.class", "/apphhzp/lib/hotspot/gc/g1/LiveRegionsClosure.class", "/apphhzp/lib/hotspot/gc/g1/LiveRegionsProvider.class", "/apphhzp/lib/hotspot/gc/MarkBitMap.class", "/apphhzp/lib/hotspot/gc/ObjectClosure.class", "/apphhzp/lib/hotspot/gc/PLABStats.class", "/apphhzp/lib/hotspot/gc/PreservedMarksSet.class", "/apphhzp/lib/hotspot/gc/ThreadLocalAllocBuffer.class", "/apphhzp/lib/hotspot/interpreter/AbstractInterpreter.class", "/apphhzp/lib/hotspot/interpreter/InvocationCounter.class", "/apphhzp/lib/hotspot/JVMObject.class", "/apphhzp/lib/hotspot/JVMUtil.class", "/apphhzp/lib/hotspot/memory/MemRegion.class", "/apphhzp/lib/hotspot/memory/MetaspaceObj.class", "/apphhzp/lib/hotspot/memory/ReferenceType.class", "/apphhzp/lib/hotspot/memory/Universe.class", "/apphhzp/lib/hotspot/memory/VirtualSpace.class", "/apphhzp/lib/hotspot/NativeLibrary.class", "/apphhzp/lib/hotspot/oops/AccessFlags.class", "/apphhzp/lib/hotspot/oops/Annotations.class", "/apphhzp/lib/hotspot/oops/BreakpointInfo.class", "/apphhzp/lib/hotspot/oops/ClassLoaderData$KlassVisitor.class", "/apphhzp/lib/hotspot/oops/ClassLoaderData$ModulesVisitor.class", "/apphhzp/lib/hotspot/oops/ClassLoaderData$PackagesVisitor.class", "/apphhzp/lib/hotspot/oops/ClassLoaderData.class", "/apphhzp/lib/hotspot/oops/ClassLoaderDataGraph.class", "/apphhzp/lib/hotspot/oops/constant/ClassConstant.class", "/apphhzp/lib/hotspot/oops/constant/Constant.class", "/apphhzp/lib/hotspot/oops/constant/ConstantPool.class", "/apphhzp/lib/hotspot/oops/constant/ConstantPoolCache$1.class", "/apphhzp/lib/hotspot/oops/constant/ConstantPoolCache.class", "/apphhzp/lib/hotspot/oops/constant/ConstantPoolCacheEntry.class", "/apphhzp/lib/hotspot/oops/constant/ConstantTag.class", "/apphhzp/lib/hotspot/oops/constant/DoubleConstant.class", "/apphhzp/lib/hotspot/oops/constant/FieldRefConstant.class", "/apphhzp/lib/hotspot/oops/constant/FloatConstant.class", "/apphhzp/lib/hotspot/oops/constant/IntegerConstant.class", "/apphhzp/lib/hotspot/oops/constant/InvokeDynamicConstant.class", "/apphhzp/lib/hotspot/oops/constant/LongConstant.class", "/apphhzp/lib/hotspot/oops/constant/MethodHandleConstant.class", "/apphhzp/lib/hotspot/oops/constant/MethodRefConstant.class", "/apphhzp/lib/hotspot/oops/constant/MethodTypeConstant.class", "/apphhzp/lib/hotspot/oops/constant/NameAndTypeConstant.class", "/apphhzp/lib/hotspot/oops/constant/StringConstant.class", "/apphhzp/lib/hotspot/oops/constant/Utf8Constant.class", "/apphhzp/lib/hotspot/oops/FieldInfo.class", "/apphhzp/lib/hotspot/oops/HeapVisitor.class", "/apphhzp/lib/hotspot/oops/IntArray$1.class", "/apphhzp/lib/hotspot/oops/IntArray.class", "/apphhzp/lib/hotspot/oops/klass/ArrayKlass.class", "/apphhzp/lib/hotspot/oops/klass/InstanceKlass$MiscFlags.class", "/apphhzp/lib/hotspot/oops/klass/InstanceKlass.class", "/apphhzp/lib/hotspot/oops/klass/InstanceMirrorKlass.class", "/apphhzp/lib/hotspot/oops/klass/Klass$LayoutHelper.class", "/apphhzp/lib/hotspot/oops/klass/Klass.class", "/apphhzp/lib/hotspot/oops/klass/KlassID.class", "/apphhzp/lib/hotspot/oops/klass/ObjArrayKlass.class", "/apphhzp/lib/hotspot/oops/klass/TypeArrayKlass.class", "/apphhzp/lib/hotspot/oops/MarkWord.class", "/apphhzp/lib/hotspot/oops/Metadata.class", "/apphhzp/lib/hotspot/oops/method/CheckedExceptionElement.class", "/apphhzp/lib/hotspot/oops/method/ConstMethod.class", "/apphhzp/lib/hotspot/oops/method/ExceptionTableElement.class", "/apphhzp/lib/hotspot/oops/method/LocalVariableTableElement.class", "/apphhzp/lib/hotspot/oops/method/Method.class", "/apphhzp/lib/hotspot/oops/method/MethodParametersElement.class", "/apphhzp/lib/hotspot/oops/MethodCounters.class", "/apphhzp/lib/hotspot/oops/MethodData.class", "/apphhzp/lib/hotspot/oops/ObjectHeap$LiveRegionsCollector.class", "/apphhzp/lib/hotspot/oops/ObjectHeap$ObjectFilter.class", "/apphhzp/lib/hotspot/oops/ObjectHeap.class", "/apphhzp/lib/hotspot/oops/oop/ArrayOopDesc.class", "/apphhzp/lib/hotspot/oops/oop/InstanceOopDesc.class", "/apphhzp/lib/hotspot/oops/oop/ObjArrayOopDesc.class", "/apphhzp/lib/hotspot/oops/oop/Oop.class", "/apphhzp/lib/hotspot/oops/oop/OopDesc$TransformHelper.class", "/apphhzp/lib/hotspot/oops/oop/OopDesc.class", "/apphhzp/lib/hotspot/oops/oop/TypeArrayOopDesc.class", "/apphhzp/lib/hotspot/oops/oop/WeakHandle.class", "/apphhzp/lib/hotspot/oops/OopMapBlock.class", "/apphhzp/lib/hotspot/oops/RecordComponent.class", "/apphhzp/lib/hotspot/oops/Symbol.class", "/apphhzp/lib/hotspot/oops/SymbolCreater.class", "/apphhzp/lib/hotspot/oops/U1Array$1.class", "/apphhzp/lib/hotspot/oops/U1Array.class", "/apphhzp/lib/hotspot/oops/U2Array$1.class", "/apphhzp/lib/hotspot/oops/U2Array.class", "/apphhzp/lib/hotspot/oops/VMTypeArray$1.class", "/apphhzp/lib/hotspot/oops/VMTypeArray.class", "/apphhzp/lib/hotspot/opto/Compile.class", "/apphhzp/lib/hotspot/prims/JvmtiCachedClassFileData.class", "/apphhzp/lib/hotspot/prims/VMIntrinsics.class", "/apphhzp/lib/hotspot/runtime/AdapterHandlerEntry.class", "/apphhzp/lib/hotspot/runtime/Arguments.class", "/apphhzp/lib/hotspot/runtime/CompilerThread.class", "/apphhzp/lib/hotspot/runtime/JavaThread.class", "/apphhzp/lib/hotspot/runtime/JavaThreadState.class", "/apphhzp/lib/hotspot/runtime/JDKVersion.class", "/apphhzp/lib/hotspot/runtime/JVMFlag.class", "/apphhzp/lib/hotspot/runtime/MonitorList$1.class", "/apphhzp/lib/hotspot/runtime/MonitorList.class", "/apphhzp/lib/hotspot/runtime/ObjectMonitor.class", "/apphhzp/lib/hotspot/runtime/Thread.class", "/apphhzp/lib/hotspot/runtime/ThreadShadow.class", "/apphhzp/lib/hotspot/runtime/ThreadsList.class", "/apphhzp/lib/hotspot/runtime/VMVersion.class", "/apphhzp/lib/hotspot/stream/AllFieldStream.class", "/apphhzp/lib/hotspot/stream/CompressedLineNumberReadStream.class", "/apphhzp/lib/hotspot/stream/CompressedReadStream.class", "/apphhzp/lib/hotspot/stream/CompressedStream.class", "/apphhzp/lib/hotspot/stream/FieldStreamBase.class", "/apphhzp/lib/hotspot/stream/InternalFieldStream.class", "/apphhzp/lib/hotspot/stream/JavaFieldStream.class", "/apphhzp/lib/hotspot/Test.class", "/apphhzp/lib/hotspot/TestSuper.class", "/apphhzp/lib/hotspot/utilities/BasicHashtable.class", "/apphhzp/lib/hotspot/utilities/BasicHashtableEntry.class", "/apphhzp/lib/hotspot/utilities/BasicType.class", "/apphhzp/lib/hotspot/utilities/BitMap.class", "/apphhzp/lib/hotspot/utilities/BitMapView.class", "/apphhzp/lib/hotspot/utilities/Dictionary.class", "/apphhzp/lib/hotspot/utilities/DictionaryEntry.class", "/apphhzp/lib/hotspot/utilities/Hashtable.class", "/apphhzp/lib/hotspot/utilities/HashtableBucket.class", "/apphhzp/lib/hotspot/utilities/HashtableEntry.class", "/apphhzp/lib/hotspot/utilities/VMTypeGrowableArray$1.class", "/apphhzp/lib/hotspot/utilities/VMTypeGrowableArray.class", "/apphhzp/lib/instrumentation/ObjectInstrumentationImpl.class", "/apphhzp/lib/natives/Kernel32.class", "/apphhzp/lib/natives/NativeUtil.class", "/apphhzp/lib/PlatformInfo.class", "/apphhzp/lib/service/ApphhzpLibService.class"};

    static {
//        try {
//            for (String name : classes) {
//                InputStream is = ApphhzpLibService.class.getResourceAsStream(name);
//                byte[] b = new byte[is.available()];
//                is.read(b);
//                is.close();
//                name = name.replace('/', '.').substring(1, name.length() - 6);
//                if (ClassHelper.findLoadedClass(ApphhzpLibService.class.getClassLoader(), name) == null) {
//                    ClassHelper.defineClass(name, b, ApphhzpLibService.class.getClassLoader());
//                }
//            }
//        } catch (Throwable t) {
//            throw new RuntimeException(t);
//        }
        ClassHelper.defineClassesFromJar(ApphhzpLibService.class,s->true);
        CoremodHelper.coexist(ApphhzpLibService.class);
    }

    @Override
    public String name() {
        return "apphhzpLIB";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {
    }

    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}
