package apphhzp.lib.service;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.ClassOption;
import apphhzp.lib.CoremodHelper;
import apphhzp.lib.OnlyInDefineClassHelper;
import apphhzp.lib.api.ApphhzpInst;
import apphhzp.lib.natives.NativeUtil;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static apphhzp.lib.ClassHelperSpecial.isHotspotJVM;
import static apphhzp.lib.service.LoadingHelper.isLoading;

public class ApphhzpLibService implements ITransformationService {
    static {
        ClassHelperSpecial.defineHiddenClass("apphhzp.lib.service.ApphhzpLibService$DoTask",
                ApphhzpLibService.class,true,ApphhzpLibService.class.getProtectionDomain(), ClassOption.STRONG,ClassOption.NESTMATE);
    }

    private static class DoTask{
        static {
            System.setProperty("apphhzpLIB.coremod","loaded");
            //Let it load first.
            try {
                Class.forName("apphhzp.lib.ClassHelperSpecial",true,ApphhzpLibService.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            ApphhzpInst inst= NativeUtil.createApphhzpInstImpl();
            inst.addTransformer(new ClassFileTransformer1(),true);
            Logger LOGGER=LogManager.getLogger();
            Class<?>[] classes = new Class[]{ClassHelperSpecial.class,NativeUtil.class};
            for (Class<?> klass:classes) {
                try {
                    inst.retransformClasses(klass);
                } catch (Throwable e) {
                    LOGGER.error("Could not retransform class: {}",klass.getName());
                    LOGGER.throwing(e);
                }
            }
            ClassHelperSpecial.defineClassesFromJar(ApphhzpLibService.class, new Predicate1());
            if (!isLoading){
                CoremodHelper.coexist(ApphhzpLibService.class);
            }
        }

        private static class Predicate1 implements Predicate<String> {
            @Override
            public boolean test(String s) {
                return isHotspotJVM || !s.startsWith("apphhzp.lib.hotspot");
            }
        }

        private static class ClassFileTransformer1 implements ClassFileTransformer {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                try {
                    if (className.startsWith("apphhzp/lib/")) {
                        InputStream is = loader.getResourceAsStream("/" + className + ".class");
                        byte[] dat = new byte[is.available()];
                        is.read(dat);
                        is.close();
                        dat= OnlyInDefineClassHelper.handle(dat,className);
                        return dat;
                    }
                }catch (Throwable t){
                    System.err.println("error while transforming class " + className);
                    return null;
                }
                return null;
            }
        }
    }

    @Override
    public String name() {
        return "apphhzpLIB";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {
    }

    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}
