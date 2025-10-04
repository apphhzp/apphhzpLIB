package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class Annotations extends JVMObject {
    public static final Type TYPE= JVM.includeJVMCI?JVM.type("Annotations"):null;
    public static final int SIZE=4*JVM.oopSize;
    public static final long FIELDS_ANNOTATIONS_OFFSET=TYPE==null?JVM.oopSize:TYPE.offset("_fields_annotations");
    public static final long CLASS_ANNOTATIONS_OFFSET=FIELDS_ANNOTATIONS_OFFSET-JVM.oopSize;
    public static final long CLASS_TYPE_ANNOTATIONS_OFFSET=FIELDS_ANNOTATIONS_OFFSET+JVM.oopSize;
    public static final long FIELDS_TYPE_ANNOTATIONS_OFFSET=CLASS_TYPE_ANNOTATIONS_OFFSET+JVM.oopSize;
    private U1Array/*AnnotationArray* */ classAnnotationsCache;
    private VMTypeArray<U1Array/*AnnotationArray* */> fieldsAnnotationsCache;
    private U1Array/*AnnotationArray* */ classTypeAnnotationsCache;
    private VMTypeArray<U1Array/*AnnotationArray* */> fieldsTypeAnnotationsCache;
    public static Annotations create() {
        long addr=unsafe.allocateMemory(SIZE);
        unsafe.setMemory(addr,SIZE, (byte) 0);
        return new Annotations(addr);
    }
    public Annotations(long addr) {
        super(addr);
    }

    @Nullable
    public U1Array classAnnotations() {
        long addr= unsafe.getAddress(this.address+CLASS_ANNOTATIONS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.classAnnotationsCache, addr)) {
            this.classAnnotationsCache=new U1Array(addr);
        }
        return this.classAnnotationsCache;
    }

    public void setClassAnnotations(@Nullable U1Array classAnnotations) {
        this.classAnnotationsCache=null;
        unsafe.putAddress(this.address+CLASS_ANNOTATIONS_OFFSET,classAnnotations==null?0L:classAnnotations.address);
    }

    @Nullable
    public VMTypeArray<U1Array> fieldsAnnotations() {
        long addr= unsafe.getAddress(this.address+FIELDS_ANNOTATIONS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.fieldsAnnotationsCache, addr)) {
            this.fieldsAnnotationsCache=new VMTypeArray<>(addr, U1Array.class,U1Array::new);
        }
        return this.fieldsAnnotationsCache;
    }

    public void setFieldsAnnotations(@Nullable VMTypeArray<U1Array> fieldsAnnotations) {
        this.fieldsAnnotationsCache=null;
        unsafe.putAddress(this.address+FIELDS_ANNOTATIONS_OFFSET, fieldsAnnotations==null?0L:fieldsAnnotations.address);
    }

    @Nullable
    public U1Array classTypeAnnotations() {
        long addr= unsafe.getAddress(this.address+CLASS_TYPE_ANNOTATIONS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.classTypeAnnotationsCache, addr)) {
            this.classTypeAnnotationsCache=new U1Array(addr);
        }
        return this.classTypeAnnotationsCache;
    }

    public void setClassTypeAnnotations(@Nullable U1Array classTypeAnnotations) {
        this.classTypeAnnotationsCache=null;
        unsafe.putAddress(this.address+CLASS_TYPE_ANNOTATIONS_OFFSET, classTypeAnnotations==null?0L:classTypeAnnotations.address);
    }

    @Nullable
    public VMTypeArray<U1Array> fieldsTypeAnnotations() {
        long addr=unsafe.getAddress(this.address+FIELDS_TYPE_ANNOTATIONS_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.fieldsTypeAnnotationsCache,addr)){
            this.fieldsTypeAnnotationsCache=new VMTypeArray<>(addr, U1Array.class,U1Array::new);
        }
        return this.fieldsTypeAnnotationsCache;
    }

    public void setFieldsTypeAnnotations(@Nullable VMTypeArray<U1Array> fieldsTypeAnnotations) {
        this.fieldsTypeAnnotationsCache=null;
        unsafe.putAddress(this.address+FIELDS_TYPE_ANNOTATIONS_OFFSET, fieldsTypeAnnotations==null?0L:fieldsTypeAnnotations.address);
    }

    public static byte[] makeJavaArray(U1Array annotations) {
        return annotations==null?null:annotations.toByteArray();
//        if (annotations != null) {
//            int length = annotations.length();
//            byte[] copy =new byte[length];
//            for (int i = 0; i< length; i++) {
//                copy[i]=annotations.get(i);
//            }
//            return copy;
//        } else {
//            return null;
//        }
    }

    @Override
    public String toString() {
        return "Annotations@0x"+Long.toHexString(this.address);
    }
}
