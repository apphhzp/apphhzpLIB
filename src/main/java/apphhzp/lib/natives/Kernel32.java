package apphhzp.lib.natives;

import apphhzp.lib.ClassHelperSpecial;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;

@SuppressWarnings("deprecation")
public interface Kernel32 extends Library {
    Kernel32 INSTANCE= ClassHelperSpecial.isWindows ? Native.loadLibrary("kernel32", Kernel32.class, W32APIOptions.UNICODE_OPTIONS) : null;

    int PAGE_NOACCESS = 1;

    int PAGE_READONLY = 2;

    int PAGE_READWRITE = 4;

    int PAGE_WRITECOPY = 8;

    int PAGE_EXECUTE = 16;

    int PAGE_EXECUTE_READ = 32;

    int PAGE_EXECUTE_READWRITE = 64;

    int PAGE_EXECUTE_WRITECOPY = 128;
    boolean VirtualProtect(long paramLong, int paramInt1, int paramInt2, Pointer paramPointer);
}
