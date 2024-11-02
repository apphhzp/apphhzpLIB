package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.oop.method.Method;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

public class CompiledMethod extends CodeBlob {
    public static final Type TYPE= JVM.type("CompiledMethod");
    public static final int SIZE=TYPE.size;
    public static final long MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET=CodeBlob.SIZE;
    public static final long METHOD_OFFSET=TYPE.offset("_method");
    private Method methodCache;
    public CompiledMethod(long addr) {
        super(addr,TYPE);
    }

    public CompiledMethod(long addr,Type type){
        super(addr,type);
    }

    public Method getMethod(){
        long addr= unsafe.getAddress(this.address+METHOD_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.methodCache,addr)){
            this.methodCache=Method.getOrCreate(addr);
        }
        return this.methodCache;
    }

    public void setMethod(@Nullable Method method){
        unsafe.putAddress(this.address+METHOD_OFFSET,method==null?0L:method.address);
    }
    public void  markForDeoptimization(boolean inc_recompile_counts){
        unsafe.putInt(this.address+MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET,inc_recompile_counts?1:2);
    }
    public int getMarkForDeoptimizationStatus(){
        return unsafe.getInt(this.address+MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET);
    }

    public boolean isMarkedForDeoptimization(){
        return this.getMarkForDeoptimizationStatus()!=0;
    }

    public boolean makeNotEntrant(){
        return false;
    }

    public void clearICCallsites() {
//        ResourceMark rm;
//        RelocIterator iter(this);
//        while(iter.next()) {
//            if (iter.type() == relocInfo::virtual_call_type) {
//                CompiledIC* ic = CompiledIC_at(&iter);
//                ic->set_to_clean(false);
//            }
//        }
    }

    public long getVerifiedEntryPoint(){
        return 0L;
    }
    public static final class States{
        public static final int
                not_installed = -1, // in construction, only the owner doing the construction is allowed to advance state
                in_use = 0,  // executable nmethod
                not_used = 1,  // not entrant, but revivable
                not_entrant = 2,  // marked for deoptimization but activations may still exist,will be transformed to zombie when all activations are gone
                unloaded = 3,  // there should be no activations, should not be called, will be
                zombie = 4;// transformed to zombie by the sweeper, when not "locked in vm".
    }
}
