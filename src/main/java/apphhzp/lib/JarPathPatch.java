package apphhzp.lib;

import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;

public final class JarPathPatch {
    private static final Class<?> moduleCL;
    private static final VarHandle resolvedRootsVar;
    private static final VarHandle jarVar1;
    private static final VarHandle jarVar2;
    private static final VarHandle filesystemVar;
    private static final VarHandle basepathsVar;
    static {
        try {
            moduleCL=Class.forName("cpw.mods.cl.ModuleClassLoader");
            resolvedRootsVar=lookup.findVarHandle(moduleCL,"resolvedRoots", Map.class);
            Class<?> cls=Class.forName("cpw.mods.jarhandling.SecureJar$ModuleDataProvider"),
            jar=Class.forName("cpw.mods.jarhandling.impl.Jar"),fs=Class.forName("cpw.mods.niofs.union.UnionFileSystem");
            jarVar1=lookup.findVarHandle(Class.forName("cpw.mods.cl.JarModuleFinder$JarModuleReference"),"jar",cls);
            jarVar2=lookup.findVarHandle(Class.forName("cpw.mods.jarhandling.impl.Jar$JarModuleDataProvider"),"jar",jar);
            filesystemVar=lookup.findVarHandle(jar,"filesystem",fs);
            basepathsVar=lookup.findVarHandle(fs,"basepaths", List.class);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
    private JarPathPatch(){
        throw new UnsupportedOperationException();
    }
    public static boolean isLoadedByMCL(Class<?> cls){
        return moduleCL.isInstance(cls.getClassLoader());
    }
    //((ModuleClassLoader)ClassHelperSpecial.class.getClassLoader()).resolvedRoots.values().iterator().next().jar.jar.filesystem.basepaths.get(0)
    @SuppressWarnings("unchecked")
    public static String getJarPath(Class<?> clazz){
        if (!moduleCL.isInstance(clazz.getClassLoader())){
            throw new IllegalArgumentException("clazz was not loaded by ModuleClassLoader.");
        }
        Map<String,?> resolvedRoots= (Map<String, ?>) resolvedRootsVar.get(clazz.getClassLoader());
        return ((List<Path>)basepathsVar.get(filesystemVar.get(jarVar2.get(jarVar1.get(resolvedRoots.get(clazz.getModule().getDescriptor().name())))))).get(0).toUri().getPath();
    }
}
