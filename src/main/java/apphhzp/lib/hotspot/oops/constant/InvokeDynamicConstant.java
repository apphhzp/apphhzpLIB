package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelper.unsafe;

public class InvokeDynamicConstant extends Constant{
    public final NameAndTypeConstant nameAndType;
    public final int bsms_index;
    public InvokeDynamicConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.InvokeDynamic);
        nameAndType=new NameAndTypeConstant(pool, unsafe.getShort(this.address+2)&0xffff);
        bsms_index=unsafe.getShort(this.address)&0xffff;
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\towner:"+this.nameAndType.name.str+
                "\n\tdesc:"+this.nameAndType.desc.str+
                "\n\tbootstrap_methods_attribute_index:"+this.bsms_index;
    }
}
