package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.*;

//OopHandle is oopDesc(jobject)**
public class Oop/*Desc*/ extends JVMObject {
    public static final Type DESC=JVM.type("oopDesc");
    public static final Type COMPRESSED=JVM.type("CompressedOops");
    public static final Type COMPRESSED_KLASS=JVM.type("CompressedKlassPointers");
    public static final long DESC_KLASS_OFFSET=DESC.offset("_metadata._klass");
    public static final long narrow_oop_base= unsafe.getLong(COMPRESSED.global("_narrow_oop._base"));
    public static final long narrow_oop_shift= unsafe.getInt(COMPRESSED.global("_narrow_oop._shift"))&0xffffffffL;
    public static final boolean narrow_oop_use_implicit_null_checks= unsafe.getByte(COMPRESSED.global("_narrow_oop._use_implicit_null_checks"))!=0;
    public static final long narrow_klass_base=unsafe.getLong(COMPRESSED_KLASS.global("_narrow_klass._base"));
    public static final long narrow_klass_shift=unsafe.getInt(COMPRESSED_KLASS.global("_narrow_klass._shift"))&0xffffffffL;
    private long narrowOop;
    private Object object;

    public Oop(long addr){
        super(addr);
        if (addr==0L){
            narrowOop =0;
            object=null;
        }else {
            narrowOop =encodeOop(addr);
            object = object(narrowOop);
        }
    }

    public Oop(Object obj) {
        this(decodeOop(getAddress(obj)));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getObject() {
        return (T) object;
    }

    public void setObject(Object object){
        this.narrowOop=getAddress(object);
        this.object=object;
    }

    @Nullable
    public Klass getKlass() {
        if (this.address==0L){
            return null;
        }
        return Klass.getKlass(object);
    }

    public long getNarrow(){
        return this.narrowOop;
    }

    public static long encodeOop(long oop) {
        if (!narrowOopEnabled())
            return oop;
        return  (oop - narrow_oop_base >> narrow_oop_shift);
    }

    public static long decodeOop(long narrowOop) {
        if (!narrowOopEnabled())
            return narrowOop;
        return narrow_oop_base + ( narrowOop << narrow_oop_shift);
    }

    public static long encodeKlass(long klass) {
        if (!narrowKlassEnabled())
            return  klass;
        return  (klass - narrow_klass_base >> narrow_klass_shift);
    }

    public static long decodeKlass(long narrowKlass) {
        if (!narrowKlassEnabled()) {
            return narrowKlass;
        }
        return narrow_klass_base + (narrowKlass << narrow_klass_shift);
    }

    /**
     *  @return {@code true} if the narrowOop pointer length is 8 or the JVM is 32-bit,{@code false} otherwise.
     */
    public static boolean narrowOopEnabled(){
        return is64BitJVM&& JVM.usingCompressedOops;
    }


    /**
     * @return {@code true} if the narrowKlass pointer length is 8 or the JVM is 32-bit,{@code false} otherwise.
     */
    public static boolean narrowKlassEnabled(){
        return is64BitJVM&& JVM.usingCompressedClassPointers;
    }

    private static final TransformHelper helper=new TransformHelper();

    public static Object object(long oop) {
        helper.changeContent(oop);
        return helper.content;
    }

    public static long getAddress(Object obj) {
        helper.changeContent(obj);
        if (!is64BitJVM|| JVM.usingCompressedOops) {
            return unsafe.getIntVolatile(helper, TransformHelper.OFFSET);
        }
        return unsafe.getLongVolatile(helper,TransformHelper.OFFSET);
    }

    public static long fromOopHandle(long addr){
        if (addr==0L){
            return 0L;
        }
        addr=unsafe.getAddress(addr);
        if (addr==0L){
            return 0L;
        }
        return unsafe.getAddress(addr);
    }

    private static class TransformHelper {
        private volatile Object content;
        private static final long OFFSET;

        static {
            try {
                OFFSET = unsafe.objectFieldOffset(TransformHelper.class.getDeclaredField("content"));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        TransformHelper() {}

        TransformHelper(Object c) {
            content = c;
        }

        synchronized void changeContent(Object c) {
            unsafe.putObjectVolatile(this, OFFSET, c);
        }

        synchronized void changeContent(long c) {
            if (!is64BitJVM|| JVM.usingCompressedOops) {
                unsafe.putIntVolatile(this, OFFSET, (int) c);
            }else {
                unsafe.putLongVolatile(this,OFFSET,c);
            }
        }
    }
}
