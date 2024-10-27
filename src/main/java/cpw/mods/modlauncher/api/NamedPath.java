
package cpw.mods.modlauncher.api;

import java.nio.file.Path;

public record NamedPath(String name, Path... paths) {
}
