package apphhzp.lib.hotspot.oop.constant;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.hotspot.oop.Symbol;

public class StringConstant extends Constant{
    public final Symbol str;
    public StringConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.String);
        str=new Symbol(ClassHelper.unsafe.getAddress(this.address));
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tstr:"+this.str;
    }
}
