package apphhzp.lib.hotspot;

public interface TestSuper {
    public static int x1 = 0;
    public default void print(){
        System.err.println("Hi!");
    }
}
