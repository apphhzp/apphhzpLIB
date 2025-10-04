package apphhzp.lib;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

import static apphhzp.lib.ClassHelperSpecial.*;

public final class InternalUnsafe {
    public static final Class<?> internalUnsafeClass;
    public static final Class<?> unsafeConstantsClass;
    private static final MethodHandle staticFieldBaseMethod;
    private static final MethodHandle staticFieldOffsetMethod;
    private static final MethodHandle objectFieldOffsetMethod;
    private static final MethodHandle getUncompressedObjectMethod;
    private static final MethodHandle defineClassMethod;
    private static final MethodHandle compareAndSetByteMethod;
    private static final MethodHandle compareAndExchangeReferenceMethod;
    private static final MethodHandle allocateUninitializedArrayMethod;

    private static final MethodHandle getReferenceAcquireMethod;
    private static final MethodHandle getBooleanAcquireMethod;
    private static final MethodHandle getByteAcquireMethod;
    private static final MethodHandle getCharAcquireMethod;
    private static final MethodHandle getShortAcquireMethod;
    private static final MethodHandle getIntAcquireMethod;
    private static final MethodHandle getLongAcquireMethod;
    private static final MethodHandle getFloatAcquireMethod;
    private static final MethodHandle getDoubleAcquireMethod;

    private static final MethodHandle putReferenceReleaseMethod;
    private static final MethodHandle putBooleanReleaseMethod;
    private static final MethodHandle putByteReleaseMethod;
    private static final MethodHandle putCharReleaseMethod;
    private static final MethodHandle putShortReleaseMethod;
    private static final MethodHandle putIntReleaseMethod;
    private static final MethodHandle putLongReleaseMethod;
    private static final MethodHandle putFloatReleaseMethod;
    private static final MethodHandle putDoubleReleaseMethod;

    private static final MethodHandle getReferenceOpaqueMethod;
    private static final MethodHandle getBooleanOpaqueMethod;
    private static final MethodHandle getByteOpaqueMethod;
    private static final MethodHandle getCharOpaqueMethod;
    private static final MethodHandle getShortOpaqueMethod;
    private static final MethodHandle getIntOpaqueMethod;
    private static final MethodHandle getLongOpaqueMethod;
    private static final MethodHandle getFloatOpaqueMethod;
    private static final MethodHandle getDoubleOpaqueMethod;

    private static final MethodHandle putReferenceOpaqueMethod;
    private static final MethodHandle putBooleanOpaqueMethod;
    private static final MethodHandle putByteOpaqueMethod;
    private static final MethodHandle putCharOpaqueMethod;
    private static final MethodHandle putShortOpaqueMethod;
    private static final MethodHandle putIntOpaqueMethod;
    private static final MethodHandle putLongOpaqueMethod;
    private static final MethodHandle putFloatOpaqueMethod;
    private static final MethodHandle putDoubleOpaqueMethod;

    private static final VarHandle bigEndianVar;
    static {
        try {
            internalUnsafeClass=Class.forName("jdk.internal.misc.Unsafe");
            unsafeConstantsClass=Class.forName("jdk.internal.misc.UnsafeConstants");
            staticFieldBaseMethod=lookup.findVirtual(internalUnsafeClass,"staticFieldBase", MethodType.methodType(Object.class, Field.class));
            staticFieldOffsetMethod = lookup.findVirtual(internalUnsafeClass, "staticFieldOffset", MethodType.methodType(long.class, Field.class));
            objectFieldOffsetMethod = lookup.findVirtual(internalUnsafeClass, "objectFieldOffset", MethodType.methodType(long.class, Field.class));
            getUncompressedObjectMethod=lookup.findVirtual(internalUnsafeClass,"getUncompressedObject", MethodType.methodType(Object.class, long.class));
            defineClassMethod = lookup.findVirtual(internalUnsafeClass, "defineClass", MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class));
            compareAndSetByteMethod = lookup.findVirtual(internalUnsafeClass, "compareAndSetByte", MethodType.methodType(boolean.class, Object.class, long.class, byte.class, byte.class));
            compareAndExchangeReferenceMethod=lookup.findVirtual(internalUnsafeClass,"compareAndExchangeReference", MethodType.methodType(Object.class, Object.class, long.class, Object.class, Object.class));
            allocateUninitializedArrayMethod=lookup.findVirtual(internalUnsafeClass,"allocateUninitializedArray", MethodType.methodType(Object.class, Class.class, int.class));

            getReferenceAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getReferenceAcquire", MethodType.methodType(Object.class, Object.class, long.class));
            getByteAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getByteAcquire", MethodType.methodType(byte.class, Object.class, long.class));
            getBooleanAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getBooleanAcquire", MethodType.methodType(boolean.class, Object.class, long.class));
            getShortAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getShortAcquire", MethodType.methodType(short.class, Object.class, long.class));
            getCharAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getCharAcquire", MethodType.methodType(char.class, Object.class, long.class));
            getIntAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getIntAcquire", MethodType.methodType(int.class, Object.class, long.class));
            getLongAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getLongAcquire", MethodType.methodType(long.class, Object.class, long.class));
            getFloatAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getFloatAcquire", MethodType.methodType(float.class, Object.class, long.class));
            getDoubleAcquireMethod=lookup.findVirtual(internalUnsafeClass,"getDoubleAcquire", MethodType.methodType(double.class, Object.class, long.class));

            putReferenceReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putReferenceRelease", MethodType.methodType(void.class, Object.class, long.class, Object.class));
            putByteReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putByteRelease", MethodType.methodType(void.class, Object.class, long.class, byte.class));
            putBooleanReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putBooleanRelease", MethodType.methodType(void.class, Object.class, long.class, boolean.class));
            putShortReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putShortRelease", MethodType.methodType(void.class, Object.class, long.class, short.class));
            putCharReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putCharRelease", MethodType.methodType(void.class, Object.class, long.class, char.class));
            putIntReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putIntRelease", MethodType.methodType(void.class, Object.class, long.class, int.class));
            putLongReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putLongRelease", MethodType.methodType(void.class, Object.class, long.class, long.class));
            putFloatReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putFloatRelease", MethodType.methodType(void.class, Object.class, long.class, float.class));
            putDoubleReleaseMethod=lookup.findVirtual(internalUnsafeClass,"putDoubleRelease", MethodType.methodType(void.class, Object.class, long.class, double.class));

            getReferenceOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getReferenceOpaque", MethodType.methodType(Object.class, Object.class, long.class));
            getByteOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getByteOpaque", MethodType.methodType(byte.class, Object.class, long.class));
            getBooleanOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getBooleanOpaque", MethodType.methodType(boolean.class, Object.class, long.class));
            getShortOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getShortOpaque", MethodType.methodType(short.class, Object.class, long.class));
            getCharOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getCharOpaque", MethodType.methodType(char.class, Object.class, long.class));
            getIntOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getIntOpaque", MethodType.methodType(int.class, Object.class, long.class));
            getLongOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getLongOpaque", MethodType.methodType(long.class, Object.class, long.class));
            getFloatOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getFloatOpaque", MethodType.methodType(float.class, Object.class, long.class));
            getDoubleOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"getDoubleOpaque", MethodType.methodType(double.class, Object.class, long.class));

            putReferenceOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putReferenceOpaque", MethodType.methodType(void.class, Object.class, long.class, Object.class));
            putByteOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putByteOpaque", MethodType.methodType(void.class, Object.class, long.class, byte.class));
            putBooleanOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putBooleanOpaque", MethodType.methodType(void.class, Object.class, long.class, boolean.class));
            putShortOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putShortOpaque", MethodType.methodType(void.class, Object.class, long.class, short.class));
            putCharOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putCharOpaque", MethodType.methodType(void.class, Object.class, long.class, char.class));
            putIntOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putIntOpaque", MethodType.methodType(void.class, Object.class, long.class, int.class));
            putLongOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putLongOpaque", MethodType.methodType(void.class, Object.class, long.class, long.class));
            putFloatOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putFloatOpaque", MethodType.methodType(void.class, Object.class, long.class, float.class));
            putDoubleOpaqueMethod=lookup.findVirtual(internalUnsafeClass,"putDoubleOpaque", MethodType.methodType(void.class, Object.class, long.class, double.class));

            bigEndianVar=lookup.findStaticVarHandle(unsafeConstantsClass,"BIG_ENDIAN", boolean.class);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
    private final Object internalUnsafe;
    InternalUnsafe(Object internalUnsafe) {
        this.internalUnsafe = internalUnsafe;
    }

    @SuppressWarnings("unchecked")
    public <T> T getUncompressedObject(long address){
        if (address==0L){
            return null;
        }
        try {
            return (T) getUncompressedObjectMethod.invoke(internalUnsafe,address);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Object staticFieldBase(Field field){
        try {
            return staticFieldBaseMethod.invoke(internalUnsafe,field);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    public long staticFieldOffset(Field field){
        try {
            return (long) staticFieldOffsetMethod.invoke(internalUnsafe,field);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    public long objectFieldOffset(Field field){
        try {
            return (long) objectFieldOffsetMethod.invoke(internalUnsafe,field);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    public Class<?> defineClass(String name, byte[] bytecodes,int off,int len, ClassLoader loader,ProtectionDomain pd) {
        try {
            return (Class<?>) defineClassMethod.invoke(internalUnsafe, name, bytecodes, off, len, loader, pd);
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public boolean compareAndSwapByte(Object o, long offset, byte expected, byte x) {
        try {
            return (boolean) compareAndSetByteMethod.invoke(internalUnsafe, o, offset, expected, x);
        } catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T compareAndExchangeReference(Object o, long offset,
                                                    Object expected,
                                                    Object x) {
        try {
            return (T) compareAndExchangeReferenceMethod.invoke(internalUnsafe,o,offset,expected,x);
        } catch (Throwable e) {
            throwOriginalException(e);
            throw new RuntimeException(e);
        }
    }

    public boolean isBigEndian(){
        return (boolean) bigEndianVar.get();
    }

    public Object allocateUninitializedArray(Class<?> componentType, int length) {
        try {
            return allocateUninitializedArrayMethod.invoke(internalUnsafe,componentType,length);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public  void putAddressRelease(long address, long x){
        if (unsafe.addressSize() == 4){
            putIntRelease(null,address,(int)x);
        } else {
            putLongRelease(null,address,x);
        }
    }

    public long getAddressAcquire(long address){
        if (unsafe.addressSize() == 4){
            return Integer.toUnsignedLong(getIntAcquire(null,address));
        } else {
            return getLongAcquire(null,address);
        }
    }

    public Object getReferenceAcquire(Object o, long offset) {
        try {
            return getReferenceAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public boolean getBooleanAcquire(Object o, long offset) {
        try {
            return (boolean) getBooleanAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public byte getByteAcquire(Object o, long offset) {
        try {
            return (byte) getByteAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public short getShortAcquire(Object o, long offset) {
        try {
            return (short) getShortAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public char getCharAcquire(Object o, long offset) {
        try {
            return (char) getCharAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public int getIntAcquire(Object o, long offset) {
        try {
            return (int) getIntAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public float getFloatAcquire(Object o, long offset) {
        try {
            return (float) getFloatAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public long getLongAcquire(Object o, long offset) {
        try {
            return (long) getLongAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public double getDoubleAcquire(Object o, long offset) {
        try {
            return (double) getDoubleAcquireMethod.invoke(internalUnsafe,o, offset);
        }catch (Throwable t) {
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putReferenceRelease(Object o, long offset, Object x) {
        try {
            putReferenceReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putBooleanRelease(Object o, long offset, boolean x) {
        try {
            putBooleanReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putByteRelease(Object o, long offset, byte x) {
        try {
            putByteReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putShortRelease(Object o, long offset, short x) {
        try {
            putShortReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putCharRelease(Object o, long offset, char x) {
        try {
            putCharReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putIntRelease(Object o, long offset, int x) {
        try {
            putIntReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putFloatRelease(Object o, long offset, float x) {
        try {
            putFloatReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putLongRelease(Object o, long offset, long x) {
        try {
            putLongReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putDoubleRelease(Object o, long offset, double x) {
        try {
            putDoubleReleaseMethod.invoke(internalUnsafe,o,offset,x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public Object getReferenceOpaque(Object o, long offset) {
        try {
            return getReferenceOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public boolean getBooleanOpaque(Object o, long offset) {
        try {
            return (boolean) getBooleanOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public byte getByteOpaque(Object o, long offset) {
        try {
            return (byte) getByteOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public short getShortOpaque(Object o, long offset) {
        try {
            return (short) getShortOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public char getCharOpaque(Object o, long offset) {
        try {
            return (char) getCharOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public int getIntOpaque(Object o, long offset) {
        try {
            return (int) getIntOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public float getFloatOpaque(Object o, long offset) {
        try {
            return (float) getFloatOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public long getLongOpaque(Object o, long offset) {
        try {
            return (long) getLongOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public double getDoubleOpaque(Object o, long offset) {
        try {
            return (double) getDoubleOpaqueMethod.invoke(internalUnsafe,o,offset);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putReferenceOpaque(Object o, long offset, Object x) {
        try {
            putReferenceOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putBooleanOpaque(Object o, long offset, boolean x) {
        try {
            putBooleanOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putByteOpaque(Object o, long offset, byte x) {
        try {
            putByteOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putShortOpaque(Object o, long offset, short x) {
        try {
            putShortOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putCharOpaque(Object o, long offset, char x) {
        try {
            putCharOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putIntOpaque(Object o, long offset, int x) {
        try {
            putIntOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putFloatOpaque(Object o, long offset, float x) {
        try {
            putFloatOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putLongOpaque(Object o, long offset, long x) {
        try {
            putLongOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }

    public void putDoubleOpaque(Object o, long offset, double x) {
        try {
            putDoubleOpaqueMethod.invoke(internalUnsafe,o, offset, x);
        }catch (Throwable t){
            throwOriginalException(t);
            throw new RuntimeException(t);
        }
    }
}
