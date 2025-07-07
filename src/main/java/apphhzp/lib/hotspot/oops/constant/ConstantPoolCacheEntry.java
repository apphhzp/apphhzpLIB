package apphhzp.lib.hotspot.oops.constant;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.interpreter.Bytecodes;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

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
    public static final int
            tos_state_shift = JVM.intConstant("ConstantPoolCacheEntry::tos_state_shift"),
            is_field_entry_shift = JVM.intConstant("ConstantPoolCacheEntry::is_field_entry_shift"),
            has_local_signature_shift  =25,
            has_appendix_shift         = 24,
            is_forced_virtual_shift = JVM.intConstant("ConstantPoolCacheEntry::is_forced_virtual_shift"),
            is_final_shift = JVM.intConstant("ConstantPoolCacheEntry::is_final_shift"),
            is_volatile_shift = JVM.intConstant("ConstantPoolCacheEntry::is_volatile_shift"),
            is_vfinal_shift = JVM.intConstant("ConstantPoolCacheEntry::is_vfinal_shift");
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
                //https://github.com/openjdk/jdk17u/blob/master/src/hotspot/share/oops/cpCache.cpp#L518
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

    public int fieldIndex() {
        long flags = this.getFlags();
        if (is_field_entry(flags)) {
            return (int) (flags & 0xFFFFL);
        }
        return -1;
    }

    public int parameterSize() {
        long flags = this.getFlags();
        if (is_method_entry(flags)) {
            return (int) (flags & 0xFFL);
        }
        return -1;
    }

    public boolean hasAppendix() {
        return (this.getF1()!=0) && (this.getFlags() & (1 << has_appendix_shift)) != 0;
    }

    public boolean hasLocalSignature() {
        return (this.getF1()!=0) && (this.getFlags() & (1 << has_local_signature_shift)) != 0;
    }

    public Method methodIfResolved(ConstantPool cpool) {
        // Decode the action of set_method and set_interface_call
        int invoke_code = this.getB1();
        if (invoke_code != 0) {
            long f1 = this.getF1();
            if (f1 != 0L) {
                switch (invoke_code) {
                    case Bytecodes.Code._invokeinterface:
                        if (getF2()!=Bytecodes.Code._invokeinterface) {
                            throw new RuntimeException();
                        }
                        return Method.getOrCreate(getF2());
                    case Bytecodes.Code._invokestatic:
                    case Bytecodes.Code._invokespecial:
                        if (hasAppendix()){
                            throw new RuntimeException();
                        }
                    case Bytecodes.Code._invokehandle:
                    case Bytecodes.Code._invokedynamic:
                        return Method.getOrCreate(f1);
                    default:
                        break;
                }
            }
        }
        invoke_code = this.getB2();
        if (invoke_code != 0) {
            if (invoke_code == Bytecodes.Code._invokevirtual) {
                if (isVFinal(this.getFlags())) {
                    // invokevirtual
                    if (!isVFinal(this.getFlags())) {
                        throw new RuntimeException();
                    }
                    return Method.getOrCreate(getF2());
                } else {
                    int holder_index = cpool.uncached_klass_ref_index_at(this.getConstantPoolIndex());
                    if (cpool.getTags().get(holder_index) == ConstantTag.Class) {
                        Klass klass = cpool.getResolvedKlass(holder_index);
                        return klass.getMethodAtVTable(this.f2AsIndex());
                    }
                }
            }
        }
        return null;
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
