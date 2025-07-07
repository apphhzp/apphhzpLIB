package apphhzp.lib.api.callbacks;

public interface GetMethodIDCallback extends Callback{
    record MethodID(Class<?> declaringClass, String methodName, String methodDesc) {
    }
    /**Return null to do nothing.*/
    MethodID callback(Class<?> declaringClass, String methodName, String methodDesc);
}
