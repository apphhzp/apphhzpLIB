package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.code.reloc.RelocInfo;
import apphhzp.lib.hotspot.compiler.CompilerType;
import apphhzp.lib.hotspot.compiler.ImmutableOopMap;
import apphhzp.lib.hotspot.compiler.ImmutableOopMapSet;
import apphhzp.lib.hotspot.runtime.JavaThread;
import apphhzp.lib.hotspot.util.RawCType;

import javax.annotation.Nullable;
import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class CodeBlob extends JVMObject {
    public static final Type TYPE = JVM.type("CodeBlob");
    public static final int SIZE = TYPE.size;
    public static final long TYPE_OFFSET=JVM.oopSize;
    public static final long SIZE_OFFSET = TYPE.offset("_size");
    public static final long HEADER_SIZE_OFFSET = TYPE.offset("_header_size");
    public static final long FRAME_COMPLETE_OFFSET_OFFSET = TYPE.offset("_frame_complete_offset");
    public static final long DATA_OFFSET_OFFSET = TYPE.offset("_data_offset");
    public static final long FRAME_SIZE_OFFSET = TYPE.offset("_frame_size");
    public static final long CODE_BEGIN_OFFSET = TYPE.offset("_code_begin");
    public static final long CODE_END_OFFSET = TYPE.offset("_code_end");
    public static final long CONTENT_BEGIN_OFFSET = TYPE.offset("_content_begin");
    public static final long DATA_END_OFFSET = TYPE.offset("_data_end");
    public static final long RELOCATION_BEGIN_OFFSET = DATA_END_OFFSET + JVM.oopSize;
    public static final long RELOCATION_END_OFFSET = RELOCATION_BEGIN_OFFSET + JVM.oopSize;
    public static final long OOP_MAPS_OFFSET = TYPE.offset("_oop_maps");
    public static final long CALLER_MUST_GC_ARGUMENTS_OFFSET = JVM.computeOffset(1, OOP_MAPS_OFFSET + JVM.oopSize);
    public static final long NAME_OFFSET = TYPE.offset("_name");
    public final Type actualType;
    private ImmutableOopMapSet oopMapsCache;

    static {
        JVM.assertOffset(SIZE_OFFSET,JVM.computeOffset(JVM.intSize,TYPE_OFFSET+JVM.intSize));
        JVM.assertOffset(CALLER_MUST_GC_ARGUMENTS_OFFSET, JVM.type("RuntimeStub").offset("_caller_must_gc_arguments"));
        JVM.assertOffset(OOP_MAPS_OFFSET, JVM.computeOffset(JVM.oopSize, RELOCATION_END_OFFSET + JVM.oopSize));
        JVM.assertOffset(NAME_OFFSET, JVM.computeOffset(JVM.oopSize, CALLER_MUST_GC_ARGUMENTS_OFFSET + 1));
    }

    @SuppressWarnings("unchecked")
    public static <T extends CodeBlob> T getCodeBlob(long addr) {
        if (addr == 0L) {
            return null;
        }
        Type type = JVM.findDynamicTypeForAddress(addr, CodeBlob.TYPE);
        if (type == null) {
            return null;
        }
        if (type == CompiledMethod.TYPE) {
            throw new RuntimeException("ShouldNotReachHere");
        } else if (type == NMethod.TYPE) {
            return (T) new NMethod(addr);
        } else if (type == RuntimeBlob.TYPE) {
            return (T) new RuntimeBlob(addr, type);
        } else if (type == BufferBlob.TYPE) {
            return (T) new BufferBlob(addr, type);
        } else if (type == AdapterBlob.TYPE) {
            return (T) new AdapterBlob(addr);
        } else if (type == VtableBlob.TYPE) {
            return (T) new VtableBlob(addr);
        } else if (type == MethodHandlesAdapterBlob.TYPE) {
            return (T) new MethodHandlesAdapterBlob(addr);
        } else if (type == RuntimeStub.TYPE) {
            return (T) new RuntimeStub(addr);
        } else if (type == SingletonBlob.TYPE) {
            return (T) new SingletonBlob(addr, type);
        } else if (type == DeoptimizationBlob.TYPE) {
            return (T) new DeoptimizationBlob(addr);
        } else if (type == ExceptionBlob.TYPE) {
            return (T) new ExceptionBlob(addr);
        } else if (type == SafepointBlob.TYPE) {
            return (T) new SafepointBlob(addr);
        } else if (type == UncommonTrapBlob.TYPE) {
            return (T) new UncommonTrapBlob(addr);
        }
        return (T) new CodeBlob(addr, type);
    }

    protected CodeBlob(long addr, Type type) {
        super(addr);
        this.actualType = type;
    }

    public int getDataOffset() {
        return unsafe.getInt(this.address + DATA_OFFSET_OFFSET);
    }

    public void setDataOffset(int offset) {
        unsafe.putInt(this.address + DATA_OFFSET_OFFSET, offset);
    }

    // Casting
    public NMethod as_nmethod_or_null() {
        return is_nmethod() ? (NMethod) this :null;
    }

    public NMethod as_nmethod(){
        if (!is_nmethod()){
            throw new IllegalStateException("must be nmethod");
        }
        return (NMethod) this;
    }

    public CompiledMethod as_compiled_method_or_null(){
        return is_compiled() ? (CompiledMethod) this :null;
    }

    public CompiledMethod as_compiled_method(){
        if (!is_compiled()){
            throw new IllegalStateException("must be compiled");
        }
        return (CompiledMethod) this;
    }

    public CodeBlob as_codeblob_or_null() {
        return this;
    }
    //Unsupported
    //public OptimizedEntryBlob* as_optimized_entry_blob() const             { assert(is_optimized_entry_blob(), "must be entry blob"); return (OptimizedEntryBlob*) this; }

    // Boundaries
    public @RawCType("address") long header_begin() {
        return this.address;
    }

    public RelocInfo relocation_begin() {
        return new RelocInfo(unsafe.getAddress(this.address + RELOCATION_BEGIN_OFFSET));
    }

    public RelocInfo relocation_end() {
        return new RelocInfo(unsafe.getAddress(this.address + RELOCATION_END_OFFSET));
    }

    public @RawCType("address") long content_begin() {
        return unsafe.getAddress(this.address + CONTENT_BEGIN_OFFSET);
    }

    // _code_end == _content_end is true for all types of blobs for now, it is also checked in the constructor
    public @RawCType("address") long content_end() {
        return unsafe.getAddress(this.address + CODE_END_OFFSET);
    }

    public @RawCType("address") long code_begin() {
        return unsafe.getAddress(this.address + CODE_BEGIN_OFFSET);
    }

    public @RawCType("address") long code_end() {
        return unsafe.getAddress(this.address + CODE_END_OFFSET);
    }

    public @RawCType("address") long data_end() {
        return unsafe.getAddress(this.address + DATA_END_OFFSET);
    }

    // Sizes
    public int size() {
        return unsafe.getInt(this.address + SIZE_OFFSET);
    }

    public int header_size() {
        return unsafe.getInt(this.address + HEADER_SIZE_OFFSET);
    }

    public int relocation_size() {
        return (int) (relocation_end().address - relocation_begin().address);
    }

    public int content_size() {
        return (int) (content_end() - content_begin());
    }

    public int code_size() {
        return (int) (code_end() - code_begin());
    }

    // Containment
    public boolean blob_contains(@RawCType("address") long addr) {
        return header_begin() <= addr && addr < data_end();
    }

    public boolean code_contains(@RawCType("address") long addr) {
        return code_begin() <= addr && addr < code_end();
    }

    public boolean contains(@RawCType("address") long addr) {
        return content_begin() <= addr && addr < content_end();
    }

    public boolean is_frame_complete_at(@RawCType("address") long addr) {
        return frame_complete_offset() != -1 &&
                code_contains(addr) && addr >= code_begin() + frame_complete_offset();
    }

    public int frame_complete_offset() {
        return unsafe.getInt(this.address + FRAME_COMPLETE_OFFSET_OFFSET);
    }

    // Frame support. Sizes are in word units.
    public int frame_size() {
        return unsafe.getInt(this.address + FRAME_SIZE_OFFSET);
    }

    public void set_frame_size(int size) {
        unsafe.putInt(this.address + FRAME_SIZE_OFFSET, size);
    }

    // Returns true, if the next frame is responsible for GC'ing oops passed as arguments
    public boolean caller_must_gc_arguments(JavaThread thread) {
        return unsafe.getByte(this.address + CALLER_MUST_GC_ARGUMENTS_OFFSET) != 0;
    }

    // Naming
    public String name() {
        return JVM.getStringRef(this.address + NAME_OFFSET);
    }

    public void set_name(String name) {
        JVM.putStringRef(this.address + NAME_OFFSET, name);
    }

    @Nullable
    public ImmutableOopMapSet oop_maps() {
        long addr = unsafe.getAddress(this.address + OOP_MAPS_OFFSET);
        if (addr == 0L) {
            return null;
        }
        if (!isEqual(this.oopMapsCache, addr)) {
            this.oopMapsCache = new ImmutableOopMapSet(addr);
        }
        return this.oopMapsCache;
    }

    public boolean is_zombie() {
        return false;
    }

    public boolean is_locked_by_vm() {
        return false;
    }

    // Typing
    public boolean is_buffer_blob() {
        return false;
    }

    public boolean is_nmethod() {
        return false;
    }

    public boolean is_runtime_stub() {
        return false;
    }

    public boolean is_deoptimization_stub() {
        return false;
    }

    public boolean is_uncommon_trap_stub() {
        return false;
    }

    public boolean is_exception_stub() {
        return false;
    }

    public boolean is_safepoint_stub() {
        return false;
    }

    public boolean is_adapter_blob() {
        return false;
    }

    public boolean is_vtable_blob() {
        return false;
    }

    public boolean is_method_handles_adapter_blob() {
        return false;
    }

    public boolean is_compiled() {
        return false;
    }

    //Unsupported
    public boolean is_optimized_entry_blob() {
        return false;
    }

    public @RawCType("CompilerType")int compiler_type(){
        return unsafe.getInt(this.address+TYPE_OFFSET);
    }

    public boolean is_compiled_by_c1()    { return compiler_type() == CompilerType.C1.id; };
    public boolean is_compiled_by_c2()    { return compiler_type() == CompilerType.C2.id; };
    public boolean is_compiled_by_jvmci()  { return compiler_type() == CompilerType.JVMCI.id; };

    public ImmutableOopMap oop_map_for_return_address(@RawCType("address")long return_address) {
        if (unsafe.getAddress(this.address+OOP_MAPS_OFFSET)==0L){
            throw new NullPointerException("nope");
        }
        //noinspection DataFlowIssue
        return oop_maps().find_map_at_offset((int) (return_address - code_begin()));
    }

    public void print_on(PrintStream st) {
        st.println("[CodeBlob (0x"+Long.toHexString(this.address)+")]");
        st.println("Framesize: "+frame_size());
    }

//    void CodeBlob::print() const { print_on(tty); }

    public void print_value_on(PrintStream st){
        st.println("[CodeBlob]");
    }

    @Override
    public String toString() {
        return actualType.name + "(" + this.name() + ")@0x" + Long.toHexString(this.address);
    }
}
