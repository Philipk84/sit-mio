package edu.icesi.mio.cco;

import Mio.Position;
import edu.icesi.mio.common.Geo;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PositionRegistry {
    private final Map<Integer, Position> latestByBus = new ConcurrentHashMap<>();

    void update(Position position) {
        Position previous = latestByBus.get(position.busId);
        if (previous != null) {
            double speedKmh = Geo.speedKmh(previous, position);
            position.speedKmh = speedKmh <= 120.0 ? speedKmh : 0.0;
        }
        latestByBus.put(position.busId, position);
    }

    Position[] latest(int lineId) {
        return latestByBus.values().stream()
                .filter(position -> lineId <= 0 || position.lineId == lineId)
                .sorted(Comparator.comparingInt(position -> position.busId))
                .toArray(Position[]::new);
    }
}
