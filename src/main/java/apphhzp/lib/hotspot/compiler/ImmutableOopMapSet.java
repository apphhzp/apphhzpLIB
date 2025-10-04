package apphhzp.lib.hotspot.compiler;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.util.RawCType;

import java.io.PrintStream;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class ImmutableOopMapSet extends JVMObject {
    public static final Type TYPE= JVM.type("ImmutableOopMapSet");
    public static final int SIZE=TYPE.size;
    public static final long COUNT_OFFSET= TYPE.offset("_count");
    public static final long SIZE_OFFSET=TYPE.offset("_size");
    public ImmutableOopMapSet(long addr) {
        super(addr);
    }
    public int count(){
        return unsafe.getInt(this.address+COUNT_OFFSET);
    }
    public int nr_of_bytes(){
        return unsafe.getInt(this.address+SIZE_OFFSET);
    }
    public @RawCType("address")long data(){
        return  this.address + SIZE + (long) ImmutableOopMapPair.SIZE * count();
    }

    public ImmutableOopMap oopmap_at_offset(int offset){
        if (!(offset >= 0 && offset < nr_of_bytes())){
            throw new IllegalArgumentException("must be within boundaries");
        }
        @RawCType("address")long addr = data() + offset;
        return new ImmutableOopMap(addr);
    }

    public ImmutableOopMapPair get_pairs(){
        return new ImmutableOopMapPair( this.address + SIZE);
    }
    public ImmutableOopMapPair pair_at(int index){
        if (!(index >= 0 && index < count())){
            throw new IllegalArgumentException("check");
        }
        return new ImmutableOopMapPair( this.address + SIZE+ (long)index *ImmutableOopMapPair.SIZE);
    }
    public ImmutableOopMap find_map_at_offset(int pc_offset){
        //ImmutableOopMapPair pairs = get_pairs();
        ImmutableOopMapPair last  = null;

        for (int i = 0,maxi=count(); i < maxi; ++i) {
            ImmutableOopMapPair now=new ImmutableOopMapPair(this.address+SIZE+ (long) i *ImmutableOopMapPair.SIZE);
            if (now.pc_offset() >= pc_offset) {
                last = now;
                break;
            }
        }

        // Heal Coverity issue: potential index out of bounds access.
        if (last==null){
            throw new RuntimeException("last may not be null");
        }
        if (last.pc_offset() != pc_offset){
            throw new RuntimeException("oopmap not found");
        }
        return last.get_from(this);
    }

    public void print_on(PrintStream st){
        ImmutableOopMap last = null;
        final int len = count();
        st.println("ImmutableOopMapSet contains "+len+" OopMaps");
        for (int i = 0; i < len; i++) {
            ImmutableOopMapPair pair = pair_at(i);
            ImmutableOopMap map = pair.get_from(this);
            if (!map.equals(last)) {
                st.println();
                map.print_on(st);
                st.print(" pc offsets: ");
            }
            last = map;
            st.print(pair.pc_offset()+" ");
        }
        st.println();
    }
}
