package apphhzp.lib.hotspot.oops;

import apphhzp.lib.ClassHelperSpecial;
import apphhzp.lib.ClassOption;
import apphhzp.lib.PlatformInfo;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.helfy.Type;
import apphhzp.lib.hotspot.JVMObject;
import apphhzp.lib.hotspot.oops.constant.ConstantPool;
import apphhzp.lib.hotspot.oops.constant.Utf8Constant;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.util.CString;
import apphhzp.lib.hotspot.util.RawCType;
import com.sun.jna.Function;
import com.sun.jna.Pointer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

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
    private static final Function new_symbol;
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
            if (JVM.Functions.throw_klass_external_name_exception_function!=null&& PlatformInfo.isX86_64()){
                long addr= Pointer.nativeValue(JVM.Functions.throw_klass_external_name_exception_function);
                long offset=-1;
                for (int i=0;i<10000;i++){
                    if ((unsafe.getByte(addr+i)&0xff)==0xcc){
                        break;
                    }
                    if ((unsafe.getByte(addr+i)&0xff)==0x48&&
                            (unsafe.getByte(addr+i+1)&0xff)==0x8b&&
                            (unsafe.getByte(addr+i+2)&0xff)==0xc8&&
                            (unsafe.getByte(addr+i+3)&0xff)==0xe8){
                        offset=addr+i+3;
                        break;
                    }
                }
                if (offset!=-1){
                    addr=offset+5+unsafe.getInt(offset+1);
                    Function tmp=Function.getFunction(new Pointer(addr));
                    boolean ok=false;
                    try {
                        String s=new String(Base64.getDecoder().decode("dmVyaWZ5IGl0"));
                        ok= Symbol.of(Pointer.nativeValue(tmp.invokePointer(new Object[]{s, s.length()}))).toString().equals(s);
                    }catch (Throwable ignored){}
                    if (ok){
                        new_symbol=tmp;
                    }else {
                        new_symbol=null;
                    }
                }else {
                    new_symbol=null;
                }
            }else {
                new_symbol=null;
            }
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }
    public static Symbol newSymbol(long buf,int len){
        if (buf==0L||len<0){
            throw new IllegalArgumentException();
        }
        byte[] str =new byte[len];
        for (int i=0;i<len;i++){
            str[i]= unsafe.getByte(buf+i);
        }
        return newSymbol(new String(str));
    }
    public static Symbol newSymbol(String s){
        if (s.length()>MAX_LENGTH) {
            throw new IllegalArgumentException("UTF8 string too large");
        }
        if (new_symbol!=null){
            //new_symbol函数自带lookup_common，不需要缓存
            return new Symbol(Pointer.nativeValue(new_symbol.invokePointer(new Object[]{s,s.length()})));
        }
        int hash = s.hashCode();
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }
        ClassWriter cw=new ClassWriter(0);
        creater.accept(cw, 0);
        cw.newUTF8(s);
        Symbol re =((Utf8Constant) Klass.asKlass(ClassHelperSpecial.defineHiddenClass(cw.toByteArray(),"apphhzp.lib.hotspot.oop.SymbolCreater",false,Symbol.class,Symbol.class.getClassLoader(),null, ClassOption.NESTMATE).lookupClass()).asInstanceKlass().getConstantPool().getConstant(12)).str;
        cache.put(re.hashCode(),re);
        return re;
    }

    public static Symbol[] newSymbols(String[] arr){
        if (new_symbol!=null){
            Symbol[] re=new Symbol[arr.length];
            for (int i = 0, arrLength = arr.length; i < arrLength; i++) {
                String s = arr[i];
                if (s.length() > MAX_LENGTH) {
                    throw new IllegalArgumentException("UTF8 string too large");
                } else {
                    //new_symbol函数自带lookup_common，不需要缓存
                    re[i]=new Symbol(Pointer.nativeValue(new_symbol.invokePointer(new Object[]{s,s.length()})));
                }
            }
            return re;
        }

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
        ConstantPool pool=Klass.asKlass(ClassHelperSpecial.defineHiddenClass(cw.toByteArray(),"apphhzp.lib.hotspot.oop.SymbolCreater",false,Symbol.class,Symbol.class.getClassLoader(),null, ClassOption.NESTMATE).lookupClass()).asInstanceKlass().getConstantPool();
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
            throw new NullPointerException("index: "+index);
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


    public static int compare_symbol(Symbol a,Symbol b) {
        if (a == b)  return 0;
        // follow the natural address order:
        return a.address > b.address ? +1 : -1;
    }
    private static int mid_hint = FIRST_SID+1;
    public static @RawCType("vmSymbolID")int find_sid(final Symbol symbol) {

        // Handle the majority of misses by a bounds check.
        // Then, use a binary search over the index.
        // Expected trip count is less than log2_SID_LIMIT, about eight.
        // This is slow but acceptable, given that calls are not
        // dynamically common.  (Method*::intrinsic_id has a cache.)
        int min = FIRST_SID, max = SID_LIMIT - 1;
        int sid = 0/*vmSymbolID::NO_SID*/, sid1;
        int cmp1;
        sid1 =min;// vm_symbol_index[min]
        cmp1 = compare_symbol(symbol, Symbol.getVMSymbol(sid1));
        if (cmp1 <= 0) {              // before the first
            if (cmp1 == 0)  sid = sid1;
        } else {
            sid1 = max;//vm_symbol_index[max]
            cmp1 = compare_symbol(symbol, Symbol.getVMSymbol(sid1));
            if (cmp1 >= 0) {            // after the last
                if (cmp1 == 0)  sid = sid1;
            } else {
                // After checking the extremes, do a binary search.
                ++min; --max;             // endpoints are done
                int mid = mid_hint;       // start at previous success
                while (max >= min) {
                    if (!(mid >= min && mid <= max)){
                        throw new RuntimeException();
                    }
                    sid1 = mid;
                    cmp1 = compare_symbol(symbol, Symbol.getVMSymbol(sid1));
                    if (cmp1 == 0) {
                        mid_hint = mid;
                        sid = sid1;
                        break;
                    }
                    if (cmp1 < 0)
                        max = mid - 1;        // symbol < symbol_at(sid)
                    else
                        min = mid + 1;

                    // Pick a new probe point:
                    mid = (max + min) / 2;
                }
            }
        }

//#ifdef ASSERT
//        if (sid == vmSymbolID::NO_SID) {
//            return sid;
//        }
//
//        // Perform the exhaustive self-check the first 1000 calls,
//        // and every 100 calls thereafter.
//        static int find_sid_check_count = -2000;
//        if ((uint)++find_sid_check_count > (uint)100) {
//            if (find_sid_check_count > 0)  find_sid_check_count = 0;
//
//            // Make sure this is the right answer, using linear search.
//            // (We have already proven that there are no duplicates in the list.)
//            vmSymbolID sid2 = vmSymbolID::NO_SID;
//            for (auto index : EnumRange<vmSymbolID>{}) {
//                Symbol* sym2 = symbol_at(index);
//                if (sym2 == symbol) {
//                    sid2 = index;
//                    break;
//                }
//            }
//            // Unless it's a duplicate, assert that the sids are the same.
//            if (Symbol::_vm_symbols[as_int(sid)] != Symbol::_vm_symbols[as_int(sid2)]) {
//                assert(sid == sid2, "binary same as linear search");
//            }
//        }
//#endif //ASSERT

        return sid;
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

    public char char_at(int index){
        if (index<0||index>=this.getLength()){
            throw new NoSuchElementException();
        }
        return (char)(unsafe.getByte(this.address+BODY_OFFSET+index)&0xff);
    }

    public long identityHash(){
        long addr_bits =(this.address>>(JVM.logMinObjAlignmentInBytes+3))&0xffffffffL;
        int  length = getLength();
        int  byte0 =unsafe.getByte(this.address+BODY_OFFSET);
        int  byte1 =unsafe.getByte(this.address+BODY_OFFSET+1);
        return ((this.getHash()&0xffff) |
                ((addr_bits ^ ((long) this.getLength() << 8) ^ ((byte0 << 8) | byte1)) << 16))&0xffffffffL;
    }

    public @RawCType("u1*") long bytes(){
        return this.address+BODY_OFFSET;
    }

    public boolean is_permanent(){
        return (this.getRefCount() == 0xffff);
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

    public boolean equals(long buf, int len) {
        if (buf==0L||this.getLength()!=len){
            return false;
        }
        for (int i=0;i<len;i++){
            if (unsafe.getByte(this.address+BODY_OFFSET+i)!=unsafe.getByte(buf+i)){
                return false;
            }
        }
        return true;
    }

    // Three-way compare for sorting; returns -1/0/1 if receiver is </==/> than arg
    // note that the ordering is not alfabetical
    //Note: this comparison is used for vtable sorting only; it doesn't matter
    // what order it defines, as long as it is a total, time-invariant order
    // Since Symbol*s are in C_HEAP, their relative order in memory never changes,
    // so use address comparison for speed
    public int fast_compare(Symbol other){
        return Long.compare(this.address, other.address);
    }

    public byte[] as_C_string(byte[] buf, int size){
        if (size > 0) {
            int len = Math.min(size - 1, this.getLength());
            for (int i = 0; i < len; i++) {
                buf[i] = (byte) char_at(i);
            }
            buf[len] = '\0';
        }
        return buf;
    }
    public byte[] as_C_string(){
        int len = getLength();
        return as_C_string(new byte[len+1], len + 1);
    }


    public String as_klass_external_name(byte[] buf, int size){
        if (size > 0) {
            byte[] str    = as_C_string(buf, size);
            int   length =  CString.strlen(str);
            // Turn all '/'s into '.'s (also for array klasses)
            for (int index = 0; index < length; index++) {
                if (str[index] == '/') {
                    str[index] = '.';
                }
            }
            return CString.toString(str);
        } else {
            return CString.toString(buf);
        }
    }

    public String as_klass_external_name(){
        byte[] str    = as_C_string();
        int   length = CString.strlen(str);
        // Turn all '/'s into '.'s (also for array klasses)
        for (int index = 0; index < length; index++) {
            if (str[index] == '/') {
                str[index] = '.';
            }
        }
        return CString.toString(str);
    }


    public void print_value_on(PrintStream st){
        st.print("'");
        st.print(this);
        st.print("'");
    }
}
