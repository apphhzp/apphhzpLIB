package apphhzp.lib;

import cpw.mods.modlauncher.api.NamedPath;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.lang.invoke.VarHandle;
import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.util.*;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;

public class CoremodHelper{
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void coexist(Class<?> caller){
        try {
            Class<?> modDirTransformerDiscoverer = Class.forName("net.minecraftforge.fml.loading.ModDirTransformerDiscoverer");
            List<NamedPath> found = (List<NamedPath>) lookup.findStaticVarHandle(modDirTransformerDiscoverer,"found",List.class).get();
            found.removeIf(namedPath -> namedPath.paths()[0].toString().equals(ClassHelperSpecial.getJarPath(caller,true)));
            Class<?> launcher=Class.forName("cpw.mods.modlauncher.Launcher"),moduleLayerHandlerClass=Class.forName("cpw.mods.modlauncher.ModuleLayerHandler");
            Object INSTANCE=lookup.findStaticVarHandle(launcher,"INSTANCE",launcher).get();
            Object moduleLayerHandler=lookup.findVarHandle(launcher,"moduleLayerHandler",moduleLayerHandlerClass).get(INSTANCE);
            EnumMap map= (EnumMap) lookup.findVarHandle(moduleLayerHandlerClass,"completedLayers",EnumMap.class).get(moduleLayerHandler);
            Class<?> layerInfoClass=Class.forName("cpw.mods.modlauncher.ModuleLayerHandler$LayerInfo");
            //noinspection unchecked
            map.values().forEach(layerInfo -> {
                try {
                    ModuleLayer layer = (ModuleLayer) lookup.findVarHandle(layerInfoClass,"layer", ModuleLayer.class).get(layerInfo);
                    layer.modules().forEach(module->{
                        if (module.getName().equals(caller.getModule().getName())){
                            try {
                                VarHandle modulesVar=lookup.findVarHandle(Configuration.class,"modules", Set.class),name2ModuleVar= lookup.findVarHandle(Configuration.class,"nameToModule", Map.class);
                                Set<ResolvedModule> modules = new HashSet<>((Collection<ResolvedModule>)modulesVar.get(layer.configuration()));
                                Map<String, ResolvedModule> nameToModule = new HashMap<>((Map<String, ResolvedModule>)name2ModuleVar.get(layer.configuration()));
                                modules.remove(nameToModule.remove(caller.getModule().getName()));
                                modulesVar.set(layer.configuration(),modules);
                                name2ModuleVar.set(layer.configuration(),nameToModule);
                            }catch (Throwable throwable){
                                throwOriginalException(throwable);
                                throw new RuntimeException(throwable);
                            }
                        }
                    });
                }catch (Throwable throwable){
                    throwOriginalException(throwable);
                    throw new RuntimeException(throwable);
                }
            });
        }catch (Throwable throwable) {
            throwOriginalException(throwable);
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

    public static byte[] getBytecodesFromFile(String className,Class<?> lookup) {
        return getBytecodesFromFile(className,lookup,false);
    }

    public static byte[] getBytecodesFromFile(String className,Class<?> lookup,boolean canBeNull){
        InputStream is=lookup.getResourceAsStream("/"+className.replace('.','/')+".class");
        if (is==null){
            if (canBeNull){
                return null;
            }
            throwOriginalException(new ClassNotFoundException(className));
        }
        try {
            byte[] dat = new byte[is.available()];
            is.read(dat);
            is.close();
            dat=OnlyInDefineClassHelper.handle(dat,className.replace('.','/'));
            return dat;
        }catch(Throwable t){
            if (canBeNull){
                return null;
            }
            throwOriginalException(t);
            throw new RuntimeException("How did you get here?",t);
        }
    }

    public static byte[] getBytecodesFromFile(String className,Module m) {
        return getBytecodesFromFile(className,m,false);
    }

    public static byte[] getBytecodesFromFile(String className,Module m,boolean canBeNull){
        try {
            InputStream is=m.getResourceAsStream("/"+className.replace('.','/')+".class");
            if (is==null){
                if (canBeNull){
                    return null;
                }
                throwOriginalException(new ClassNotFoundException(className));
            }
            byte[] dat = new byte[is.available()];
            is.read(dat);
            is.close();
            dat=OnlyInDefineClassHelper.handle(dat,className.replace('.','/'));
            return dat;
        }catch(Throwable t){
            if (canBeNull){
                return null;
            }
            throwOriginalException(t);
            throw new RuntimeException("How did you get here?",t);
        }
    }
    public static byte[] getBytecodesFromFile(String className,ClassLoader cl) {
        return getBytecodesFromFile(className,cl,false);
    }

    public static byte[] getBytecodesFromFile(String className,ClassLoader cl,boolean canBeNull){
        try {
            InputStream is=cl.getResourceAsStream("/"+className.replace('.','/')+".class");
            if (is==null){
                if (canBeNull){
                    return null;
                }
                throwOriginalException(new ClassNotFoundException(className));
            }
            byte[] dat = new byte[is.available()];
            is.read(dat);
            is.close();
            dat=OnlyInDefineClassHelper.handle(dat,className.replace('.','/'));
            return dat;
        }catch(Throwable t){
            if (canBeNull){
                return null;
            }
            throwOriginalException(t);
            throw new RuntimeException("How did you get here?",t);
        }
    }
}
