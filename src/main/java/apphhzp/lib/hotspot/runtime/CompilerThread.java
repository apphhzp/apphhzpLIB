package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.ci.CiEnv;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class CompilerThread extends JavaThread{
    public static final Type TYPE= JVM.type("CompilerThread");
    public static final int SIZE=TYPE.size;
    public static final long ENV_OFFSET=TYPE.offset("_env");
    private CiEnv envCache;
    protected CompilerThread(long addr) {
        super(addr);
    }

    @Nullable
    public synchronized CiEnv getCiEnv(){
        long addr= unsafe.getAddress(this.address+ENV_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.envCache,addr)){
            this.envCache=new CiEnv(addr);
        }
        return this.envCache;
    }

    @Override
    public String toString() {
        return "CompilerThread@0x"+Long.toHexString(this.address);
    }
}
