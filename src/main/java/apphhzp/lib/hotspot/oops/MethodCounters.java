package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.compiler.CompLevel;
import apphhzp.lib.hotspot.interpreter.InvocationCounter;

import static apphhzp.lib.ClassHelper.unsafe;

public class MethodCounters extends JVMObject {
    public static final Type TYPE= JVM.type("MethodCounters");
    public static final int SIZE=TYPE.size;
    public static final long INVOCATION_COUNTER_OFFSET=TYPE.offset("_invocation_counter");
    public static final long BACKEDGE_COUNTER_OFFSET=TYPE.offset("_backedge_counter");
    public static final long NMETHOD_AGE_OFFSET=TYPE.offset("_nmethod_age");
    public static final long INVOKE_MASK_OFFSET=TYPE.offset("_invoke_mask");
    public static final long BACKEDGE_MASK_OFFSET=TYPE.offset("_backedge_mask");
    public static final long INTERPRETER_THROWOUT_COUNT_OFFSET=JVM.usingServerCompiler?TYPE.offset("_interpreter_throwout_count"):-1;
    public static final long NUMBER_OF_BREAKPOINTS_OFFSET=JVM.includeJVMTI ?TYPE.offset("_number_of_breakpoints"):-1;
    public static final long HIGHEST_COMP_LEVEL_OFFSET=(INTERPRETER_THROWOUT_COUNT_OFFSET==-1?BACKEDGE_MASK_OFFSET+8:(NUMBER_OF_BREAKPOINTS_OFFSET==-1?INTERPRETER_THROWOUT_COUNT_OFFSET+2:NUMBER_OF_BREAKPOINTS_OFFSET+2));
    public static final long HIGHEST_OSR_COMP_LEVEL_OFFSET =HIGHEST_COMP_LEVEL_OFFSET+1;
    public final InvocationCounter invocationCounter;
    public final InvocationCounter backedgeCounter;
    public MethodCounters(long addr) {
        super(addr);
        this.invocationCounter=new InvocationCounter(this.address+INVOCATION_COUNTER_OFFSET);
        this.backedgeCounter=new InvocationCounter(this.address+BACKEDGE_COUNTER_OFFSET);
    }

    public int getNMethodAge(){
        return unsafe.getInt(this.address+NMETHOD_AGE_OFFSET);
    }

    public void setNMethodAge(int age){
        unsafe.putInt(this.address+NMETHOD_AGE_OFFSET,age);
    }

    public int getInvokeMask(){
        return unsafe.getInt(this.address+INVOKE_MASK_OFFSET);
    }

    public void setInvokeMask(int mask){
        unsafe.putInt(this.address+INVOKE_MASK_OFFSET,mask);
    }

    public int getBackedgeMask(){
        return unsafe.getInt(this.address+BACKEDGE_MASK_OFFSET);
    }

    public void setBackedgeMask(int mask){
        unsafe.putInt(this.address+BACKEDGE_MASK_OFFSET,mask);
    }

    public int getInterpreterThrowoutCount(){
        if (!JVM.usingServerCompiler){
            return 0;
        }
        return unsafe.getShort(this.address+INTERPRETER_THROWOUT_COUNT_OFFSET)&0xffff;
    }

    public void setInterpreterThrowoutCount(int count){
        if (JVM.usingServerCompiler){
            unsafe.putShort(this.address+INTERPRETER_THROWOUT_COUNT_OFFSET, (short) (count&0xffff));
        }
    }

    public int getNumberOfBreakpoints(){
        if (!JVM.includeJVMTI){
            return 0;
        }
        return unsafe.getShort(this.address+NUMBER_OF_BREAKPOINTS_OFFSET)&0xffff;
    }

    public void setNumberOfBreakpoints(int number){
        if (JVM.includeJVMTI){
            unsafe.putShort(this.address+NUMBER_OF_BREAKPOINTS_OFFSET, (short) (number&0xffff));
        }
    }

    public CompLevel getHighestCompLevel(){
        return CompLevel.of(unsafe.getByte(this.address+HIGHEST_COMP_LEVEL_OFFSET));
    }
    public void setHighestCompLevel(CompLevel level){
        unsafe.putByte(this.address+HIGHEST_COMP_LEVEL_OFFSET, (byte) level.id);
    }

    public CompLevel getHighestOsrCompLevel(){
        return CompLevel.of(unsafe.getByte(this.address+ HIGHEST_OSR_COMP_LEVEL_OFFSET));
    }
    public void setHighestOsrCompLevel(CompLevel level){
        unsafe.putByte(this.address+ HIGHEST_OSR_COMP_LEVEL_OFFSET, (byte) level.id);
    }
}
