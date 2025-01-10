package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelper.unsafe;

public class IntegerConstant extends Constant{
    public final int val;
    public IntegerConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.Integer);
        val= unsafe.getInt(this.address);
    }

    public static void modify(long addr,int newVal){
        unsafe.putInt(addr,newVal);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tvalue:"+this.val;
    }
}
