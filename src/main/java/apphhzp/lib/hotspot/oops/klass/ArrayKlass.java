package apphhzp.lib.hotspot.oops.klass;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class ArrayKlass extends Klass {
    public static final Type TYPE = JVM.type("ArrayKlass");
    public static final int SIZE = TYPE.size;
    public static final long DIMENSION_OFFSET=TYPE.offset("_dimension");
    public static final long HIGHER_DIMENSION_OFFSET=TYPE.offset("_higher_dimension");
    public static final long LOWER_DIMENSION_OFFSET=TYPE.offset("_lower_dimension");
    private Klass higherDimensionCache;
    private Klass lowerDimensionCache;
    protected ArrayKlass(long addr) {
        super(addr);
    }

    public int getDimension() {
        return unsafe.getInt(this, DIMENSION_OFFSET);
    }

    @Nullable
    public Klass getHigherDimension(){
        long addr= unsafe.getAddress(this.address+HIGHER_DIMENSION_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!JVMObject.isEqual(this.higherDimensionCache, addr)) {
            this.higherDimensionCache=Klass.getOrCreate(addr);
        }
        return this.higherDimensionCache;
    }

    @Nullable
    public Klass getLowerDimension(){
        long addr= unsafe.getAddress(this.address+LOWER_DIMENSION_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!JVMObject.isEqual(this.lowerDimensionCache, addr)) {
            this.lowerDimensionCache=Klass.getOrCreate(addr);
        }
        return this.lowerDimensionCache;
    }

    public int getLog2ElementSize() {
        return (this.getLayout() >> LayoutHelper._lh_log2_element_size_shift) & 0xff;
    }

    public int getArrayHeaderInBytes() {
        return (this.getLayout() >> LayoutHelper._lh_header_size_shift) & 0xff;
    }

    @Override
    public String toString() {
        return "Array"+super.toString();
    }
}
