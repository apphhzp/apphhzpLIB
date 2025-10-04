package apphhzp.lib;

import apphhzp.lib.api.stackframe.LiveStackFrameInfo;
import apphhzp.lib.api.stackframe.StackFrameInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;

public final class StackWalkerHelper {
    private static final Class<?> stackFrameInfoClass;
    public static final StackWalker IMPL_WALKER;
    public static final StackWalker retainRefWalker;
    public static final StackWalker extendedWalker;
    private static final Object ENUM_LOCALS_AND_OPERANDS;
    private static final MethodHandle stackWalkerConstructor;
    private static final MethodHandle traverserConstructor;
    private static final MethodHandle liveTraverserConstructor;
    private static final MethodHandle initFrameBufferMethod;
    private static final MethodHandle callStackWalkMethod;
    private static final MethodHandle framesMethod;
    private static final MethodHandle computeFormatMethod;
    private static final VarHandle walkerModeVar;
    private static final VarHandle frameBufferVar;
    private static final VarHandle startPosVar;
    private static final VarHandle currentBatchSizeVar;
    private static final VarHandle declaringClassObjectVar;
    private static final VarHandle SFIMemberName;
    private static final VarHandle extendedOptionVar;

    static{
        try {
            stackFrameInfoClass=Class.forName("java.lang.StackFrameInfo");
            Class<?> klass=Class.forName("java.lang.StackWalker$ExtendedOption");
            extendedOptionVar=lookup.findVarHandle(StackWalker.class,"extendedOption",klass);
            ENUM_LOCALS_AND_OPERANDS=lookup.findStaticVarHandle(klass,"LOCALS_AND_OPERANDS",klass).get();
            stackWalkerConstructor=lookup.findConstructor(StackWalker.class, MethodType.methodType(void.class, EnumSet.class, int.class,klass));
            IMPL_WALKER = constructStackWalker(EnumSet.of(StackWalker.Option.RETAIN_CLASS_REFERENCE,StackWalker.Option.SHOW_HIDDEN_FRAMES,StackWalker.Option.SHOW_REFLECT_FRAMES),0,ExtendedOption.LOCALS_AND_OPERANDS);
            retainRefWalker= constructStackWalker(EnumSet.of(StackWalker.Option.RETAIN_CLASS_REFERENCE),0,null);
            extendedWalker =constructStackWalker(EnumSet.of(StackWalker.Option.RETAIN_CLASS_REFERENCE),0,ExtendedOption.LOCALS_AND_OPERANDS);
            Class<?> abstractTraverser=Class.forName("java.lang.StackStreamFactory$AbstractStackWalker"),
                    frameBuffer=Class.forName("java.lang.StackStreamFactory$FrameBuffer");
            traverserConstructor=lookup.findConstructor(Class.forName("java.lang.StackStreamFactory$StackFrameTraverser"),MethodType.methodType(void.class,StackWalker.class, Function.class));
            liveTraverserConstructor=lookup.findConstructor(Class.forName("java.lang.StackStreamFactory$LiveStackInfoTraverser"),MethodType.methodType(void.class,StackWalker.class, Function.class));
            initFrameBufferMethod=lookup.findVirtual(abstractTraverser,"initFrameBuffer",MethodType.methodType(void.class));
            callStackWalkMethod=lookup.findVirtual(abstractTraverser,"callStackWalk",MethodType.methodType(Object.class,long.class,int.class,int.class,int.class,Object[].class));
            framesMethod=lookup.findVirtual(frameBuffer,"frames",MethodType.methodType(Object[].class));
            computeFormatMethod=lookup.findVirtual(StackTraceElement.class,"computeFormat",MethodType.methodType(void.class));

            walkerModeVar=lookup.findVarHandle(abstractTraverser,"mode",long.class);
            frameBufferVar=lookup.findVarHandle(abstractTraverser,"frameBuffer",frameBuffer);
            startPosVar =lookup.findStaticVarHandle(frameBuffer,"START_POS",int.class);
            currentBatchSizeVar=lookup.findVarHandle(frameBuffer,"currentBatchSize",int.class);
            declaringClassObjectVar=lookup.findVarHandle(StackTraceElement.class,"declaringClassObject",Class.class);
            SFIMemberName=lookup.findVarHandle(stackFrameInfoClass,"memberName", Object.class);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
    private StackWalkerHelper() {throw new UnsupportedOperationException();}
    public enum ExtendedOption {
        LOCALS_AND_OPERANDS
    }

    public static StackWalker constructStackWalker(Set<StackWalker.Option> options){
        return constructStackWalker(options,0,null);
    }

    public static StackWalker constructStackWalker(EnumSet<StackWalker.Option> options, int estimateDepth){
        return constructStackWalker(options,estimateDepth,null);
    }

    public static StackWalker constructStackWalker(Set<StackWalker.Option> options,int estimateDepth ,ExtendedOption extendedOption){
        try {
            Object obj=null;
            if (extendedOption==ExtendedOption.LOCALS_AND_OPERANDS){
                obj=ENUM_LOCALS_AND_OPERANDS;
            }
            return (StackWalker) stackWalkerConstructor.invoke(options,estimateDepth,obj);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static List<StackWalker.StackFrame> getStackFrames(){
        return getStackFrames(retainRefWalker);
    }

    public static List<StackWalker.StackFrame> getStackFrames(StackWalker walker){
        try {
            List<StackWalker.StackFrame> re=new ArrayList<>();
            final boolean[] flag = {false};
            Function<? super Stream<StackWalker.StackFrame>, ?> func=(x)->{
                x.forEach((obj)-> {
                    if (!obj.getClassName().equals("apphhzp.lib.StackWalkerHelper")||!obj.getMethodName().equals("getStackFrames")) {
                        if (flag[0]) {
                            re.add(obj);
                        }
                    }else {
                        flag[0] = true;
                    }
                });
                return null;
            };
            callStackWalk(extendedOptionVar.get(walker)==ENUM_LOCALS_AND_OPERANDS
                    ?liveTraverserConstructor.invoke(walker,func)
                    :traverserConstructor.invoke(walker,func));
            return re;
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static List<StackTraceElement> getStackTrace(){
        return getStackTrace(retainRefWalker);
    }

    public static List<StackTraceElement> getStackTrace(StackWalker walker){
        try {
            List<StackTraceElement> re=new ArrayList<>();
            final boolean[] flag = {false};
            Function<? super Stream<StackWalker.StackFrame>, ?> func=(x)->{
                x.forEach((obj)-> {
                    if (!obj.getClassName().equals("apphhzp.lib.StackWalkerHelper")||!obj.getMethodName().equals("getStackTrace")) {
                        if (flag[0]){
                            re.add(convertFrameToTrace(obj));
                        }
                    }else {
                        flag[0] =true;
                    }
                });
                return null;
            };
            callStackWalk(extendedOptionVar.get(walker)==ENUM_LOCALS_AND_OPERANDS
                    ?liveTraverserConstructor.invoke(walker,func)
                    :traverserConstructor.invoke(walker,func));
            return re;
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static List<LiveStackFrameInfo> getLiveStackFrame(){
        return getLiveStackFrame(extendedWalker);
    }

    public static List<LiveStackFrameInfo> getLiveStackFrame(StackWalker walker){
        if (extendedOptionVar.get(walker)!=ENUM_LOCALS_AND_OPERANDS){
            throw new IllegalArgumentException("Missing LOCALS_AND_OPERANDS option");
        }
        try {
            List<LiveStackFrameInfo> re=new ArrayList<>();
            final boolean[] flag = {false};
            Function<? super Stream<StackWalker.StackFrame>, ?> func=(x)->{
                x.forEach((obj)-> {
                    if (!obj.getClassName().equals("apphhzp.lib.StackWalkerHelper")||!obj.getMethodName().equals("getLiveStackFrame")) {
                        if (flag[0]){
                            re.add(new LiveStackFrameInfo(obj));
                        }
                    }else {
                        flag[0] =true;
                    }
                });
                return null;
            };
            callStackWalk(liveTraverserConstructor.invoke(walker,func));
            return re;
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public static StackTraceElement convertFrameToTrace(StackWalker.StackFrame frame){
        try {
            Class<?> declaringClass=stackFrameInfoClass.isInstance(frame)? StackFrameInfo.getDeclaringClass(SFIMemberName.get(frame)) :frame.getDeclaringClass();
            ModuleDescriptor.Version version=declaringClass.getModule().getDescriptor()==null?null:declaringClass.getModule().getDescriptor().version().orElse(null);
            StackTraceElement ste=new StackTraceElement(declaringClass.getClassLoader()==null?null:declaringClass.getClassLoader().getName(),declaringClass.getModule().getName(),version==null?null:version.toString(),declaringClass.getName(), frame.getMethodName(), frame.getFileName(), frame.getLineNumber());
            declaringClassObjectVar.set(ste,declaringClass);
            computeFormatMethod.invoke(ste);
            return ste;
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    private static void callStackWalk(Object traverser)throws Throwable{
        initFrameBufferMethod.invoke(traverser);
        Object buffer=frameBufferVar.get(traverser);
        callStackWalkMethod.invoke(traverser,(long)walkerModeVar.get(traverser),0,
                (int)currentBatchSizeVar.get(buffer)-(int)startPosVar.get(),
                (int)startPosVar.get(),
                framesMethod.invoke(buffer));
    }
}
