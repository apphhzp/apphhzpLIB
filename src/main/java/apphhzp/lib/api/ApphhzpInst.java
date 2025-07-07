package apphhzp.lib.api;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;

public interface ApphhzpInst {
    void
    addTransformer(ClassFileTransformer transformer, boolean canRetransform);
    void
    addTransformer(ClassFileTransformer transformer);
    boolean
    isRetransformClassesSupported();
    void
    retransformClasses(Class<?>... classes) throws UnmodifiableClassException;
    boolean
    isRedefineClassesSupported();
    void
    redefineClasses(ClassDefinition... definitions)
            throws  ClassNotFoundException, UnmodifiableClassException;
    boolean
    isModifiableClass(Class<?> theClass);
    Class<?>[]
    getAllLoadedClasses();
    Class<?>[]
    getInitiatedClasses(ClassLoader loader);
    long
    getObjectSize(Object objectToSize);
    void
    appendToBootstrapClassLoaderSearch(JarFile jarfile);
    void
    appendToSystemClassLoaderSearch(JarFile jarfile);
    boolean
    isNativeMethodPrefixSupported();
    void
    setNativeMethodPrefix(ClassFileTransformer transformer, String prefix);
}
