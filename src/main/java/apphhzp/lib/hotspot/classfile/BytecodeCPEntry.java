package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.hotspot.classfile.BytecodeCPEntry.tag.*;

// Entries in a yet-to-be-created constant pool.  Limited types for now.
public class BytecodeCPEntry {
    public static final class tag {
        public static final int ERROR_TAG=0,
        UTF8=1,
        KLASS=2,
        STRING=3,
        NAME_AND_TYPE=4,
        METHODREF=5;
    };

    public @RawCType("u1")int _tag;
    public static final class Union {
        public Symbol utf8;
        public @RawCType("u2")int klass;
        public @RawCType("u2")int string;
//        struct {
//            u2 name_index;
//            u2 type_index;
//        } name_and_type;
        public @RawCType("u2")int name_and_type_name_index;
        public @RawCType("u2")int name_and_type_type_index;
//        public struct {
//            u2 class_index;
//            u2 name_and_type_index;
//        } methodref;
        public @RawCType("u2")int methodref_class_index;
        public @RawCType("u2")int methodref_name_and_type_index;
        //public @RawCType("uintptr_t")long hash;
        public long hash(){
            long re=utf8==null?0:utf8.address;
            re|=klass;
            re|=string;
            re|=name_and_type_name_index|((long) name_and_type_type_index <<16);
            re|=methodref_class_index|((long) methodref_name_and_type_index <<16);
            return re;
        }
    }

    public final Union _u;

    public BytecodeCPEntry(){
        _tag=(tag.ERROR_TAG);
        _u=new Union();
    }
    public BytecodeCPEntry(@RawCType("u1")int tag){
        _tag=(tag);
        _u=new Union();
    }

    public static BytecodeCPEntry utf8(Symbol symbol) {
        BytecodeCPEntry bcpe=new BytecodeCPEntry(UTF8);
        bcpe._u.utf8 = symbol;
        return bcpe;
    }

    public static BytecodeCPEntry klass(@RawCType("u2")int index) {
        BytecodeCPEntry bcpe=new BytecodeCPEntry(KLASS);
        bcpe._u.klass = index;
        return bcpe;
    }

    public static BytecodeCPEntry string(@RawCType("u2")int index) {
        BytecodeCPEntry bcpe=new BytecodeCPEntry(STRING);
        bcpe._u.string = index;
        return bcpe;
    }

    public static BytecodeCPEntry name_and_type(@RawCType("u2")int name, @RawCType("u2")int type) {
        BytecodeCPEntry bcpe=new BytecodeCPEntry(NAME_AND_TYPE);
        bcpe._u.name_and_type_name_index = name;
        bcpe._u.name_and_type_type_index = type;
        return bcpe;
    }

    public static BytecodeCPEntry methodref(@RawCType("u2")int class_index, @RawCType("u2")int nat) {
        BytecodeCPEntry bcpe=new BytecodeCPEntry(METHODREF);
        bcpe._u.methodref_class_index = class_index;
        bcpe._u.methodref_name_and_type_index = nat;
        return bcpe;
    }

    public static boolean equals(BytecodeCPEntry e0, BytecodeCPEntry e1) {
        return e0._tag == e1._tag && e0._u.hash() == e1._u.hash();
    }

    public static @RawCType("unsigned")int hash(BytecodeCPEntry e0) {
        return (int) (e0._tag ^ e0._u.hash());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==this) return true;
        if (obj instanceof BytecodeCPEntry bytecodeCPEntry) {
            return equals(bytecodeCPEntry, this);
        }
        return false;
    }
}
