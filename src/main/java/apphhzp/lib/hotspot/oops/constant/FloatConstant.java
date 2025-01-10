package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelper.unsafe;

public class FloatConstant extends Constant{
    public final float val;
    public FloatConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.Float);
        this.val= unsafe.getFloat(this.address);
    }

    public static void modify(long addr,float newVal){
        unsafe.putFloat(addr,newVal);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tvalue:"+this.val;
    }
}
