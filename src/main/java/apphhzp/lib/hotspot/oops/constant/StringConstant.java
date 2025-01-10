package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.hotspot.oops.Symbol;

public class StringConstant extends Constant{
    public final Symbol str;
    public StringConstant(ConstantPool pool, int which) {
        super(pool, which, ConstantTag.String);
        str=Symbol.of(ClassHelper.unsafe.getAddress(this.address));
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tstr:"+this.str;
    }
}
