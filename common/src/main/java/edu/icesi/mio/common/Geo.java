package edu.icesi.mio.common;

import Mio.Position;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class Geo {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final DateTimeFormatter EVENT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Geo() {
    }

    public static double speedKmh(Position from, Position to) {
        try {
            LocalDateTime fromTime = LocalDateTime.parse(from.timestamp, EVENT_TIME);
            LocalDateTime toTime = LocalDateTime.parse(to.timestamp, EVENT_TIME);
            long seconds = Duration.between(fromTime, toTime).getSeconds();
            if (seconds <= 0) {
                return 0.0;
            }
            return distanceKm(from.latitude, from.longitude, to.latitude, to.longitude) / (seconds / 3600.0);
        } catch (DateTimeParseException ex) {
            return 0.0;
        }
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
