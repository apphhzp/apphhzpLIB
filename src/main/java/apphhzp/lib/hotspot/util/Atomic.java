package apphhzp.lib.hotspot.util;

import static apphhzp.lib.ClassHelperSpecial.internalUnsafe;
import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class Atomic {
    public static <T> T atomic_compare_exchange_oop(T[] arr,int index, T exchange_value,
                                                    T compare_value){
        return internalUnsafe.compareAndExchangeReference(arr,
                unsafe.arrayBaseOffset(arr.getClass())+ (long) unsafe.arrayIndexScale(arr.getClass()) *index,
                compare_value,exchange_value);
    }
}
