package apphhzp.lib;

import apphhzp.lib.service.ApphhzpLibService;
import cpw.mods.modlauncher.api.NamedPath;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static apphhzp.lib.ClassHelper.lookup;

public class CoremodHelper{
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void coexist(Class<?> caller){
        try {

            Class<?> modDirTransformerDiscoverer = Class.forName("net.minecraftforge.fml.loading.ModDirTransformerDiscoverer");
            List<NamedPath> found = (List<NamedPath>) lookup.findStaticVarHandle(modDirTransformerDiscoverer,"found",List.class).get();
            found.removeIf(namedPath -> ClassHelper.getJarPath(caller).equals(namedPath.paths()[0].toString()));
            Class<?> launcher=Class.forName("cpw.mods.modlauncher.Launcher"),moduleLayerHandlerClass=Class.forName("cpw.mods.modlauncher.ModuleLayerHandler");
            Object INSTANCE=lookup.findStaticVarHandle(launcher,"INSTANCE",launcher).get();
            Object moduleLayerHandler=lookup.findVarHandle(launcher,"moduleLayerHandler",moduleLayerHandlerClass).get(INSTANCE);
            EnumMap map= (EnumMap) lookup.findVarHandle(moduleLayerHandlerClass,"completedLayers",EnumMap.class).get(moduleLayerHandler);
            Class<?> layerInfoClass=Class.forName("cpw.mods.modlauncher.ModuleLayerHandler$LayerInfo");
            //noinspection unchecked
            map.values().forEach(layerInfo -> {
                try {
                    ModuleLayer layer = (ModuleLayer) lookup.findVarHandle(layerInfoClass,"layer", ModuleLayer.class).get(layerInfo); //Helper.getFieldValue(layerInfo, "layer", ModuleLayer.class);
                    layer.modules().forEach(module->{
                        if (module.getName().equals(caller.getModule().getName())){
                            try {
                                VarHandle modulesVar=lookup.findVarHandle(Configuration.class,"modules", Set.class),name2ModuleVar= lookup.findVarHandle(Configuration.class,"nameToModule", Map.class);
                                Set<ResolvedModule> modules = new HashSet<>((Collection<ResolvedModule>)modulesVar.get(layer.configuration()));//Helper.getFieldValue(layer.configuration(), "modules", Set.class)
                                Map<String, ResolvedModule> nameToModule = new HashMap<>((Map<String, ResolvedModule>)name2ModuleVar.get(layer.configuration()));
                                modules.remove(nameToModule.remove(caller.getModule().getName()));
                                modulesVar.set(layer.configuration(),modules);
                                name2ModuleVar.set(layer.configuration(),nameToModule);
                            }catch (Throwable throwable){
                                throw new RuntimeException(throwable);
                            }
                        }
                    });
                }catch (Throwable throwable){
                    throw new RuntimeException(throwable);
                }
            });
        }catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static ClassNode bytes2ClassNote(byte[] bytes, final String name){
        final String internalName = name.replace('.', '/');
        final Type classDesc = Type.getObjectType(internalName);
        ClassNode clazz = new ClassNode(Opcodes.ASM9);
        if (bytes.length > 0) {
            final ClassReader classReader = new ClassReader(bytes);
            classReader.accept(clazz, 0);
        } else {
            clazz.name = classDesc.getInternalName();
            clazz.version = 52;
            clazz.superName = "java/lang/Object";
        }
        return clazz;
    }

    public static byte[] classNote2bytes(ClassNode classNode,boolean shouldComputeFrames){
        ClassWriter writer=new ClassWriter(shouldComputeFrames?ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES:ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
