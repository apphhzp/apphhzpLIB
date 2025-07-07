package apphhzp.lib.hotspot;

public interface NativeLibrary {
    String name();
    long lookup(String entry);
    default boolean open() {
        return false;
    }
}
