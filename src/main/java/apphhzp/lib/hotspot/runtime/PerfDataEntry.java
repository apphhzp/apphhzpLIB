package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.utilities.BasicType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class PerfDataEntry extends JVMObject {
    public static final Type TYPE= JVM.type("PerfDataEntry");
    public static final int SIZE=TYPE.size;
    public static final long ENTRY_LENGTH_OFFSET=TYPE.offset("entry_length");
    public static final long NAME_OFFSET_OFFSET=TYPE.offset("name_offset");
    public static final long VECTOR_LENGTH_OFFSET=TYPE.offset("vector_length");
    public static final long DATA_TYPE_OFFSET=TYPE.offset("data_type");
    public static final long FLAGS_OFFSET=TYPE.offset("flags");
    public static final long DATA_UNITS_OFFSET=TYPE.offset("data_units");
    public static final long DATA_VARIABILITY_OFFSET=TYPE.offset("data_variability");
    public static final long DATA_OFFSET_OFFSET=TYPE.offset("data_offset");
    public PerfDataEntry(long addr) {
        super(addr);
    }

    public int entryLength() {
        return unsafe.getInt(this.address+ENTRY_LENGTH_OFFSET);
    }

    public int nameOffset(){
        return unsafe.getInt(this.address+NAME_OFFSET_OFFSET);
    }

    public int vectorLength() {
        return unsafe.getInt(this.address+VECTOR_LENGTH_OFFSET);
    }

    public int dataType(){
        return BasicType.charToBasicType((char) unsafe.getByte(this.address+DATA_TYPE_OFFSET));
    }

    public byte flags(){
        return unsafe.getByte(this.address+FLAGS_OFFSET);
    }

    public byte dataUnits(){
        return unsafe.getByte(this.address+DATA_UNITS_OFFSET);
    }

    public byte dataVariability(){
        return unsafe.getByte(this.address+DATA_VARIABILITY_OFFSET);
    }

    public int dataOffset(){
        return unsafe.getInt(this.address+DATA_OFFSET_OFFSET);
    }

    public static final class Units{
        public static final int U_None=JVM.intConstant("PerfData::U_None"),
                U_Bytes=JVM.intConstant("PerfData::U_Bytes"),
                U_Ticks=JVM.intConstant("PerfData::U_Ticks"),
                U_Events=JVM.intConstant("PerfData::U_Events"),
                U_String=JVM.intConstant("PerfData::U_String"),
                U_Hertz=JVM.intConstant("PerfData::U_Hertz");
    }

    public static final class Flags{
        public static final int F_None=0x0,F_Supported=0x1;
    }

    public String name(){
        return JVM.getString(this.address+this.nameOffset());
    }

    public boolean supported() {
        return (flags() & Flags.F_Supported) != 0;
    }

    public boolean booleanValue() {
        if (!(vectorLength()==0&&dataType() ==BasicType.T_BOOLEAN)) {
            throw new UnsupportedOperationException("not a boolean");
        }
        return unsafe.getByte(this.address+this.dataOffset())!=0;
    }

    public byte byteValue(){
        if (!(vectorLength()==0&&dataType() ==BasicType.T_BYTE)) {
            throw new UnsupportedOperationException("not a byte");
        }
        return unsafe.getByte(this.address+this.dataOffset());
    }

    public char charValue(){
        if (!(vectorLength()==0&&dataType() ==BasicType.T_CHAR)) {
            throw new UnsupportedOperationException("not a char");
        }
        return  unsafe.getChar(this.address+this.dataOffset());
    }

    public short shortValue(){
        if (!(vectorLength()==0&&dataType() ==BasicType.T_SHORT)) {
            throw new UnsupportedOperationException("not a short");
        }
        return unsafe.getShort(this.address+this.dataOffset());
    }

    public int intValue(){
        if (!(vectorLength()==0&&dataType() ==BasicType.T_INT)) {
            throw new UnsupportedOperationException("not a int");
        }
        return unsafe.getInt(this.address+this.dataOffset());
    }

    public long longValue(){
        if (!(vectorLength()==0&&dataType() ==BasicType.T_LONG)) {
            throw new UnsupportedOperationException("not a long");
        }
        return unsafe.getLong(this.address+this.dataOffset());
    }

    public float floatValue(){
        if (!(vectorLength()==0&&dataType() ==BasicType.T_FLOAT)) {
            throw new UnsupportedOperationException("not a float");
        }
        return unsafe.getFloat(this.address+this.dataOffset());
    }

    public double doubleValue(){
        if (!(vectorLength()==0&&dataType() ==BasicType.T_DOUBLE)) {
            throw new UnsupportedOperationException("not a double");
        }
        return unsafe.getDouble(this.address+this.dataOffset());
    }

    public boolean[] booleanArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_BOOLEAN)) {
            throw new UnsupportedOperationException("not a boolean vector");
        }
        boolean[] res = new boolean[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] = unsafe.getByte(address+off+i)!=0;
        }
        return res;
    }

    public char[] charArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_CHAR)) {
            throw new UnsupportedOperationException("not a boolean vector");
        }
        char[] res = new char[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] =  unsafe.getChar(address+off+i*2L);
        }
        return res;
    }

    public byte[] byteArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_BYTE)) {
            throw new UnsupportedOperationException("not a byte vector");
        }
        byte[] res = new byte[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] = unsafe.getByte(address+off+i);
        }
        return res;
    }

    public short[] shortArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_SHORT)) {
            throw new UnsupportedOperationException("not a short vector");
        }
        short[] res = new short[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] = unsafe.getShort(address+off+i*2L);
        }
        return res;
    }

    public int[] intArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_INT)) {
            throw new UnsupportedOperationException("not a int vector");
        }
        int[] res = new int[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] = unsafe.getInt(address+off+i*4L);
        }
        return res;
    }

    public long[] longArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_LONG)) {
            throw new UnsupportedOperationException("not a long vector");
        }
        long[] res = new long[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] = unsafe.getLong(address+off+i*8L);
        }
        return res;
    }

    public float[] floatArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_FLOAT)) {
            throw new UnsupportedOperationException("not a float vector");
        }

        float[] res = new float[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] = unsafe.getFloat(address+off+i*4L);
        }
        return res;
    }

    public double[] doubleArrayValue() {
        int len = vectorLength();
        if (!(len > 0 &&dataType()==BasicType.T_DOUBLE)) {
            throw new UnsupportedOperationException("not a double vector");
        }
        double[] res = new double[len];
        final int off = dataOffset();
        for (int i = 0; i < len; i++) {
            res[i] = unsafe.getDouble(address+off+i*8L);
        }
        return res;
    }

    public String valueAsString() {
        int dataType = dataType();
        int len = vectorLength();
        String str = null;
        if (len == 0) { // scalar
            if (dataType == BasicType.T_BOOLEAN) {
                str = Boolean.toString(booleanValue());
            } else if (dataType == BasicType.T_CHAR) {
                str = "'" + charValue() + "'";
            } else if (dataType == BasicType.T_BYTE) {
                str = Byte.toString(byteValue());
            } else if (dataType == BasicType.T_SHORT) {
                str = Short.toString(shortValue());
            } else if (dataType ==  BasicType.T_INT) {
                str = Integer.toString(intValue());
            } else if (dataType == BasicType.T_LONG) {
                str = Long.toString(longValue());
            } else if (dataType == BasicType.T_FLOAT) {
                str = Float.toString(floatValue());
            } else if (dataType == BasicType.T_DOUBLE) {
                str = Double.toString(doubleValue());
            } else {
                str = "<unknown scalar value>";
            }
        } else { // vector
            if (dataType == BasicType.T_BOOLEAN) {
                boolean[] res = booleanArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.T_CHAR) {
                // char[] is returned as a String
                str = new String(charArrayValue());
            } else if (dataType == BasicType.T_BYTE) {
                // byte[] is returned as a String
                byte[] val=byteArrayValue();
                for (int i=0;i<val.length;i++){
                    if (val[i]==0){
                        val= Arrays.copyOf(val,i);
                        break;
                    }
                }
                str = new String(val,StandardCharsets.US_ASCII);  //CStringUtilities.getString(addr.addOffsetTo(dataOffset()), );
            } else if (dataType == BasicType.T_SHORT) {
                short[] res = shortArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType ==  BasicType.T_INT) {
                int[] res = intArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.T_LONG) {
                long[] res = longArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.T_FLOAT) {
                float[] res = floatArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.T_DOUBLE) {
                double[] res = doubleArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else {
                str = "<unknown vector value>";
            }
        }

        // add units
        int dataUnitsValue = dataUnits();

        if (dataUnitsValue == Units.U_Bytes) {
            str += " byte(s)";
        } else if (dataUnitsValue == Units.U_Ticks) {
            str += " tick(s)";
        } else if (dataUnitsValue == Units.U_Events) {
            str += " event(s)";
        } else if (dataUnitsValue == Units.U_Hertz) {
            str += " Hz";
        }

        return str;
    }

    @Override
    public String toString() {
        return "PerfDataEntry@0x"+Long.toHexString(this.address);
    }
}
