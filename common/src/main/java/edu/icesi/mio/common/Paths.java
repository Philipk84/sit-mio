package edu.icesi.mio.common;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Paths {
    private Paths() {
    }

    public static Path projectFile(String value) {
        Path direct = Path.of(value);
        if (direct.isAbsolute() || Files.exists(direct)) {
            return direct;
        }
        Path parent = Path.of(System.getProperty("user.dir")).getParent();
        if (parent != null && Files.exists(parent.resolve(value))) {
            return parent.resolve(value);
        }
        return direct;
    }
}
