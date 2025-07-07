package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.interpreter.InvocationCounter;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.util.RawCType;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class MethodCounters extends JVMObject {
    public static final Type TYPE= JVM.type("MethodCounters");
    public static final int SIZE=TYPE.size;
    public static final long INVOCATION_COUNTER_OFFSET=TYPE.offset("_invocation_counter");
    public static final long BACKEDGE_COUNTER_OFFSET=TYPE.offset("_backedge_counter");
    public static final long PREV_TIME_OFFSET=JVM.computeOffset(8,BACKEDGE_COUNTER_OFFSET+InvocationCounter.SIZE);
    public static final long RATE_OFFSET=JVM.computeOffset(4,PREV_TIME_OFFSET+8);
    public static final long NMETHOD_AGE_OFFSET=TYPE.offset("_nmethod_age");
    public static final long INVOKE_MASK_OFFSET=TYPE.offset("_invoke_mask");
    public static final long BACKEDGE_MASK_OFFSET=TYPE.offset("_backedge_mask");
    public static final long PREV_EVENT_COUNT_OFFSET=JVM.computeOffset(JVM.intSize,BACKEDGE_MASK_OFFSET+JVM.intSize);
    public static final long INTERPRETER_THROWOUT_COUNT_OFFSET=JVM.usingServerCompiler?TYPE.offset("_interpreter_throwout_count"):-1;
    public static final long NUMBER_OF_BREAKPOINTS_OFFSET=JVM.includeJVMTI ?TYPE.offset("_number_of_breakpoints"):-1;
    public static final long HIGHEST_COMP_LEVEL_OFFSET=NUMBER_OF_BREAKPOINTS_OFFSET==-1?(INTERPRETER_THROWOUT_COUNT_OFFSET==-1?JVM.computeOffset(1,PREV_EVENT_COUNT_OFFSET+JVM.intSize):JVM.computeOffset(1,INTERPRETER_THROWOUT_COUNT_OFFSET+2)):(JVM.computeOffset(1,NUMBER_OF_BREAKPOINTS_OFFSET+2));
    public static final long HIGHEST_OSR_COMP_LEVEL_OFFSET =JVM.computeOffset(1,HIGHEST_COMP_LEVEL_OFFSET+1);
    public static final int HotMethodDetectionLimit= (int) JVM.getFlag("HotMethodDetectionLimit").getIntx();
    static {
        JVM.assertOffset(NMETHOD_AGE_OFFSET,JVM.computeOffset(JVM.intSize,RATE_OFFSET+4));
        JVM.assertOffset(SIZE,JVM.computeOffset(8,HIGHEST_OSR_COMP_LEVEL_OFFSET+1));
    }

    public final InvocationCounter invocationCounter;
    public final InvocationCounter backedgeCounter;
    public MethodCounters(long addr) {
        super(addr);
        this.invocationCounter=new InvocationCounter(this.address+INVOCATION_COUNTER_OFFSET);
        this.backedgeCounter=new InvocationCounter(this.address+BACKEDGE_COUNTER_OFFSET);
    }

    public static MethodCounters allocate(Method m, JavaThread thread) {
        if (true){
            throw new UnsupportedOperationException("This is hard to do...");
        }
        long addr=unsafe.allocateMemory((long) method_counters_size() *JVM.wordSize);
        MethodCounters re=new MethodCounters(addr);
        unsafe.setMemory(addr,(long) method_counters_size() *JVM.wordSize, (byte) 0);
        re.set_prev_time(0);
        re.set_rate(0);
        re.setNMethodAge(2147483647);
        re.setHighestCompLevel(0);
        re.setHighestOsrCompLevel(0);
        re.setInterpreterThrowoutCount(0);
        re.setNumberOfBreakpoints(0);
        if (JVM.getFlag("StressCodeAging").getBool()){
            re.setNMethodAge((int) JVM.getFlag("HotMethodDetectionLimit").getIntx());
        }
        double scale=1.0;

        return re;
    }
    public static int method_counters_size() {
        return (int) (JVM.alignUp(SIZE, JVM.wordSize) / JVM.wordSize);
    }
    public long prev_time(){
        return unsafe.getLong(this.address+PREV_TIME_OFFSET);
    }

    public void set_prev_time(long time) {
        unsafe.putLong(this.address+PREV_TIME_OFFSET, time);
    }

    public float rate(){
        return unsafe.getFloat(this.address+RATE_OFFSET);
    }

    public void set_rate(float rate){
        unsafe.putFloat(this.address+RATE_OFFSET, rate);
    }

    public int getNMethodAge(){
        return unsafe.getInt(this.address+NMETHOD_AGE_OFFSET);
    }

    public void setNMethodAge(int age){
        unsafe.putInt(this.address+NMETHOD_AGE_OFFSET,age);
    }

    public void reset_nmethod_age() {
        this.setNMethodAge(HotMethodDetectionLimit);
    }

    public static boolean is_nmethod_hot(int age){
        return age <= 0;
    }
    public static boolean is_nmethod_warm(int age){
        return age < HotMethodDetectionLimit;
    }
    public static boolean is_nmethod_age_unset(int age){
        return age > HotMethodDetectionLimit;
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
        }else {
            if (count!=0){
                throw new IllegalArgumentException(String.valueOf(count));
            }
        }
    }

    public int getNumberOfBreakpoints(){
        if (!JVM.includeJVMTI){
            throw new UnsupportedOperationException();
        }
        return unsafe.getShort(this.address+NUMBER_OF_BREAKPOINTS_OFFSET)&0xffff;
    }

    public void setNumberOfBreakpoints(int number){
        if (JVM.includeJVMTI){
            unsafe.putShort(this.address+NUMBER_OF_BREAKPOINTS_OFFSET, (short) (number&0xffff));
        }else {
            throw new UnsupportedOperationException();
        }
    }

    public @RawCType("CompLevel") int getHighestCompLevel(){
        return unsafe.getByte(this.address+HIGHEST_COMP_LEVEL_OFFSET)&0xff;
    }
    public void setHighestCompLevel(@RawCType("CompLevel")int level){
        unsafe.putByte(this.address+HIGHEST_COMP_LEVEL_OFFSET, (byte) (level&0xff));
    }

    public @RawCType("CompLevel") int getHighestOsrCompLevel(){
        return (unsafe.getByte(this.address+ HIGHEST_OSR_COMP_LEVEL_OFFSET)&0xff);
    }
    public void setHighestOsrCompLevel(@RawCType("CompLevel") int level){
        unsafe.putByte(this.address+ HIGHEST_OSR_COMP_LEVEL_OFFSET, (byte) (level&0xff));
    }
}
