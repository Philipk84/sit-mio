package edu.icesi.mio.common;

import Mio.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CsvRouteParser {
    private CsvRouteParser() {
    }

    public static Optional<Route> parse(String line) {
        if (line == null || line.trim().isEmpty() || line.startsWith("\"LINEID\"")) {
            return Optional.empty();
        }
        List<String> parts = splitCsv(line);
        if (parts.size() < 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Route(
                    Integer.parseInt(clean(parts.get(0))),
                    clean(parts.get(2)),
                    clean(parts.get(3))
            ));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static List<String> splitCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String clean(String value) {
        return value.replace("\"", "").trim();
    }
}
