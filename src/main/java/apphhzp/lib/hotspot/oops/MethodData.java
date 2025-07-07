package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oops.method.Method;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class MethodData extends Metadata {
    public static final Type TYPE= JVM.type("MethodData");
    public static final int SIZE=TYPE.size;
    public static final long METHOD_OFFSET=TYPE.offset("_method");
    public static final long SIZE_OFFSET=TYPE.offset("_size");
    public static final long NOF_DECOMPILES_OFFSET=TYPE.offset("_compiler_counters._nof_decompiles");
    public static final long NOF_OVERFLOW_RECOMPILES_OFFSET=TYPE.offset("_compiler_counters._nof_overflow_recompiles");
    public static final long NOF_OVERFLOW_TRAPS_OFFSET=TYPE.offset("_compiler_counters._nof_overflow_traps");
    public static final long TRAP_HIST_ARRAY_OFFSET=TYPE.offset("_compiler_counters._trap_hist._array[0]");
    public static final long EFLAGS_OFFSET=TYPE.offset("_eflags");
    public static final long ARG_LOCAL_OFFSET=TYPE.offset("_arg_local");
    private Method methodCache;

    public MethodData(long addr) {
        super(addr);
    }

    public Method getMethod(){
        long addr=unsafe.getAddress(this.address+METHOD_OFFSET);
        if (!isEqual(this.methodCache,addr)){
            this.methodCache=Method.getOrCreate(addr);
        }
        return this.methodCache;
    }

    public long getNofDecompiles(){
        return unsafe.getInt(this.address+NOF_DECOMPILES_OFFSET)&0xffffffffL;
    }
    public void setNofDecompiles(long val){
        unsafe.putInt(this.address+NOF_DECOMPILES_OFFSET,(int) (val&0xffffffffL));
    }

    public long incNofDecompiles(){
        long re=this.getNofDecompiles()+1;
        this.setNofDecompiles(re);
        return re;
    }

    public long incDecompileCount(){
        long dec_count = incNofDecompiles();
        if (dec_count > JVM.PerMethodRecompilationCutoff) {
            //this.getMethod().setNotCompilable("decompile_count > PerMethodRecompilationCutoff", CompLevel.FULL_OPTIMIZATION);
        }
        return dec_count;
    }

    @Override
    public String toString() {
        return "MethodData@0x"+Long.toHexString(this.address);
    }
}
