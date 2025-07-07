package apphhzp.lib.api.stackframe;

import java.lang.invoke.VarHandle;

import static apphhzp.lib.ClassHelperSpecial.lookup;
import static apphhzp.lib.ClassHelperSpecial.throwOriginalException;

public class LiveStackFrameInfo extends StackFrameInfo{
    private static final Class<?> JDK_CLASS;
    private static final Class<?> P32_CLASS;
    private static final Class<?> P64_CLASS;
    private static final int MODE_INTERPRETED;
    private static final int MODE_COMPILED;
    private static final VarHandle monitorsVar;
    private static final VarHandle localsVar;
    private static final VarHandle operandsVar;
    private static final VarHandle modeVar;
    private final Object[] monitors;
    private final Object[] locals;
    private final Object[] operands;
    private final int mode;

    static {
        try {
            JDK_CLASS=Class.forName("java.lang.LiveStackFrameInfo");
            P32_CLASS=Class.forName("java.lang.LiveStackFrameInfo$PrimitiveSlot32");
            P64_CLASS=Class.forName("java.lang.LiveStackFrameInfo$PrimitiveSlot64");
            MODE_INTERPRETED= (int) lookup.findStaticVarHandle(JDK_CLASS,"MODE_INTERPRETED",int.class).get();
            MODE_COMPILED= (int) lookup.findStaticVarHandle(JDK_CLASS,"MODE_COMPILED",int.class).get();
            monitorsVar=lookup.findVarHandle(JDK_CLASS,"monitors",Object[].class);
            localsVar=lookup.findVarHandle(JDK_CLASS,"locals",Object[].class);
            operandsVar=lookup.findVarHandle(JDK_CLASS,"operands",Object[].class);
            modeVar=lookup.findVarHandle(JDK_CLASS,"mode",int.class);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public LiveStackFrameInfo(Object info) {
        super(info);
        if (!isInfo(info)){
            throw new IllegalArgumentException("Illegal class: "+info.getClass());
        }
        monitors= (Object[]) monitorsVar.get(info);
        transform(monitors);
        locals= (Object[]) localsVar.get(info);
        transform(locals);
        operands= (Object[]) operandsVar.get(info);
        transform(operands);
        mode= (int) modeVar.get(info);
    }

    public static boolean isLiveInfo(Object obj){
        return JDK_CLASS.isInstance(obj);
    }

    private static void transform(Object[] arr){
        for (int i=0,len=arr.length; i<len; i++){
            if (isPrimitiveSlot32(arr[i])){
                arr[i]=new PrimitiveSlot32(arr[i]);
            }else if (isPrimitiveSlot64(arr[i])){
                arr[i]=new PrimitiveSlot64(arr[i]);
            }else if (isLiveInfo(arr[i])){
                arr[i]=new LiveStackFrameInfo(arr[i]);
            }else if (isInfo(arr[i])){
                arr[i]=new StackFrameInfo(arr[i]);
            }
        }
    }

    public Object[] getMonitors() {
        return monitors;
    }


    public Object[] getLocals() {
        return locals;
    }


    public Object[] getStack() {
        return operands;
    }

    @Override
    public String toString() {
        StringBuilder retVal = new StringBuilder(super.toString());
        if (mode != 0) {
            retVal.append("(");
            if ((mode & MODE_INTERPRETED) == MODE_INTERPRETED) {
                retVal.append(" interpreted ");
            }
            if ((mode & MODE_COMPILED) == MODE_COMPILED) {
                retVal.append(" compiled ");
            }
            retVal.append(")");
        }
        return retVal.toString();
    }

    public boolean isInterpreted() {
        return (mode & MODE_INTERPRETED) == MODE_INTERPRETED;
    }

    public boolean isCompiled() {
        return (mode & MODE_COMPILED) == MODE_COMPILED;
    }

    public static boolean isPrimitiveSlot32(Object obj){
        return P32_CLASS.isInstance(obj);
    }

    public static boolean isPrimitiveSlot64(Object obj){
        return P64_CLASS.isInstance(obj);
    }

    public abstract static class PrimitiveSlot {
        /**
         * Constructor.
         */
        protected PrimitiveSlot() {}
        /**
         * Returns the size, in bytes, of the slot.
         */
        public abstract int size();

        /**
         * Returns the int value if this primitive value is of size 4
         * @return the int value if this primitive value is of size 4
         *
         * @throws UnsupportedOperationException if this primitive value is not
         * of size 4.
         */
        public int intValue() {
            throw new UnsupportedOperationException("this " + size() + "-byte primitive");
        }

        /**
         * Returns the long value if this primitive value is of size 8
         * @return the long value if this primitive value is of size 8
         *
         * @throws UnsupportedOperationException if this primitive value is not
         * of size 8.
         */
        public long longValue() {
            throw new UnsupportedOperationException("this " + size() + "-byte primitive");
        }
    }

    public static class PrimitiveSlot32 extends PrimitiveSlot {
        private static final VarHandle valVar;
        static {
            try {
                valVar=lookup.findVarHandle(P32_CLASS,"value",int.class);
            }catch (Throwable t){
                throwOriginalException(t);
                throw new RuntimeException(t);
            }
        }
        private final int value;
        public PrimitiveSlot32(Object value){
            if (!P32_CLASS.isInstance(value)){
                throw new IllegalArgumentException("Illegal class: "+value.getClass());
            }
            this.value = (int) valVar.get(value);
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class PrimitiveSlot64 extends PrimitiveSlot {
        private static final VarHandle valVar;
        static {
            try {
                valVar=lookup.findVarHandle(P64_CLASS,"value",long.class);
            }catch (Throwable t){
                throwOriginalException(t);
                throw new RuntimeException(t);
            }
        }
        private final long value;
        public PrimitiveSlot64(Object value) {
            if (!P64_CLASS.isInstance(value)){
                throw new IllegalArgumentException("Illegal class: "+value.getClass());
            }
            this.value = (long) valVar.get(value);
        }

        @Override
        public int size() {
            return 8;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
