package apphhzp.lib.service;

import apphhzp.lib.CoremodHelper;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ApphhzpLibService implements ITransformationService {
    static {
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
