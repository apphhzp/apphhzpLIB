package apphhzp.lib.hotspot.runtime;

import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.oops.Symbol;
import apphhzp.lib.hotspot.oops.method.Method;
import apphhzp.lib.hotspot.util.RawCType;
import apphhzp.lib.hotspot.utilities.BasicType;

/** Fingerprinter computes a unique ID for a given method.
 *  The ID is a bitvector characterizing the methods signature (incl. the receiver).*/
public class Fingerprinter extends SignatureIterator{
    private @RawCType("uint64_t") long _accumulator;
    private int _param_size;
    private int _shift_count;
    private final Method _method;

    public Fingerprinter(Method method) {
        super(method.signature());
        _method = method;
        compute_fingerprint_and_return_type();
    }
    public Fingerprinter(Symbol signature, boolean is_static) {
        super(signature);
        _method=null;
        compute_fingerprint_and_return_type(is_static);
    }
    private void initialize_accumulator() {
        _accumulator = 0;
        _shift_count = fp_result_feature_size + fp_static_feature_size;
        _param_size = 0;
    }
    private void compute_fingerprint_and_return_type(){
        compute_fingerprint_and_return_type(false);
    }
    private void compute_fingerprint_and_return_type(boolean static_flag) {
        // See if we fingerprinted this method already
        if (_method != null){
            if (static_flag){
                throw new RuntimeException("must not be passed by caller");
            }
            static_flag = _method.is_static();
            _fingerprint = _method.getConstMethod().fingerprint();

            if (_fingerprint != zero_fingerprint()) {
                _return_type = _method.result_type();
                if (!BasicType.is_java_type(_return_type)){
                    throw new RuntimeException("return type must be a java type");
                }
                return;
            }

            if (_method.size_of_parameters() > fp_max_size_of_parameters) {
                _fingerprint = overflow_fingerprint();
                _method.getConstMethod().set_fingerprint(_fingerprint);
                // as long as we are here compute the return type:
                _return_type = new ResultTypeFinder(_method.signature()).type();
                if (!BasicType.is_java_type(_return_type)){
                    throw new RuntimeException("return type must be a java type");
                }
                return;
            }
        }

        // Note:  This will always take the slow path, since _fp==zero_fp.
        initialize_accumulator();
        do_parameters_on(this);
        if (!fp_is_valid_type(_return_type, true)){
            throw new RuntimeException("bad result type");
        }
        // Fill in the return type and static bits:
        //noinspection IntegerMultiplicationImplicitCastToLong
        _accumulator |= _return_type << fp_static_feature_size;
        if (static_flag) {
            _accumulator |= fp_is_static_bit;
        } else {
            _param_size += 1;  // this is the convention for Method::compute_size_of_parameters
        }

        // Detect overflow.  (We counted _param_size correctly.)
        if (_method == null && _param_size > fp_max_size_of_parameters) {
            // We did a one-pass computation of argument size, return type,
            // and fingerprint.
            _fingerprint = overflow_fingerprint();
            return;
        }
        if (!(_shift_count < JVM.BitsPerLong)){
            throw new RuntimeException("shift count overflow "+_shift_count+" ("+_param_size+" vs. "+fp_max_size_of_parameters+"): "+_signature.toString());
        }
        if (!((_accumulator >> _shift_count) == fp_parameters_done)){
            throw new RuntimeException("must be zero");
        }
        // This is the result, along with _return_type:
        _fingerprint = _accumulator;

        // Cache the result on the method itself:
        if (_method != null) {
            _method.getConstMethod().set_fingerprint(_fingerprint);
        }
    }

    public void do_type(@RawCType("BasicType")int type) {
        if (!fp_is_valid_type(type)){
            throw new IllegalArgumentException("bad parameter type");
        }
        _accumulator |= ((long) type << _shift_count);
        _shift_count += fp_parameter_feature_size;
        _param_size += (BasicType.is_double_word_type(type) ? 2 : 1);
    }
}
