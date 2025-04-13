package apphhzp.lib.hotspot;

import apphhzp.lib.ClassHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class JVMUtil {
    public static final Class<?> nativeLibImpl;
    public static final MethodHandle constructor;
    public static final MethodHandle open;
    public static final MethodHandle find;
    public static NativeLibrary findJvm() throws Throwable {
//        if (!isHotspotJVM){
//            return entry->0;
//        }
        Path jvmDir = Paths.get(System.getProperty("java.home"));
        Path maybeJre = jvmDir.resolve("jre");
        if (Files.isDirectory(maybeJre)) {
            jvmDir = maybeJre;
        }
        jvmDir = jvmDir.resolve("bin");
        String os = System.getProperty("os.name").toLowerCase();
        Path pathToJvm;
        String name;
        if (os.contains("win")) {
            pathToJvm = findFirstFile(jvmDir, "server/jvm.dll", "client/jvm.dll");
            name="jvm.dll";
        } else if (os.contains("nix") || os.contains("nux")) {
            pathToJvm = findFirstFile(jvmDir, "lib/amd64/server/libjvm.so", "lib/i386/server/libjvm.so");
            name="libjvm.so";
        } else {
            throw new RuntimeException("Unsupported OS (probably MacOS X): " + os);
        }
        String path=pathToJvm.normalize().toString();
        //System.err.println(path);
        Object library = constructor.invoke(JVMUtil.class, path, false,true);
        open.invoke(library);
        return new NativeLibrary() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public long find(String entry) {
                try {
                    return (long) find.invoke(library, entry);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };
    }
    private static Path findFirstFile(Path directory, String... files) {
        for (String file : files) {
            Path path = directory.resolve(file);
            if (Files.exists(path)) return path;
        }
        throw new RuntimeException("Failed to find one of the required paths!: " + Arrays.toString(files));
    }
    static {
        try {
            nativeLibImpl=Class.forName("jdk.internal.loader.NativeLibraries$NativeLibraryImpl",true,null);
            constructor = ClassHelper.lookup.findConstructor(nativeLibImpl, MethodType.methodType(void.class, Class.class,String.class,boolean.class,boolean.class));
            open=ClassHelper.lookup.findVirtual(nativeLibImpl,"open",MethodType.methodType(boolean.class));
            find=ClassHelper.lookup.findVirtual(nativeLibImpl,"find",MethodType.methodType(long.class, String.class));
        }catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
}
