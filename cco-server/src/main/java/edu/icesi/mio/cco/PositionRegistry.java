package edu.icesi.mio.cco;

import Mio.Position;
import Mio.RouteMapData;
import Mio.RoutePoint;
import Mio.Station;
import edu.icesi.mio.common.Geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PositionRegistry {
    private final Map<Integer, Position> latestByBus = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, List<Position>>> trailByRouteAndBus = new ConcurrentHashMap<>();
    private final Map<String, Station> stopsByRouteAndId = new ConcurrentHashMap<>();

    void update(Position position) {
        Position previous = latestByBus.get(position.busId);
        if (previous != null) {
            double speedKmh = Geo.speedKmh(previous, position);
            position.speedKmh = speedKmh <= 120.0 ? speedKmh : 0.0;
        }
        latestByBus.put(position.busId, position);
        trailByRouteAndBus
                .computeIfAbsent(position.lineId, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(position.busId, ignored -> Collections.synchronizedList(new ArrayList<>()))
                .add(position);
        registerFixedStop(previous, position);
    }

    Position[] latest(int lineId) {
        return latestByBus.values().stream()
                .filter(position -> lineId <= 0 || position.lineId == lineId)
                .sorted(Comparator.comparingInt(position -> position.busId))
                .toArray(Position[]::new);
    }

    RouteMapData routeDetails(RouteMapData repositoryDetails) {
        int lineId = repositoryDetails.route.lineId;
        Map<Integer, List<Position>> trailsByBus = trailByRouteAndBus.getOrDefault(lineId, new ConcurrentHashMap<>());
        List<Position> trail = trailsByBus.values().stream()
                .max(Comparator.comparingInt(List::size))
                .map(this::snapshot)
                .orElseGet(ArrayList::new);
        RoutePoint[] points = new RoutePoint[trail.size()];
        for (int i = 0; i < trail.size(); i++) {
            Position position = trail.get(i);
            points[i] = new RoutePoint(lineId, position.latitude, position.longitude, i);
        }
        Station[] stations = stopsByRouteAndId.values().stream()
                .filter(station -> station.lineId == lineId)
                .sorted(Comparator.comparingInt(station -> station.id))
                .toArray(Station[]::new);
        return new RouteMapData(repositoryDetails.route, stations, points);
    }

    private void registerFixedStop(Position previous, Position current) {
        if (current.stopId <= 0) {
            return;
        }
        String key = stopKey(current.lineId, current.stopId);
        if (stopsByRouteAndId.containsKey(key)) {
            return;
        }
        LatLng stopLocation = estimateStopLocation(previous, current);
        stopsByRouteAndId.put(key, new Station(
                current.stopId,
                current.lineId,
                "Parada " + current.stopId,
                stopLocation.latitude,
                stopLocation.longitude,
                "stop"
        ));
    }

    private List<Position> snapshot(List<Position> positions) {
        synchronized (positions) {
            return new ArrayList<>(positions);
        }
    }

    private String stopKey(int lineId, int stopId) {
        return lineId + ":" + stopId;
    }

    private LatLng estimateStopLocation(Position previous, Position current) {
        if (previous == null || current.odometer <= 0) {
            return new LatLng(current.latitude, current.longitude);
        }
        double segmentMeters = distanceMeters(previous.latitude, previous.longitude, current.latitude, current.longitude);
        if (segmentMeters <= 0.1) {
            return new LatLng(current.latitude, current.longitude);
        }
        double ratio = Math.min(1.0, current.odometer / segmentMeters);
        double latitude = current.latitude - (current.latitude - previous.latitude) * ratio;
        double longitude = current.longitude - (current.longitude - previous.longitude) * ratio;
        return new LatLng(latitude, longitude);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static final class LatLng {
        private final double latitude;
        private final double longitude;

        private LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
