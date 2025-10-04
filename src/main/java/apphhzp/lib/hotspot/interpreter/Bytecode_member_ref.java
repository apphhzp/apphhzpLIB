package apphhzp.lib.hotspot.interpreter;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.ConstantPoolCache;
import apphhzp.lib.hotspot.oops.constant.ConstantPoolCacheEntry;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.runtime.bytecode.Bytecode;
import apphhzp.lib.hotspot.runtime.signature.ResultTypeFinder;
import apphhzp.lib.hotspot.util.RawCType;

public class Bytecode_member_ref extends Bytecode {
    protected final Method _method;                          // method containing the bytecode

    protected Bytecode_member_ref(Method method, int bci) {
        super(method, method.bcp_from(bci));
        _method = method;
    }

    protected final Method method() {
        return _method;
    }

    protected ConstantPool constants() {
        return _method.constants();
    }

    protected ConstantPoolCache cpcache() {
        return _method.constants().getCache();
    }

    protected ConstantPoolCacheEntry cpcache_entry() {
        int index = this.index();
        return cpcache().entry_at(ConstantPool.decode_cpcache_index(index, true));
    }

    /**
     * @return cache index (loaded from instruction)
     */
    public int index() {
        // Note:  Rewriter::rewrite changes the Java_u2 of an invokedynamic to a native_u4,
        // at the same time it allocates per-call-site CP cache entries.
        @RawCType("Bytecodes::Code") int rawc = code();
        if (has_index_u4(rawc))
            return get_index_u4(rawc);
        else
            return get_index_u2_cpcache(rawc);
    }

    /**
     * @return constant pool index
     */
    public int pool_index() {
        return cpcache_entry().constant_pool_index();
    }

    /**
     * @return the klass of the method or field
     */
    public Symbol klass() {
        return constants().klass_ref_at_noresolve(index());
    }

    /**
     * @return the name of the method or field
     */
    public Symbol name() {
        return constants().name_ref_at(index());
    }

    /**
     * @return the signature of the method or field
     */
    public Symbol signature() {
        return constants().signature_ref_at(index());
    }

    /**
     * @return the result type of the getfield or invoke
     */
    public @RawCType("BasicType") int result_type() {
        ResultTypeFinder rts = new ResultTypeFinder(signature());
        return rts.type();
    }
}
