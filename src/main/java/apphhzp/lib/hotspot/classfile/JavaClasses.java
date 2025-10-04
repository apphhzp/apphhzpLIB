package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.oop.OopDesc;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.stream.AllFieldStream;
import apphhzp.lib.hotspot.stream.InternalFieldStream;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

import javax.annotation.Nullable;
import java.lang.invoke.VarHandle;

import static apphhzp.lib.ClassHelperSpecial.*;

public class JavaClasses {
    public static class ClassLoader{
        public static final long loader_data_offset;
        static {
            int ld_offset =-1;
            for (AllFieldStream stream = new AllFieldStream(VMClasses.classLoaderKlass()); !stream.done(); stream.next()){
                if (stream.field().isInternal()){
                    if(stream.name().toString().equals("loader_data")){
                        ld_offset =stream.getOffset();
                    }
                }
            }
            if (ld_offset ==-1){
                throw new RuntimeException("Could not get loader_data_offset");
            }
            loader_data_offset= ld_offset;
        }

        public static long getPointerFrom(java.lang.ClassLoader loader){
            return JVM.oopSize==8?unsafe.getLong(loader, loader_data_offset): unsafe.getInt(loader, loader_data_offset);
        }

        public static ClassLoaderData loader_data_acquire(OopDesc loader) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            return ClassLoaderData.getOrCreate(unsafe.getLong(loader.getObject(),loader_data_offset));//HeapAccess<MO_ACQUIRE>::load_at(loader, _loader_data_offset);
        }

        public static ClassLoaderData loader_data_raw(OopDesc loader) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            return ClassLoaderData.getOrCreate(unsafe.getLong(loader.getObject(),loader_data_offset));//RawAccess<>::load_at(loader, _loader_data_offset);
        }

        public static void release_set_loader_data(OopDesc loader, ClassLoaderData new_data) {
            if (loader.address==0L){
                throw new IllegalArgumentException("loader must not be NULL");
            }
            unsafe.putLong(loader.getObject(),loader_data_offset, new_data.address);
            //HeapAccess<MO_RELEASE>::store_at(loader, _loader_data_offset, new_data);
        }
    }

    public static class String{

    }

    public static class Class{
        public static final long oop_size_offset;
        static {
            int ld_offset =-1;
            for (AllFieldStream stream = new AllFieldStream(VMClasses.classLoaderKlass()); !stream.done(); stream.next()){
                if (stream.field().isInternal()){
                    if(stream.name().toString().equals("oop_size")){
                        ld_offset =stream.getOffset();
                    }
                }
            }
            if (ld_offset ==-1){
                throw new RuntimeException("Could not get oop_size_offset");
            }
            oop_size_offset= ld_offset;
        }

        public static int getOopSizeRaw(java.lang.Class<?> oop){
            return unsafe.getInt(oop,oop_size_offset);
        }
//        public static @RawCType("BasicType")int primitive_type(OopDesc java_class){
////            Klass ak = ((Klass*)java_class->metadata_field(_array_klass_offset));
////            BasicType type = T_VOID;
////            if (ak != NULL) {
////                // Note: create_basic_type_mirror above initializes ak to a non-null value.
////                type = ArrayKlass::cast(ak)->element_type();
////            } else {
////            }
////            return type;
//        }
    }

    public static class Thread{
        public static final VarHandle eetopGetter;
        static {
            try {
                eetopGetter= ClassHelperSpecial.lookup.findVarHandle(java.lang.Thread.class,"eetop",long.class);
            } catch (Throwable throwable){
                throw new RuntimeException(throwable);
            }
        }

        public static JavaThread thread(java.lang.Thread thread){
            Long val= (Long) eetopGetter.get(thread);
            if (val==0L){
                return null;
            }
            return JavaThread.getOrCreate(val );
        }

        public static void setThread(java.lang.Thread thread,@Nullable JavaThread newThread){
            eetopGetter.set(thread,newThread==null?0L:newThread.address);
        }
    }

    public static class StackFrameInfo{
        public static final java.lang.Class<?> clazz;
        public static final long version_offset;
        static {
            try {
                clazz=java.lang.Class.forName("java.lang.StackFrameInfo");
                long offset=-1;
                for (InternalFieldStream fieldStream=new InternalFieldStream(Klass.asKlass(clazz).asInstanceKlass()); !fieldStream.done(); fieldStream.next()){
                    if (fieldStream.name().toString().equals("version")){
                        offset=fieldStream.getOffset();
                    }
                }
                if (offset==-1){
                    throw new RuntimeException("Could not get _version_offset!");
                }
                version_offset= offset;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public static short getVersion(Object object){
            if (!object.getClass().isAssignableFrom(clazz)){
                throw new IllegalArgumentException();
            }
            return unsafe.getShort(object,version_offset);
        }

        public static void setVersion(Object object,short version){
            if (!object.getClass().isAssignableFrom(clazz)){
                throw new IllegalArgumentException();
            }
            unsafe.putShort(object,version_offset,version);
        }
    }

    public static class MethodHandle{
        private static final VarHandle typeVar;
        static {
            try {
                typeVar=lookup.findVarHandle(java.lang.invoke.MethodHandle.class,"type", java.lang.invoke.MethodType.class);
            }catch (Throwable t){
                throwOriginalException(t);
                throw new RuntimeException(t);
            }
        }

        public static java.lang.invoke.MethodType type(java.lang.invoke.MethodHandle handle){
            return (java.lang.invoke.MethodType) typeVar.get(handle);
        }
    }

    public static class MethodType{
        private static final VarHandle rtypeVar;
        private static final VarHandle ptypesVar;
        static {
            try {
                rtypeVar=lookup.findVarHandle(java.lang.invoke.MethodType.class,"rtype", java.lang.Class.class);
                ptypesVar=lookup.findVarHandle(java.lang.invoke.MethodType.class,"ptypes", java.lang.Class[].class);
            }catch (Throwable t){
                throwOriginalException(t);
                throw new RuntimeException(t);
            }
        }
        public static java.lang.Class<?>[] ptypes(java.lang.invoke.MethodType type){
            return (java.lang.Class<?>[]) ptypesVar.get(type);
        }
        public static java.lang.Class<?> rtypes(java.lang.invoke.MethodType type){
            return (java.lang.Class<?>) rtypeVar.get(type);
        }
        public static int ptype_count(java.lang.invoke.MethodType mt) {
            return ptypes(mt).length;
        }
        public static java.lang.Class<?> ptype(java.lang.invoke.MethodType mt, int idx) {
            return ptypes(mt)[idx];
        }
    }

    public static class MemberName{
        public static final java.lang.Class<?> clazz;
        private static final VarHandle clazzVar;
        private static final VarHandle nameVar;
        private static final VarHandle typeVar;
        private static final VarHandle flagsVar;
        private static final VarHandle methodVar;
        static {
            try {
                clazz= java.lang.Class.forName("java.lang.invoke.MemberName");
                clazzVar=lookup.findVarHandle(clazz,"clazz", java.lang.Class.class);
                nameVar=lookup.findVarHandle(clazz,"name", java.lang.String.class);
                typeVar=lookup.findVarHandle(clazz,"type", Object.class);
                flagsVar=lookup.findVarHandle(clazz,"flags", int.class);
                methodVar=lookup.findVarHandle(clazz,"method", java.lang.Class.forName("java.lang.invoke.ResolvedMethodName"));
            }catch (Throwable t){
                throwOriginalException(t);
                throw new RuntimeException(t);
            }
        }
        private MemberName(){}
        public static Method vmtarget(Object mname) {
            if (!clazz.isInstance(mname)){
                throw new IllegalArgumentException("wrong type");
            }
            Object method = methodVar.get(mname);
            return method == null ? null : ResolvedMethodName.vmtarget(method);
        }

        public static void set_type(Object mname,Object type){
            if (!clazz.isInstance(mname)){
                throw new IllegalArgumentException("wrong type");
            }
            typeVar.set(mname,type);
        }

        public static Object type(Object mname){
            if (!clazz.isInstance(mname)){
                throw new IllegalArgumentException("wrong type");
            }
            return typeVar.get(mname);
        }

        public static void set_flags(Object mname,int flags){
            if (!clazz.isInstance(mname)){
                throw new IllegalArgumentException("wrong type");
            }
            flagsVar.set(mname,flags);
        }

        public static int flags(Object mname){
            if (!clazz.isInstance(mname)){
                throw new IllegalArgumentException("wrong type");
            }
            return (int) flagsVar.get(mname);
        }
        public static void set_clazz(Object mname, java.lang.Class<?> clazz){
            if (!MemberName.clazz.isInstance(mname)){
                throw new IllegalArgumentException("wrong type");
            }
            clazzVar.set(mname,clazz);
        }
        public static void set_name(Object mname, java.lang.String name){
            if (!clazz.isInstance(mname)){
                throw new IllegalArgumentException("wrong type");
            }
            nameVar.set(mname,name);
        }
    }

    public static class ResolvedMethodName{
        public static final java.lang.Class<?> clazz;
        private static final long _vmtarget_offset;
        private static final long _vmholder_offset;
        static{
            try {
                clazz= java.lang.Class.forName("java.lang.invoke.ResolvedMethodName");
                long offset1=-1,offset2=-1;
                for (AllFieldStream stream=new AllFieldStream(Klass.asKlass(clazz).asInstanceKlass());!stream.done(); stream.next()){
                    if (stream.name().toString().equals("vmtarget")){
                        offset1=stream.getOffset();
                    }else if (stream.name().toString().equals("vmholder")){
                        offset2=stream.getOffset();
                    }
                }
                if (offset1==-1||offset2==-1){
                    throw new RuntimeException("Could not get offset!");
                }
                _vmtarget_offset=offset1;
                _vmholder_offset=offset2;
            }catch (Throwable t){
                throwOriginalException(t);
                throw new RuntimeException(t);
            }
        }
        public static Method vmtarget(Object resolved_method) {
            if (!clazz.isInstance(resolved_method)){
                throw new IllegalArgumentException("wrong type");
            }
            return Method.getOrCreate(JVM.getAddress(resolved_method,_vmtarget_offset));
        }
        // Used by redefinition to change Method* to new Method* with same hash (name, signature)
        public static void set_vmtarget(Object resolved_method, Method m) {
            if (!clazz.isInstance(resolved_method)){
                throw new IllegalArgumentException("wrong type");
            }
            JVM.putAddress(resolved_method,_vmtarget_offset,m==null?0L:m.address);
        }
        public static void set_vmholder(Object resolved_method, Object holder) {
            if (!clazz.isInstance(resolved_method)){
                throw new IllegalArgumentException("wrong type");
            }
            unsafe.putObject(resolved_method,_vmholder_offset,holder);
        }
        public static Object find_resolved_method(Method m) {
            Method method = m;

//            // lookup ResolvedMethod oop in the table, or create a new one and intern it
//            oop resolved_method = ResolvedMethodTable::find_method(method);
//            if (resolved_method != NULL) {
//                return resolved_method;
//            }

            InstanceKlass k = Klass.asKlass(clazz).asInstanceKlass();
            if (!k.is_initialized()) {
                throw new RuntimeException();
            }

            Object new_resolved_method = null;
            try {
                new_resolved_method = unsafe.allocateInstance(clazz);
            } catch (InstantiationException e) {
                throwOriginalException(e);
                throw new RuntimeException(e);
            }


            if (method.is_old()) {
                if (method.is_deleted()) {
                    throw new NoSuchMethodError();
                } else {
                    method = method.get_new_method();
                }
            }

            InstanceKlass holder = method.method_holder();

            set_vmtarget(new_resolved_method, (method));
            // Add a reference to the loader (actually mirror because hidden classes may not have
            // distinct loaders) to ensure the metadata is kept alive.
            // This mirror may be different than the one in clazz field.
            set_vmholder(new_resolved_method, holder.getMirror());

            // Set flag in class to indicate this InstanceKlass has entries in the table
            // to avoid walking table during redefinition if none of the redefined classes
            // have any membernames in the table.
            holder.set_has_resolved_methods();

            return new_resolved_method;
        }
    }
    public static class BoxingObject{
        public static @RawCType("BasicType")int basic_type(Object box) {
            if (box == null)  return BasicType.T_ILLEGAL;
            if (box.getClass().equals(Boolean.class)) {
                return BasicType.T_BOOLEAN;
            } else if (box.getClass().equals(Character.class)) {
                return BasicType.T_CHAR;
            } else if (box.getClass().equals(Float.class)) {
                return BasicType.T_FLOAT;
            } else if (box.getClass().equals(Double.class)) {
                return BasicType.T_DOUBLE;
            } else if (box.getClass().equals(Byte.class)) {
                return BasicType.T_BYTE;
            } else if (box.getClass().equals(Short.class)) {
                return BasicType.T_SHORT;
            } else if (box.getClass().equals(Integer.class)) {
                return BasicType.T_INT;
            } else if (box.getClass().equals(Long.class)) {
                return BasicType.T_LONG;
            }
            return BasicType.T_ILLEGAL;
        }

        public static boolean is_instance(Object box)                 { return basic_type(box) != BasicType.T_ILLEGAL; }
        public static boolean is_instance(Object box, @RawCType("BasicType")int type) { return basic_type(box) == type; }


        public static @RawCType("BasicType")int get_value(Object box,@RawCType("jvalue*") Object[] value) {
            if (box.getClass().equals(Boolean.class)) {
                value[0] = box;
                return BasicType.T_BOOLEAN;
            } else if (box.getClass().equals(Character.class)) {
                value[0] = box;
                return BasicType.T_CHAR;
            } else if (box.getClass().equals(Float.class)) {
                value[0] = box;
                return BasicType.T_FLOAT;
            } else if (box.getClass().equals(Double.class)) {
                value[0] = box;
                return BasicType.T_DOUBLE;
            } else if (box.getClass().equals(Byte.class)) {
                value[0] = box;
                return BasicType.T_BYTE;
            } else if (box.getClass().equals(Short.class)) {
                value[0] = box;
                return BasicType.T_SHORT;
            } else if (box.getClass().equals(Integer.class)) {
                value[0] = box;
                return BasicType.T_INT;
            } else if (box.getClass().equals(Long.class)) {
                value[0] = box;
                return BasicType.T_LONG;
            }
            return BasicType.T_ILLEGAL;
        }


//        public static @RawCType("BasicType")int java_lang_boxing_object::set_value(oop box, jvalue* value) {
//            BasicType type = vmClasses::box_klass_type(box->klass());
//            switch (type) {
//                case T_BOOLEAN:
//                    box->bool_field_put(_value_offset, value->z);
//                    break;
//                case T_CHAR:
//                    box->char_field_put(_value_offset, value->c);
//                    break;
//                case T_FLOAT:
//                    box->float_field_put(_value_offset, value->f);
//                    break;
//                case T_DOUBLE:
//                    box->double_field_put(_long_value_offset, value->d);
//                    break;
//                case T_BYTE:
//                    box->byte_field_put(_value_offset, value->b);
//                    break;
//                case T_SHORT:
//                    box->short_field_put(_value_offset, value->s);
//                    break;
//                case T_INT:
//                    box->int_field_put(_value_offset, value->i);
//                    break;
//                case T_LONG:
//                    box->long_field_put(_long_value_offset, value->j);
//                    break;
//                default:
//                    return T_ILLEGAL;
//            } // end switch
//            return type;
//        }

    }
}
