package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.hotspot.oops.Symbol;

import static apphhzp.lib.ClassHelper.unsafe;

public class Utf8Constant extends Constant{
    public final Symbol str;
    public Utf8Constant(ConstantPool pool, int which) {
        super(pool, which,  ConstantTag.Utf8);
        str=Symbol.of(unsafe.getAddress(this.address));
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tlength:"+this.str.getLength()+
                "\n\tstr:"+this.str;
    }
}
