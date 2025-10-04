package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.method.data.MethodData;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantPoolCache;
import apphhzp.lib.hotspot.oops.constant.ConstantPoolCacheEntry;
import apphhzp.lib.hotspot.oops.constant.ConstantTag;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.oops.method.data.ProfileData;
import apphhzp.lib.hotspot.runtime.bytecode.Bytes;
import apphhzp.lib.hotspot.util.CString;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;
import static apphhzp.lib.helfy.JVM.type2name;

public class BytecodePrinter implements BytecodeClosure {
    // %%% This field is not GC-ed, and so can contain garbage
    // between critical sections.  Use only pointer-comparison
    // operations on the pointer, except within a critical section.
    // (Also, ensure that occasional false positives are benign.)
    private Method _current_method;
    private boolean _is_wide;
    private @RawCType("Bytecodes::Code") int _code;
    private @RawCType("address") long _next_pc;                // current decoding position

    private void align() {
        _next_pc = JVM.alignUp(_next_pc, 4);
    }
    // signed
    private int get_byte() {
        return unsafe.getByte(_next_pc++);
    }

    private short get_short() {
        short i = (short) Bytes.get_Java_u2(_next_pc);
        _next_pc += 2;
        return i;
    }

    private int get_int() {
        int i = Bytes.get_Java_u4(_next_pc);
        _next_pc += 4;
        return i;
    }

    private int get_index_u1() {
        return unsafe.getByte(_next_pc++)&0xff;
    }

    private int get_index_u2() {
        int i = Bytes.get_Java_u2(_next_pc);
        _next_pc += 2;
        return i;
    }

    private int get_index_u1_cpcache() {
        return get_index_u1() + ConstantPool.CPCACHE_INDEX_TAG;
    }

    private int get_index_u2_cpcache() {
        int i = Bytes.get_native_u2(_next_pc)&0xffff;
        _next_pc += 2;
        return i + ConstantPool.CPCACHE_INDEX_TAG;
    }

    private int get_index_u4() {
        int i = Bytes.get_native_u4(_next_pc);
        _next_pc += 4;
        return i;
    }

    private int get_index_special() {
        return (is_wide()) ? get_index_u2() : get_index_u1();
    }

    private Method method() {
        return _current_method;
    }

    private boolean is_wide() {
        return _is_wide;
    }

    private @RawCType("Bytecodes::Code") int raw_code() {
        return _code;
    }


    private boolean check_index(int i,@RawCType("int&") int[] cp_index, PrintStream st) {
        ConstantPool constants = method().constants();
        int ilimit = constants.length();
        @RawCType("Bytecodes::Code")int code = raw_code();
        if (Bytecodes.uses_cp_cache(code)) {
            boolean okay = true;
            switch (code) {
                case Bytecodes.Code._fast_aldc:
                case Bytecodes.Code._fast_aldc_w:
                    okay = check_obj_index(i, cp_index, st);
                    break;
                case Bytecodes.Code._invokedynamic:
                    okay = check_invokedynamic_index(i, cp_index, st);
                    break;
                default:
                    okay = check_cp_cache_index(i, cp_index, st);
                    break;
            }
            if (!okay) {
                return false;
            }
        }


        // check cp index
        if (cp_index[0] >= 0 && cp_index[0] < ilimit) {
            if (JVM.getFlag("WizardMode").getBool()) {
                st.printf(" cp[%d]", cp_index[0]);
            }
            return true;
        }
        st.println(" CP["+cp_index[0]+"] not in CP");
        return false;
    }

    private boolean check_cp_cache_index(int i,@RawCType("int&") int[] cp_index, PrintStream st) {
        ConstantPool constants = method().constants();
        int climit = 0;

        ConstantPoolCache cache = constants.getCache();
        // If rewriter hasn't run, the index is the cp_index
        if (cache == null){
            cp_index[0] = i;
            return true;
        }
        //climit = cache->length();  // %%% private!
        @RawCType("size_t")long size = (long) cache.size() * JVM.oopSize;
        size -= ConstantPoolCache.SIZE;
        size /= ConstantPoolCacheEntry.SIZE;
        climit = (int) size;

        if (JVM.includeAssert){
            {
                final int CPCACHE_INDEX_TAG = ConstantPool.CPCACHE_INDEX_TAG;
                if (i >= CPCACHE_INDEX_TAG && i < climit + CPCACHE_INDEX_TAG) {
                    i -= CPCACHE_INDEX_TAG;
                } else {
                    st.println(" CP["+i+"] missing bias?");
                    return false;
                }
            }
        }
        if (i >= 0 && i < climit) {
            cp_index[0] = cache.entry_at(i).constant_pool_index();
        } else {
            st.println(i+" not in CP[*]?");
            return false;
        }
        return true;
    }

    private boolean check_obj_index(int i, @RawCType("int&") int[] cp_index, PrintStream st) {
        ConstantPool constants = method().constants();
        i -= ConstantPool.CPCACHE_INDEX_TAG;

        if (i >= 0 && i < constants.getCache().getResolvedReferences().length) {
            cp_index[0] = constants.object_to_cp_index(i);
            return true;
        } else {
            st.println(i+" not in OBJ[*]?");
            return false;
        }
    }

    private boolean check_invokedynamic_index(int i,@RawCType("int&") int[] cp_index, PrintStream st) {
        if (!ConstantPool.is_invokedynamic_index(i)){
            throw new RuntimeException("not secondary index?");
        }
        i = ConstantPool.decode_invokedynamic_index(i) + ConstantPool.CPCACHE_INDEX_TAG;
        return check_cp_cache_index(i, cp_index, st);
    }

    private void print_constant(int i, PrintStream st) {
        int orig_i = i;
        int[] tmp_arr=new int[]{i};
        if (!check_index(orig_i, tmp_arr, st)){
            return;
        }
        i=tmp_arr[0];
        ConstantPool constants = method().constants();
        @RawCType("constantTag")int tag = constants.tag_at(i);

        if (tag== ConstantTag.Integer) {
            st.println(" "+constants.int_at(i));
        } else if (tag==ConstantTag.Long) {
            st.println(" "+constants.long_at(i));
        } else if (tag==ConstantTag.Float) {
            st.printf(" %f\n", constants.float_at(i));
        } else if (tag==ConstantTag.Double) {
            st.printf(" %f\n", constants.double_at(i));
        } else if (tag==ConstantTag.String) {
            String string = CString.toString(constants.string_at_noresolve(i));
            st.println(" "+string);
        } else if (tag==ConstantTag.Class) {
            st.println(" "+constants.resolved_klass_at(i).external_name());
        } else if (tag==ConstantTag.UnresolvedClass||tag==ConstantTag.UnresolvedClassInError) {
            st.println(" <unresolved klass at "+i+">");
        } else if (tag==ConstantTag.MethodType) {
            int i2 = constants.method_type_index_at(i);
            st.printf(" <MethodType> %d", i2);
            print_symbol(constants.symbol_at(i2), st);
        } else if (tag==ConstantTag.MethodHandle) {
            int kind = constants.method_handle_ref_kind_at(i);
            int i2 = constants.method_handle_index_at(i);
            st.printf(" <MethodHandle of kind %d index at %d>", kind, i2);
            print_field_or_method(-i, i2, st);
        } else {
            st.println(" bad tag="+tag+" at "+i);
        }
    }

    public void print_symbol(Symbol sym, PrintStream st) {
        byte[] buf=new byte[40];
        int len = sym.getLength();
        if (len >= 40){
            st.printf(" %s...[%d]\n", sym.toString().substring(0,40), len);
        } else {
            st.print(" ");
            //sym.print_on(st);
            st.print(sym);
            st.println();
        }
    }

    private void print_field_or_method(int i, PrintStream st) {
        int orig_i = i;
        int[] tmp_arr=new int[]{i};
        if (!check_index(orig_i, tmp_arr, st)){
            return;
        }
        i=tmp_arr[0];
        print_field_or_method(orig_i, i, st);
    }

    private void print_field_or_method(int orig_i, int i, PrintStream st) {
        ConstantPool constants = method().constants();
        @RawCType("constantTag")int tag = constants.tag_at(i);

        boolean has_klass = true;

        switch (tag) {
            case ConstantTag.InterfaceMethodref:
            case ConstantTag.Methodref:
            case ConstantTag.Fieldref:
                break;
            case ConstantTag.NameAndType:
            case ConstantTag.Dynamic:
            case ConstantTag.InvokeDynamic:
                has_klass = false;
                break;
            default:
                st.println(" bad tag="+tag+" at "+i);
                return;
        }
        Symbol name = constants.uncached_name_ref_at(i);
        Symbol signature = constants.uncached_signature_ref_at(i);
        String sep = (tag==ConstantTag.Fieldref ? "/" : "");
        if (has_klass) {
            Symbol klass = constants.klass_name_at(constants.uncached_klass_ref_index_at(i));
            st.printf(" %d <%s.%s%s%s> \n", i, klass.toString(), name.toString(), sep, signature.toString());
        } else {
            if (tag==ConstantTag.Dynamic || tag==ConstantTag.InvokeDynamic){
                int bsm = constants.bootstrap_method_ref_index_at(i);
                st.printf(" bsm=%d", bsm);
            }
            st.printf(" %d <%s%s%s>\n", i, name.toString(), sep, signature.toString());
        }
    }

    private void print_attributes(int bci, PrintStream st) {
// Show attributes of pre-rewritten codes
        @RawCType("Bytecodes.Code")int code = Bytecodes.java_code(raw_code());
        // If the code doesn't have any fields there's nothing to print.
        // note this is ==1 because the tableswitch and lookupswitch are
        // zero size (for some reason) and we want to print stuff out for them.
        if (Bytecodes.length_for(code) == 1) {
            st.println();
            return;
        }

        switch(code) {
            // Java specific bytecodes only matter.
            case Bytecodes.Code._bipush:
                st.println(" "+get_byte());
                break;
            case Bytecodes.Code._sipush:
                st.println(" "+get_short());
                break;
            case Bytecodes.Code._ldc:
                if (Bytecodes.uses_cp_cache(raw_code())) {
                print_constant(get_index_u1_cpcache(), st);
            } else {
                print_constant(get_index_u1(), st);
            }
            break;

            case Bytecodes.Code._ldc_w:
            case Bytecodes.Code._ldc2_w:
                if (Bytecodes.uses_cp_cache(raw_code())) {
                print_constant(get_index_u2_cpcache(), st);
            } else {
                print_constant(get_index_u2(), st);
            }
            break;

            case Bytecodes.Code._iload:
            case Bytecodes.Code._lload:
            case Bytecodes.Code._fload:
            case Bytecodes.Code._dload:
            case Bytecodes.Code._aload:
            case Bytecodes.Code._istore:
            case Bytecodes.Code._lstore:
            case Bytecodes.Code._fstore:
            case Bytecodes.Code._dstore:
            case Bytecodes.Code._astore:
                st.println(" #"+get_index_special());
                break;

            case Bytecodes.Code._iinc:
            { int index = get_index_special();
                int offset = is_wide() ? get_short(): get_byte();
                st.println(" #"+index+" "+offset);
            }
            break;

            case Bytecodes.Code._newarray: {
                @RawCType("BasicType")int atype = get_index_u1();
                String str = type2name(atype);
                if (str == null || BasicType.is_reference_type(atype)) {
                    throw new RuntimeException("Unidentified basic type");
                }
                st.println(" "+str);
            }
            break;
            case Bytecodes.Code._anewarray: {
                int klass_index = get_index_u2();
                ConstantPool constants = method().constants();
                Symbol name = constants.klass_name_at(klass_index);
                st.println(" "+name+" ");
            }
            break;
            case Bytecodes.Code._multianewarray: {
                int klass_index = get_index_u2();
                int nof_dims = get_index_u1();
                ConstantPool constants = method().constants();
                Symbol name = constants.klass_name_at(klass_index);
                st.println(" "+name+" "+nof_dims);
            }
            break;

            case Bytecodes.Code._ifeq:
            case Bytecodes.Code._ifnull:
            case Bytecodes.Code._iflt:
            case Bytecodes.Code._ifle:
            case Bytecodes.Code._ifne:
            case Bytecodes.Code._ifnonnull:
            case Bytecodes.Code._ifgt:
            case Bytecodes.Code._ifge:
            case Bytecodes.Code._if_icmpeq:
            case Bytecodes.Code._if_icmpne:
            case Bytecodes.Code._if_icmplt:
            case Bytecodes.Code._if_icmpgt:
            case Bytecodes.Code._if_icmple:
            case Bytecodes.Code._if_icmpge:
            case Bytecodes.Code._if_acmpeq:
            case Bytecodes.Code._if_acmpne:
            case Bytecodes.Code._goto:
            case Bytecodes.Code._jsr:
                st.println(" "+(bci + get_short()));
                break;

            case Bytecodes.Code._goto_w:
            case Bytecodes.Code._jsr_w:
                st.println(" "+ (bci + get_int()));
                break;

            case Bytecodes.Code._ret:
                st.println(" "+get_index_special());
                break;

            case Bytecodes.Code._tableswitch:
            {
                align();
                int  default_dest = bci + get_int();
                int  lo           = get_int();
                int  hi           = get_int();
                int  len          = hi - lo + 1;
                @RawCType("jint*")int[] dest        = new int[len];
                for (int i = 0; i < len; i++) {
                    dest[i] = bci + get_int();
                }
                st.print(" "+default_dest+" "+lo+" " +hi+ " ");
                @RawCType("const char *")String comma = "";
                for (int ll = lo; ll <= hi; ll++) {
                    int idx = ll - lo;
                    st.printf("%s %d:" +dest[idx]+ " (delta: %d)", comma, ll, dest[idx]-bci);
                    comma = ",";
                }
                st.println();
            }
            break;
            case Bytecodes.Code._lookupswitch:
            { align();
                int  default_dest = bci + get_int();
                int  len          = get_int();
                @RawCType("jint*")int[] key         = new int[len];
                @RawCType("jint*")int[] dest        =  new int[len];
                for (int i = 0; i < len; i++) {
                    key [i] = get_int();
                    dest[i] = bci + get_int();
                };
                st.printf(" %d %d ", default_dest, len);
                String comma = "";
                for (int ll = 0; ll < len; ll++)  {
                    st.printf("%s " +key[ll]+ ":" +dest[ll], comma);
                    comma = ",";
                }
                st.println();
            }
            break;

            case Bytecodes.Code._putstatic:
            case Bytecodes.Code._getstatic:
            case Bytecodes.Code._putfield:
            case Bytecodes.Code._getfield:
                print_field_or_method(get_index_u2_cpcache(), st);
                break;

            case Bytecodes.Code._invokevirtual:
            case Bytecodes.Code._invokespecial:
            case Bytecodes.Code._invokestatic:
                print_field_or_method(get_index_u2_cpcache(), st);
                break;

            case Bytecodes.Code._invokeinterface:
            { int i = get_index_u2_cpcache();
                int n = get_index_u1();
                get_byte();            // ignore zero byte
                print_field_or_method(i, st);
            }
            break;

            case Bytecodes.Code._invokedynamic:
                print_field_or_method(get_index_u4(), st);
                break;

            case Bytecodes.Code._new:
            case Bytecodes.Code._checkcast:
            case Bytecodes.Code._instanceof:
            {
                int i = get_index_u2();
                ConstantPool constants = method().constants();
                Symbol name = constants.klass_name_at(i);
                st.println(" "+i+" <"+name+">");
            }
            break;

            case Bytecodes.Code._wide:
                // length is zero not one, but printed with no more info.
                break;

            default:
                throw new RuntimeException("ShouldNotReachHere()");
        }
    }

    private void bytecode_epilog(int bci, PrintStream st) {
        MethodData mdo = method().getMethodData();
        if (mdo != null){
            ProfileData data = mdo.bci_to_data(bci);
            if (data != null) {
                String os="  "+mdo.dp_to_di(data.dp());
                st.print(os);
                int cnt=6-os.length();
                while (cnt>0){
                    st.print(" ");
                    --cnt;
                }
                data.print_data_on(st, mdo);
            }
        }
    }

    public BytecodePrinter() {
        _is_wide = false;
        _code = Bytecodes.Code._illegal;
    }


    @Override
    public void trace(Method method,@RawCType("address") long bcp,@RawCType("uintptr_t") long tos,@RawCType("uintptr_t") long tos2, PrintStream st) {
        if (!_current_method.equals(method)) {
            // Note 1: This code will not work as expected with true MT/MP.
            //         Need an explicit lock or a different solution.
            // It is possible for this block to be skipped, if a garbage
            // _current_method pointer happens to have the same bits as
            // the incoming method.  We could lose a line of trace output.
            // This is acceptable in a debug-only feature.
            st.println();
            //st.printf("[%ld] ", (long) Thread::current()->osthread()->thread_id());
            method.print_name(st);
            st.println();
            _current_method = method();
        }
        @RawCType("Bytecodes::Code")int code;
        if (is_wide()) {
            // bcp wasn't advanced if previous bytecode was _wide.
            code = Bytecodes.code_at(method(), bcp+1);
        } else {
            code = Bytecodes.code_at(method(), bcp);
        }
        _code = code;
        int bci = (int) (bcp - method.code_base());
        //st.printf("[%ld] ", (long) Thread::current()->osthread()->thread_id());
        if (JVM.getFlag("Verbose").getBool()){
            st.printf("%4d  0x" +Long.toHexString(tos)+ " 0x" +Long.toHexString(tos2)+ " "+Bytecodes.name(code), bci);
        } else {
            st.printf("%4d  "+Bytecodes.name(code), bci);
        }
        _next_pc = is_wide() ? bcp+2 : bcp+1;
        print_attributes(bci,st);
        // Set is_wide for the next one, since the caller of this doesn't skip
        // the next bytecode.
        _is_wide = (code == Bytecodes.Code._wide);
        _code = Bytecodes.Code._illegal;
    }

    @Override
    public void trace(Method method,@RawCType("address") long bcp, PrintStream st) {
        _current_method = method;
        @RawCType("Bytecodes::Code")int code = Bytecodes.code_at(method, bcp);
        // Set is_wide
        _is_wide = (code == Bytecodes.Code._wide);
        if (is_wide()) {
            code = Bytecodes.code_at(method, bcp+1);
        }
        _code = code;
        int bci = (int) (bcp - method.code_base());
        // Print bytecode index and name
        if (is_wide()) {
            st.printf("%d %s_w", bci, Bytecodes.name(code));
        } else {
            st.printf("%d %s", bci, Bytecodes.name(code));
        }
        _next_pc = is_wide() ? bcp+2 : bcp+1;
        print_attributes(bci, st);
        bytecode_epilog(bci, st);
    }
}
