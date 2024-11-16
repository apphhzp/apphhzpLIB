package apphhzp.lib;


public class PlatformInfo {
    public static String getOS(){
        String os = System.getProperty("os.name");
        if (os.equals("Linux")) {
            return "linux";
        } else if (os.equals("FreeBSD")) {
            return "bsd";
        } else if (os.equals("NetBSD")) {
            return "bsd";
        } else if (os.equals("OpenBSD")) {
            return "bsd";
        } else if (os.contains("Darwin") || os.contains("OS X")) {
            return "darwin";
        } else if (os.startsWith("Windows")) {
            return "win32";
        }
        throw new UnsupportedOperationException("Operating system " + os + " not yet supported");
    }

    public static boolean knownCPU(String cpu) {
        final String[] KNOWN =
                new String[] {"i386", "x86", "x86_64", "amd64", "ppc64", "ppc64le", "aarch64"};
        for(String s : KNOWN) {
            if(s.equals(cpu))
                return true;
        }
        return false;
    }

    public static String getCPU(){
        String cpu = System.getProperty("os.arch");
        if (!knownCPU(cpu)) {
            throw new UnsupportedOperationException("CPU type " + cpu + " not yet supported");
        }
        return switch (cpu) {
            case "i386" -> "x86";
            case "x86_64" -> "amd64";
            case "ppc64le" -> "ppc64";
            default -> cpu;
        };
    }
}
