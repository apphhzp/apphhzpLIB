package apphhzp.lib.hotspot.runtime.signature;

import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.klass.Klass;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;

import java.security.ProtectionDomain;

public class ResolvingSignatureStream extends SignatureStream{
    private Klass _load_origin;
    private boolean         _handles_cached;
    private ClassLoader       _class_loader;       // cached when needed
    private ProtectionDomain _protection_domain;  // cached when needed

    private void initialize_load_origin(Klass load_origin) {
        _load_origin = load_origin;
        _handles_cached = (load_origin == null);
    }
    private void need_handles() {
        if (!_handles_cached) {
            cache_handles();
            _handles_cached = true;
        }
    }
    private void cache_handles(){
        if (_load_origin==null){
            throw new RuntimeException();
        }
        _class_loader = _load_origin.getClassLoaderData().getClassLoader();
        _protection_domain = _load_origin.asClass().getProtectionDomain();
    }
    public ResolvingSignatureStream(Symbol signature, Klass load_origin){
        this(signature,load_origin,true);
    }
    public ResolvingSignatureStream(Symbol signature, Klass load_origin, boolean is_method){
        super(signature,is_method);
        if (load_origin==null){
            throw new NullPointerException();
        }
        initialize_load_origin(load_origin);
    }
    public ResolvingSignatureStream(Symbol signature, ClassLoader class_loader, ProtectionDomain protection_domain){
        this(signature,class_loader,protection_domain,true);
    }
    public ResolvingSignatureStream(Symbol signature, ClassLoader class_loader, ProtectionDomain protection_domain, boolean is_method){
        super(signature, is_method);
        _class_loader=(class_loader);
        _protection_domain=(protection_domain);
        initialize_load_origin(null);
    }
    public ResolvingSignatureStream(Method method){
        super(method.signature(),true);
        initialize_load_origin(method.method_holder());
    }
    //public ResolvingSignatureStream(fieldDescriptor& field);


    public Klass load_origin()       { return _load_origin; }
    public ClassLoader class_loader()      { need_handles(); return _class_loader; }
    public ProtectionDomain protection_domain() { need_handles(); return _protection_domain; }

    public Klass as_klass_if_loaded(){
        Klass klass = as_klass(CachedOrNull);
        return klass;
    }
    public Klass as_klass(@RawCType("FailureMode")int failure_mode) {
        need_handles();
        return super.as_klass(_class_loader, _protection_domain,
                failure_mode);
    }
    public Class<?> as_java_mirror(@RawCType("FailureMode")int failure_mode) {
        if (is_reference()) {
            need_handles();
        }
        return super.as_java_mirror(_class_loader, _protection_domain,
                failure_mode);
    }
}
