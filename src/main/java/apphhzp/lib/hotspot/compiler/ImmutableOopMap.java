package apphhzp.lib.hotspot.compiler;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class ImmutableOopMap extends JVMObject {
    public static final Type TYPE= JVM.type("ImmutableOopMap");
    public static final int SIZE=TYPE.size;
    public static final long COUNT_OFFSET=TYPE.offset("_count");
    public ImmutableOopMap(long addr) {
        super(addr);
    }
    public int count(){
        return unsafe.getInt(this.address+COUNT_OFFSET);
    }
    public @RawCType("address") long data_addr(){
        return this.address + SIZE;
    }

    public void print_on(PrintStream st){
        OopMapValue omv;
        st.print("ImmutableOopMap {");
        for(OopMapStream oms=new OopMapStream(this); !oms.is_done(); oms.next()) {
            omv = oms.current();
            omv.print_on(st);
        }
        st.print("}");
    }
}
