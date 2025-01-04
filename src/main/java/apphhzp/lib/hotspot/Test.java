package apphhzp.lib.hotspot;


import java.util.function.Function;

public class Test {
    public int val;
    public Test(int val) {
        this.val = val;
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
}
