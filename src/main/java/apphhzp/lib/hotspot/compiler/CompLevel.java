package apphhzp.lib.hotspot.compiler;

import java.util.NoSuchElementException;

import static apphhzp.lib.helfy.JVM.intConstant;

public enum CompLevel{
    ALL(intConstant("CompLevel_all"),"CompLevel_all"),
    NONE(intConstant("CompLevel_none"),"CompLevel_none"),
    SIMPLE(intConstant("CompLevel_simple"),"CompLevel_simple"),
    LIMITED_PROFILE(intConstant("CompLevel_limited_profile"),"CompLevel_limited_profile"),
    FULL_PROFILE(intConstant("CompLevel_full_profile"),"CompLevel_full_profile"),
    FULL_OPTIMIZATION(intConstant("CompLevel_full_optimization"),"CompLevel_full_optimization");
    public final int id;
    public final String desc;
    CompLevel(int id, String name){
        this.id=id;
        this.desc=name;
    }

    public static CompLevel of(int lvl){
        CompLevel re = lvl == ALL.id ? ALL : lvl == NONE.id ? NONE : lvl == SIMPLE.id ? SIMPLE : lvl == LIMITED_PROFILE.id ? LIMITED_PROFILE : lvl == FULL_PROFILE.id ? FULL_PROFILE : lvl == FULL_OPTIMIZATION.id ? FULL_OPTIMIZATION : null;
        if (re==null){
            throw new NoSuchElementException(""+lvl);
        }
        return re;
    }
}
