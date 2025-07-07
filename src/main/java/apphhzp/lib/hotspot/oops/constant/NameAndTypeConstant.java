package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class NameAndTypeConstant extends Constant{
    public final Utf8Constant name;
    public final Utf8Constant desc;
    public NameAndTypeConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.NameAndType);
        name=new Utf8Constant(pool, unsafe.getShort(this.address)&0xffff);
        desc=new Utf8Constant(pool, unsafe.getShort(this.address+2)&0xffff);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tname:"+name.str+
                "\n\tdesc:"+desc.str;
    }
}
