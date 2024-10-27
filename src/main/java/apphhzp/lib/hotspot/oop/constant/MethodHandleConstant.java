package apphhzp.lib.hotspot.oop.constant;

import static apphhzp.lib.ClassHelper.unsafe;

public class MethodHandleConstant extends Constant{
    public final int refType;
    public final MethodRefConstant refMethod;
    public MethodHandleConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.MethodHandle);
        refType= unsafe.getShort(this.address)&0xffff;
        refMethod=new MethodRefConstant(pool,unsafe.getShort(this.address+2)&0xffff);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\trefType:"+ConstantTag.getREFTypeName(this.refType)+
                "\n\tref_owner:"+this.refMethod.klass.name.str+
                "\n\tref_name:"+this.refMethod.nameAndType.name.str+
                "\n\tref_desc:"+this.refMethod.nameAndType.desc.str;
    }
}
