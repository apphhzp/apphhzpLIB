package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.util.RawCType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class BytecodeConstantPool {
    private final ConstantPool _orig;
    private final List<BytecodeCPEntry> _entries;
    private final Object2IntMap<BytecodeCPEntry> _indices;
    private @RawCType("u2")int find_or_add(BytecodeCPEntry bcpe){
        @RawCType("u2")int index = _entries.size();
        boolean created;
        int probe;
        if (_indices.containsKey(bcpe)){
            created = false;
            probe = _indices.getInt(bcpe);
        }else {
            created = true;
            _indices.put(bcpe, probe=index);
        }
        if (created) {
            _entries.add(bcpe);
        } else {
            index = probe;
        }
        return index + _orig.length();
    }
    public BytecodeConstantPool(ConstantPool orig){
        _orig=(orig);
        _entries=new ArrayList<>();
        _indices=new Object2IntOpenHashMap<>();
    }

    public BytecodeCPEntry at(@RawCType("u2")int index){
        return _entries.get(index);
    }

    public InstanceKlass pool_holder(){
        return _orig.pool_holder();
    }

    public @RawCType("u2")int utf8(Symbol sym) {
        return find_or_add(BytecodeCPEntry.utf8(sym));
    }

    public @RawCType("u2")int klass(Symbol class_name) {
        return find_or_add(BytecodeCPEntry.klass(utf8(class_name)));
    }

    public @RawCType("u2")int string(Symbol str) {
        return find_or_add(BytecodeCPEntry.string(utf8(str)));
    }

    public @RawCType("u2")int name_and_type(Symbol name, Symbol sig) {
        return find_or_add(BytecodeCPEntry.name_and_type(utf8(name), utf8(sig)));
    }

    public @RawCType("u2")int methodref(Symbol class_name, Symbol name, Symbol sig) {
        return find_or_add(BytecodeCPEntry.methodref(
                klass(class_name), name_and_type(name, sig)));
    }

    public ConstantPool create_constant_pool(){
        if (_entries.isEmpty()) {
            return _orig;
        }

        ConstantPool cp = ConstantPool.allocate(_orig.length() + _entries.size());

        cp.setHolder(_orig.pool_holder());
        //constantPoolHandle cp_h(THREAD, cp);
        _orig.copy_cp_to(1, _orig.length() - 1, cp, 1);

        // Preserve dynamic constant information from the original pool
        cp.copy_fields(_orig);

        for (int i = 0; i < _entries.size(); ++i) {
            BytecodeCPEntry entry = _entries.get(i);
            int idx = i + _orig.length();
            switch (entry._tag) {
                case BytecodeCPEntry.tag.UTF8:
                    entry._u.utf8.incrementRefCount();
                    cp.symbol_at_put(idx, entry._u.utf8);
                    break;
                case BytecodeCPEntry.tag.KLASS:
                    cp.klass_index_at_put(
                            idx, entry._u.klass);
                    break;
                case BytecodeCPEntry.tag.STRING:
                    cp.unresolved_string_at_put(
                            idx, cp.symbol_at(entry._u.string));
                    break;
                case BytecodeCPEntry.tag.NAME_AND_TYPE:
                    cp.name_and_type_at_put(idx,
                            entry._u.name_and_type_name_index,
                            entry._u.name_and_type_type_index);
                    break;
                case BytecodeCPEntry.tag.METHODREF:
                    cp.method_at_put(idx,
                            entry._u.methodref_class_index,
                            entry._u.methodref_name_and_type_index);
                    break;
                default:
                    throw new RuntimeException("ShouldNotReachHere()");
            }
        }
        cp.initialize_unresolved_klasses(_orig.pool_holder().getClassLoaderData());
        return cp;
    }
}
