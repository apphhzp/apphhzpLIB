package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.hotspot.JVMObject;

public class Constant extends JVMObject {
    public final ConstantPool constantPool;
    public final int which;
    public final byte tag;
    public Constant(ConstantPool pool,int which,byte tag){
        super(pool.constantAddress(which));
        this.constantPool=pool;
        this.which =which;
        this.tag=tag;
    }

    @Override
    public String toString() {
        return ConstantTag.getTagName(this.tag)+"Constant@0x"+Long.toHexString(this.address)+" in ConstantPool@0x"+Long.toHexString(constantPool.address)+"["+this.which +"]";
    }
}
