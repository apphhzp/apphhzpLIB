package apphhzp.lib.hotspot;


import java.lang.instrument.ClassFileTransformer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import static apphhzp.lib.ClassHelperSpecial.lookup;

public class Test {
    public int val;
    public static long vavv=1;
    public Test() {

    }
    public static void print(int a){
        System.err.println(c(a));
    }

    public static double c(int a){
        return a*a*(1+Math.random());
    }

    public void add(int x){
        val += x;
    }

    public static Object  doit(){
        return Debugger.test(1);
    }

    public static boolean check(Object re,Object obj){
        return obj instanceof ClassFileTransformer&&re instanceof byte[];
    }
    public static void  adsdsa(){
        System.err.println("sdas");
    }
    private static final MethodHandle startM;
    private static final VarHandle eetopVar;
    static {
        try {
            startM=lookup.findVirtual(Thread.class,"start0", MethodType.methodType(void.class));
            eetopVar=lookup.findVarHandle(Thread.class,"eetop",long.class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
