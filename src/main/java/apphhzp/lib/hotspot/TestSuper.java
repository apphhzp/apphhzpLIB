package apphhzp.lib.hotspot;

public class TestSuper {
    public static int x1;
    public void print(){
        System.err.println("Hi!");
    }

    public static void test() {
        call1();
    }

    public static void call1() {
        x1++;
        //System.err.println("Hello World!");
    }

    public static void call33() {
        x1--;
    }
}
