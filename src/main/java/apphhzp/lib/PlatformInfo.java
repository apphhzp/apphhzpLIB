package apphhzp.lib;


import static apphhzp.lib.ClassHelperSpecial.internalUnsafe;

public class PlatformInfo {

    private static final String os,cpu;
    static {
        String _os = System.getProperty("os.name");
        if (_os.equals("Linux")) {
             os="linux";
        } else if (_os.equals("FreeBSD")) {
             os="bsd";
        } else if (_os.equals("NetBSD")) {
             os="bsd";
        } else if (_os.equals("OpenBSD")) {
             os="bsd";
        } else if (_os.contains("Darwin") || _os.contains("OS X")) {
             os="darwin";
        } else if (_os.startsWith("Windows")) {
             os="win32";
        }else {
            throw new UnsupportedOperationException("Operating system " + _os + " not yet supported");
        }
        String _cpu = System.getProperty("os.arch");
        if (!knownCPU(_cpu)) {
            throw new UnsupportedOperationException("CPU type " + _cpu + " not yet supported");
        }
        cpu=switch (_cpu) {
            case "i386" -> "x86";
            case "x86_64" -> "amd64";
            case "ppc64le" -> "ppc64";
            default -> _cpu;
        };
    }
    public static String getOS(){
        return os;
    }

    public static boolean knownCPU(String cpu) {
        final String[] KNOWN =
                new String[] {"i386", "x86", "x86_64", "amd64", "ppc64", "ppc64le", "aarch64","arm"};
        for(String s : KNOWN) {
            if(s.equals(cpu))
                return true;
        }
        return false;
    }

    public static String getCPU(){
        return cpu;
    }

    public static boolean isX86(){
        return cpu.equals("x86")||cpu.equals("amd64");
    }

    public static boolean isX86_64(){
        return cpu.equals("amd64");
    }

    public static boolean isBigEndian(){
        return internalUnsafe.isBigEndian();
    }
    public static boolean isAArch64(){
        return cpu.equals("aarch64");
    }

    public static boolean isPPC64(){
        return cpu.equals("ppc64");
    }

    public static boolean isLittleEndian(){
        return !internalUnsafe.isBigEndian();
    }
}
