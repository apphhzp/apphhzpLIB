package apphhzp.lib.service;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.CoremodHelper;
import apphhzp.lib.hotspot.Debugger;
import apphhzp.lib.hotspot.oops.Symbol;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static apphhzp.lib.ClassHelper.lookup;

public class ApphhzpLibService implements ITransformationService {
    static {
        ClassHelper.defineClassesFromJar(ApphhzpLibService.class,s->true);
        CoremodHelper.coexist(ApphhzpLibService.class);
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
