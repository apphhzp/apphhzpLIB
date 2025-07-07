package apphhzp.lib.hotspot.oops.constant;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class MethodRefConstant extends Constant{
    public final ClassConstant klass;
    public final NameAndTypeConstant nameAndType;
    public MethodRefConstant(ConstantPool pool, int which) {
        super(pool, which,pool.getTags().get(which));
        klass=new ClassConstant(pool,unsafe.getShort(this.address)&0xffff);
        nameAndType=new NameAndTypeConstant(pool,unsafe.getShort(this.address+2)&0xffff);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\towner:"+klass.name.str+
                "\n\tmethodName:"+nameAndType.name.str+
                "\n\tmethodDesc:"+nameAndType.desc.str;
    }
}
