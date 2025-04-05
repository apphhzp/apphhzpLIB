package apphhzp.lib.natives;

import apphhzp.lib.ClassHelper;
import apphhzp.lib.api.ObjectInstrumentation;
import apphhzp.lib.helfy.JVM;
import apphhzp.lib.hotspot.runtime.JavaThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.Locale;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public final class NativeUtil {
    static {
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            final String s=new File(".").getAbsolutePath();
            String s1 = s.substring(0, s.length() - 2);
            String path= s1 +"/apphhzpLIB.dll";
            try {
                InputStream is = NativeUtil.class.getResourceAsStream("/apphhzpLIB.dll");
                //noinspection DataFlowIssue
                byte[] dat = new byte[is.available()];
                //noinspection ResultOfMethodCallIgnored
                is.read(dat);
                is.close();
                File f=new File(path);
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(dat);
                fos.close();
                System.load(f.getAbsolutePath());
            } catch (Throwable t) {
                try {
                    System.load(path);
                }catch (UnsatisfiedLinkError error){
                    path= s1 +"/apphhzpLIB2.dll";
                    try {
                        InputStream is = NativeUtil.class.getResourceAsStream("/apphhzpLIB.dll");
                        //noinspection DataFlowIssue
                        byte[] dat = new byte[is.available()];
                        //noinspection ResultOfMethodCallIgnored
                        is.read(dat);
                        is.close();
                        File f=new File(path);
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(dat);
                        fos.close();
                        System.load(f.getAbsolutePath());
                    }catch (Throwable throwable){
                        try {
                            System.load(path);
                        }catch (Throwable throwable1){
                            throw new RuntimeException(throwable1);
                        }
                    }
                }catch (Throwable t2){
                    throw new RuntimeException(t2);
                }
            }
        }
    }
    public static final int MB_OK= 0,
            MB_OKCANCEL= 1,
            MB_ABORTRETRYIGNORE= 2,
            MB_YESNOCANCEL=3,
            MB_YESNO=4,
            MB_RETRYCANCEL=5,
            MB_CANCELTRYCONTINUE=6,
            MB_ICONHAND=16,
            MB_ICONQUESTION=32,
            MB_ICONEXCLAMATION= 48,
            MB_ICONASTERISK= 64,
            MB_USERICON= 128,
            MB_ICONWARNING=MB_ICONEXCLAMATION,
            MB_ICONERROR=MB_ICONHAND,
            MB_ICONINFORMATION=MB_ICONASTERISK,
            MB_ICONSTOP=MB_ICONHAND,
            MB_DEFBUTTON1=0,
            MB_DEFBUTTON2= 256,
            MB_DEFBUTTON3= 512;
    public static native int postMsg(long hwnd,int msg,long wParam,long lParam);
    public static native long getActiveWindow();
    public static native void createMsgBox(String text,String title,int flags);
    public static native Instrumentation createInstrumentationImpl();
    public static native Object[] getObjectsWithTag(long tag);
    public static native <T> T[] getInstancesOfClass(Class<T> klass);
    public static native ObjectInstrumentation createObjectInstrumentationImpl();
//    public static void createThread(CppThreadTask task){
//        if (ClassHelper.isHotspotJVM){
//            createThread(task, JavaThread.JNI_ATTACH_STATE_OFFSET);
//        }
//    }
    public static native void createThread(Runnable task,String name);
}
