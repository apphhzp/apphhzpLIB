package apphhzp.lib.hotspot.oop.constant;

import apphhzp.lib.hotspot.oop.Klass;

import static apphhzp.lib.ClassHelper.unsafe;

public class ClassConstant extends Constant{
    public final Utf8Constant name;
    public final Klass resolved;
    public final int resolvedKlassIndex;
    public ClassConstant(ConstantPool pool, int which) {
        super(pool, which,pool.getTags().get(which));
        resolvedKlassIndex=unsafe.getShort(this.address)&0xffff;
        if (pool.getResolvedKlasses().getAddress(resolvedKlassIndex)!=0L) {
            resolved = pool.getResolvedKlass(resolvedKlassIndex);
        }else {
            resolved=null;
        }
        name=new Utf8Constant(pool,unsafe.getShort(this.address+2)&0xffff);
    }

    @Override
    public String toString() {
        return super.toString()+
                "\n\tname:"+name.str+
                "\n\tresolvedKlass:"+(this.resolved==null?"null":resolved.getName()+"@0x"+Long.toHexString(resolved.address));
    }
}
