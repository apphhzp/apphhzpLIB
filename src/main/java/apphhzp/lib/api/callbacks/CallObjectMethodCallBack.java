package apphhzp.lib.api.callbacks;

/**
 * Return null to do nothing.<br>
 * Return {@code Callback.NULL} to return null.
 * */
public interface CallObjectMethodCallBack extends Callback{
    Object pre(Object obj,Class<?> declaringClass,String name,String desc,Object... args);
    Object post(Object oldResult, Object obj,Class<?> declaringClass,String name,String desc,Object... args);
}
