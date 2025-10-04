package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

public class KlassItable {
    private InstanceKlass _klass;             // my klass
    private int _table_offset;      // offset of start of itable data within klass (in words)
    private int _size_offset_table; // size of offset table (in itableOffset entries)
    private int _size_method_table; // size of methodtable (in itableMethodEntry entries)
//    public KlassItable(InstanceKlass klass) {
//        _klass = klass;
//        if (klass.itable_length() > 0) {
//            itableOffsetEntry* offset_entry = (itableOffsetEntry*)klass->start_of_itable();
//            if (offset_entry  != NULL && offset_entry->interface_klass() != NULL) { // Check that itable is initialized
//                // First offset entry points to the first method_entry
//                intptr_t* method_entry  = (intptr_t *)(((address)klass) + offset_entry->offset());
//                intptr_t* end         = klass->end_of_itable();
//
//                _table_offset      = (intptr_t*)offset_entry - (intptr_t*)klass;
//                _size_offset_table = (method_entry - ((intptr_t*)offset_entry)) / itableOffsetEntry::size();
//                _size_method_table = (end - method_entry)                  / itableMethodEntry::size();
//                assert(_table_offset >= 0 && _size_offset_table >= 0 && _size_method_table >= 0, "wrong computation");
//                return;
//            }
//        }
//
//        // The length of the itable was either zero, or it has not yet been initialized.
//        _table_offset      = 0;
//        _size_offset_table = 0;
//        _size_method_table = 0;
//    }

    public @RawCType("intptr_t*")long vtable_start(){
        return (_klass.address) + (long) _table_offset * JVM.oopSize;
    }
    public static int method_count_for_interface(InstanceKlass interf) {
        if (JVM.ENABLE_EXTRA_CHECK&&!(interf.isInterface())){
            throw new RuntimeException("must be");
        }
        VMTypeArray<Method> methods = interf.getMethods();
        int nof_methods = methods.length();
        int length = 0;
        while (nof_methods > 0) {
            Method m = methods.get(nof_methods-1);
            if (m.has_itable_index()) {
                length = m.itable_index() + 1;
                break;
            }
            nof_methods -= 1;
        }
        if (JVM.ENABLE_EXTRA_CHECK){
            int nof_methods_copy = nof_methods;
            while (nof_methods_copy > 0) {
                Method mm = methods.get(--nof_methods_copy);
                if (!(!mm.has_itable_index() || mm.itable_index() < length)){
                    throw new RuntimeException();
                }
            }
        }
        // return the rightmost itable index, plus one; or 0 if no methods have
        // itable indices
        return length;
    }

}
