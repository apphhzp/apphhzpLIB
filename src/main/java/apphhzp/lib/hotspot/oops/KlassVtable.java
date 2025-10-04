package apphhzp.lib.hotspot.oops;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.classfile.SystemDictionary;
import apphhzp.lib.hotspot.oops.klass.InstanceKlass;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

public class KlassVtable {
    private final Klass _klass;            // my klass
    private final int _tableOffset;      // offset of start of vtable data within klass
    private final int _length;           // length of vtable (number of entries)

    public KlassVtable(Klass klass, @RawCType("void*") long base, int length) {
        _klass = klass;
        _tableOffset = (int) (base - klass.address);
        _length = length;
    }

    public @RawCType("VTableEntry*")long table() {
        return (_klass.address + _tableOffset);
    }

    public Klass klass() {
        return _klass;
    }

    public int length() {
        return _length;
    }
    public Method method_at(int i){
        if (!(i >= 0 && i < _length)){
            throw new IndexOutOfBoundsException("index out of bounds: "+i);
        }
        Method method=new VTableEntry(table()+ (long) i *VTableEntry.SIZE).method();
        if (method==null){
            throw new NullPointerException("should not be null");
        }
        return method;
    }

    public Method unchecked_method_at(int i) {
        if (!(i >= 0 && i < _length)){
            throw new IndexOutOfBoundsException("index out of bounds: "+i);
        }
        return new VTableEntry(table()+ (long) i *VTableEntry.SIZE).method();
    }
    public void dump_vtable(PrintStream tty){
        tty.println("vtable dump --");
        for (int i = 0; i < length(); i++) {
            Method m = unchecked_method_at(i);
            if (m != null) {
                tty.printf("      (%5d)  ", i);
                m.access_flags().print_on(tty);
                if (m.is_default_method()) {
                    tty.print("default ");
                }
                if (m.is_overpass()) {
                    tty.print("overpass");
                }
                tty.print(" --  ");
                m.print_name(tty);
                tty.println();
            }
        }
    }

    public void print(PrintStream tty){
        tty.printf("klassVtable for klass %s (length %d):\n", _klass.internal_name(), length());
        for (int i = 0; i < length(); i++) {
            new VTableEntry(table()+ (long) i *VTableEntry.SIZE).print(tty);
            tty.println();
        }
    }

    public void put_method_at(Method m, int index){
        if (!(index >= 0 && index < _length)){
            throw new IndexOutOfBoundsException("index out of bounds: "+index);
        }
        if (m.is_private()){
            throw new IllegalArgumentException("private methods should not be in vtable");
        }
//        JVMTI_ONLY(assert(!m->is_old() || ik()->is_being_redefined(), "old methods should not be in vtable"));
//        if (is_preinitialized_vtable()) {
//            // At runtime initialize_vtable is rerun as part of link_class_impl()
//            // for shared class loaded by the non-boot loader to obtain the loader
//            // constraints based on the runtime classloaders' context. The dumptime
//            // method at the vtable index should be the same as the runtime method.
//            assert(table()[index].method() == m,
//            "archived method is different from the runtime method");
//        } else {
        new VTableEntry(table()+ (long) index *VTableEntry.SIZE).set(m);
        //}
    }

    // get the vtable index of a miranda method with matching "name" and "signature"
    public int index_of_miranda(Symbol name, Symbol signature) {
        // search from the bottom, might be faster
        for (int i = (length() - 1); i >= 0; i--) {
            Method m = new VTableEntry(table()+ (long) i *VTableEntry.SIZE).method();
            if (is_miranda_entry_at(i) &&
                    m.name().equals(name) && m.signature().equals(signature)) {
                return i;
            }
        }
        return Method.VtableIndexFlag.invalid_vtable_index;
    }

    // check if an entry at an index is miranda
// requires that method m at entry be declared ("held") by an interface.
    public boolean is_miranda_entry_at(int i) {
        Method m = method_at(i);
        InstanceKlass holder = m.method_holder();

        // miranda methods are public abstract instance interface methods in a class's vtable
        if (holder.isInterface()) {
            if (JVM.ENABLE_EXTRA_CHECK&&!(m.is_public())){
                throw new RuntimeException("should be public");
            }
            //assert(ik().implements_interface(holder) , "this class should implement the interface");
            if (is_miranda(m, ik().getMethods(), ik().getDefaultMethods(), ik().getSuperKlass(), klass().isInterface())) {
                return true;
            }
        }
        return false;
    }


    public boolean is_miranda(Method m, VMTypeArray<Method> class_methods,
                              VMTypeArray<Method> default_methods,Klass superK,
                                 boolean is_interface) {
        if (m.is_static() || m.is_private() || m.is_overpass()) {
            return false;
        }
        Symbol name = m.name();
        Symbol signature = m.signature();

        // First look in local methods to see if already covered
        if (InstanceKlass.find_local_method(class_methods, name, signature,
                Klass.OverpassLookupMode.find,
                Klass.StaticLookupMode.skip,
                Klass.PrivateLookupMode.skip) != null)
        {
            return false;
        }

        // Check local default methods
        if ((default_methods != null) &&
                (InstanceKlass.find_method(default_methods, name, signature) != null))
        {
            return false;
        }

        // Iterate on all superclasses, which should be InstanceKlasses.
        // Note that we explicitly look for overpasses at each level.
        // Overpasses may or may not exist for supers for pass 1,
        // they should have been created for pass 2 and later.

        for (Klass cursuper = superK; cursuper != null; cursuper = cursuper.getSuperKlass())
        {
            Method found_mth = (cursuper.asInstanceKlass()).find_local_method(name, signature,
                Klass.OverpassLookupMode.find,
                Klass.StaticLookupMode.skip,
                Klass.PrivateLookupMode.skip);
            // Ignore non-public methods in java.lang.Object if klass is an interface.
            if (found_mth != null && (!is_interface ||
                    !SystemDictionary.is_nonpublic_Object_method(found_mth))) {
            return false;
        }
        }

        return true;
    }
    public InstanceKlass ik(){
        return _klass.asInstanceKlass();
    }
}
