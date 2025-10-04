package apphhzp.lib.hotspot.code.blob;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.util.RawCType;

import javax.annotation.Nullable;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

/**This class is used internally by nmethods, to cache exception/pc/handler information.*/
public class ExceptionCache extends JVMObject {
    public static final Type TYPE= JVM.type("ExceptionCache");
    public static final int SIZE=TYPE.size;
    public static final int cache_size=16;
    public static final long EXCEPTION_TYPE_OFFSET=0;
    public static final long PC_OFFSET=JVM.computeOffset(JVM.oopSize,EXCEPTION_TYPE_OFFSET+JVM.oopSize);
    public static final long HANDLER_OFFSET=JVM.computeOffset(JVM.oopSize,PC_OFFSET+ (long) JVM.oopSize *cache_size);
    public static final long COUNT_OFFSET=JVM.computeOffset(JVM.intSize,HANDLER_OFFSET+ (long) JVM.oopSize *cache_size);
    public static final long NEXT_OFFSET=JVM.computeOffset(JVM.oopSize,COUNT_OFFSET+JVM.intSize);
    public static final long PURGE_LIST_NEXT=JVM.computeOffset(JVM.oopSize,NEXT_OFFSET+JVM.oopSize);
    static {
        JVM.assertOffset(SIZE,JVM.computeOffset(JVM.oopSize,PURGE_LIST_NEXT+JVM.oopSize));
    }
    public ExceptionCache(long addr) {
        super(addr);
    }

    public ExceptionCache create(Throwable exception,@RawCType("address")long pc, @RawCType("address")long handler){
        if (pc==0){
            throw new IllegalArgumentException("Must be non null");
        }
        if (exception==null){
            throw new IllegalArgumentException("Must be non null");
        }
        if (handler==0L){
            throw new IllegalArgumentException("Must be non null");
        }
        ExceptionCache cache=new ExceptionCache(unsafe.allocateMemory(SIZE));
        unsafe.putInt(cache.address+COUNT_OFFSET,0);
        unsafe.putAddress(cache.address+EXCEPTION_TYPE_OFFSET,Klass.asKlass(exception.getClass()).address);
        cache.set_next(null);
        cache.set_purge_list_next(null);
        add_address_and_handler(pc,handler);
        return cache;
    }

    public int count(){
        return unsafe.getIntVolatile(null,this.address+COUNT_OFFSET);
    }
    public @RawCType("address") long pc_at(int index) {
        if (!(index >= 0 && index < count())){
            throw new IndexOutOfBoundsException(index);
        }
        return unsafe.getAddress(this.address+PC_OFFSET+ (long) index *JVM.oopSize);
    }
    public void set_pc_at(int index,@RawCType("address") long a){
        if (!(index >= 0 && index < cache_size)){
            throw new IndexOutOfBoundsException(index);
        }
        unsafe.putAddress(this.address+PC_OFFSET+ (long) index *JVM.oopSize,a);
    }

    public @RawCType("address") long handler_at(int index) {
        if (!(index >= 0 && index < count())){
            throw new IndexOutOfBoundsException(index);
        }
        return unsafe.getAddress(this.address+HANDLER_OFFSET+(long) JVM.oopSize*index);
    }
    public void set_handler_at(int index,@RawCType("address") long a){
        if (!(index >= 0 && index < cache_size)){
            throw new IndexOutOfBoundsException(index);
        }
        unsafe.putAddress(this.address+HANDLER_OFFSET+(long)index *JVM.oopSize,a);
    }

    /** increment_count is only called under lock, but there may be concurrent readers.*/
    public void increment_count(){
        unsafe.putOrderedInt(null,this.address+COUNT_OFFSET,count()+1);
    }

    public Klass exception_type(){
        return Klass.getOrCreate(unsafe.getAddress(this.address+EXCEPTION_TYPE_OFFSET));
    }
    @Nullable
    public ExceptionCache next(){
        long addr=unsafe.getAddress(this.address+NEXT_OFFSET);
        if (addr==0L){
            return null;
        }
        return new ExceptionCache(addr);
    }

    public void set_next(@Nullable ExceptionCache ec) {
        unsafe.putAddress(this.address+NEXT_OFFSET,ec==null?0L:ec.address);
    }

    @Nullable
    public ExceptionCache purge_list_next(){
        long addr=unsafe.getAddress(this.address+PURGE_LIST_NEXT);
        if (addr==0L){
            return null;
        }
        return new ExceptionCache(addr);
    }

    public void set_purge_list_next(@Nullable ExceptionCache ec){
        unsafe.putAddress(this.address+PURGE_LIST_NEXT,ec==null?0L:ec.address);
    }

    public @RawCType("address") long match(Throwable exception, @RawCType("address") long pc) {
        if (pc==0L){
            throw new IllegalArgumentException("Must be non null");
        }
        if (exception==null){
            throw new IllegalArgumentException("Must be non null");
        }
        if (Klass.asKlass(exception.getClass()).equals(exception_type())){
            return (test_address(pc));
        }
        return 0L;
    }


    public boolean match_exception_with_space(Throwable exception) {
        if (exception==null){
            throw new IllegalArgumentException("Must be non null");
        }
        return Klass.asKlass(exception.getClass()).equals(exception_type()) && count() < cache_size;
    }

    public @RawCType("address") long test_address(@RawCType("address") long addr) {
        int limit = count();
        for (int i = 0; i < limit; i++) {
            if (pc_at(i) == addr) {
                return handler_at(i);
            }
        }
        return 0L;
    }

    public boolean add_address_and_handler(@RawCType("address") long addr, @RawCType("address") long handler) {
        if (test_address(addr) == handler){
            return true;
        }
        int index = count();
        if (index < cache_size) {
            set_pc_at(index, addr);
            set_handler_at(index, handler);
            increment_count();
            return true;
        }
        return false;
    }
}
