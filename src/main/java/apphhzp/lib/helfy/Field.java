package apphhzp.lib.helfy;

public class Field implements Comparable<Field> {
    public static final Field EMPTY=new FakeField();
    public final String name;
    public final String typeName;
    public final long offset;
    public final boolean isStatic;
    public final boolean isFromJVMCI;

    Field(String name, String typeName, long offset, boolean isStatic) {
        this(name, typeName, offset, isStatic, false);
    }

    Field(String name, String typeName, long offset, boolean isStatic,boolean isFromJVMCI) {
        this.name = name;
        this.typeName = typeName;
        this.offset = offset;
        this.isStatic = isStatic;
        this.isFromJVMCI = isFromJVMCI;
    }

    @Override
    public int compareTo(Field o) {
        if (isStatic != o.isStatic) {
            return isStatic ? -1 : 1;
        }
        return Long.compare(offset, o.offset);
    }

    @Override
    public String toString() {
        if (isStatic) {
            if (isFromJVMCI){
                return "static " + typeName + ' ' + name +" (JVMCI)"+ " @ 0x" + Long.toHexString(offset);
            }
            return "static " + typeName + ' ' + name + " @ 0x" + Long.toHexString(offset);
        } else {
            if (isFromJVMCI){
                return typeName + ' ' + name+" (JVMCI) @ " + offset;
            }
            return typeName + ' ' + name + " @ " + offset;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==this){
            return true;
        }
        if (obj instanceof Field field){
            return field.isStatic==this.isStatic&&field.name.equals(this.name)&&field.typeName.equals(this.name)&&field.offset==this.offset;
        }
        return false;
    }

    private static final class FakeField extends Field{
        private FakeField() {
            super("","",0,false);
        }

        @Override
        public String toString() {
            return "<FakeField>";
        }
    }
}
