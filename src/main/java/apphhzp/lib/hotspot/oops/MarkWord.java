package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;

public class MarkWord{
    public static final Type TYPE= JVM.type("markWord");
    public static final int SIZE=TYPE.size;
    public static final long no_hash_in_place=JVM.longConstant("markWord::no_hash_in_place");
    public static final long no_lock_in_place=JVM.longConstant("markWord::no_lock_in_place");
    public static final long PROTOTYPE=no_hash_in_place|no_lock_in_place;
    public final long value;
    public MarkWord(long value){
        this.value = value;
    }
}
