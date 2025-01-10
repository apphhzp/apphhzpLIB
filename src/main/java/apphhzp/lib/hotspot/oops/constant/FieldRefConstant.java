package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelper.unsafe;

public class FieldRefConstant extends Constant{
    public final ClassConstant klass;
    public final NameAndTypeConstant nameAndType;
    public FieldRefConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.Fieldref);
        klass=new ClassConstant(pool, unsafe.getShort(this.address)&0xffff);
        nameAndType=new NameAndTypeConstant(pool, unsafe.getShort(this.address+2)&0xffff);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\towner:"+klass.name.str+
                "\n\tname:"+nameAndType.name.str+
                "\n\tdesc:"+nameAndType.desc.str;
    }
}
