package edu.icesi.mio.common;

import Mio.Datagram;
import Mio.Position;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class CsvDatagramParser {
    private static final DateTimeFormatter EVENT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CsvDatagramParser() {
    }

    public static Optional<Datagram> parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return Optional.empty();
        }
        String[] parts = line.split(",", -1);
        if (parts.length < 12 || parts[0].startsWith("\"")) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Datagram(
                    intValue(parts[0]),
                    parts[1],
                    intValue(parts[2]),
                    longValue(parts[3]),
                    intValue(parts[4]),
                    intValue(parts[5]),
                    intValue(parts[6]),
                    intValue(parts[7]),
                    intValue(parts[8]),
                    longValue(parts[9]),
                    parts[10],
                    intValue(parts[11])
            ));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Position toPosition(Datagram datagram) {
        return new Position(
                datagram.busId,
                datagram.lineId,
                datagram.stopId,
                datagram.odometer,
                datagram.latitude / 10_000_000.0,
                datagram.longitude / 10_000_000.0,
                0.0,
                datagram.datagramDate
        );
    }

    public static int month(Datagram datagram) {
        try {
            return LocalDateTime.parse(datagram.datagramDate, EVENT_TIME).getMonthValue();
        } catch (DateTimeParseException ex) {
            return -1;
        }
    }

    private static int intValue(String value) {
        return Integer.parseInt(value.trim());
    }

    private static long longValue(String value) {
        return Long.parseLong(value.trim());
    }
}
