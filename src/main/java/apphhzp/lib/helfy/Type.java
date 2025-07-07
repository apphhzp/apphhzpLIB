package apphhzp.lib.helfy;

import apphhzp.lib.ClassHelperSpecial;

import java.util.NoSuchElementException;
import java.util.Set;

public class Type {
    private static final Field[] NO_FIELDS = new Field[0];
    public static final Type EMPTY=new FakeType();

    public final String name;
    public final String superName;
    public final int size;
    public final boolean isOop;
    public final boolean isInt;
    public final boolean isUnsigned;
    public final Field[] fields;

    Type(String name, String superName, int size, boolean isOop, boolean isInt, boolean isUnsigned, Set<Field> fields) {
        this.name = name;
        this.superName = superName;
        this.size = size;
        this.isOop = isOop;
        this.isInt = isInt;
        this.isUnsigned = isUnsigned;
        this.fields = fields == null ? NO_FIELDS : fields.toArray(new Field[0]);
    }

    public Field field(String name) {
        for (Field field : fields) {
            if (field.name.equals(name)) {
                return field;
            }
        }
        throw new NoSuchElementException("No such field: " + name);
    }

    public boolean contains(String name){
        for (Field field : fields) {
            if (field.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public long global(String name) {
        Field field = field(name);
        if (field.isStatic) {
            return field.offset;
        }

        throw new IllegalArgumentException("Static field expected");
    }

    public long offset(String name) {
        Field field = field(name);
        if (!field.isStatic) {
            return field.offset;
        }
        throw new IllegalArgumentException("Instance field expected");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (superName != null) sb.append(" extends ").append(superName);
        sb.append(" @ ").append(size).append('\n');
        for (Field field:fields) {
            sb.append("  ").append(field).append('\n');
        }
        return sb.toString();
    }

    private static final class FakeType extends Type{
        private static final long fakeAddress= ClassHelperSpecial.unsafe.allocateMemory(16);
        private FakeType() {
            super("", null, 0, false, false, false, null);
        }

        @Override
        public Field field(String name) {
            return Field.EMPTY;
        }

        @Override
        public boolean contains(String name) {
            return false;
        }

        @Override
        public long global(String name) {
            return fakeAddress;
        }

        @Override
        public long offset(String name) {
            return 0;
        }

        @Override
        public String toString() {
            return "<EmptyType>";
        }
    }

    public static class UnknownType extends Type{

        public UnknownType(String name, Set<Field> fields) {
            super(name,null,0,false,false,false, fields);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(name);
            sb.append(" @ unknown\n");
            for (Field field:fields) {
                sb.append("  ").append(field).append('\n');
            }
            return sb.toString();
        }
    }
}
