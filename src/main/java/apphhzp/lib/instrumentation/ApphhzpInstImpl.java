package apphhzp.lib.instrumentation;

import apphhzp.lib.api.ApphhzpInst;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;

public class ApphhzpInstImpl implements ApphhzpInst {
    private final ApphhzpTransformerManager mTransformerManager;
    private ApphhzpTransformerManager      mRetransfomableTransformerManager;
    // needs to store a native pointer, so use 64 bits
    private final     long                    mNativeAgent;
    private final     boolean                 mEnvironmentSupportsRedefineClasses;
    private volatile  boolean                 mEnvironmentSupportsRetransformClassesKnown;
    private volatile  boolean                 mEnvironmentSupportsRetransformClasses;
    private final     boolean                 mEnvironmentSupportsNativeMethodPrefix;
    private static final Module UNNAMED_MODULE;
    static {
        try {
            UNNAMED_MODULE= (Module) lookup.findStaticVarHandle(Class.forName("jdk.internal.loader.BootLoader"),"UNNAMED_MODULE", Module.class).get();
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    private ApphhzpInstImpl(long    nativeAgent,
                        boolean environmentSupportsRedefineClasses,
                        boolean environmentSupportsNativeMethodPrefix) {
        mTransformerManager                    = new ApphhzpTransformerManager(false);
        mRetransfomableTransformerManager      = null;
        mNativeAgent                           = nativeAgent;
        mEnvironmentSupportsRedefineClasses    = environmentSupportsRedefineClasses;
        mEnvironmentSupportsRetransformClassesKnown = false; // false = need to ask
        mEnvironmentSupportsRetransformClasses = false;      // don't know yet
        mEnvironmentSupportsNativeMethodPrefix = environmentSupportsNativeMethodPrefix;
    }

    public void
    addTransformer(ClassFileTransformer transformer) {
        addTransformer(transformer, false);
    }

    public synchronized void
    addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        if (transformer == null) {
            throw new NullPointerException("null passed as 'transformer' in addTransformer");
        }
        if (canRetransform) {
            if (!isRetransformClassesSupported()) {
                throw new UnsupportedOperationException(
                        "adding retransformable transformers is not supported in this environment");
            }
            if (mRetransfomableTransformerManager == null) {
                mRetransfomableTransformerManager = new ApphhzpTransformerManager(true);
            }
            mRetransfomableTransformerManager.addTransformer(transformer);
            if (mRetransfomableTransformerManager.getTransformerCount() == 1) {
                setHasRetransformableTransformers(mNativeAgent, true);
            }
        } else {
            mTransformerManager.addTransformer(transformer);
            if (mTransformerManager.getTransformerCount() == 1) {
                setHasTransformers(mNativeAgent, true);
            }
        }
    }

    public boolean
    isModifiableClass(Class<?> theClass) {
        if (theClass == null) {
            throw new NullPointerException(
                    "null passed as 'theClass' in isModifiableClass");
        }
        return isModifiableClass0(mNativeAgent, theClass);
    }

    public boolean isModifiableModule(Module module) {
        if (module == null) {
            throw new NullPointerException("'module' is null");
        }
        return true;
    }

    public boolean
    isRetransformClassesSupported() {
        // ask lazily since there is some overhead
        if (!mEnvironmentSupportsRetransformClassesKnown) {
            mEnvironmentSupportsRetransformClasses = isRetransformClassesSupported0(mNativeAgent);
            mEnvironmentSupportsRetransformClassesKnown = true;
        }
        return mEnvironmentSupportsRetransformClasses;
    }

    public void
    retransformClasses(Class<?>... classes) {
        if (!isRetransformClassesSupported()) {
            throw new UnsupportedOperationException(
                    "retransformClasses is not supported in this environment");
        }
        if (classes.length == 0) {
            return; // no-op
        }
        retransformClasses0(mNativeAgent, classes);
    }

    public boolean
    isRedefineClassesSupported() {
        return mEnvironmentSupportsRedefineClasses;
    }

    public void
    redefineClasses(ClassDefinition...  definitions)
            throws  ClassNotFoundException {
        if (!isRedefineClassesSupported()) {
            throw new UnsupportedOperationException("redefineClasses is not supported in this environment");
        }
        if (definitions == null) {
            throw new NullPointerException("null passed as 'definitions' in redefineClasses");
        }
        for (int i = 0; i < definitions.length; ++i) {
            if (definitions[i] == null) {
                throw new NullPointerException("element of 'definitions' is null in redefineClasses");
            }
        }
        if (definitions.length == 0) {
            return; // short-circuit if there are no changes requested
        }

        redefineClasses0(mNativeAgent, definitions);
    }


    public Class<?>[] getAllLoadedClasses() {
        return getAllLoadedClasses0(mNativeAgent);
    }

    public Class<?>[] getInitiatedClasses(ClassLoader loader) {
        return getInitiatedClasses0(mNativeAgent, loader);
    }

    public long
    getObjectSize(Object objectToSize) {
        if (objectToSize == null) {
            throw new NullPointerException("null passed as 'objectToSize' in getObjectSize");
        }
        return getObjectSize0(mNativeAgent, objectToSize);
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        throw new UnsupportedOperationException();
    }


    public boolean
    isNativeMethodPrefixSupported() {
        return mEnvironmentSupportsNativeMethodPrefix;
    }

    public synchronized void
    setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        if (!isNativeMethodPrefixSupported()) {
            throw new UnsupportedOperationException(
                    "setNativeMethodPrefix is not supported in this environment");
        }
        if (transformer == null) {
            throw new NullPointerException(
                    "null passed as 'transformer' in setNativeMethodPrefix");
        }
        ApphhzpTransformerManager mgr = findTransformerManager(transformer);
        if (mgr == null) {
            throw new IllegalArgumentException(
                    "transformer not registered in setNativeMethodPrefix");
        }
        mgr.setNativeMethodPrefix(transformer, prefix);
        String[] prefixes = mgr.getNativeMethodPrefixes();
        setNativeMethodPrefixes(mNativeAgent, prefixes, mgr.isRetransformable());
    }


    private ApphhzpTransformerManager
    findTransformerManager(ClassFileTransformer transformer) {
        if (mTransformerManager.includesTransformer(transformer)) {
            return mTransformerManager;
        }
        if (mRetransfomableTransformerManager != null &&
                mRetransfomableTransformerManager.includesTransformer(transformer)) {
            return mRetransfomableTransformerManager;
        }
        return null;
    }


    /*
     *  Natives
     */
    private native boolean
    isModifiableClass0(long nativeAgent, Class<?> theClass);

    private native boolean
    isRetransformClassesSupported0(long nativeAgent);

    private native void
    setHasTransformers(long nativeAgent, boolean has);

    private native void
    setHasRetransformableTransformers(long nativeAgent, boolean has);

    private native void
    retransformClasses0(long nativeAgent, Class<?>[] classes);

    private native void
    redefineClasses0(long nativeAgent, ClassDefinition[]  definitions)
            throws  ClassNotFoundException;


    private native Class<?>[]
    getAllLoadedClasses0(long nativeAgent);

    private native Class<?>[]
    getInitiatedClasses0(long nativeAgent, ClassLoader loader);

    private native long
    getObjectSize0(long nativeAgent, Object objectToSize);

    private native void
    setNativeMethodPrefixes(long nativeAgent, String[] prefixes, boolean isRetransformable);

    /*
     *  Internals
     */

    // WARNING: the native code knows the name & signature of this method
    private byte[]
    transform(  Module              module,
                ClassLoader         loader,
                String              classname,
                Class<?>            classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[]              classfileBuffer,
                boolean             isRetransformer) {
        ApphhzpTransformerManager mgr = isRetransformer?
                mRetransfomableTransformerManager :
                mTransformerManager;
        // module is null when not a class load or when loading a class in an
        // unnamed module and this is the first type to be loaded in the package.
        if (module == null) {
            if (classBeingRedefined != null) {
                module = classBeingRedefined.getModule();
            } else {
                module = (loader == null) ? UNNAMED_MODULE
                        : loader.getUnnamedModule();
            }
        }
        if (mgr == null) {
            return null; // no manager, no transform
        } else {
            return mgr.transform(   module,
                    loader,
                    classname,
                    classBeingRedefined,
                    protectionDomain,
                    classfileBuffer);
        }
    }

    public static class ApphhzpTransformerManager {
        private static class TransformerInfo {
            final ClassFileTransformer mTransformer;
            String mPrefix;

            TransformerInfo(ClassFileTransformer transformer) {
                mTransformer = transformer;
                mPrefix = null;
            }

            ClassFileTransformer transformer() {
                return mTransformer;
            }

            String getPrefix() {
                return mPrefix;
            }

            void setPrefix(String prefix) {
                mPrefix = prefix;
            }
        }


        private TransformerInfo[] mTransformerList;


        private final boolean mIsRetransformable;

        ApphhzpTransformerManager(boolean isRetransformable) {
            mTransformerList = new TransformerInfo[0];
            mIsRetransformable = isRetransformable;
        }

        boolean isRetransformable() {
            return mIsRetransformable;
        }

        public synchronized void
        addTransformer(ClassFileTransformer transformer) {
            TransformerInfo[] oldList = mTransformerList;
            TransformerInfo[] newList = new TransformerInfo[oldList.length + 1];
            System.arraycopy(oldList,
                    0,
                    newList,
                    0,
                    oldList.length);
            newList[oldList.length] = new TransformerInfo(transformer);
            mTransformerList = newList;
        }

        synchronized boolean
        includesTransformer(ClassFileTransformer transformer) {
            for (TransformerInfo info : mTransformerList) {
                if (info.transformer() == transformer) {
                    return true;
                }
            }
            return false;
        }


        public byte[]
        transform(Module module,
                  ClassLoader loader,
                  String classname,
                  Class<?> classBeingRedefined,
                  ProtectionDomain protectionDomain,
                  byte[] classfileBuffer) {
            boolean someoneTouchedTheBytecode = false;

            TransformerInfo[] transformerList = mTransformerList;

            byte[] bufferToUse = classfileBuffer;

            // order matters, gotta run 'em in the order they were added
            for (TransformerInfo transformerInfo : transformerList) {
                ClassFileTransformer transformer = transformerInfo.transformer();
                byte[] transformedBytes = null;

                try {
                    transformedBytes = transformer.transform(module,
                            loader,
                            classname,
                            classBeingRedefined,
                            protectionDomain,
                            bufferToUse);
                } catch (Throwable t) {
                    // don't let any one transformer mess it up for the others.
                    // This is where we need to put some logging. What should go here? FIXME
                }
                if (transformedBytes != null) {
                    someoneTouchedTheBytecode = true;
                    bufferToUse = transformedBytes;
                }
            }

            // if someone modified it, return the modified buffer.
            // otherwise return null to mean "no transforms occurred"
            byte[] result;
            if (someoneTouchedTheBytecode) {
                result = bufferToUse;
            } else {
                result = null;
            }

            return result;
        }


        int
        getTransformerCount() {
            TransformerInfo[] transformerList = mTransformerList;
            return transformerList.length;
        }

        boolean
        setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
            TransformerInfo[] transformerList = mTransformerList;

            for (TransformerInfo transformerInfo : transformerList) {
                ClassFileTransformer aTransformer = transformerInfo.transformer();
                if (aTransformer == transformer) {
                    transformerInfo.setPrefix(prefix);
                    return true;
                }
            }
            return false;
        }


        String[]
        getNativeMethodPrefixes() {
            TransformerInfo[] transformerList = mTransformerList;
            String[] prefixes = new String[transformerList.length];

            for (int x = 0; x < transformerList.length; x++) {
                TransformerInfo transformerInfo = transformerList[x];
                prefixes[x] = transformerInfo.getPrefix();
            }
            return prefixes;
        }
    }
}
