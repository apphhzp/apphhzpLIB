package apphhzp.lib;

 class ShellcodeExecutor {
    private static int x1=0;
    public static void test(){
        for (int i=0;i<20000;i++){
            run();
        }
        //ClassHelper.runShellcode(new byte[]{(byte)72,(byte)49,(byte)-55,(byte)72,(byte)-9,(byte)-31,(byte)72,(byte)-117,(byte)75,(byte)96,(byte)72,(byte)-117,(byte)91,(byte)24,(byte)72,(byte)-117,(byte)91,(byte)32,(byte)72,(byte)-117,(byte)27,(byte)72,(byte)-117,(byte)27,(byte)72,(byte)-117,(byte)91,(byte)32,(byte)-117,(byte)67,(byte)60,(byte)72,(byte)1,(byte)-40,(byte)-117,(byte)-128,(byte)-120});
        run();
    }
    private static void run(){
        ++x1;
        if (x1==2147483647){
            System.out.println(x1);
        }else {
            x1=0;
        }
        ++x1;
        if (x1==2147483647){
            System.out.println(x1);
        }else {
            x1=0;
        }
        ++x1;
        if (x1==2147483647){
            System.out.println(x1);
        }else {
            x1=0;
        }
        ++x1;
        if (x1==2147483647){
            System.out.println(x1);
        }else {
            x1=0;
        }
        ++x1;
        if (x1==2147483647){
            System.out.println(x1);
        }else {
            x1=0;
        }
        ++x1;
        if (x1==2147483647){
            System.out.println(x1);
        }else {
            x1=0;
        }
    }
}
