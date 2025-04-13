package apphhzp.lib.hotspot;

public interface NativeLibrary {
    String name();
    long find(String entry);
    default long lookup(String name) throws NoSuchMethodException {
        long addr = find(name);
        if (0 == addr) {
            throw new NoSuchMethodException("Cannot find symbol " + name + " in library " + name());
        }
        return addr;
    }
    default boolean open() {
        return false;
    }


}
