package apphhzp.lib.hotspot.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
/**
 * jbyte=signed char<br>
 * jchar=unsigned short<br>
 * jboolean=unsigned char<br>
 * intx=intptr_t
 * */
@Target({PARAMETER, FIELD,LOCAL_VARIABLE,METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface RawCType {
    String value();
}
