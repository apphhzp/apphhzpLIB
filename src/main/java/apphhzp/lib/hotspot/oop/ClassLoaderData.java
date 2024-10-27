package apphhzp.lib.hotspot.oop;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static apphhzp.lib.ClassHelper.unsafe;

public class ClassLoaderData extends JVMObject {
    public static final Type TYPE= JVM.type("ClassLoaderData");
    public static final Type GRAPH_TYPE=JVM.type("ClassLoaderDataGraph");
    public static final long CLASS_LOADER_OFFSET=TYPE.offset("_class_loader");
    public static final long HAS_CLASS_HOLDER_OFFSET=TYPE.offset("_has_class_mirror_holder");
    public static final long KLASSES_OFFSET=TYPE.offset("_klasses");
    public static final long NEXT_OFFSET=TYPE.offset("_next");
    private static final Map<Long,ClassLoaderData> CACHE=new HashMap<>();
    private ClassLoaderData nextCache;
    public static ClassLoaderData getOrCreate(long addr){
        if (addr==0L){
            throw new IllegalArgumentException("ClassLoaderData pointer is NULL(0L).");
        }
        if (CACHE.containsKey(addr)){
            return CACHE.get(addr);
        }
        ClassLoaderData re=new ClassLoaderData(addr);
        CACHE.put(addr,re);
        return re;
    }
    private ClassLoaderData(long address){
        super(address);
    }

    public List<Klass> getKlasses(){
        ArrayList<Klass> re=new ArrayList<>();
        long base=unsafe.getAddress(this.address+KLASSES_OFFSET);
        if (base!=0L){
            Klass tmp=Klass.getOrCreate(base);
            re.add(tmp);
            while ((tmp=tmp.getNextKlass())!=null){
                re.add(tmp);
            }
        }
        return re;
    }

    public void setKlasses(Klass header){
        unsafe.putAddress(this.address+KLASSES_OFFSET,header==null?0L:header.address);
    }
    @Nullable
    public ClassLoaderData getNextCLD(){
        long addr = unsafe.getAddress(this.address + NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        if (!isEqual(this.nextCache,addr)) {
            this.nextCache = getOrCreate(addr);
        }
        return this.nextCache;
    }

    public void setNextCLD(ClassLoaderData next){
        unsafe.putAddress(this.address+NEXT_OFFSET,next==null?0L:next.address);
    }
    @Nullable
    public ClassLoader getClassLoader(){
        long addr= Oop.fromOopHandle(this.address+CLASS_LOADER_OFFSET);
        return new Oop(addr).getObject();
    }

    /* If true, CLD is dedicated to one class and that class determines the CLDs lifecycle.
       For example, a non-strong hidden class.
       Arrays of these classes are also assigned to these class loader datas.
    */
    public boolean hasClassMirrorHolder(){
        return unsafe.getByte(this.address+HAS_CLASS_HOLDER_OFFSET)!=0;
    }

    public static List<ClassLoaderData> getAllClassLoaderData(){
        ArrayList<ClassLoaderData> re=new ArrayList<>();
        ClassLoaderData tmp=getOrCreate(unsafe.getAddress(GRAPH_TYPE.global("_head")));
        re.add(tmp);
        while ((tmp=tmp.getNextCLD())!=null){
            re.add(tmp);
        }
        return re;
    }

    @Override
    public String toString() {
        return "ClassLoaderData@0x"+Long.toHexString(this.address);
    }
}
