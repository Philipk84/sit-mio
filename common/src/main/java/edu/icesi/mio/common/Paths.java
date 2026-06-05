package edu.icesi.mio.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Paths {
    private Paths() {
    }

    public static Path projectFile(String value) {
        Path direct = Path.of(value);
        if (direct.isAbsolute() || Files.exists(direct)) {
            return direct;
        }
        for (Path base : searchBases()) {
            Path candidate = base.resolve(value);
            if (Files.exists(candidate)) {
                return candidate;
            }
            Path nested = base.resolve(fileStem(value)).resolve(value);
            if (Files.exists(nested)) {
                return nested;
            }
        }
        return direct;
    }

    private static List<Path> searchBases() {
        List<Path> bases = new ArrayList<>();
        Path current = Path.of(System.getProperty("user.dir"));
        for (Path path = current; path != null; path = path.getParent()) {
            bases.add(path);
        }
        return bases;
    }

    private static String fileStem(String value) {
        String fileName = Path.of(value).getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
