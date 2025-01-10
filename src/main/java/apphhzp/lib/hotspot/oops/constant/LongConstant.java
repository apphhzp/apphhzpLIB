package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelper.unsafe;

public class LongConstant extends Constant{
    public final long val;
    public LongConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.Long);
        val= unsafe.getLong(this.address);
    }

    public static void modify(long addr,long newVal){
        unsafe.putLong(addr,newVal);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tvalue:"+this.val;
    }
}
