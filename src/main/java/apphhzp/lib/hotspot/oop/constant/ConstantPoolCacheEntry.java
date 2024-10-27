package apphhzp.lib.hotspot.oop.constant;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oop.Klass;
import apphhzp.lib.hotspot.oop.method.Method;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelper.unsafe;

// _flags     [tos|0|F=1|0|0|0|f|v|0 |0000|field_index] (for field entries)
// bit length [ 4 |1| 1 |1|1|1|1|1|1 |1     |-3-|----16-----]
// _flags     [tos|0|F=0|S|A|I|f|0|vf|indy_rf|000|00000|psize] (for method entries)
// bit length [ 4 |1| 1 |1|1|1|1|1|1 |-4--|--8--|--8--]
public class ConstantPoolCacheEntry extends JVMObject {
    public static final Type TYPE = JVM.type("ConstantPoolCacheEntry");
    public static final int SIZE = TYPE.size;
    public static final long INDICES_OFFSET = TYPE.offset("_indices");
    public static final long F1_OFFSET = TYPE.offset("_f1");
    public static final long F2_OFFSET = TYPE.offset("_f2");
    public static final long FLAGS_OFFSET = TYPE.offset("_flags");
    public static final int is_volatile_shift = JVM.intConstant("ConstantPoolCacheEntry::is_volatile_shift");
    public static final int is_final_shift = JVM.intConstant("ConstantPoolCacheEntry::is_final_shift");
    public static final int is_forced_virtual_shift = JVM.intConstant("ConstantPoolCacheEntry::is_forced_virtual_shift");
    public static final int is_vfinal_shift = JVM.intConstant("ConstantPoolCacheEntry::is_vfinal_shift");
    public static final int is_field_entry_shift = JVM.intConstant("ConstantPoolCacheEntry::is_field_entry_shift");
    public static final int tos_state_shift = JVM.intConstant("ConstantPoolCacheEntry::tos_state_shift");
    public final ConstantPoolCache holder;

    public ConstantPoolCacheEntry(long addr,ConstantPoolCache cache) {
        super(addr);
        this.holder=cache;
    }

    /* bit number |31                0|
     * bit length |-8--|-8--|---16----|
     * --------------------------------
     * _indices   [ b2 | b1 |  index  ]  index = constant_pool_index
     */
    @SuppressWarnings("GrazieInspection")
    public long getIndices() {
        return unsafe.getAddress(this.address + INDICES_OFFSET);
    }

    public int getB1(){
        return (int)((this.getIndices()>>16)&0xffL);
    }

    public int getB2(){
        return (int)((this.getIndices()>>24)&0xffL);
    }

    public int getConstantPoolIndex(){
        return (int) (this.getIndices()&0xffffL);
    }

    public void setIndices(long indices) {
        unsafe.putAddress(this.address + INDICES_OFFSET, indices);
    }

    public long getF1(){
        return unsafe.getAddress(this.address+F1_OFFSET);
    }

    public void setF1(long f1){
        unsafe.putAddress(this.address+F1_OFFSET,f1);
    }

    @Nullable
    public Method f1AsMethod() {
        if (is_method_entry(this.getFlags())) {
            int code=getB1();
            if (code!= Opcodes.INVOKEINTERFACE&&code!=0){
                return Method.getOrCreate(unsafe.getAddress(this.address + F1_OFFSET));
            }
        }
        return null;
    }

    @Nullable
    public Klass f1AsKlass() {
        if (is_method_entry(this.getFlags())){
            int code=getB1();
            if (code==Opcodes.INVOKEINTERFACE){
                //https://github.com/openjdk/jdk17u/blob/c9d83d392f3809bf536afdcb52142ee6916acff0/src/hotspot/share/oops/cpCache.cpp#L518
                return Klass.getOrCreate(unsafe.getAddress(this.address+F1_OFFSET));
            }
        }
        return null;
    }

    //vtable or res_ref index, or vfinal method ptr
    public long getF2() {
        return unsafe.getAddress(this.address + F2_OFFSET);
    }

    public void setF2(long f2) {
        unsafe.putAddress(this.address + F2_OFFSET,f2);
    }

    public long f2AsOffset(){
        if (is_field_entry(this.getFlags())){
            return this.getF2();
        }
        return -1;
    }

    //The vtable/itable index for virtual calls only, unused by non-virtual.
    public int f2AsIndex(){
        long flags=this.getFlags();
        if (is_method_entry(flags)){
            if (getB2()==Opcodes.INVOKEVIRTUAL&&!isVFinal(flags)){
                return (int) this.getF2();
            }
        }
        return -1;
    }

    public Method vtableIndex2Method(int index){
        MethodRefConstant constant=this.holder.getConstantPool().getConstant(this.getConstantPoolIndex());
        Klass holder=constant.klass.resolved;
        //noinspection DataFlowIssue
        return holder.getMethodAtVTable(index);
    }


    // The is_vfinal flag indicates this is a method pointer for a final method, not an index.
    @Nullable
    public Method f2AsMethod() {
        long flags=this.getFlags();
        if (is_method_entry(flags)){
            if (isVFinal(flags)||this.getB1()==Opcodes.INVOKEINTERFACE){
                return Method.getOrCreate(this.getF2());
            }
        }
        return null;
    }

    public long getFlags() {
        return unsafe.getAddress(this.address + FLAGS_OFFSET);
    }

    public int field_index() {
        long flags = this.getFlags();
        if (is_field_entry(flags)) {
            return (int) (flags & 0xFFFFL);
        }
        return -1;
    }

    public int parameter_size() {
        long flags = this.getFlags();
        if (is_method_entry(flags)) {
            return (int) (flags & 0xFFL);
        }
        return -1;
    }

    public boolean isResolved(){
        return this.getB1()!=0||this.getB2()!=0;
    }

    // (v) is the field volatile?
    public static boolean isVolatile(long flags) {
        return (flags & (1L << is_volatile_shift)) != 0;
    }

    // (f) is the field or method final?
    public static boolean isFinal(long flags) {
        return (flags & (1L << is_final_shift)) != 0;
    }

    // (I) is the interface reference forced to virtual mode?
    public static boolean isForcedVirtual(long flags) {
        return (flags & (1L << is_forced_virtual_shift)) != 0;
    }

    // (vf) did the call resolve to a final method?
    public static boolean isVFinal(long flags) {
        return (flags & (1L << is_vfinal_shift)) != 0;
    }

    public static boolean is_method_entry(long flags) {
        return (flags & (1L << is_field_entry_shift)) == 0;
    }

    public static boolean is_field_entry(long flags) {
        return (flags & (1L << is_field_entry_shift)) != 0;
    }
}
