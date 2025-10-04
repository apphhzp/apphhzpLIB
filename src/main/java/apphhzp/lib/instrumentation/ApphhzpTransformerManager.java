package apphhzp.lib.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

interface ApphhzpTransformerManager {
    boolean isRetransformable();

    void addTransformer(ClassFileTransformer transformer);

    boolean includesTransformer(ClassFileTransformer transformer);


    byte[] transform(Module module,
                     ClassLoader loader,
                     String classname,
                     Class<?> classBeingRedefined,
                     ProtectionDomain protectionDomain,
                     byte[] classfileBuffer);


    int getTransformerCount();

    boolean setNativeMethodPrefix(ClassFileTransformer transformer, String prefix);


    String[] getNativeMethodPrefixes();
}
