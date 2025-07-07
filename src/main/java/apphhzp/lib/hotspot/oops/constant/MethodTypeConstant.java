package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class MethodTypeConstant extends Constant{
    public final Utf8Constant type_desc;
    public MethodTypeConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.MethodType);
        this.type_desc=new Utf8Constant(pool, unsafe.getShort(this.address)&0xffff);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tref_type:"+type_desc.str;
    }
}
