package apphhzp.lib;


import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("ConstantValue")
public class OnlyInDefineClassHelper {
    private static String DIST= FMLLoader.getDist()==null?"null":FMLLoader.getDist().name();
    private static final String ONLYIN = "Lnet/minecraftforge/api/distmarker/OnlyIn;";
    private static final String ONLYINS = "Lnet/minecraftforge/api/distmarker/OnlyIns;";

    public static byte[] handle(byte[] codes,String name){
        if (DIST.equals("null")){
            if ((DIST=(FMLLoader.getDist()==null?"null":FMLLoader.getDist().name())).equals("null")){
                return codes;
            }
        }
        ClassNode classNode=CoremodHelper.bytes2ClassNote(codes,name);
        if (handle(classNode)){
            codes=CoremodHelper.classNote2bytes(classNode,false);
        }
        return codes;
    }

    public static boolean handle(final ClassNode classNode) {
        if (DIST.equals("null")){
            if ((DIST=(FMLLoader.getDist()==null?"null":FMLLoader.getDist().name())).equals("null")){
                return false;
            }
        }
        AtomicBoolean changes = new AtomicBoolean();
        if (remove(classNode.visibleAnnotations, DIST)) {
            throw new RuntimeException("Attempted to load class "+ classNode.name  + " for invalid dist "+ DIST);
        }
        if (classNode.interfaces!=null){
            unpack(classNode.visibleAnnotations).stream()
                    .filter(ann-> Objects.equals(ann.desc, ONLYIN))
                    .filter(ann->ann.values.indexOf("_interface") != -1)
                    .filter(ann->!Objects.equals(((String[])ann.values.get(ann.values.indexOf("value") + 1))[1], DIST))
                    .map(ann -> ((Type)ann.values.get(ann.values.indexOf("_interface") + 1)).getInternalName())
                    .forEach(intf -> {
                        if (classNode.interfaces.remove(intf)) {
                            changes.compareAndSet(false, true);
                        }
                    });

            //Remove Class level @OnlyIn/@OnlyIns annotations, this is important if anyone gets ambitious and tries to reflect an annotation with _interface set.
            if (classNode.visibleAnnotations != null) {
                Iterator<AnnotationNode> itr = classNode.visibleAnnotations.iterator();
                while (itr.hasNext()) {
                    AnnotationNode ann = itr.next();
                    if (Objects.equals(ann.desc, ONLYIN) || Objects.equals(ann.desc, ONLYINS)) {
                        itr.remove();
                        changes.compareAndSet(false, true);
                    }
                }
            }
        }

        Iterator<FieldNode> fields = classNode.fields.iterator();
        while(fields.hasNext())
        {
            FieldNode field = fields.next();
            if (remove(field.visibleAnnotations, DIST))
            {
                fields.remove();
                changes.compareAndSet(false, true);
            }
        }

        LambdaGatherer lambdaGatherer = new LambdaGatherer();
        Iterator<MethodNode> methods = classNode.methods.iterator();
        while(methods.hasNext())
        {
            MethodNode method = methods.next();
            if (remove(method.visibleAnnotations, DIST))
            {
                methods.remove();
                lambdaGatherer.accept(method);
                changes.compareAndSet(false, true);
            }
        }

        // remove dynamic synthetic lambda methods that are inside of removed methods
        for (List<Handle> dynamicLambdaHandles = lambdaGatherer.getDynamicLambdaHandles();
             !dynamicLambdaHandles.isEmpty(); dynamicLambdaHandles = lambdaGatherer.getDynamicLambdaHandles())
        {
            lambdaGatherer = new LambdaGatherer();
            methods = classNode.methods.iterator();
            while (methods.hasNext())
            {
                MethodNode method = methods.next();
                if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) continue;
                for (Handle dynamicLambdaHandle : dynamicLambdaHandles)
                {
                    if (method.name.equals(dynamicLambdaHandle.getName()) && method.desc.equals(dynamicLambdaHandle.getDesc()))
                    {
                        methods.remove();
                        lambdaGatherer.accept(method);
                        changes.compareAndSet(false, true);
                    }
                }
            }
        }
        return changes.get();
    }

    @SuppressWarnings("unchecked")
    private static List<AnnotationNode> unpack(final List<AnnotationNode> anns) {
        if (anns == null) return Collections.emptyList();
        List<AnnotationNode> ret = anns.stream().filter(ann->Objects.equals(ann.desc, ONLYIN)).collect(Collectors.toList());
        anns.stream().filter(ann->Objects.equals(ann.desc, ONLYINS) && ann.values != null)
                .map( ann -> (List<AnnotationNode>)ann.values.get(ann.values.indexOf("value") + 1))
                .filter(v -> v != null)
                .forEach(v -> v.forEach(ret::add));
        return ret;
    }

    private static boolean remove(final List<AnnotationNode> anns, final String side)
    {
        return unpack(anns).stream().
                filter(ann->Objects.equals(ann.desc, ONLYIN)).
                filter(ann-> !ann.values.contains("_interface")).
                anyMatch(ann -> !Objects.equals(((String[])ann.values.get(ann.values.indexOf("value")+1))[1], side));
    }

    private static class LambdaGatherer extends MethodVisitor {
        private static final Handle META_FACTORY = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false);
        private final List<Handle> dynamicLambdaHandles = new ArrayList<>();

        public LambdaGatherer() {
            super(Opcodes.ASM9);
        }

        public void accept(MethodNode method) {
            stream(method.instructions.iterator()).
                    filter(insnNode->insnNode.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN).
                    forEach(insnNode->insnNode.accept(this));
        }

        private static <T> Stream<T> stream(Iterator<T> iterator) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs)
        {
            if (META_FACTORY.equals(bsm))
            {
                Handle dynamicLambdaHandle = (Handle) bsmArgs[1];
                dynamicLambdaHandles.add(dynamicLambdaHandle);
            }
        }

        public List<Handle> getDynamicLambdaHandles()
        {
            return dynamicLambdaHandles;
        }
    }
}
