package apphhzp.lib.hotspot.ci;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.compiler.CompileTask;
import apphhzp.lib.hotspot.opto.Compile;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class CiEnv extends JVMObject {
    public static final Type TYPE = JVM.type("ciEnv");
    public static final int SIZE = TYPE.size;
    public static final long TASK_OFFSET=TYPE.offset("_task");
    public static final long COMPILER_OFFSET = TYPE.offset("_compiler_data");
    private CompileTask taskCache;
    private Compile compileCache;

    public CiEnv(long addr) {
        super(addr);
    }

    public CompileTask getCompileTask(){
        long addr=unsafe.getAddress(this.address+TASK_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.taskCache,addr)){
            this.taskCache=new CompileTask(addr);
        }
        return this.taskCache;
    }

    @Nullable
    public Compile getCompiler() {
        long addr = unsafe.getAddress(this.address + COMPILER_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.compileCache, addr)) {
            this.compileCache = new Compile(addr);
        }
        return this.compileCache;
    }

    @Override
    public String toString() {
        return "ciEnv@0x"+Long.toHexString(this.address);
    }
}
