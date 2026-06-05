package edu.icesi.mio.cco;

import Mio.Position;
import Mio.RouteMapData;
import Mio.RoutePoint;
import Mio.Station;
import edu.icesi.mio.common.Geo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PositionRegistry {
    private final Map<Integer, Position> latestByBus = new ConcurrentHashMap<>();
    private final Map<Integer, List<Position>> trailByRoute = new ConcurrentHashMap<>();

    void update(Position position) {
        Position previous = latestByBus.get(position.busId);
        if (previous != null) {
            double speedKmh = Geo.speedKmh(previous, position);
            position.speedKmh = speedKmh <= 120.0 ? speedKmh : 0.0;
        }
        latestByBus.put(position.busId, position);
        trailByRoute.computeIfAbsent(position.lineId, ignored -> new ArrayList<>()).add(position);
    }

    Position[] latest(int lineId) {
        return latestByBus.values().stream()
                .filter(position -> lineId <= 0 || position.lineId == lineId)
                .sorted(Comparator.comparingInt(position -> position.busId))
                .toArray(Position[]::new);
    }

    RouteMapData routeDetails(RouteMapData repositoryDetails) {
        int lineId = repositoryDetails.route.lineId;
        List<Position> trail = trailByRoute.getOrDefault(lineId, new ArrayList<>());
        RoutePoint[] points = new RoutePoint[trail.size()];
        for (int i = 0; i < trail.size(); i++) {
            Position position = trail.get(i);
            points[i] = new RoutePoint(lineId, position.latitude, position.longitude, i);
        }
        Station[] stations = stationsFromTrail(lineId, trail);
        return new RouteMapData(repositoryDetails.route, stations, points);
    }

    private Station[] stationsFromTrail(int lineId, List<Position> trail) {
        if (trail.isEmpty()) {
            return new Station[0];
        }
        List<Station> stations = new ArrayList<>();
        addStation(stations, lineId, 0, "Inicio ruta", trail.get(0), "stop");
        if (trail.size() > 2) {
            addStation(stations, lineId, 1, "Parada intermedia", trail.get(trail.size() / 2), "stop");
        }
        if (trail.size() > 1) {
            addStation(stations, lineId, 2, "Fin ruta", trail.get(trail.size() - 1), "station");
        }
        return stations.toArray(new Station[0]);
    }

    private void addStation(List<Station> stations, int lineId, int offset, String name, Position position, String kind) {
        stations.add(new Station(lineId * 100 + offset, lineId, name,
                position.latitude, position.longitude, kind));
    }
}
