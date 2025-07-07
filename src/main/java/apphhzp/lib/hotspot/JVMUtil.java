package apphhzp.lib.hotspot;

import apphhzp.lib.ClassHelperSpecial;
import com.sun.jna.Native;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class JVMUtil {
    public static final Class<?> nativeLibImpl;
    private static final MethodHandle find;
    private static final MethodHandle load;
    private static final MethodHandle findSymbol;
    private static final VarHandle handleVar;
    private static final Field nameField;

    public static NativeLibrary findJvm() {
        if (!ClassHelperSpecial.isHotspotJVM){
            throw new RuntimeException("Unsupported JVM!");
        }
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                return create("jvm.dll");
            } else {
                try {
                    return create("libjvm.so");
                }catch (UnsatisfiedLinkError e){
                    return create("libjvm.dylib");
                }
            }
        }catch (UnsatisfiedLinkError e){
            throw new RuntimeException("JVM library not found", e);
        }
    }

    private static NativeLibrary create(String name){
        try {
            Object nl= unsafe.allocateInstance(nativeLibImpl);
            unsafe.putObjectVolatile(nl,unsafe.objectFieldOffset(nameField),name);
            try {
                load.invoke(nl,name,false,false,true);
            }catch (UnsatisfiedLinkError t){
                throw t;
            }catch (Throwable t){
                throw new RuntimeException(t);
            }
            long handle= (long) handleVar.get(nl);
            return new NativeLibrary() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public long lookup(String entry) {
                    try {
                        long x= (long) find.invoke(nl,entry);
                        if (x==0){
                            try {
                                return (long) findSymbol.invoke(handle,entry);
                            }catch (UnsatisfiedLinkError e){
                                return 0;
                            }
                        }
                        return x;
                    }catch (Throwable t){
                        throw new RuntimeException(t);
                    }
                }
            };
        }catch (InstantiationException t){
            throw new RuntimeException(t);
        }
    }

    static {
        try {
            nativeLibImpl=Class.forName("jdk.internal.loader.NativeLibraries$NativeLibraryImpl",true,null);
            load= ClassHelperSpecial.lookup.findStatic(Class.forName("jdk.internal.loader.NativeLibraries",true,null),"load",
                    MethodType.methodType(boolean.class,nativeLibImpl,String.class,boolean.class,boolean.class,boolean.class));
            find= ClassHelperSpecial.lookup.findStatic(Class.forName("jdk.internal.loader.NativeLibraries",true,null),"findEntry0",MethodType.methodType(long.class,nativeLibImpl, String.class));
            nameField=nativeLibImpl.getDeclaredField("name");
            handleVar=lookup.findVarHandle(nativeLibImpl,"handle",long.class);
            findSymbol=lookup.findStatic(Native.class,"findSymbol",MethodType.methodType(long.class,long.class,String.class));
        }catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    //    public static NativeLibrary findJvm() throws Throwable {
//        Path jvmDir = Paths.get(System.getProperty("java.home"));
//        Path maybeJre = jvmDir.resolve("jre");
//        if (Files.isDirectory(maybeJre)) {
//            jvmDir = maybeJre;
//        }
//        jvmDir = jvmDir.resolve("bin");
//        String os = System.getProperty("os.name").toLowerCase();
//        Path pathToJvm;
//        String name;
//        if (os.contains("win")) {
//            pathToJvm = findFirstFile(jvmDir, "server/jvm.dll", "client/jvm.dll");
//            name="jvm.dll";
//        } else if (os.contains("nix") || os.contains("nux")) {
//            pathToJvm = findFirstFile(jvmDir, "lib/amd64/server/libjvm.so", "lib/i386/server/libjvm.so");
//            name="libjvm.so";
//        } else {
//            pathToJvm=findFirstFile(jvmDir,"lib/server/libjvm.dylib");
//            name="libjvm.dylib";
//        }
//        String path=pathToJvm.normalize().toString();
//        Object library = constructor.invoke(JVMUtil.class, path, false,true);
//        open.invoke(library);
//        return new NativeLibrary() {
//            @Override
//            public String name() {
//                return name;
//            }
//
//            @Override
//            public long lookup(String entry) {
//                try {
//                    return (long) find.invoke(library, entry);
//                } catch (Throwable t) {
//                    throw new RuntimeException(t);
//                }
//            }
//        };
//    }
    //    private static Path findFirstFile(Path directory, String... files) {
//        for (String file : files) {
//            Path path = directory.resolve(file);
//            if (Files.exists(path)) return path;
//        }
//        throw new RuntimeException("Failed to find one of the required paths!: " + Arrays.toString(files));
//    }
}
