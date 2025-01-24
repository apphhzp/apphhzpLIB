package apphhzp.lib;

import java.util.Set;

public enum ClassOption {
    NESTMATE(1),
    STRONG(4);
    private final int flag;

    ClassOption(int flag) {
        this.flag = flag;
    }

    static int optionsToFlag(Set<ClassOption> options) {
        int flags = 0;
        for (ClassOption cp : options) {
            flags |= cp.flag;
        }
        return flags;
    }
}
