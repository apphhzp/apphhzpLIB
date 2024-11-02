package apphhzp.lib.hotspot.compiler;

import java.util.NoSuchElementException;

public enum CompilerType {
    NONE(0,"compiler_none"),
    C1(1,"compiler_c1"),
    C2(2,"compiler_c2"),
    JVMCI(3,"compiler_jvmci");
    public static final int NUMBER_OF_COMPILER_TYPES = 4;
    public final int id;
    public final String name;
    CompilerType(int id, String name) {
        this.name = name;
        this.id = id;
    }

    public static CompilerType of(int id) {
        CompilerType type = id == 0 ? NONE : id == 1 ? C1 : id == 2 ? C2 : id == 3 ? JVMCI : null;
        if (type==null){
            throw new NoSuchElementException(""+id);
        }
        return type;
    }
}
