package apphhzp.lib.hotspot.oops.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.klass.Klass;

import javax.annotation.Nullable;
import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.is64BitJVM;
import static apphhzp.lib.ClassHelperSpecial.unsafe;

//OopHandle is oopDesc(jobject)**
//typedef class oopDesc* oop;
public class OopDesc extends JVMObject {
    public static final Type TYPE =JVM.type("oopDesc");
    public static final int SIZE=TYPE.size;
    public static final Type COMPRESSED=JVM.type("CompressedOops");
    public static final Type COMPRESSED_KLASS=JVM.type("CompressedKlassPointers");
    public static final long DESC_KLASS_OFFSET= TYPE.offset("_metadata._klass");
    public static final long narrow_oop_base= unsafe.getAddress(COMPRESSED.global("_narrow_oop._base"));
    public static final long narrow_oop_shift= unsafe.getInt(COMPRESSED.global("_narrow_oop._shift"))&0xffffffffL;
    public static final boolean narrow_oop_use_implicit_null_checks= unsafe.getByte(COMPRESSED.global("_narrow_oop._use_implicit_null_checks"))!=0;
    public static final long narrow_klass_base=unsafe.getAddress(COMPRESSED_KLASS.global("_narrow_klass._base"));
    public static final long narrow_klass_shift=unsafe.getInt(COMPRESSED_KLASS.global("_narrow_klass._shift"))&0xffffffffL;
    private long narrowOop;
    private Object object;
    public static final OopDesc NULL=new OopDesc(0);
    public static OopDesc of(long addr){
        if (addr==0L){
            return null;
        }

        Klass klass=getKlassForOopHandle(addr);
        if (klass.isInstanceKlass()){
            return new OopDesc(addr);
        }else if (klass.isTypeArrayKlass()){
            return new TypeArrayOopDesc(addr);
        }else if (klass.isObjArrayKlass()){
            return new ObjArrayOopDesc(addr);
        }else {
            throw new RuntimeException("Unknown oopDesc type klass: " + klass);
        }
    }

    public static OopDesc of(Object o){
        return of(decodeOop(getEncodedAddress(o)));
    }

    public static Klass getKlassForOopHandle(long handle) {
        if (handle == 0L) {
            return null;
        }
        if (JVM.usingCompressedClassPointers) {
            return Klass.getOrCreate((OopDesc.decodeKlass(unsafe.getInt(handle+OopDesc.DESC_KLASS_OFFSET)&0xffffffffL)));
        } else {
            return Klass.getOrCreate(unsafe.getAddress(handle+DESC_KLASS_OFFSET));
        }
    }

    protected OopDesc(long addr){
        super(addr);
        if (addr==0L){
            narrowOop =0;
            object=null;
        }else {
            narrowOop =encodeOop(addr);
            object = object(narrowOop);
        }
    }

    protected OopDesc(Object obj) {
        this(decodeOop(getEncodedAddress(obj)));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getObject() {
        return (T) object;
    }

    public void setObject(Object object){
        this.narrowOop= getEncodedAddress(object);
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

    public final long getObjectSize(){
        if (this.address==0L){
            return 0L;
        }
        return sizeGivenKlass(this,this.getKlass());
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
     *  @return {@code false} if the narrowOop pointer length is 8 or the JVM is 32-bit,{@code true} otherwise.
     */
    public static boolean narrowOopEnabled(){
        return is64BitJVM&& JVM.usingCompressedOops;
    }


    /**
     * @return {@code false} if the narrowKlass pointer length is 8 or the JVM is 32-bit,{@code true} otherwise.
     */
    public static boolean narrowKlassEnabled(){
        return is64BitJVM&& JVM.usingCompressedClassPointers;
    }

    private static final TransformHelper helper=new TransformHelper();

    public static Object object(long oop) {
        helper.changeContent(oop);
        return helper.content;
    }

    public static long getAddress(Object obj){
        if (obj==null){
            return 0L;
        }
        return decodeOop(getEncodedAddress(obj));
    }

    public static long getEncodedAddress(Object obj) {
        if (obj==null){
            return 0L;
        }
        helper.changeContent(obj);
        if (!is64BitJVM|| JVM.usingCompressedOops) {
            return unsafe.getIntVolatile(helper, TransformHelper.OFFSET)&0xffffffffL;
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

    public static int sizeGivenKlass(OopDesc oop,Klass klass)  {
        int lh = klass.getLayout();
        int s;
        if (lh > Klass.LayoutHelper._lh_neutral_value) {
            if (!Klass.LayoutHelper.needsSlowPath(lh)){
                s = lh;
            } else {
                s = (int) klass.oopSize(oop);
            }
        } else {
            if (lh < Klass.LayoutHelper._lh_neutral_value) {
                long size_in_bytes;
                long array_length = ((ArrayOopDesc)oop).getLength();
                size_in_bytes = array_length << Klass.LayoutHelper.log2ElementSize(lh);
                size_in_bytes += Klass.LayoutHelper.headerSize(lh);
                s = (int)(JVM.alignUp(size_in_bytes,JVM.objectAlignmentInBytes));
            } else {
                s = (int) klass.oopSize(oop);
            }
        }
        return s;
    }

    @Override
    public String toString() {
        return "oopDesc@0x"+Long.toHexString(this.address);
    }

    public void print_value_on(PrintStream st){
        if (object instanceof String str){
            st.print(str);
            print_address_on(st);
        } else {
            getKlass().oop_print_value_on(this, st);
        }
    }

    public void print_address_on(PrintStream st){
        st.print("{"+"0x"+Long.toHexString(this.address)+"}");
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

        private TransformHelper() {}

        private TransformHelper(Object c) {
            content = c;
        }

        private synchronized void changeContent(Object c) {
            unsafe.putObjectVolatile(this, OFFSET, c);
        }

        private synchronized void changeContent(long c) {
            if (!is64BitJVM|| JVM.usingCompressedOops) {
                unsafe.putIntVolatile(this, OFFSET, (int) c);
            }else {
                unsafe.putLongVolatile(this,OFFSET,c);
            }
        }
    }
}
