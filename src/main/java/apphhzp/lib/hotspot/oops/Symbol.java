package apphhzp.lib.hotspot.oops;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.ClassOption;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.Utf8Constant;
import apphhzp.lib.hotspot.oops.klass.Klass;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelper.unsafe;

public class Symbol extends JVMObject {
    public static final Type TYPE=JVM.type("Symbol");
    public static final int SIZE=TYPE.size;
    public static final long VM_SYMBOLS_ADDRESS =TYPE.global("_vm_symbols[0]");
    public static final long HASH_REF_OFFSET=TYPE.offset("_hash_and_refcount");
    public static final long LENGTH_OFFSET=TYPE.offset("_length");
    public static final long BODY_OFFSET=TYPE.offset("_body");
    public static final int FIRST_SID=JVM.intConstant("vmSymbols::FIRST_SID");
    public static final int SID_LIMIT=JVM.intConstant("vmSymbols::SID_LIMIT");
    public static final int MAX_LENGTH=JVM.intConstant("Symbol::max_symbol_length");
    private static final Int2ObjectMap<Symbol> cache=new Int2ObjectOpenHashMap<>();
    private static final Long2BooleanMap cached=new Long2BooleanOpenHashMap();
    private static final Object2ObjectMap<String,Symbol> vmSymbols=new Object2ObjectOpenHashMap<>();
    private static final Symbol[] vmSymbolsArray=new Symbol[SID_LIMIT+1];
    private static final  ClassReader creater;
    static {
        try {
            InputStream is = Symbol.class.getResourceAsStream("/apphhzp/lib/hotspot/oops/SymbolCreater.class");
            byte[] createrCode;
            if (is==null){
                createrCode=Base64.getDecoder().decode("yv66vgAAAD0AEAoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWBwAIAQAmYXBwaGh6cC9saWIvaG90c3BvdC9vb3BzL1N5bWJvbENyZWF0ZXIBAARDb2RlAQAPTGluZU51bWJlclRhYmxlAQASTG9jYWxWYXJpYWJsZVRhYmxlAQAEdGhpcwEAKExhcHBoaHpwL2xpYi9ob3RzcG90L29vcHMvU3ltYm9sQ3JlYXRlcjsBAApTb3VyY2VGaWxlAQASU3ltYm9sQ3JlYXRlci5qYXZhACEABwACAAAAAAABAAEABQAGAAEACQAAAC8AAQABAAAABSq3AAGxAAAAAgAKAAAABgABAAAAAwALAAAADAABAAAABQAMAA0AAAABAA4AAAACAA8=");
            }else {
                createrCode = new byte[is.available()];
                is.read(createrCode);
                is.close();
            }
            creater=new ClassReader(createrCode);
            for (int i=FIRST_SID;i<SID_LIMIT;i++) {
                Symbol symbol=getVMSymbol(i);
                vmSymbols.put(symbol.toString(),symbol);
            }
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }
    public static Symbol newSymbol(String s){
        if (s.length()>MAX_LENGTH) {
            throw new IllegalArgumentException("UTF8 string too large");
        }
        int hash = s.hashCode();
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }
        ClassWriter cw=new ClassWriter(0);
        creater.accept(cw, 0);
        cw.newUTF8(s);
        Symbol re =((Utf8Constant) Klass.asKlass(ClassHelper.defineHiddenClass(cw.toByteArray(),"apphhzp.lib.hotspot.oop.SymbolCreater",false,Symbol.class,Symbol.class.getClassLoader(),null, ClassOption.NESTMATE).lookupClass()).asInstanceKlass().getConstantPool().getConstant(12)).str;
        cache.put(re.hashCode(),re);
        return re;
    }

    public static Symbol[] newSymbols(String[] arr){
        Symbol[] re=new Symbol[arr.length];
        ClassWriter cw=new ClassWriter(0);
        creater.accept(cw, 0);
        for (int i=0;i<arr.length;i++){
            String s=arr[i];
            if (s.length()>MAX_LENGTH) {
                throw new IllegalArgumentException("UTF8 string too large");
            }
            if (cache.containsKey(s.hashCode())){
                re[i]=cache.get(s.hashCode());
            }else {
                cw.newUTF8(arr[i]);
            }
        }
        ConstantPool pool=Klass.asKlass(ClassHelper.defineHiddenClass(cw.toByteArray(),"apphhzp.lib.hotspot.oop.SymbolCreater",false,Symbol.class,Symbol.class.getClassLoader(),null, ClassOption.NESTMATE).lookupClass()).asInstanceKlass().getConstantPool();
        for (int i=0;i<re.length;i++){
            if (re[i]==null){
                re[i]=pool.findSymbol(arr[i]);
            }
        }
        return re;
    }

//    public static Symbol create(byte[] bytes){
//        return create(bytes,1);
//    }
//
//    public static Symbol create(byte[] bytes, int ref_count){
//        if (bytes.length>MAX_LENGTH){
//            throw new IllegalArgumentException("String too large:"+bytes.length);
//        }
//        long addr=unsafe.allocateMemory(SIZE+bytes.length);
//        unsafe.putShort(addr + LENGTH_OFFSET, (short) bytes.length);
//        for (int i = 0; i < bytes.length; i++) {
//            unsafe.putByte(addr + BODY_OFFSET + i, bytes[i]);
//        }
//        unsafe.putInt(addr,((new Random().nextInt())<<16)|ref_count);
//        return new Symbol(addr);
//    }

    public static Symbol onlyLookup(String s){
        if (cache.containsKey(s.hashCode())){
            return cache.get(s.hashCode());
        }
        return null;
    }

    public static Symbol lookupOrCreate(String s){
        Symbol re=onlyLookup(s);
        if (re==null){
            re= newSymbol(s);
        }
        return re;
    }

    public static Symbol getVMSymbol(int index){
        if (index<FIRST_SID||index>=SID_LIMIT){
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (vmSymbolsArray[index]!=null){
            return vmSymbolsArray[index];
        }
        long addr=unsafe.getAddress(VM_SYMBOLS_ADDRESS + (long)index*JVM.oopSize);
        if (addr==0L){
            return null;
        }
        return vmSymbolsArray[index]=Symbol.of(addr);
    }

    public static Symbol getVMSymbol(String s){
        return vmSymbols.get(s);
    }

    public static Symbol of(long addr){
        if (addr==0L){
            throw new NullPointerException();
        }
        Symbol re=new Symbol(addr);
        cache.put(re.toString().hashCode(),re);
        return re;
    }

    private Symbol(long addr){
        super(addr);
    }

    public int getHash(){
        return unsafe.getInt(this.address+HASH_REF_OFFSET)>>>16;
    }

    public int getRefCount(){
        return unsafe.getInt(this.address+HASH_REF_OFFSET)&0xffff;
    }

    public long getHashAndRefCount(){
        return unsafe.getInt(this.address+HASH_REF_OFFSET)&0xffffffffL;
    }

    public void setRefCount(int count){
        unsafe.putInt(this.address+HASH_REF_OFFSET,(this.getHash()<<16)|(count&0xffff));
    }

    public int getLength(){
        return unsafe.getShort(this.address+LENGTH_OFFSET)&0xffff;
    }

    public void setLength(int len){
        unsafe.putShort(this.address+LENGTH_OFFSET,(short)(len&0xffff));
    }

    public void incrementRefCount(){
        this.setRefCount(this.getRefCount()+1);
    }

    public void decrementRefCount(){
        this.setRefCount(this.getRefCount()-1);
    }

    public char getCChar(int index){
        if (index<0||index>=this.getLength()){
            throw new NoSuchElementException();
        }
        return (char)unsafe.getByte(this.address+BODY_OFFSET+index);
    }

    public long identityHash(){
        long addr_bits =(this.address>>(JVM.logMinObjAlignmentInBytes+3))&0xffffffffL;
        int  length = getLength();
        int  byte0 =unsafe.getByte(this.address+BODY_OFFSET);
        int  byte1 =unsafe.getByte(this.address+BODY_OFFSET+1);
        return ((this.getHash()&0xffff) |
                ((addr_bits ^ ((long) this.getLength() << 8) ^ ((byte0 << 8) | byte1)) << 16))&0xffffffffL;
    }

    @Override
    public String toString() {
        long body = this.address + BODY_OFFSET;
        int length=this.getLength();
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = unsafe.getByte(body + i);
        }
        return new String(b, StandardCharsets.UTF_8);
    }
}
