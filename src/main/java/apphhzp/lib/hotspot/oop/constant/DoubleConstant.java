package apphhzp.lib.hotspot.oop.constant;

import static apphhzp.lib.ClassHelper.unsafe;

public class DoubleConstant extends Constant{
    public final double val;
    public DoubleConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.Double);
        this.val= unsafe.getDouble(this.address);
    }

    public static void modify(long addr,double newValue){
        unsafe.putDouble(addr,newValue);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tvalue:"+this.val;
    }
}
